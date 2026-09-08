package com.auramusic.innertube.pages

import com.auramusic.innertube.models.Thumbnail
import com.auramusic.innertube.models.YouTubeVideoItem
import com.auramusic.innertube.models.response.YouTubeSearchResponse

data class YouTubeSearchResult(
    val items: List<YouTubeVideoItem>,
    val continuation: String? = null,
)

object YouTubeSearchPage {
    fun fromYouTubeSearchResponse(response: YouTubeSearchResponse): YouTubeSearchResult {
        val items = mutableListOf<YouTubeVideoItem>()
        var continuation: String? = null

        response.contents?.twoColumnSearchResultsRenderer?.primaryContents?.sectionListRenderer?.contents?.forEach { content ->
            content.itemSectionRenderer?.contents?.forEach { item ->
                val videoRenderer = item.videoRenderer ?: return@forEach
                val videoId = videoRenderer.videoId ?: return@forEach
                val title = videoRenderer.title?.runs?.joinToString("") { it.text.orEmpty() } ?: return@forEach
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

                val isLive = videoRenderer.viewCountText?.simpleText?.contains("watching") == true
                    || videoRenderer.lengthText == null

                items.add(
                    YouTubeVideoItem(
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
                )
            }

            content.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token?.let {
                continuation = it
            }
        }

        return YouTubeSearchResult(items, continuation)
    }
}
