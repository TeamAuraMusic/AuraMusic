package com.auramusic.flow

import android.util.Log
import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.auramusic.innertube.YouTube.SearchFilter
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.YTItem

object FlowVideo {
    private const val TAG = "FlowVideo"
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
     * Available video quality options
     */
    enum class VideoQuality(val height: Int, val label: String) {
        QUALITY_360P(360, "360p"),
        QUALITY_480P(480, "480p"),
        QUALITY_720P(720, "720p"),
        QUALITY_1080P(1080, "1080p")
    }

    // Default preferred quality - 720p
    var currentPreferredQuality: VideoQuality = VideoQuality.QUALITY_720P

    /**
     * Set preferred video quality
     */
    fun setPreferredVideoQuality(quality: VideoQuality) {
        currentPreferredQuality = quality
    }

    /**
     * Get all available qualities from a list of video streams
     */
    private fun getAvailableQualities(streams: List<org.schabi.newpipe.extractor.stream.VideoStream>): List<VideoQuality> {
        val qualities = mutableSetOf<VideoQuality>()
        for (stream in streams) {
            val height = stream.height
            when {
                height >= 1080 -> qualities.add(VideoQuality.QUALITY_1080P)
                height >= 720 -> qualities.add(VideoQuality.QUALITY_720P)
                height >= 480 -> qualities.add(VideoQuality.QUALITY_480P)
                height >= 360 -> qualities.add(VideoQuality.QUALITY_360P)
            }
        }
        return qualities.sortedBy { it.height }
    }

    /**
     * Find the best stream matching the preferred quality
     * Falls back to lower quality if preferred not available
     */
    private fun findBestStream(
        streams: List<org.schabi.newpipe.extractor.stream.VideoStream>,
        requireAudio: Boolean
    ): org.schabi.newpipe.extractor.stream.VideoStream? {
        if (streams.isEmpty()) return null

        // Filter streams based on whether we need audio
        val filteredStreams = if (requireAudio) {
            streams.filter { !it.isVideoOnly() }
        } else {
            streams
        }

        // Try to find exact quality match
        for (quality in listOf(currentPreferredQuality, VideoQuality.QUALITY_1080P, VideoQuality.QUALITY_720P, VideoQuality.QUALITY_480P, VideoQuality.QUALITY_360P)) {
            val matching = filteredStreams.filter { stream ->
                val height = stream.height
                // Less restrictive: prefer exact match or higher quality
                // For 720p, accept 720-1080+ (any quality at or above)
                height >= quality.height
            }
            if (matching.isNotEmpty()) {
                // Return highest quality among matching (max height, not max bitrate)
                return matching.maxByOrNull { it.height }
            }
        }

        // Fallback to any stream with audio if required, or any stream
        // Use max height (quality) instead of max bitrate
        return if (requireAudio && filteredStreams.isNotEmpty()) {
            filteredStreams.maxByOrNull { it.height }
        } else {
            streams.maxByOrNull { it.height }
        }
    }

