package com.auramusic.auravideo

import android.util.Log
import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.auramusic.innertube.YouTube.SearchFilter
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.YTItem

object AuraVideo {
    private const val TAG = "AuraVideo"
    data class VideoStreamResult(
        val url: String,
        val mimeType: String
    )

    data class AudioPreviewSource(
        val url: String,
        val mimeType: String,
        val durationMs: Long = 0L,
        /** Required headers for clients that return a fingerprint-bound stream URL. */
        val requestHeaders: Map<String, String> = emptyMap(),
    )

    /**
     * A richer description of how to play a video. YouTube only offers muxed
     * (audio+video) streams up to 720p, so to reach 1080p or higher we must
     * combine a video-only stream with a matching audio-only stream.
     */
    sealed class VideoStreamSource {
        /** Single URL that contains both audio and video (muxed). */
        data class Single(
            val url: String,
            val mimeType: String,
        ) : VideoStreamSource()

        /** Separate video and audio streams that must be merged for playback. */
        data class Merged(
            val videoUrl: String,
            val videoMimeType: String,
            val audioUrl: String,
            val audioMimeType: String,
            val height: Int,
        ) : VideoStreamSource()
    }

    data class VideoSearchResult(
        val videoId: String,
        val title: String,
        val channelName: String,
        val thumbnailUrl: String
    )

    /**
     * The resolutions worth asking for. A height of zero means "whatever this video
     * happens to have", which is a real choice rather than a gap in the list: most
     * videos stop at 1080p, so a fixed 1080p request spends its fallback on every
     * other video.
     */
    enum class VideoQuality(val height: Int, val label: String) {
        QUALITY_AUTO(0, "Auto"),
        QUALITY_144P(144, "144p"),
        QUALITY_240P(240, "240p"),
        QUALITY_360P(360, "360p"),
        QUALITY_480P(480, "480p"),
        QUALITY_720P(720, "720p"),
        QUALITY_1080P(1080, "1080p"),
        QUALITY_1440P(1440, "1440p"),
        QUALITY_2160P(2160, "2160p");

        val isAuto: Boolean get() = this == QUALITY_AUTO
    }

    /**
     * Video codecs. Each is published at a different set of resolutions and costs the
     * device a different amount to decode, so which one is best is a property of the
     * hardware rather than of the video.
     */
    enum class VideoCodec(val label: String) {
        CODEC_AUTO("Auto"),
        CODEC_H264("H.264"),
        CODEC_VP9("VP9"),
        CODEC_AV1("AV1");

        val isAuto: Boolean get() = this == CODEC_AUTO
    }

    // Default preferred quality - 720p
    var currentPreferredQuality: VideoQuality = VideoQuality.QUALITY_720P

    /** Default preferred codec - decided per stream from what the device can decode. */
    var currentPreferredCodec: VideoCodec = VideoCodec.CODEC_AUTO

    /**
     * Set preferred video quality
     */
    fun setPreferredVideoQuality(quality: VideoQuality) {
        currentPreferredQuality = quality
    }

    /**
     * Set preferred video codec
     */
    fun setPreferredCodec(codec: VideoCodec) {
        currentPreferredCodec = codec
    }

    /**
     * Resolutions actually on offer for a video, coarsest matching: a 1080-high stream
     * satisfies a request for 1080p, and a request below it. Used by the quality pickers
     * so the choices on offer are the ones this video can actually deliver.
     */
    fun availableQualities(streams: List<org.schabi.newpipe.extractor.stream.VideoStream>): List<VideoQuality> {
        val tiers = mutableSetOf<Int>()
        for (stream in streams) {
            val height = stream.height
            if (height <= 0) continue
            tiers.add(
                VideoQuality.entries
                    .filter { !it.isAuto && it.height <= height }
                    .maxByOrNull { it.height }
                    ?.height ?: continue,
            )
        }
        return VideoQuality.entries.filter { !it.isAuto && it.height in tiers }.sortedBy { it.height }
    }

