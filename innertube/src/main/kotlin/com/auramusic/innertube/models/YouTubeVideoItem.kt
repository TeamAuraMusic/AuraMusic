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
    /**
     * Every channel the byline links to, in order. Collaborative and music videos can credit
     * two channels at once ("A & B"); the single [channelId] above only ever points at the
     * first of them.
     */
    val channels: List<Artist> = emptyList(),
)