    /**
     * Search for an official music video for a song
     */
    suspend fun searchOfficialMusicVideo(songTitle: String, artistName: String): Result<VideoSearchResult> = runCatching {
        val songTitleLower = songTitle.lowercase().trim()
        val artistNameLower = artistName.lowercase().trim()
        
        // Keywords to exclude (lyrics videos, live performances, etc.)
        val excludeKeywords = listOf(
            "lyrics", "lyric video", "lyrical", 
            "live", "concert", "performance",
            "reaction", "react", "cover",
            "remix", "instrumental",
            "visualizer", "audio"
        )
        
        // Preferred channels (official music channels)
        val preferredChannels = listOf(
            "vevo", "official", "music", "record"
        )
        
        // Try multiple search queries to find the best video
        val searchQueries = listOf(
            "$songTitle $artistName official music video",
            "$songTitle $artistName music video",
            "$songTitle $artistName"
        )
        
        for (searchQuery in searchQueries) {
            Log.d(TAG, "searchOfficialMusicVideo: Searching with query: $searchQuery")
            val searchResult = runCatching { YouTube.search(searchQuery, SearchFilter.FILTER_VIDEO).getOrThrow() }
            
            if (searchResult.isFailure) {
                Log.d(TAG, "searchOfficialMusicVideo: Search failed for query: $searchQuery")
                continue
            }
            
            val items = searchResult.getOrNull()?.items ?: continue
            if (items.isEmpty()) continue
            
            // Score each video based on relevance
            var bestScoredVideo: Pair<YTItem, Int>? = null // (item, score)
            
            for (item in items) {
                val title = item.title.lowercase()
                val channelName = (item as? SongItem)?.artists?.firstOrNull()?.name?.lowercase() ?: ""
                
                var score = 0
                
                // Check if this is an actual video song (has video content)
                val isActualVideoSong = item is SongItem && item.isVideoSong
                if (isActualVideoSong) score += 30
                
                // Check for exclude keywords (lyrics, live, etc.)
                val hasExcludedKeyword = excludeKeywords.any { title.contains(it) }
                if (hasExcludedKeyword) {
                    score -= 50 // Penalize heavily
                }
                
                // Check for preferred channel
                val hasPreferredChannel = preferredChannels.any { 
                    channelName.contains(it) || title.contains(it) 
                }
                if (hasPreferredChannel) score += 20
                
                // Title matching - check if song title is in video title
                val titleHasSongName = title.contains(songTitleLower) || 
                    songTitleLower.contains(title.take(minOf(30, title.length)))
                if (titleHasSongName) score += 25
                
                // Check if artist name is mentioned (in title or channel)
                val artistInTitle = title.contains(artistNameLower) || 
                    artistNameLower.split(" ").any { it.length > 2 && title.contains(it) }
                if (artistInTitle) score += 15
                
                // Check for "official" keyword
                if (title.contains("official")) score += 10
                
                // Check for "music video" or "MV"
                if (title.contains("music video") || title.contains(" mv ")) score += 10
                
                Log.d(TAG, "searchOfficialMusicVideo: Scored '$item.title' = $score (excluded: $hasExcludedKeyword)")
                
                // Only consider videos with positive score
                if (score > 0) {
                    if (bestScoredVideo == null || score > bestScoredVideo.second) {
                        bestScoredVideo = item to score
                    }
                }
            }
            
            val selectedVideo = bestScoredVideo?.first
            
            if (selectedVideo != null) {
                Log.d(TAG, "searchOfficialMusicVideo: Found video: ${selectedVideo.title} (score=${bestScoredVideo?.second})")
                
                // Extract artist name from SongItem if available
                val channelName = if (selectedVideo is SongItem) {
                    selectedVideo.artists.firstOrNull()?.name ?: artistName
                } else {
                    artistName
                }
                
                return@runCatching VideoSearchResult(
                    videoId = selectedVideo.id,
                    title = selectedVideo.title,
                    channelName = channelName,
                    thumbnailUrl = selectedVideo.thumbnail ?: ""
                )
            }
        }
        
        throw Exception("No music video found for '$songTitle' by '$artistName'")
    }

    /**
     * Get video stream URL, with automatic search fallback
     */
    suspend fun getVideoStreamUrlWithFallback(songTitle: String, artistName: String, videoId: String, isVideoSong: Boolean = true): Result<VideoSearchResult> = runCatching {
        if (isVideoSong) {
            // For video songs, try direct video lookup first since they have real video content
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
        } else {
            Log.d(TAG, "Skipping direct lookup for regular (non-video) song, searching for music video instead")
        }
        
        // For regular songs or when direct lookup failed, search for official music video
        val searchResult = searchOfficialMusicVideo(songTitle, artistName).getOrNull()
            ?: throw Exception("No video available for this song")
        
        searchResult
    }

    /**
     * Extract base MIME type without codec parameters.
     * YouTube returns MIME types like "video/webm; codecs=\"vp9\"" which can cause
     * ExoPlayer NAL parsing errors when set as top-level MIME type.
     */
    private fun sanitizeMimeType(mimeType: String?): String {
        if (mimeType == null) return "video/mp4"
        // Extract only the base type (e.g., "video/mp4" from "video/mp4; codecs=\"avc1.4d401f\"")
        return mimeType.split(";").first().trim()
    }

