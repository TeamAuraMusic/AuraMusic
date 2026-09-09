package com.auramusic.innertube.pages

import com.auramusic.innertube.models.Thumbnail
import com.auramusic.innertube.models.YouTubeSearchResultItem
import com.auramusic.innertube.models.YouTubeVideoItem
import com.auramusic.innertube.models.response.YouTubeSearchResponse
data class YouTubeSearchResult(
    val items: List<YouTubeSearchResultItem>,
    val continuation: String? = null,
)

object YouTubeSearchPage {

    fun fromYouTubeSearchResponse(response: YouTubeSearchResponse): YouTubeSearchResult {
        val items = mutableListOf<YouTubeSearchResultItem>()
        var continuation: String? = null

        response.contents?.twoColumnSearchResultsRenderer?.primaryContents?.sectionListRenderer?.contents?.forEach { content ->
            content.itemSectionRenderer?.contents?.forEach { item ->
                when {
                    item.videoRenderer != null -> {
                        fromVideoRenderer(item.videoRenderer)?.let {
                            items.add(YouTubeSearchResultItem.Video(it))
                        }
                    }
                    item.channelRenderer != null -> {
                        fromChannelRenderer(item.channelRenderer)
                            ?.let { items.add(it) }
                    }
                    item.playlistRenderer != null -> {
                        fromPlaylistRenderer(item.playlistRenderer)
                            ?.let { items.add(it) }
                    }
                    item.lockupViewModel != null -> {
                        fromLockupViewModel(item.lockupViewModel)
                            ?.let { items.add(it) }
                    }
                }
            }

            content.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token?.let {
                continuation = it
            }
        }

        return YouTubeSearchResult(items.distinctBy { item ->
            when (item) {
                is YouTubeSearchResultItem.Video -> "video:${item.video.videoId}"
                is YouTubeSearchResultItem.Channel -> "channel:${item.channelId}"
                is YouTubeSearchResultItem.Playlist -> "playlist:${item.playlistId}"
            }
        }, continuation)
    }

    fun fromVideoRenderer(videoRenderer: YouTubeSearchResponse.VideoRenderer): YouTubeVideoItem? {
        val videoId = videoRenderer.videoId ?: return null
        val title = videoRenderer.title?.runs?.joinToString("") { it.text.orEmpty() } ?: return null
        val channelRuns = videoRenderer.longBylineText?.runs
        val channelName = channelRuns?.joinToString("") { it.text.orEmpty() } ?: ""
        val channelId = channelRuns?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.browseId

        val viewCount = videoRenderer.viewCountText?.simpleText
            ?: videoRenderer.viewCountText?.runs?.joinToString("") { it.text.orEmpty() }

        val publishedTime = videoRenderer.publishedTimeText?.simpleText
        val duration = videoRenderer.lengthText?.simpleText
        val description = videoRenderer.descriptionSnippet?.runs?.joinToString("") { it.text.orEmpty() }

        val thumbnails = videoRenderer.thumbnail?.thumbnails?.map {
            Thumbnail(url = it.url.orEmpty(), width = it.width, height = it.height)
        }.orEmpty()

        val channelThumbnailUrl = videoRenderer.channelThumbnailSupportedRenderers
            ?.channelThumbnailWithLinkRenderer?.thumbnail?.thumbnails
            ?.maxByOrNull { it.width ?: 0 }?.url

        val isLive = viewCount?.contains("watching") == true || duration == null

        return YouTubeVideoItem(
            videoId = videoId,
            title = title,
            channelName = channelName,
            channelId = channelId,
            channelThumbnailUrl = channelThumbnailUrl,
            viewCountText = viewCount,
            publishedTimeText = publishedTime,
            durationText = duration,
            thumbnails = thumbnails,
            isLive = isLive,
            description = description,
        )
    }

    fun fromChannelRenderer(channelRenderer: YouTubeSearchResponse.ChannelRenderer): YouTubeSearchResultItem.Channel? {
        val channelId = channelRenderer.channelId ?: return null
        val title = channelRenderer.title?.simpleText ?: return null
        return YouTubeSearchResultItem.Channel(
            channelId = channelId,
            title = title,
            subscriberCountText = channelRenderer.subscriberCountText?.simpleText,
            videoCountText = channelRenderer.videoCountText?.simpleText,
            thumbnails = channelRenderer.thumbnail?.thumbnails?.map {
                Thumbnail(url = it.url.orEmpty(), width = it.width, height = it.height)
            }.orEmpty(),
            description = channelRenderer.descriptionSnippet?.runs?.joinToString("") { it.text.orEmpty() },
        )
    }

    fun fromPlaylistRenderer(playlistRenderer: YouTubeSearchResponse.PlaylistRendererData): YouTubeSearchResultItem.Playlist? {
        val playlistId = playlistRenderer.playlistId ?: return null
        val title = playlistRenderer.title?.simpleText ?: return null
        return YouTubeSearchResultItem.Playlist(
            playlistId = playlistId,
            title = title,
            itemCountText = playlistRenderer.videoCountText?.simpleText,
            channelName = playlistRenderer.longBylineText?.runs?.joinToString("") { it.text.orEmpty() }
                ?.takeIf { it.isNotBlank() },
            thumbnails = playlistRenderer.thumbnail?.thumbnails?.map {
                Thumbnail(url = it.url.orEmpty(), width = it.width, height = it.height)
            }.orEmpty(),
        )
    }

    /**
     * Parses a search `lockupViewModel`. YouTube's newer responses return channels and
     * playlists (and sometimes videos) as lockups; previously only playlist lockups
     * were recognized, so the All filter ended up showing videos only.
     */
    fun fromLockupViewModel(lockupViewModel: kotlinx.serialization.json.JsonElement): YouTubeSearchResultItem? {
        val lockup = Lockup.parse(lockupViewModel) ?: return null
        return when (lockup.contentType) {
            Lockup.TYPE_PLAYLIST -> {
                val playlistId = lockup.playlistId ?: return null
                val title = lockup.title ?: return null
                YouTubeSearchResultItem.Playlist(
                    playlistId = playlistId,
                    title = title,
                    itemCountText = lockup.metadataText.firstOrNull(),
                    channelName = lockup.metadataText.getOrNull(1),
                    thumbnails = lockup.thumbnails,
                )
            }
            Lockup.TYPE_CHANNEL -> {
                val channelId = lockup.channelId ?: return null
                val title = lockup.title ?: return null
                val hasVideos = lockup.metadataText.firstOrNull { it.contains("video", ignoreCase = true) }
                val hasSubscribers = lockup.metadataText.firstOrNull { it.contains("subscriber", ignoreCase = true) }
                YouTubeSearchResultItem.Channel(
                    channelId = channelId,
                    title = title,
                    subscriberCountText = hasSubscribers ?: lockup.metadataText.firstOrNull(),
                    videoCountText = hasVideos,
                    thumbnails = lockup.thumbnails,
                    description = null,
                )
            }
            Lockup.TYPE_VIDEO -> {
                Lockup.toVideoItem(lockup)?.let { YouTubeSearchResultItem.Video(it) }
            }
            else -> null
        }
    }
}