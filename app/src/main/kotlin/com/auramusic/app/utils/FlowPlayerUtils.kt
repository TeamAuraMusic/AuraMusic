package com.auramusic.app.utils

import com.auramusic.app.constants.VideoQuality
import com.auramusic.flow.FlowVideo
import com.auramusic.flow.FlowVideo.VideoStreamResult
import com.auramusic.flow.FlowVideo.VideoSearchResult
import com.auramusic.flow.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object FlowPlayerUtils {
    private const val logTag = "FlowPlayerUtils"

    /**
     * Set the preferred video quality before fetching video URLs
     */
    fun setPreferredVideoQuality(quality: VideoQuality) {
        // Map PreferenceKeys.VideoQuality to FlowVideo.VideoQuality
        val flowQuality = when (quality) {
            VideoQuality.QUALITY_360P -> com.auramusic.flow.FlowVideo.VideoQuality.QUALITY_360P
            VideoQuality.QUALITY_480P -> com.auramusic.flow.FlowVideo.VideoQuality.QUALITY_480P
            VideoQuality.QUALITY_720P -> com.auramusic.flow.FlowVideo.VideoQuality.QUALITY_720P
            VideoQuality.QUALITY_1080P -> com.auramusic.flow.FlowVideo.VideoQuality.QUALITY_1080P
        }
        FlowVideo.setPreferredVideoQuality(flowQuality)
    }

    suspend fun getVideoStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(logTag).d("FlowPlayerUtils: Fetching video stream URL for videoId: $videoId")
        
        FlowVideo.getVideoStreamUrl(videoId).map { result ->
            Timber.tag(logTag).d("FlowPlayerUtils: Successfully obtained video URL with mimeType: ${result.mimeType}")
            "${result.url}|${result.mimeType}"
        }.also { result ->
            result.onSuccess { _ ->
                Timber.tag(logTag).d("FlowPlayerUtils: Successfully obtained video URL")
            }.onFailure { e ->
                Timber.tag(logTag).e(e, "FlowPlayerUtils: Failed to get video URL")
            }
        }
    }

    /**
     * Resolve a video stream as either a single muxed URL or a pair of
     * video-only + audio-only streams that the player must merge.
     *
     * For quality > 720p, a single muxed URL is physically not available from
     * YouTube — the caller MUST handle the [FlowVideo.VideoStreamSource.Merged]
     * case to actually display 1080p video.
     */
    suspend fun getVideoStreamSource(videoId: String): Result<FlowVideo.VideoStreamSource> =
        withContext(Dispatchers.IO) {
            Timber.tag(logTag).d("FlowPlayerUtils: Resolving stream source for videoId: $videoId")
            FlowVideo.getVideoStreamSource(videoId).also { result ->
                result.onSuccess { source ->
                    when (source) {
                        is FlowVideo.VideoStreamSource.Single ->
                            Timber.tag(logTag).d("FlowPlayerUtils: Single source (${source.mimeType})")
                        is FlowVideo.VideoStreamSource.Merged ->
                            Timber.tag(logTag)
                                .d("FlowPlayerUtils: Merged source ${source.height}p video + audio")
                    }
                }.onFailure { e ->
                    Timber.tag(logTag).e(e, "FlowPlayerUtils: Failed to resolve stream source")
                }
            }
        }

    /**
     * Get video stream URL with automatic search fallback for regular songs
     * This searches for official music video if direct lookup fails
     */
    suspend fun getVideoStreamUrlWithFallback(songTitle: String, artistName: String, videoId: String, isVideoSong: Boolean = true): Result<VideoSearchResult> = withContext(Dispatchers.IO) {
        Timber.tag(logTag).d("FlowPlayerUtils: Trying to get video with fallback for: $songTitle by $artistName (isVideoSong=$isVideoSong)")
        
        try {
            val result = FlowVideo.getVideoStreamUrlWithFallback(songTitle, artistName, videoId, isVideoSong)
            result.onSuccess { searchResult ->
                Timber.tag(logTag).d("FlowPlayerUtils: Found video via fallback: ${searchResult.videoId}")
            }
            result
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "FlowPlayerUtils: Fallback search also failed")
            Result.failure(e)
        }
    }

    suspend fun hasVideoPlayback(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            FlowVideo.hasVideoPlayback(videoId)
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "FlowPlayerUtils: Error checking video availability")
            false
        }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        FlowVideo.getVideoDetails(videoId)
    }
}