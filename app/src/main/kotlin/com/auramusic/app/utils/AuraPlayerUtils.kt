package com.auramusic.app.utils

import com.auramusic.app.constants.VideoQuality
import com.auramusic.auravideo.AuraVideo
import com.auramusic.auravideo.AuraVideo.VideoStreamResult
import com.auramusic.auravideo.AuraVideo.VideoSearchResult
import com.auramusic.auravideo.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object AuraPlayerUtils {
    private const val logTag = "AuraPlayerUtils"

    /**
     * Set the preferred video quality before fetching video URLs
     */
    fun setPreferredVideoQuality(quality: VideoQuality) {
        // Map PreferenceKeys.VideoQuality to AuraVideo.VideoQuality
        val auraQuality = when (quality) {
            VideoQuality.QUALITY_360P -> AuraVideo.VideoQuality.QUALITY_360P
            VideoQuality.QUALITY_480P -> AuraVideo.VideoQuality.QUALITY_480P
            VideoQuality.QUALITY_720P -> AuraVideo.VideoQuality.QUALITY_720P
            VideoQuality.QUALITY_1080P -> AuraVideo.VideoQuality.QUALITY_1080P
        }
        AuraVideo.setPreferredVideoQuality(auraQuality)
    }

    suspend fun getVideoStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(logTag).d("AuraPlayerUtils: Fetching video stream URL for videoId: $videoId")

        AuraVideo.getVideoStreamUrl(videoId).map { result ->
            Timber.tag(logTag).d("AuraPlayerUtils: Successfully obtained video URL with mimeType: ${result.mimeType}")
            "${result.url}|${result.mimeType}"
        }.also { result ->
            result.onSuccess { _ ->
                Timber.tag(logTag).d("AuraPlayerUtils: Successfully obtained video URL")
            }.onFailure { e ->
                Timber.tag(logTag).e(e, "AuraPlayerUtils: Failed to get video URL")
            }
        }
    }

    /**
     * Resolve a video stream as either a single muxed URL or a pair of
     * video-only + audio-only streams that the player must merge.
     *
     * For quality > 720p, a single muxed URL is physically not available from
     * YouTube — the caller MUST handle the [AuraVideo.VideoStreamSource.Merged]
     * case to actually display 1080p video.
     */
    suspend fun getVideoStreamSource(videoId: String): Result<AuraVideo.VideoStreamSource> =
        withContext(Dispatchers.IO) {
            Timber.tag(logTag).d("AuraPlayerUtils: Resolving stream source for videoId: $videoId")
            AuraVideo.getVideoStreamSource(videoId).also { result ->
                result.onSuccess { source ->
                    when (source) {
                        is AuraVideo.VideoStreamSource.Single ->
                            Timber.tag(logTag).d("AuraPlayerUtils: Single source (${source.mimeType})")
                        is AuraVideo.VideoStreamSource.Merged ->
                            Timber.tag(logTag)
                                .d("AuraPlayerUtils: Merged source ${source.height}p video + audio")
                    }
                }.onFailure { e ->
                    Timber.tag(logTag).e(e, "AuraPlayerUtils: Failed to resolve stream source")
                }
            }
        }

    /**
     * Get video stream URL with automatic search fallback for regular songs
     * This searches for official music video if direct lookup fails
     */
    suspend fun getVideoStreamUrlWithFallback(songTitle: String, artistName: String, videoId: String, isVideoSong: Boolean = true): Result<VideoSearchResult> = withContext(Dispatchers.IO) {
        Timber.tag(logTag).d("AuraPlayerUtils: Trying to get video with fallback for: $songTitle by $artistName (isVideoSong=$isVideoSong)")

        try {
            val result = AuraVideo.getVideoStreamUrlWithFallback(songTitle, artistName, videoId, isVideoSong)
            result.onSuccess { searchResult ->
                Timber.tag(logTag).d("AuraPlayerUtils: Found video via fallback: ${searchResult.videoId}")
            }
            result
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "AuraPlayerUtils: Fallback search also failed")
            Result.failure(e)
        }
    }

    suspend fun hasVideoPlayback(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            AuraVideo.hasVideoPlayback(videoId)
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "AuraPlayerUtils: Error checking video availability")
            false
        }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        AuraVideo.getVideoDetails(videoId)
    }
}

/** Maps app-level video quality constants to the library enum, used by settings UI. */
fun appVideoQualityToLibrary(quality: VideoQuality): AuraVideo.VideoQuality = when (quality) {
    VideoQuality.QUALITY_360P -> AuraVideo.VideoQuality.QUALITY_360P
    VideoQuality.QUALITY_480P -> AuraVideo.VideoQuality.QUALITY_480P
    VideoQuality.QUALITY_720P -> AuraVideo.VideoQuality.QUALITY_720P
    VideoQuality.QUALITY_1080P -> AuraVideo.VideoQuality.QUALITY_1080P
}
