/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import android.net.ConnectivityManager
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.PoTokenProvider
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeClient
import com.auramusic.innertube.models.YouTubeClient.Companion.ANDROID
import com.auramusic.innertube.models.YouTubeClient.Companion.ANDROID_NO_SDK
import com.auramusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_65_10
import com.auramusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.auramusic.innertube.models.YouTubeClient.Companion.IPADOS
import com.auramusic.innertube.models.YouTubeClient.Companion.VISIONOS
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.auramusic.innertube.models.response.PlayerResponse
import com.auramusic.app.constants.AudioQuality
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal fun shouldUseSlowUrlFallback(
    clientName: String,
    mainClientName: String,
    signatureCipher: String?,
): Boolean = clientName != mainClientName || !signatureCipher.isNullOrBlank()

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    /**
     * Hard deadline for resolving a playable stream URL (PO token + player
     * requests + signature fallbacks across all clients). Bounded so failures
     * surface quickly into the app's retry/skip logic instead of spinning.
     */
    private const val STREAM_RESOLUTION_TIMEOUT_MS = 30_000L

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
        /** Client that produced the stream, for logging/debug. */
        val streamClient: String = "unknown",
        /** Headers that must accompany every request to [streamUrl]. */
        val streamHeaders: Map<String, String> = emptyMap(),
    )
    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     *
     * The whole resolution chain (PO token minting + one /player request per
     * fallback client + NewPipe signature work) is bounded by a hard time
     * budget. Without it, a degraded network or an anti-bot outage makes the
     * resolver hang for minutes — the UI shows "loading" forever and the queue
     * never advances, which is exactly the stuck-Next-song report on TV.
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        poTokenProvider: PoTokenProvider? = null,
    ): Result<PlaybackData> = try {
        withTimeout(STREAM_RESOLUTION_TIMEOUT_MS) {
            resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager, poTokenProvider)
        }
    } catch (e: TimeoutCancellationException) {
        Timber.tag(logTag).w("Stream resolution timed out after ${STREAM_RESOLUTION_TIMEOUT_MS}ms for $videoId")
        Result.failure(
            PlaybackException(
                "Stream resolution timed out",
                e,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            )
        )
    }

    private suspend fun resolvePlaybackData(
        videoId: String,
        playlistId: String?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        poTokenProvider: PoTokenProvider?,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        val isLoggedIn = YouTube.cookie != null
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")

        val mainClientPoToken = poTokenProvider?.getPlayerPoToken(videoId)
        if (mainClientPoToken != null) {
            Timber.tag(logTag).d("Obtained PO token for MAIN_CLIENT (length=${mainClientPoToken.length})")
        }

        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, poToken = mainClientPoToken).getOrNull()
        val audioConfig = mainPlayerResponse?.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse?.videoDetails
        val playbackTracking = mainPlayerResponse?.playbackTracking

        // Order in which clients are used to build a usable stream URL.
        //
        // Measured live: VISIONOS serves the whole file. ANDROID_VR 1.61 does too but is
        // increasingly bot-checked. ANDROID_VR 1.65 and IPADOS often serve URLs whose first
        // 512 KB downloads and every later range is 403, so they go last among guests; the
        // two-point probe in buildPlaybackData rejects those URLs. WEB_REMIX keeps its
        // session PO token; plain ANDROID URLs are ciphered and need NewPipe. Metadata
        // (loudness/history tracking) always comes from MAIN_CLIENT.
        val streamClients: Array<YouTubeClient> = arrayOf(
            VISIONOS,
            ANDROID_VR_NO_AUTH,
            WEB_REMIX,
            ANDROID_VR_1_65_10,
            IPADOS,
            ANDROID_NO_SDK,
            ANDROID,
        )

        // A client whose stream just died for this song is tried last on the retry, so the
        // retry actually moves to a different source instead of re-serving the same URL.
        val recentlyFailed = failedStreamClients[videoId]
            ?.takeIf { System.currentTimeMillis() - it.second < FAILED_CLIENT_TTL_MS }
            ?.first
        val orderedClients = streamClients.sortedBy { it.clientName == recentlyFailed?.clientName && it.clientVersion == recentlyFailed.clientVersion }

        var fallbackFailure: String? = null
        for (client in orderedClients) {
            if (client.loginRequired && !isLoggedIn) {
                Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                continue
            }

            val response = if (client == MAIN_CLIENT && mainPlayerResponse != null) {
                mainPlayerResponse
            } else {
                // Guest clients must never carry the user's session, otherwise a
                // logged-in request to a cookie-free fallback still bot-checks.
                // Web-family clients additionally need the session-bound PO token
                // inside serviceIntegrityDimensions; guests must NOT receive it.
                val clientPoToken = if (client.useWebPoTokens) mainClientPoToken else null
                YouTube.player(
                    videoId,
                    playlistId,
                    client,
                    poToken = clientPoToken,
                    setLogin = client == MAIN_CLIENT,
                )
                    .getOrNull()
                    ?: run {
                        Timber.tag(logTag).d("Skipping client ${client.clientName} - player response failed")
                        continue
                    }
            }
            fallbackFailure = response.playabilityStatus.reason

            buildPlaybackData(
                videoId = videoId,
                response = response,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager,
                poTokenProvider = poTokenProvider,
                audioConfig = audioConfig,
                videoDetails = videoDetails,
                playbackTracking = playbackTracking,
                clientName = client.clientName,
                client = client,
            )?.let { return@runCatching it }
        }

        val errorReason = fallbackFailure ?: mainPlayerResponse?.playabilityStatus?.reason
        if (mainPlayerResponse?.playabilityStatus?.status != "OK" && errorReason != null) {
            throw PlaybackException(errorReason, null, PlaybackException.ERROR_CODE_REMOTE_ERROR)
        }
        throw Exception("Could not find stream url")
    }

    private suspend fun buildPlaybackData(
        videoId: String,
        response: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        poTokenProvider: PoTokenProvider?,
        audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        videoDetails: PlayerResponse.VideoDetails?,
        playbackTracking: PlayerResponse.PlaybackTracking?,
        clientName: String,
        client: YouTubeClient,
    ): PlaybackData? {
        if (response.playabilityStatus.status != "OK") {
            Timber.tag(logTag).d("Player response status not OK for $clientName: ${response.playabilityStatus.reason}")
            return null
        }

        val format = findFormat(response, audioQuality, connectivityManager)
            ?: run {
                Timber.tag(logTag).d("No suitable format found for client: $clientName")
                return null
            }
        // Only web-family clients take a `pot` parameter on their stream URLs;
        // appending one to guest-client URLs can invalidate them.
        // WEB_REMIX commonly returns ciphered URLs. Unlike guest clients, it may
        // still be the only client with a usable authenticated/PO-token response,
        // so let NewPipe resolve its signature instead of discarding the format.
        val streamUrl = findUrlOrNull(
            format,
            videoId,
            response,
            allowSlowFallback = shouldUseSlowUrlFallback(
                clientName,
                MAIN_CLIENT.clientName,
                format.signatureCipher,
            ),
        )
            ?.let { addStreamingPoTokenIfNeeded(it, videoId, poTokenProvider, appendPot = client.useWebPoTokens) }
            ?: run {
                Timber.tag(logTag).d("Stream URL not found for client: $clientName")
                return null
            }
        val streamExpiresInSeconds = response.streamingData?.expiresInSeconds
            ?: run {
                Timber.tag(logTag).d("Stream expiration time not found for client: $clientName")
                return null
            }

        // Probe the URL before committing to it. YouTube now serves some
        // clients URLs that look valid but 403 at fetch time because they are
        // PO-token gated server-side. Handing such a URL to ExoPlayer
        // surfaces as ERROR_CODE_IO_BAD_HTTP_STATUS (2004) ("temporarily
        // unavailable / rate-limited"). A 1-byte ranged GET with the client's
        // own headers is cheap and lets us fall through to the next client
        // instead.
        if (!isStreamUrlUsable(streamUrl, client, format.contentLength)) {
            Timber.tag(logTag).w("Stream URL from $clientName failed probe (POT-gated/expired), trying next client")
            return null
        }

        Timber.tag(logTag).d("Using stream from $clientName: ${format.mimeType}, bitrate=${format.bitrate}")
        lastStreamClients[videoId] = client
        return PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
            streamClient = clientName,
            streamHeaders = streamHeadersForClient(client),
        )
    }

    /**
     * Headers that must accompany every request to a URL served by [client].
     * googlevideo validates the request fingerprint loosely, but web-family
     * URLs in particular are sensitive to a missing browser-ish User-Agent
     * and Referer/Origin; guest clients want their app UA echoed.
     */
    fun streamHeadersForClient(client: YouTubeClient): Map<String, String> = buildMap {
        put("User-Agent", client.userAgent)
        put("Accept", "*/*")
        put("Accept-Language", "en-US,en;q=0.9")
        when (client.clientName) {
            "WEB_REMIX" -> {
                put("Referer", "https://music.youtube.com/")
                put("Origin", "https://music.youtube.com")
            }
            else -> {
                put("Referer", "https://www.youtube.com/")
                put("Origin", "https://www.youtube.com")
            }
        }
    }
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

