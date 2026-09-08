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

        val isLive = viewCount?.contains("watching") == true || duration == null

        return YouTubeVideoItem(
            videoId = videoId,
            title = title,
            channelName = channelName,
            channelId = channelId,
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
}