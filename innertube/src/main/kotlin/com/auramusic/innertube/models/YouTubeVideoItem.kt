package com.auramusic.innertube.models

data class YouTubeVideoItem(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String? = null,
    val channelThumbnailUrl: String? = null,
    val viewCountText: String? = null,
    val publishedTimeText: String? = null,
    val durationText: String? = null,
    val thumbnails: List<Thumbnail> = emptyList(),
    val isLive: Boolean = false,
    val description: String? = null,
)
