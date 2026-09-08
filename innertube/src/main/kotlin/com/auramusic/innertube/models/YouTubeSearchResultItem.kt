package com.auramusic.innertube.models

/**
 * A single result from a regular YouTube (WEB) search or feed.
 * Unlike the music search, YouTube search mixes videos, channels, playlists,
 * shorts and live streams in the same list, so we model each of them here.
 */
sealed class YouTubeSearchResultItem {
    data class Video(
        val video: YouTubeVideoItem,
    ) : YouTubeSearchResultItem()

    data class Channel(
        val channelId: String,
        val title: String,
        val subscriberCountText: String? = null,
        val videoCountText: String? = null,
        val thumbnails: List<Thumbnail> = emptyList(),
        val description: String? = null,
    ) : YouTubeSearchResultItem()

    data class Playlist(
        val playlistId: String,
        val title: String,
        val itemCountText: String? = null,
        val channelName: String? = null,
        val thumbnails: List<Thumbnail> = emptyList(),
    ) : YouTubeSearchResultItem()
}