package com.auramusic.flow

import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.auramusic.innertube.YouTube.SearchFilter
import com.auramusic.innertube.models.YTItem

object FlowVideo {
    data class VideoStreamResult(
        val url: String,
        val mimeType: String
    )

    data class VideoSearchResult(
        val videoId: String,
        val title: String,
        val channelName: String,
        val thumbnailUrl: String
    )

    /**
     * Search for an official music video for a song
     */
    suspend fun searchOfficialMusicVideo(songTitle: String, artistName: String): Result<VideoSearchResult> = runCatching {
        val searchQuery = "$songTitle $artistName official music video"
        
        val searchResult = YouTube.search(searchQuery, SearchFilter.FILTER_VIDEO).getOrThrow()
        
        // Find the best matching video - prefer official music videos
        var bestVideo: YTItem? = null
        
        for (item in searchResult.items) {
            if (item is YTItem) {
                val title = item.title.lowercase()
                // Prefer videos that contain "official", "music video", "mv", "vevo", or the song title
                val isPreferred = title.contains("official") || 
                                  title.contains("music video") ||
                                  title.contains(" mv ") ||
                                  title.contains("vevo") ||
                                  title.contains(songTitle.lowercase())
                
                if (isPreferred && bestVideo == null) {
                    bestVideo = item
                }
            }
        }
        
        // Fall back to first result if no preferred video found
        bestVideo = bestVideo ?: searchResult.items.firstOrNull()
        
        if (bestVideo == null) {
            throw Exception("No music video found")
        }
        
        // Build the VideoSearchResult from the YTItem
        VideoSearchResult(
            videoId = bestVideo.id,
            title = bestVideo.title,
            channelName = bestVideo.title, // YTItem doesn't have channelName, use title as fallback
            thumbnailUrl = bestVideo.thumbnail ?: ""
        )
    }

    /**
     * Get video stream URL, with automatic search fallback
     */
    suspend fun getVideoStreamUrlWithFallback(songTitle: String, artistName: String, videoId: String): Result<VideoSearchResult> = runCatching {
        // First try: direct video lookup
        val directResult = runCatching { getVideoStreamUrl(videoId).getOrNull() }
        
        if (directResult.isSuccess && directResult.getOrNull() != null) {
            val details = getVideoDetails(videoId).getOrNull()
            return@runCatching VideoSearchResult(
                videoId = videoId,
                title = details?.title ?: songTitle,
                channelName = details?.channelName ?: artistName,
                thumbnailUrl = details?.thumbnailUrl ?: ""
            )
        }
        
        // Direct lookup failed - try searching for official music video
        val searchResult = searchOfficialMusicVideo(songTitle, artistName).getOrNull()
            ?: throw Exception("No video available for this song")
        
        searchResult
    }