    /**
     * Enhanced MIME type sanitization to prevent NAL parsing errors
     * - Forces MP4 format for problematic streams
     * - Removes problematic codec parameters that cause parsing issues
     */
    private fun sanitizeMimeTypeForExoPlayer(mimeType: String?): String {
        if (mimeType == null) return "video/mp4"
        
        // Extract base type
        val baseType = mimeType.split(";").first().trim()
        
        // Force MP4 format for problematic streams
        if (baseType.startsWith("video/webm") || baseType.startsWith("video/3gpp")) {
            return "video/mp4"
        }
        
        // Remove problematic codec parameters that cause NAL parsing errors
        return baseType
    }

    /**
     * Check if a MIME type is MP4/H.264 which ExoPlayer handles most reliably
     */
    private fun isMp4Format(mimeType: String?): Boolean {
        val base = sanitizeMimeType(mimeType)
        return base == "video/mp4" || base == "video/3gpp"
    }

    /**
     * Get video stream URL using NewPipeExtractor
     * Prioritizes muxed MP4 streams (video+audio, H.264) for best compatibility
     */
    suspend fun getVideoStreamUrl(videoId: String): Result<VideoStreamResult> = runCatching {
        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
        
        if (streamInfo != null) {
            val muxedVideoStreams = streamInfo.videoStreams // These have both video and audio
            val videoOnlyStreams = streamInfo.videoOnlyStreams // These are video only (no audio)
            
            Log.d(TAG,"FlowVideo: Found ${muxedVideoStreams.size} muxed and ${videoOnlyStreams.size} video-only streams")
            
            // Priority 1: Muxed MP4 streams (best compatibility with ExoPlayer)
            val mp4MuxedStreams = muxedVideoStreams.filter { isMp4Format(it.format?.mimeType) }
            val bestMp4Muxed = findBestStream(mp4MuxedStreams, requireAudio = true)
            if (bestMp4Muxed != null) {
                val url = bestMp4Muxed.content ?: bestMp4Muxed.url
                if (url != null) {
                    Log.d(TAG,"FlowVideo: Using muxed MP4 stream with resolution ${bestMp4Muxed.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMp4Muxed.format?.mimeType))
                }
            }

            // Priority 2: Any muxed stream matching preferred quality
            val bestMuxedStream = findBestStream(muxedVideoStreams, requireAudio = true)
            if (bestMuxedStream != null) {
                val url = bestMuxedStream.content ?: bestMuxedStream.url
                if (url != null) {
                    Log.d(TAG,"FlowVideo: Using muxed stream with resolution ${bestMuxedStream.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMuxedStream.format?.mimeType))
                }
            }
            
            // Priority 3: Video-only MP4 streams
            val mp4VideoOnlyStreams = videoOnlyStreams.filter { isMp4Format(it.format?.mimeType) }
            val bestMp4VideoOnly = findBestStream(mp4VideoOnlyStreams, requireAudio = false)
            if (bestMp4VideoOnly != null) {
                val url = bestMp4VideoOnly.content ?: bestMp4VideoOnly.url
                if (url != null) {
                    Log.d(TAG,"FlowVideo: Using video-only MP4 stream with resolution ${bestMp4VideoOnly.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMp4VideoOnly.format?.mimeType))
                }
            }

            // Priority 4: Any video-only stream
            val bestVideoOnlyStream = findBestStream(videoOnlyStreams, requireAudio = false)
            if (bestVideoOnlyStream != null) {
                val url = bestVideoOnlyStream.content ?: bestVideoOnlyStream.url
                if (url != null) {
                    Log.d(TAG,"FlowVideo: Using video-only stream with resolution ${bestVideoOnlyStream.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestVideoOnlyStream.format?.mimeType))
                }
            }
            
            // Last resort: highest quality muxed stream regardless of format
            if (muxedVideoStreams.isNotEmpty()) {
                val fallbackStream = muxedVideoStreams.maxByOrNull { it.bitrate }
                    if (fallbackStream != null) {
                        val url = fallbackStream.content ?: fallbackStream.url
                        if (url != null) {
                            Log.d(TAG,"FlowVideo: Using fallback muxed stream with resolution ${fallbackStream.resolution}")
                            return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(fallbackStream.format?.mimeType))
                        }
                    }
            }
            
            // Last resort: highest quality video-only stream
            if (videoOnlyStreams.isNotEmpty()) {
                val fallbackStream = videoOnlyStreams.maxByOrNull { it.bitrate }
                if (fallbackStream != null) {
                    val url = fallbackStream.content ?: fallbackStream.url
                    if (url != null) {
                        Log.d(TAG,"FlowVideo: Using fallback video-only stream with resolution ${fallbackStream.resolution}")
                        return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(fallbackStream.format?.mimeType))
                    }
                }
            }
        }

        // Fallback to YouTube player API if NewPipe fails
        Log.d(TAG,"FlowVideo: NewPipe failed, trying YouTube player API")
        val playerResponse = YouTube.player(videoId, client = WEB_REMIX).getOrThrow()
        
        if (playerResponse.playabilityStatus.status != "OK") {
            throw Exception("Playability error: ${playerResponse.playabilityStatus.reason ?: playerResponse.playabilityStatus.status}")
        }

        val streamingData = playerResponse.streamingData ?: throw Exception("No streaming data")

        // Check if we need signature deobfuscation
        val needsDeobfuscation = streamingData.formats?.any { it.signatureCipher != null || it.cipher != null } == true ||
                streamingData.adaptiveFormats.any { it.signatureCipher != null || it.cipher != null }

        // Try muxed formats first (has both audio and video), prefer MP4
        val muxedFormats = streamingData.formats ?: emptyList()
        val mp4MuxedFormats = muxedFormats.filter { isMp4Format(it.mimeType) }
        
        // Try MP4 muxed first, then any muxed
        for (formats in listOf(mp4MuxedFormats, muxedFormats)) {
            val bestFormat = findBestFormat(formats, currentPreferredQuality, requireAudio = true)
            var url = bestFormat?.url
            if (url == null && bestFormat != null && needsDeobfuscation) {
                url = NewPipeExtractor.getStreamUrl(bestFormat, videoId)
            }
            if (url != null) {
                Log.d(TAG,"FlowVideo: Using YouTube muxed format with height ${bestFormat?.height}")
                return@runCatching VideoStreamResult(url, sanitizeMimeType(bestFormat?.mimeType))
            }
        }

        // Try adaptive formats, prefer MP4
        val adaptiveFormats = streamingData.adaptiveFormats
        val mp4AdaptiveFormats = adaptiveFormats.filter { isMp4Format(it.mimeType) }
        
        for (formats in listOf(mp4AdaptiveFormats, adaptiveFormats)) {
            val bestFormat = findBestFormat(formats, currentPreferredQuality, requireAudio = false)
            var url = bestFormat?.url
            if (url == null && bestFormat != null && needsDeobfuscation) {
                url = NewPipeExtractor.getStreamUrl(bestFormat, videoId)
            }
            if (url != null) {
                Log.d(TAG,"FlowVideo: Using YouTube adaptive format with height ${bestFormat?.height}")
                return@runCatching VideoStreamResult(url, sanitizeMimeType(bestFormat?.mimeType))
            }
        }

        throw Exception("No video URL available")
    }

    /**
     * Find best format based on preferred quality
     */
    private fun findBestFormat(
        formats: List<com.auramusic.innertube.models.response.PlayerResponse.StreamingData.Format>,
        quality: VideoQuality,
        requireAudio: Boolean
    ): com.auramusic.innertube.models.response.PlayerResponse.StreamingData.Format? {
        if (formats.isEmpty()) return null

        // Filter to video formats
        val videoFormats = formats.filter { it.isVideo }

        // Try to find exact quality match
        val targetHeight = quality.height
        val exactMatch = videoFormats.find { (it.height ?: 0) == targetHeight }
        if (exactMatch != null) {
            return exactMatch
        }

        // Try to find close quality (within range)
        val closeMatch = videoFormats.find { (it.height ?: 0) in (targetHeight - 120)..(targetHeight + 60) }
        if (closeMatch != null) {
            return closeMatch
        }

        // Fallback to highest bitrate video format
        return videoFormats.maxByOrNull { it.bitrate }
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