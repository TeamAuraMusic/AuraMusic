package com.auramusic.auravideo

data class VideoItem(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String?,
    val thumbnailUrl: String,
    val duration: Int,
    val viewCount: Long,
    val uploadDate: String?,
    val description: String?,
)

data class VideoStream(
    val url: String,
    val mimeType: String,
    val bitrate: Int,
    val width: Int,
    val height: Int,
)