    suspend fun getVideoStreamUrl(videoId: String): Result<VideoStreamResult> = runCatching {
        // First try NewPipeExtractor which gives us working stream URLs
        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
        
        if (streamInfo != null) {
            val muxedVideoStreams = streamInfo.videoStreams // These have both video and audio
            val videoOnlyStreams = streamInfo.videoOnlyStreams // These are video only (no audio)
            
            if (muxedVideoStreams.isNotEmpty()) {
                // Prefer muxed streams (video+audio) - prevents silent audio issue
                // Prefer 720p quality
                val preferred720p = muxedVideoStreams.filter { it.resolution?.contains("720") == true }
                val bestStream = if (preferred720p.isNotEmpty()) {
                    preferred720p.maxByOrNull { it.bitrate }
                } else {
                    muxedVideoStreams.maxByOrNull { it.bitrate }
                }
                
                if (bestStream != null) {
                    val url = bestStream.content ?: bestStream.url
                    val mimeType = bestStream.format?.mimeType ?: "video/mp4"
                    if (url != null) {
                        return@runCatching VideoStreamResult(url, mimeType)
                    }
                }
            }
            
            // Fallback to video-only streams if muxed not available
            if (videoOnlyStreams.isNotEmpty()) {
                val preferred720p = videoOnlyStreams.filter { it.resolution?.contains("720") == true }
                val bestStream = if (preferred720p.isNotEmpty()) {
                    preferred720p.maxByOrNull { it.bitrate }
                } else {
                    videoOnlyStreams.maxByOrNull { it.bitrate }
                }
                
                if (bestStream != null) {
                    val url = bestStream.content ?: bestStream.url
                    val mimeType = bestStream.format?.mimeType ?: "video/mp4"
                    if (url != null) {
                        return@runCatching VideoStreamResult(url, mimeType)
                    }
                }
            }
        }

        // Fallback to YouTube player API
        val playerResponse = YouTube.player(videoId, client = WEB_REMIX).getOrThrow()
        
        if (playerResponse.playabilityStatus.status != "OK") {
            throw Exception("Playability error: ${playerResponse.playabilityStatus.reason ?: playerResponse.playabilityStatus.status}")
        }

        val streamingData = playerResponse.streamingData ?: throw Exception("No streaming data")

        // Check if we need signature deobfuscation
        val needsDeobfuscation = streamingData.formats?.any { it.signatureCipher != null || it.cipher != null } == true ||
                streamingData.adaptiveFormats.any { it.signatureCipher != null || it.cipher != null }

        // Try muxed formats first (has both audio and video)
        val muxedFormats = streamingData.formats ?: emptyList()
        // Prefer 720p muxed videos (height between 720 and 1080)
        val preferred720pMuxed = muxedFormats.filter { it.isVideo && (it.height ?: 0) in 720..1080 }
        val videoMuxed = if (preferred720pMuxed.isNotEmpty()) {
            preferred720pMuxed.maxByOrNull { it.bitrate }
        } else {
            muxedFormats.filter { it.isVideo }.maxByOrNull { it.bitrate }
        }
        
        var muxedUrl = videoMuxed?.url
        val muxedMimeType = videoMuxed?.mimeType
        
        // If URL is missing and we have signature cipher, try to deobfuscate
        if (muxedUrl == null && videoMuxed != null && needsDeobfuscation) {
            muxedUrl = NewPipeExtractor.getStreamUrl(videoMuxed, videoId)
        }
        
        if (muxedUrl != null) {
            return@runCatching VideoStreamResult(muxedUrl, muxedMimeType ?: "video/mp4")
        }

        // Try adaptive formats - only use as last resort (usually video-only)
        val adaptiveFormats = streamingData.adaptiveFormats
        // Prefer 720p adaptive videos
        val preferred720pAdaptive = adaptiveFormats.filter { it.isVideo && (it.height ?: 0) in 720..1080 }
        val adaptiveVideo = if (preferred720pAdaptive.isNotEmpty()) {
            preferred720pAdaptive.maxByOrNull { it.bitrate }
        } else {
            adaptiveFormats.filter { it.isVideo }.maxByOrNull { it.bitrate }
        }
        
        var adaptiveUrl = adaptiveVideo?.url
        val adaptiveMimeType = adaptiveVideo?.mimeType
        
        // If URL is missing and we have signature cipher, try to deobfuscate
        if (adaptiveUrl == null && adaptiveVideo != null && needsDeobfuscation) {
            adaptiveUrl = NewPipeExtractor.getStreamUrl(adaptiveVideo, videoId)
        }
        
        if (adaptiveUrl != null) {
            return@runCatching VideoStreamResult(adaptiveUrl, adaptiveMimeType ?: "video/mp4")
        }

        throw Exception("No video URL available")
    }

    suspend fun hasVideoPlayback(videoId: String): Boolean {
        return try {
            // First try NewPipeExtractor
            val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
            if (streamInfo != null) {
                val hasVideo = streamInfo.videoStreams.isNotEmpty() || streamInfo.videoOnlyStreams.isNotEmpty()
                if (hasVideo) return true
            }

            // Fallback to YouTube player API
            val playerResponse = YouTube.player(videoId, client = WEB_REMIX).getOrNull()
            val streamingData = playerResponse?.streamingData
            
            if (streamingData != null) {
                val hasMuxedVideo = streamingData.formats?.any { it.isVideo } == true
                val hasAdaptiveVideo = streamingData.adaptiveFormats.any { it.isVideo }
                return hasMuxedVideo || hasAdaptiveVideo
            }
            
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> = runCatching {
        val response = YouTube.player(videoId, client = WEB_REMIX).getOrThrow()
        val videoDetails = response.videoDetails ?: throw Exception("No video details")

        VideoItem(
            id = videoDetails.videoId,
            title = videoDetails.title ?: "",
            channelName = videoDetails.author ?: "",
            channelId = videoDetails.channelId,
            thumbnailUrl = videoDetails.thumbnail?.thumbnails?.lastOrNull()?.url ?: "",
            duration = videoDetails.lengthSeconds.toIntOrNull() ?: 0,
            viewCount = videoDetails.viewCount?.toLongOrNull() ?: 0L,
            uploadDate = null,
            description = null,
        )
    }
}