return format
    }

    /**
     * Find the best video format (with video and audio) for a given videoId
     */
    suspend fun getVideoStreamUrl(
        videoId: String,
        playlistId: String? = null,
        poTokenProvider: PoTokenProvider? = null,
    ): Result<String> = runCatching {
        Timber.tag(logTag).d("Fetching video stream URL for videoId: $videoId")

        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        val poToken = poTokenProvider?.getPlayerPoToken(videoId)

        // WEB_REMIX video streams are bot-checked (IO_UNSPECIFIED 2000) whether
        // the user is logged in or not. Prefer the non-auth guest clients first
        // so video keeps working regardless of session state; MAIN_CLIENT is used
        // for metadata and as a last resort for age-restricted content.
        val guestClients = arrayOf(VISIONOS, ANDROID_VR_1_65_10, ANDROID_VR_NO_AUTH, IPADOS, ANDROID_NO_SDK, ANDROID)
        for (client in guestClients) {
            val guestResponse = YouTube.player(
                videoId,
                playlistId,
                client,
                poToken = null,
                setLogin = false,
            ).getOrNull() ?: continue
            val guestMuxed = guestResponse.streamingData?.formats?.filter { it.isVideo }?.maxByOrNull { it.bitrate }
            val guestFormat = guestMuxed ?: guestResponse.streamingData?.adaptiveFormats
                ?.filter { it.isVideo }?.maxByOrNull { it.bitrate } ?: continue
            // Guests never take a `pot` parameter.
            val guestUrl = findUrlOrNull(guestFormat, videoId, guestResponse)
            if (guestUrl != null) {
                Timber.tag(logTag).d("Found video stream from guest client ${client.clientName}")
                return@runCatching guestUrl
            }
        }

        val mainPlayerResponse = YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, poToken).getOrNull()

        // Try muxed formats first (contain both video and audio in one stream)
        val muxedFormats = mainPlayerResponse?.streamingData?.formats ?: emptyList()
        val muxedFormat = muxedFormats
            .filter { it.isVideo }
            .maxByOrNull { it.bitrate }

        if (muxedFormat != null && mainPlayerResponse != null) {
            val url = findUrlOrNull(muxedFormat, videoId, mainPlayerResponse)
                ?.let { addStreamingPoTokenIfNeeded(it, videoId, poTokenProvider, appendPot = true) }
            if (url != null) {
                Timber.tag(logTag).d("Found muxed video format: ${muxedFormat.mimeType}, resolution: ${muxedFormat.height}p")
                return@runCatching url
            }
        }

        // Fallback: video-only adaptive format (will have NO audio)
        val adaptiveFormats = mainPlayerResponse?.streamingData?.adaptiveFormats ?: emptyList()
        val videoOnlyFormat = adaptiveFormats
            .filter { it.isVideo }
            .maxByOrNull { it.bitrate }

        if (videoOnlyFormat != null && mainPlayerResponse != null) {
            val url = findUrlOrNull(videoOnlyFormat, videoId, mainPlayerResponse)
                ?.let { addStreamingPoTokenIfNeeded(it, videoId, poTokenProvider, appendPot = true) }
            if (url != null) {
                Timber.tag(logTag).d("Found video-only format (no audio): ${videoOnlyFormat.mimeType}, resolution: ${videoOnlyFormat.height}p")
                return@runCatching url
            }
        }

        throw Exception("No video stream available for videoId: $videoId")
    }

    /**
     * Cheap check if a video has video playback available without resolving stream URLs.
     */
    suspend fun hasVideoPlayback(videoId: String): Boolean {
        return try {
            val playerResponse = YouTube.player(videoId, null, MAIN_CLIENT).getOrNull()
            val streamingData = playerResponse?.streamingData ?: return false
            val hasMuxed = streamingData.formats?.any { it.isVideo } == true
            val hasAdaptiveVideo = streamingData.adaptiveFormats.any { it.isVideo }
            hasMuxed || hasAdaptiveVideo
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Liveness probe for a GVS stream URL, using ranged GETs (HEAD is sometimes rejected by
     * googlevideo while ranged GETs succeed). Only 2xx/206 counts as usable.
     *
     * Probes the start AND a point past the first chunk. Some guest clients (seen with
     * ANDROID_VR 1.65 and IPADOS) are served URLs where the first 512 KB is free but every
     * later range is 403 — the song starts, then dies a few seconds in with
     * IO_BAD_HTTP_STATUS, usually while the app is in the background. A start-only probe
     * passes those URLs.
     */
    private fun isStreamUrlUsable(url: String, client: YouTubeClient, contentLength: Long?): Boolean {
        if (!url.contains("googlevideo.com") && !url.contains("youtube.com/videoplayback")) {
            // Not a GVS URL (e.g. local/other source) — accept as-is.
            return true
        }
        val length = contentLength ?: url.toUri().getQueryParameter("clen")?.toLongOrNull()
        val deepOffset = length?.let { (it / 2).coerceAtLeast(0L) }?.takeIf { it > PROBE_FREE_PREFIX_BYTES }
        return probeRange(url, client, 0L) && (deepOffset == null || probeRange(url, client, deepOffset))
    }

    private fun probeRange(url: String, client: YouTubeClient, offset: Long): Boolean = try {
        val requestBuilder = okhttp3.Request.Builder()
            .url(url)
            .header("Range", "bytes=$offset-$offset")
        streamHeadersForClient(client).forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.tag(logTag).d("Stream probe at $offset got HTTP ${response.code}")
            }
            response.isSuccessful
        }
    } catch (e: Exception) {
        Timber.tag(logTag).e(e, "Stream probe at $offset failed with exception")
        false
    }

    /** googlevideo serves this much of a gated URL before it starts answering 403. */
    private const val PROBE_FREE_PREFIX_BYTES = 1024L * 1024L

    private fun getSignatureTimestampOrNull(videoId: String): Int? {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        return NewPipeExtractor.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }

    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        playerResponse: PlayerResponse,
        allowSlowFallback: Boolean = true,
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")

        // First check if format already has a URL from newPipePlayer
        if (!format.url.isNullOrEmpty()) {
            Timber.tag(logTag).d("Using URL from format directly")
            return format.url
        }
        if (!allowSlowFallback) {
            Timber.tag(logTag).d("Skipping slow URL fallback on primary playback path")
            return null
        }

        // Try to get URL using NewPipeExtractor signature deobfuscation
        val deobfuscatedUrl = NewPipeExtractor.getStreamUrl(format, videoId)
        if (deobfuscatedUrl != null) {
            Timber.tag(logTag).d("Stream URL obtained via deobfuscation")
            return deobfuscatedUrl
        }

        // Fallback: try to get URL from StreamInfo
        Timber.tag(logTag).d("Trying StreamInfo fallback for URL")
        val streamUrls = YouTube.getNewPipeStreamUrls(videoId)
        if (streamUrls.isNotEmpty()) {
            val streamUrl = streamUrls.find { it.first == format.itag }?.second
            if (streamUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained from StreamInfo")
                return streamUrl
            }

            // If exact itag not found, try to find any audio stream
            val audioStream = streamUrls.find { urlPair ->
                playerResponse.streamingData?.adaptiveFormats?.any {
                    it.itag == urlPair.first && it.isAudio
                } == true
            }?.second

            if (audioStream != null) {
                Timber.tag(logTag).d("Audio stream URL obtained from StreamInfo (different itag)")
                return audioStream
            }
        }

        Timber.tag(logTag).e("Failed to get stream URL")
        return null
    }

    private suspend fun addStreamingPoTokenIfNeeded(
        streamUrl: String,
        videoId: String,
        poTokenProvider: PoTokenProvider?,
        appendPot: Boolean,
    ): String {
        if (!appendPot) return streamUrl
        if (!streamUrl.contains("googlevideo.com") && !streamUrl.contains("youtube.com/videoplayback")) {
            return streamUrl
        }
        if (streamUrl.toUri().getQueryParameter("pot") != null) {
            return streamUrl
        }

        val streamingPoToken = poTokenProvider?.getStreamingPoToken(videoId)
        if (streamingPoToken.isNullOrBlank()) {
            Timber.tag(logTag).w("No streaming PO token available for $videoId; using unmodified GVS URL")
            return streamUrl
        }

        Timber.tag(logTag).d("Appending streaming PO token to GVS URL (length=${streamingPoToken.length})")
        return streamUrl.toUri().buildUpon()
            .appendQueryParameter("pot", streamingPoToken)
            .build()
            .toString()
    }

    /** Client whose stream failed mid-play, per song, with when it failed. */
    private val failedStreamClients = java.util.concurrent.ConcurrentHashMap<String, Pair<YouTubeClient, Long>>()
    private val lastStreamClients = java.util.concurrent.ConcurrentHashMap<String, YouTubeClient>()
    private const val FAILED_CLIENT_TTL_MS = 10 * 60_000L

    /**
     * Called when playback of [videoId] failed: remembers which client served the stream so
     * the next resolution prefers a different one.
     */
    fun forceRefreshForVideo(videoId: String) {
        Timber.tag(logTag).d("Force refreshing for videoId: $videoId")
        lastStreamClients[videoId]?.let { failedStreamClients[videoId] = it to System.currentTimeMillis() }
    }
}
