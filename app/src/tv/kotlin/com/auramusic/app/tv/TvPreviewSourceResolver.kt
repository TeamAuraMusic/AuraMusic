package com.auramusic.app.tv

import android.content.Context
import android.net.ConnectivityManager
import com.auramusic.app.constants.AudioQuality
import com.auramusic.app.constants.AudioQualityKey
import com.auramusic.app.extensions.toEnum
import com.auramusic.app.playback.potoken.WebViewPoTokenProvider
import com.auramusic.app.utils.YTPlayerUtils
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.auravideo.AuraVideo
import kotlinx.coroutines.CancellationException
import timber.log.Timber

internal suspend fun <T> resolveDirectThenFallback(
    direct: suspend () -> Result<T>,
    fallback: suspend () -> T,
): T {
    val directResult = direct()
    directResult.getOrNull()?.let { return it }
    (directResult.exceptionOrNull() as? CancellationException)?.let { throw it }
    return fallback()
}

/** Resolves previews through AuraVideo first, then the app's full playback resolver. */
internal object TvPreviewSourceResolver {
    @Volatile private var tokenProvider: WebViewPoTokenProvider? = null

    suspend fun resolve(context: Context, mediaId: String): AuraVideo.AudioPreviewSource {
        val appContext = context.applicationContext
        return resolveDirectThenFallback(
            direct = { AuraVideo.getAudioPreviewSource(mediaId) },
            fallback = {
                Timber.d("Direct TV preview source unavailable; trying AuraMusic playback resolver")
                val connectivityManager = requireNotNull(
                    appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager,
                ) { "Connectivity service unavailable for TV preview resolution" }
                val quality = appContext.dataStore.get(AudioQualityKey).toEnum(AudioQuality.AUTO)
                val provider = tokenProvider ?: synchronized(this) {
                    tokenProvider ?: WebViewPoTokenProvider(appContext).also { tokenProvider = it }
                }
                val playback = YTPlayerUtils.playerResponseForPlayback(
                    videoId = mediaId,
                    audioQuality = quality,
                    connectivityManager = connectivityManager,
                    poTokenProvider = provider,
                ).getOrThrow()
                val mimeType = playback.format.mimeType.substringBefore(';').trim()
                require(mimeType.startsWith("audio/", ignoreCase = true)) {
                    "AuraMusic playback resolver returned non-audio TV preview for $mediaId"
                }
                AuraVideo.AudioPreviewSource(
                    url = playback.streamUrl,
                    mimeType = mimeType,
                    durationMs = playback.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000L) ?: 0L,
                    requestHeaders = playback.streamHeaders,
                )
            },
        ).also { source ->
            require(source.url.isNotBlank()) { "Resolved TV preview URL was blank for $mediaId" }
        }
    }
}