    /**
     * Whether the device has a hardware decoder for a video codec. Software decoding a modern
     * codec at 720p+ pins a big CPU core for the whole session - which is exactly what makes
     * the rest of the app stutter - so the stream picker needs to know before it prefers one.
     */
    private fun hasHardwareDecoderFor(mimeType: String): Boolean = try {
        android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            .codecInfos
            .filter { !it.isEncoder }
            .filter { info -> info.supportedTypes.any { type -> type.equals(mimeType, true) } }
            .any { info ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    !info.isSoftwareOnly
                } else {
                    !info.name.startsWith("OMX.google.") && !info.name.startsWith("c2.android.")
                }
            }
    } catch (_: Exception) {
        // If the codec list cannot be read, assume hardware support so behaviour is unchanged.
        true
    }

    private val hardwareVp9 by lazy { hasHardwareDecoderFor("video/x-vnd.on2.vp9") }
    private val hardwareAv1 by lazy { hasHardwareDecoderFor("video/av01") }

    /**
     * Which family a MIME type belongs to, in a fixed order so the numbers compare.
     * HEVC is in the list although it is not offered as a choice: YouTube publishes it,
     * and it has to rank somewhere rather than falling through as "unknown".
     */
    private fun codecFamily(mimeType: String?): Int {
        val mime = mimeType?.lowercase() ?: return -1
        return when {
            "avc1" in mime || "avc3" in mime -> 0
            "hev1" in mime || "hvc1" in mime -> 1
            "vp09" in mime || "vp9" in mime -> 2
            "av01" in mime -> 3
            else -> -1
        }
    }

    private val VideoCodec.family: Int
        get() = when (this) {
            VideoCodec.CODEC_H264 -> 0
            VideoCodec.CODEC_VP9 -> 2
            VideoCodec.CODEC_AV1 -> 3
            VideoCodec.CODEC_AUTO -> -1
        }

    /**
     * Lower is better. H.264 is hardware-decoded on effectively every Android device, so it is
     * the default; VP9 and AV1 are only worth their better compression when the device can
     * decode them in hardware - otherwise they cost a CPU core for the entire playback.
     *
     * An explicitly chosen codec outranks all of that, including on a device that would
     * decode it slowly. It is a preference the user set on purpose, and quietly handing
     * back something else would leave them with no way to get the stream they asked for.
     */
    private fun codecRank(stream: org.schabi.newpipe.extractor.stream.VideoStream, codec: VideoCodec): Int {
        val family = codecFamily(stream.format?.mimeType)
        if (family < 0) return 2
        if (!codec.isAuto) return if (family == codec.family) 0 else 1
        return when (family) {
            0 -> 0
            1 -> 1
            2 -> if (hardwareVp9) 1 else 3
            3 -> if (hardwareAv1) 1 else 4
            else -> 2
        }
    }

    /** Same ranking, for a stream described by a MIME type rather than an extracted stream. */
    private fun codecRank(mimeType: String?, codec: VideoCodec): Int {
        val family = codecFamily(mimeType)
        if (family < 0) return 2
        if (!codec.isAuto) return if (family == codec.family) 0 else 1
        return when (family) {
            0 -> 0
            1 -> 1
            2 -> if (hardwareVp9) 1 else 3
            3 -> if (hardwareAv1) 1 else 4
            else -> 2
        }
    }

    /**
     * Frame-rate tier of a stream. Anything above the standard ladder ("720p60") costs twice
     * the decode work and drives the video surface at 60 Hz, and the rest of the UI has to
     * share its frame budget with that. A plain "720p" choice means the standard rate; high
     * frame rate is an explicit quality, not a silent upgrade.
     */
    private fun frameRateRank(stream: org.schabi.newpipe.extractor.stream.VideoStream): Int =
        if (stream.fps > 30) 1 else 0

    /**
     * Highest resolution first, then the standard frame rate, then the cheapest codec to
     * decode, then highest bitrate.
     */
    private fun bestOf(
        candidates: List<org.schabi.newpipe.extractor.stream.VideoStream>,
        codec: VideoCodec,
    ): org.schabi.newpipe.extractor.stream.VideoStream? =
        candidates.sortedWith(
            compareByDescending<org.schabi.newpipe.extractor.stream.VideoStream> { it.height }
                .thenBy { frameRateRank(it) }
                .thenBy { codecRank(it, codec) }
                .thenByDescending { it.bitrate },
        ).firstOrNull()

    /**
     * Find the best stream matching the preferred quality
     * Falls back to lower quality if preferred not available
     */
    private fun findBestStream(
        streams: List<org.schabi.newpipe.extractor.stream.VideoStream>,
        requireAudio: Boolean,
        quality: VideoQuality = currentPreferredQuality,
        codec: VideoCodec = currentPreferredCodec,
    ): org.schabi.newpipe.extractor.stream.VideoStream? {
        if (streams.isEmpty()) return null

        // A request for audio means the muxed streams, which carry sound; a video-only
        // stream would play silent, so it is never returned here unless nothing else is
        // on offer at all.
        val candidates = if (requireAudio) streams.filter { !it.isVideoOnly() }.ifEmpty { streams } else streams

        // "Auto" takes the best the video has, which is the whole point of asking.
        if (quality.isAuto) return bestOf(candidates, codec)

        // Tiers at or below the one asked for, highest first: a request is a ceiling, so
        // a video that tops out at 480p answers a 1080p request with 480p rather than
        // nothing.
        val ladder = VideoQuality.entries
            .filter { !it.isAuto && it.height <= quality.height }
            .sortedByDescending { it.height }
        for (tier in ladder) {
            val matching = candidates.filter { it.height in 1..tier.height }
            if (matching.isNotEmpty()) return bestOf(matching, codec)
        }
        return bestOf(candidates, codec)
    }

    /**
     * Search for an official music video for a song
     */
    suspend fun searchOfficialMusicVideo(songTitle: String, artistName: String): Result<VideoSearchResult> = runCatching {
        val songTitleLower = songTitle.lowercase().trim()
        val artistNameLower = artistName.lowercase().trim()
        
        // Normalize: strip bracketed/parenthesized content for comparison
        val cleanSongTitle = songTitleLower.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
        
        // Keywords to exclude (lyrics videos, live performances, etc.)
        val excludeKeywords = listOf(
            "lyrics", "lyric video", "lyrical",
            "live", "concert", "performance",
            "reaction", "react", "cover",
            "remix", "instrumental",
            "visualizer", "audio",
            "karaoke", "sped up", "slowed",
            "nightcore", "behind the scenes",
            "making of", "fan made", "shorts",
            "teaser", "trailer"
        )
        
        // Artist name tokens for matching
        val artistTokens = artistNameLower.split(" ", ",", "&").map { it.trim() }
            .filter { it.length > 2 && it !in listOf("the", "and", "feat", "ft", "with", "de", "la") }
        
        // Try multiple search queries and keep the best result across ALL queries
        val searchQueries = listOf(
            "$songTitle $artistName official music video",
            "$songTitle $artistName music video",
            "$songTitle $artistName"
        )
        
        var overallBest: Pair<YTItem, Int>? = null
        
        for (searchQuery in searchQueries) {
            Log.d(TAG, "searchOfficialMusicVideo: Searching with query: $searchQuery")
            val searchResult = runCatching { YouTube.search(searchQuery, SearchFilter.FILTER_VIDEO).getOrThrow() }
            
            if (searchResult.isFailure) {
                Log.d(TAG, "searchOfficialMusicVideo: Search failed for query: $searchQuery")
                continue
            }
            
            val items = searchResult.getOrNull()?.items ?: continue
            if (items.isEmpty()) continue
            
            for (item in items) {
                val title = item.title.lowercase()
                val cleanTitle = title.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
                val channelName = (item as? SongItem)?.artists?.firstOrNull()?.name?.lowercase() ?: ""
                
                var score = 0
                
                // Check if this is an actual video song (has video content)
                val isActualVideoSong = item is SongItem && item.isVideoSong
                if (isActualVideoSong) score += 30
                
                // Check for exclude keywords (lyrics, live, etc.)
                val hasExcludedKeyword = excludeKeywords.any { title.contains(it) }
                if (hasExcludedKeyword) {
                    score -= 50
                }
                
                // Title matching - require the song title to appear in the video title
                val titleMatch = cleanTitle.contains(cleanSongTitle)
                if (titleMatch) score += 40
                
                // Artist matching - check title and channel name
                val artistMatch = title.contains(artistNameLower) || 
                    channelName.contains(artistNameLower) ||
                    (artistTokens.isNotEmpty() && artistTokens.all { token -> 
                        title.contains(token) || channelName.contains(token)
                    })
                if (artistMatch) score += 35
                
                // Both title AND artist must match for a confident result
                if (!titleMatch && !artistMatch) {
                    score -= 60
                } else if (!titleMatch) {
                    score -= 25
                } else if (!artistMatch) {
                    score -= 30
                }
                
                // Prefer official/VEVO channels
                if (channelName.contains("vevo") || channelName.endsWith("official")) score += 25
                if (title.contains("official")) score += 10
                if (title.contains("music video") || title.contains(" mv ") || title.endsWith(" mv")) score += 10
                
                Log.d(TAG, "searchOfficialMusicVideo: Scored '${item.title}' = $score (title=$titleMatch, artist=$artistMatch, excluded=$hasExcludedKeyword)")
                
                // Only consider videos with positive score and require minimum confidence
                if (score > 10) {
                    if (overallBest == null || score > overallBest.second) {
                        overallBest = item to score
                    }
                }
            }
            
            // If we found a high-confidence match, no need to try more queries
            if (overallBest != null && overallBest.second >= 80) {
                Log.d(TAG, "searchOfficialMusicVideo: High confidence match found, skipping remaining queries")
                break
            }
        }
        
        val selectedVideo = overallBest?.first
        
        if (selectedVideo != null) {
            Log.d(TAG, "searchOfficialMusicVideo: Found video: ${selectedVideo.title} (score=${overallBest?.second})")
            
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
        
        throw Exception("No music video found for '$songTitle' by '$artistName'")
    }

    /**
     * Get video stream URL, with automatic search fallback
     */
    suspend fun getVideoStreamUrlWithFallback(songTitle: String, artistName: String, videoId: String, isVideoSong: Boolean = true): Result<VideoSearchResult> = runCatching {
        if (isVideoSong) {
            // For video songs, keep the original video id and let the caller do
            // the single authoritative stream resolution. Probing here causes a
            // duplicate NewPipe extraction before video mode can show anything.
            return@runCatching VideoSearchResult(
                videoId = videoId,
                title = songTitle,
                channelName = artistName,
                thumbnailUrl = ""
            )
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
        // Keep the real container type. Lying about WebM/3GPP as MP4 can make
        // ExoPlayer select the wrong extractor and produce a black/no-show video.
        return sanitizeMimeType(mimeType)
    }

    /**
     * Check if a MIME type is MP4/H.264 which ExoPlayer handles most reliably
     */
    private fun isMp4Format(mimeType: String?): Boolean {
        val base = sanitizeMimeType(mimeType)
        return base == "video/mp4" || base == "video/3gpp"
    }

    /**
     * Pick the best audio-only stream — prefer the highest-bitrate MP4/M4A
     * (AAC) for ExoPlayer compatibility, falling back to anything else.
     */
    private fun pickBestAudioStream(
        audioStreams: List<org.schabi.newpipe.extractor.stream.AudioStream>
    ): org.schabi.newpipe.extractor.stream.AudioStream? {
        if (audioStreams.isEmpty()) return null
        val mp4Audios = audioStreams.filter {
            val mime = it.format?.mimeType?.split(";")?.first()?.trim()
            mime == "audio/mp4" || mime == "audio/m4a"
        }
        return mp4Audios.maxByOrNull { it.averageBitrate }
            ?: audioStreams.maxByOrNull { it.averageBitrate }
    }

    suspend fun getAudioPreviewSource(videoId: String): Result<AudioPreviewSource> = runCatching {
        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
            ?: error("Could not resolve audio preview for $videoId")
        val stream = pickBestAudioStream(streamInfo.audioStreams)
            ?: error("No audio-only stream available for $videoId")
        val url = stream.content?.takeIf(String::isNotBlank)
            ?: stream.url?.takeIf(String::isNotBlank)
            ?: error("Resolved audio preview URL was blank for $videoId")
        val mimeType = stream.format?.mimeType?.substringBefore(';')?.trim()
        require(mimeType?.startsWith("audio/", ignoreCase = true) == true) {
            "Resolved preview stream was not audio-only for $videoId"
        }
        AudioPreviewSource(
            url = url,
            mimeType = mimeType,
            durationMs = streamInfo.duration.coerceAtLeast(0L) * 1000L,
        )
    }

    /**
     * Resolve a video as either a single muxed stream or a pair of video-only +
     * audio-only streams that the caller will merge at the media-source layer.
     *
     * YouTube serves muxed streams only up to 720p (often only 360p in
     * practice). For 1080p and above the only option is a video-only DASH
     * stream paired with a separate audio-only stream — that's why this
     * function returns a [VideoStreamSource.Merged] in those cases.
     */
    suspend fun getVideoStreamSource(
        videoId: String,
        quality: VideoQuality = currentPreferredQuality,
        codec: VideoCodec = currentPreferredCodec,
    ): Result<VideoStreamSource> = runCatching {
        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)

        if (streamInfo != null) {
            val muxedVideoStreams = streamInfo.videoStreams
            val videoOnlyStreams = streamInfo.videoOnlyStreams
            val audioStreams = streamInfo.audioStreams

            Log.d(
                TAG,
                "getVideoStreamSource: ${muxedVideoStreams.size} muxed, " +
                        "${videoOnlyStreams.size} video-only, ${audioStreams.size} audio-only " +
                        "(preferred=${quality.label}, ${codec.label})"
            )

            fun mergedSource(): VideoStreamSource.Merged? {
                val videoOnly = findBestStream(videoOnlyStreams, requireAudio = false, quality = quality, codec = codec)
                val audioOnly = pickBestAudioStream(audioStreams)
                val videoUrl = videoOnly?.content ?: videoOnly?.url
                val audioUrl = audioOnly?.content ?: audioOnly?.url
                if (videoOnly != null && audioOnly != null && !videoUrl.isNullOrBlank() && !audioUrl.isNullOrBlank()) {
                    Log.d(
                        TAG,
                        "getVideoStreamSource: Using MERGED video-only ${videoOnly.resolution} " +
                                "+ audio ${audioOnly.averageBitrate}bps"
                    )
                    return VideoStreamSource.Merged(
                        videoUrl = videoUrl,
                        videoMimeType = sanitizeMimeTypeForExoPlayer(videoOnly.format?.mimeType),
                        audioUrl = audioUrl,
                        audioMimeType = sanitizeMimeType(audioOnly.format?.mimeType),
                        height = videoOnly.height,
                    )
                }
                return null
            }

            fun singleSource(): VideoStreamSource.Single? {
                val streamGroups = listOf(
                    muxedVideoStreams.filter { isMp4Format(it.format?.mimeType) },
                    muxedVideoStreams,
                )
                for (streams in streamGroups) {
                    val stream = findBestStream(streams, requireAudio = true, quality = quality, codec = codec)
                    val url = stream?.content ?: stream?.url
                    if (stream != null && !url.isNullOrBlank()) {
                        Log.d(TAG, "getVideoStreamSource: Using SINGLE muxed stream ${stream.resolution}")
                        return VideoStreamSource.Single(
                            url = url,
                            mimeType = sanitizeMimeTypeForExoPlayer(stream.format?.mimeType),
                        )
                    }
                }
                return null
            }

            // Above 720p the merged path is the only one on offer, because muxed streams
            // cap out below that - and "auto" means taking the best available, which is
            // the same path for the same reason.
            if (quality.isAuto || quality.height > 720) {
                mergedSource()?.let { return@runCatching it }
                Log.d(TAG, "getVideoStreamSource: Merged path unavailable, falling back to muxed/single")
            }

            singleSource()?.let { return@runCatching it }

            // Some videos only expose separate streams. Merge them even for
            // <=720p rather than handing ExoPlayer a silent video-only URL.
            mergedSource()?.let { return@runCatching it }
        }

        // Fall back to the older resolver, which includes the YouTube player API
        // fallback if NewPipe cannot extract stream info at all.
        val singleResult = runCatching { getVideoStreamUrl(videoId).getOrThrow() }
            .getOrNull()
            ?: throw Exception("No playable video stream found")

        VideoStreamSource.Single(singleResult.url, singleResult.mimeType)
    }

    /**
     * Get video stream URL using NewPipeExtractor
     * Prioritizes muxed MP4 streams (video+audio, H.264) for best compatibility
     */
    suspend fun getVideoStreamUrl(
        videoId: String,
        quality: VideoQuality = currentPreferredQuality,
        codec: VideoCodec = currentPreferredCodec,
    ): Result<VideoStreamResult> = runCatching {
        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
        
        if (streamInfo != null) {
            val muxedVideoStreams = streamInfo.videoStreams // These have both video and audio
            val videoOnlyStreams = streamInfo.videoOnlyStreams // These are video only (no audio)
            
            Log.d(TAG,"AuraVideo: Found ${muxedVideoStreams.size} muxed and ${videoOnlyStreams.size} video-only streams")
            
            // Priority 1: Muxed MP4 streams (best compatibility with ExoPlayer)
            val mp4MuxedStreams = muxedVideoStreams.filter { isMp4Format(it.format?.mimeType) }
            val bestMp4Muxed = findBestStream(mp4MuxedStreams, requireAudio = true, quality = quality, codec = codec)
            if (bestMp4Muxed != null) {
                val url = bestMp4Muxed.content ?: bestMp4Muxed.url
                if (url != null) {
                    Log.d(TAG,"AuraVideo: Using muxed MP4 stream with resolution ${bestMp4Muxed.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMp4Muxed.format?.mimeType))
                }
            }

            // Priority 2: Any muxed stream matching preferred quality
            val bestMuxedStream = findBestStream(muxedVideoStreams, requireAudio = true, quality = quality, codec = codec)
            if (bestMuxedStream != null) {
                val url = bestMuxedStream.content ?: bestMuxedStream.url
                if (url != null) {
                    Log.d(TAG,"AuraVideo: Using muxed stream with resolution ${bestMuxedStream.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMuxedStream.format?.mimeType))
                }
            }
            
            // Priority 3: Video-only MP4 streams
            val mp4VideoOnlyStreams = videoOnlyStreams.filter { isMp4Format(it.format?.mimeType) }
            val bestMp4VideoOnly = findBestStream(mp4VideoOnlyStreams, requireAudio = false, quality = quality, codec = codec)
            if (bestMp4VideoOnly != null) {
                val url = bestMp4VideoOnly.content ?: bestMp4VideoOnly.url
                if (url != null) {
                    Log.d(TAG,"AuraVideo: Using video-only MP4 stream with resolution ${bestMp4VideoOnly.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestMp4VideoOnly.format?.mimeType))
                }
            }

            // Priority 4: Any video-only stream
            val bestVideoOnlyStream = findBestStream(videoOnlyStreams, requireAudio = false, quality = quality, codec = codec)
            if (bestVideoOnlyStream != null) {
                val url = bestVideoOnlyStream.content ?: bestVideoOnlyStream.url
                if (url != null) {
                    Log.d(TAG,"AuraVideo: Using video-only stream with resolution ${bestVideoOnlyStream.resolution}")
                    return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(bestVideoOnlyStream.format?.mimeType))
                }
            }
            
            // Last resort: highest quality muxed stream regardless of format
            if (muxedVideoStreams.isNotEmpty()) {
                val fallbackStream = muxedVideoStreams.maxByOrNull { it.bitrate }
                    if (fallbackStream != null) {
                        val url = fallbackStream.content ?: fallbackStream.url
                        if (url != null) {
                            Log.d(TAG,"AuraVideo: Using fallback muxed stream with resolution ${fallbackStream.resolution}")
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
                        Log.d(TAG,"AuraVideo: Using fallback video-only stream with resolution ${fallbackStream.resolution}")
                        return@runCatching VideoStreamResult(url, sanitizeMimeTypeForExoPlayer(fallbackStream.format?.mimeType))
                    }
                }
            }
        }

        // Fallback to YouTube player API if NewPipe fails
        Log.d(TAG,"AuraVideo: NewPipe failed, trying YouTube player API")
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
            val bestFormat = findBestFormat(formats, quality, requireAudio = true, codec = codec)
            var url = bestFormat?.url
            if (url == null && bestFormat != null && needsDeobfuscation) {
                url = NewPipeExtractor.getStreamUrl(bestFormat, videoId)
            }
            if (url != null) {
                Log.d(TAG,"AuraVideo: Using YouTube muxed format with height ${bestFormat?.height}")
                return@runCatching VideoStreamResult(url, sanitizeMimeType(bestFormat?.mimeType))
            }
        }

        // Try adaptive formats, prefer MP4
        val adaptiveFormats = streamingData.adaptiveFormats
        val mp4AdaptiveFormats = adaptiveFormats.filter { isMp4Format(it.mimeType) }
        
        for (formats in listOf(mp4AdaptiveFormats, adaptiveFormats)) {
            val bestFormat = findBestFormat(formats, quality, requireAudio = false, codec = codec)
            var url = bestFormat?.url
            if (url == null && bestFormat != null && needsDeobfuscation) {
                url = NewPipeExtractor.getStreamUrl(bestFormat, videoId)
            }
            if (url != null) {
                Log.d(TAG,"AuraVideo: Using YouTube adaptive format with height ${bestFormat?.height}")
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
        requireAudio: Boolean,
        codec: VideoCodec = currentPreferredCodec,
    ): com.auramusic.innertube.models.response.PlayerResponse.StreamingData.Format? {
        val videoFormats = formats.filter { it.isVideo && (it.height ?: 0) > 0 }
        if (videoFormats.isEmpty()) return null

        // A request is a ceiling, so anything above it is only reached when the video
        // offers nothing at or below - which is what "auto" asks for from the start.
        val withinCeiling = videoFormats.filter { quality.isAuto || (it.height ?: 0) <= quality.height }
        val pool = withinCeiling.ifEmpty { videoFormats }

        return pool.sortedWith(
            compareByDescending<com.auramusic.innertube.models.response.PlayerResponse.StreamingData.Format> {
                it.height ?: 0
            }
                .thenBy { codecRank(it.mimeType, codec) }
                .thenByDescending { it.bitrate },
        ).firstOrNull()
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
