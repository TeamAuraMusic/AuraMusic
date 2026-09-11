package com.auramusic.innertube.models

/**
 * A community post published on a channel's Posts tab.
 */
data class YouTubeChannelPost(
    val postId: String,
    val authorName: String,
    val authorThumbnail: String?,
    val publishedTimeText: String?,
    val voteCountText: String?,
    val commentCountText: String? = null,
    val contentText: String,
    val imageUrls: List<String>,
)