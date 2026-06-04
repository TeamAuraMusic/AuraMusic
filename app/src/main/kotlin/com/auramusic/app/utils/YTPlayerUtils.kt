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
import com.auramusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.auramusic.innertube.models.YouTubeClient.Companion.IPADOS
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.auramusic.innertube.models.response.PlayerResponse
import com.auramusic.app.constants.AudioQuality
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_NO_AUTH,
        IPADOS,
    )
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )
    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        poTokenProvider: PoTokenProvider? = null,
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
            YouTube.player(videoId, playlistId, MAIN_CLIENT, poToken = mainClientPoToken).getOrThrow()
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking

        buildPlaybackData(
            videoId = videoId,
            response = mainPlayerResponse,
            audioQuality = audioQuality,
            connectivityManager = connectivityManager,
            poTokenProvider = poTokenProvider,
            audioConfig = audioConfig,
            videoDetails = videoDetails,
            playbackTracking = playbackTracking,
            clientName = MAIN_CLIENT.clientName,
        ) ?: run {
            Timber.tag(logTag).d("MAIN_CLIENT did not produce a usable stream; trying fallbacks")
            var fallbackFailure: String? = null
            for (client in STREAM_FALLBACK_CLIENTS) {
                if (client.loginRequired && !isLoggedIn) {
                    Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                val fallbackPoToken = poTokenProvider?.getPlayerPoToken(videoId)
                val fallbackResponse = YouTube.player(videoId, playlistId, client, poToken = fallbackPoToken)
                    .getOrNull()
                    ?: continue
                fallbackFailure = fallbackResponse.playabilityStatus.reason

                buildPlaybackData(
                    videoId = videoId,
                    response = fallbackResponse,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    poTokenProvider = poTokenProvider,
                    audioConfig = audioConfig,
                    videoDetails = videoDetails,
                    playbackTracking = playbackTracking,
                    clientName = client.clientName,
                )?.let { return@runCatching it }
            }

            val errorReason = fallbackFailure ?: mainPlayerResponse.playabilityStatus.reason
            if (mainPlayerResponse.playabilityStatus.status != "OK" && errorReason != null) {
                throw PlaybackException(errorReason, null, PlaybackException.ERROR_CODE_REMOTE_ERROR)
            }
            throw Exception("Could not find stream url")
        }
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
        val streamUrl = findUrlOrNull(format, videoId, response, allowSlowFallback = clientName != MAIN_CLIENT.clientName)
            ?.let { addStreamingPoTokenIfNeeded(it, videoId, poTokenProvider) }
            ?: run {
                Timber.tag(logTag).d("Stream URL not found for client: $clientName")
                return null
            }
        val streamExpiresInSeconds = response.streamingData?.expiresInSeconds
            ?: run {
                Timber.tag(logTag).d("Stream expiration time not found for client: $clientName")
                return null
            }

        Timber.tag(logTag).d("Using stream from $clientName: ${format.mimeType}, bitrate=${format.bitrate}")
        return PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
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
        val mainPlayerResponse = YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, poToken).getOrThrow()

        // Try muxed formats first (contain both video and audio in one stream)
        val muxedFormats = mainPlayerResponse.streamingData?.formats ?: emptyList()
        val muxedFormat = muxedFormats
            .filter { it.isVideo }
            .maxByOrNull { it.bitrate }

        if (muxedFormat != null) {
            val url = findUrlOrNull(muxedFormat, videoId, mainPlayerResponse)
                ?.let { addStreamingPoTokenIfNeeded(it, videoId, poTokenProvider) }
            if (url != null) {
                Timber.tag(logTag).d("Found muxed video format: ${muxedFormat.mimeType}, resolution: ${muxedFormat.height}p")
                return@runCatching url
            }
        }

        // Fallback: video-only adaptive format (will have NO audio)
        val adaptiveFormats = mainPlayerResponse.streamingData?.adaptiveFormats ?: emptyList()
        val videoOnlyFormat = adaptiveFormats
            .filter { it.isVideo }
            .maxByOrNull { it.bitrate }

        if (videoOnlyFormat != null) {
            val url = findUrlOrNull(videoOnlyFormat, videoId, mainPlayerResponse)
                ?.let { addStreamingPoTokenIfNeeded(it, videoId, poTokenProvider) }
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

    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }

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
    ): String {
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

    fun forceRefreshForVideo(videoId: String) {
        Timber.tag(logTag).d("Force refreshing for videoId: $videoId")
    }
}
