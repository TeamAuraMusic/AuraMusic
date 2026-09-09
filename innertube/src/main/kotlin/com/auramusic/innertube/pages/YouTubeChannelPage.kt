package com.auramusic.innertube.pages

import com.auramusic.innertube.models.Thumbnail
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A regular YouTube channel's header info and one video tab, parsed from the
 * WEB /youtubei/v1/browse response (browseId = channelId, params = tab params).
 */
data class YouTubeChannelPage(
    val channelId: String,
    val title: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val subscriberCountText: String?,
    val videosCountText: String?,
    val description: String?,
    val videos: List<YouTubeVideoItem>,
    val continuation: String? = null,
) {
    companion object {
        // Tab params from youtube.com channel tabs.
        const val VIDEOS_PARAMS = "EgZ2aWRlb3PyBgQKAjoA"
        const val SHORTS_PARAMS = "EgZzaG9ydHPyBgUKA5oBAA%3D%3D"
        const val LIVE_PARAMS = "EgdzdHJlYW1z8gYECgJ6AA%3D%3D"

        fun fromJson(channelId: String, element: JsonElement): YouTubeChannelPage {
            val root = element.jsonObject
            val header = root["header"]?.jsonObject
            val metadata = root["metadata"]?.jsonObject
                ?.get("channelMetadataRenderer")?.jsonObject
            val microformat = root["microformat"]?.jsonObject
                ?.get("microformatDataRenderer")?.jsonObject

            // Header can be pageHeaderRenderer (2024+) or c4TabbedHeaderRenderer (legacy).
            var title: String? = null
            var avatarUrl: String? = null
            var bannerUrl: String? = null
            var subscriberCountText: String? = null
            var videosCountText: String? = null

            header?.get("pageHeaderRenderer")?.jsonObject?.let { pageHeader ->
                val content = pageHeader["content"]?.jsonObject
                    ?.get("pageHeaderViewModel")?.jsonObject
                val metadataRows = content?.get("metadata")?.jsonObject
                    ?.get("contentMetadataViewModel")?.jsonObject
                    ?.get("metadataRows")?.jsonArray
                metadataRows?.forEach { row ->
                    val parts = row.jsonObject["metadataParts"]?.jsonArray
                    parts?.forEach { part ->
                        val text = part.jsonObject["text"]?.jsonObject
                            ?.get("content")?.jsonPrimitive?.content
                        if (text != null) {
                            when {
                                text.contains("subscriber", ignoreCase = true) ->
                                    subscriberCountText = text
                                text.contains("video", ignoreCase = true) ->
                                    videosCountText = text
                            }
                        }
                    }
                }
                val image = content?.get("image")?.jsonObject
                    ?.get("decoratedAvatarViewModel")?.jsonObject
                    ?.get("avatar")?.jsonObject
                    ?.get("avatarViewModel")?.jsonObject
                    ?.get("image")?.jsonObject
                    ?.get("sources")?.jsonArray
                    ?.mapNotNull { it.jsonObject["url"]?.jsonPrimitive?.content }
                avatarUrl = image?.lastOrNull()
            }
            header?.get("c4TabbedHeaderRenderer")?.jsonObject?.let { c4 ->
                title = c4["title"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                avatarUrl = c4["avatar"]?.jsonObject?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                bannerUrl = c4["banner"]?.jsonObject?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                subscriberCountText = c4["subscriberCountText"]?.jsonObject
                    ?.get("simpleText")?.jsonPrimitive?.content
                videosCountText = c4["videosCountText"]?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            }

            metadata?.let { meta ->
                if (title == null) {
                    title = meta["title"]?.jsonPrimitive?.content
                }
                if (avatarUrl == null) {
                    avatarUrl = meta["avatar"]?.jsonObject?.get("thumbnails")?.jsonArray
                        ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                }
            }
            microformat?.let { micro ->
                if (thumbnailUrlHelper(micro) != null && bannerUrl == null) {
                    bannerUrl = thumbnailUrlHelper(micro)
                }
            }

            val description = metadata?.get("description")?.jsonPrimitive?.content

            // Videos live under either tabRenderer or richGridRenderer.
            val videos = mutableListOf<YouTubeVideoItem>()
            var continuation: String? = null
            root["contents"]?.jsonObject?.get("twoColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray?.forEach { tabEl ->
                    val tabContent = tabEl.jsonObject["tabRenderer"]?.jsonObject
                        ?.get("content")?.jsonObject ?: return@forEach
                    // Videos/Live use sectionListRenderer; Shorts uses richGridRenderer.
                    val sections: List<JsonElement> =
                        tabContent["sectionListRenderer"]?.jsonObject
                            ?.get("contents")?.jsonArray?.toList()
                            ?: tabContent["richGridRenderer"]?.jsonObject
                                ?.get("contents")?.jsonArray?.toList()
                            ?: emptyList()
                    sections.forEach { sectionEl ->
                        val itemSectionContents = sectionEl.jsonObject["itemSectionRenderer"]
                            ?.jsonObject?.get("contents")?.jsonArray?.toList()
                        val items: List<JsonElement> = when {
                            itemSectionContents != null -> itemSectionContents
                            sectionEl.jsonObject["richItemRenderer"] != null ->
                                listOf(sectionEl)
                            else -> emptyList()
                        }
                        items.forEach { itemEl ->
                            val richContent = itemEl.jsonObject["richItemRenderer"]
                                ?.jsonObject?.get("content")?.jsonObject
                            val directVideoRenderer = itemEl.jsonObject["videoRenderer"]?.jsonObject
                            val renderer = when {
                                directVideoRenderer != null -> directVideoRenderer
                                itemEl.jsonObject["richItemRenderer"]?.jsonObject != null ->
                                    richContent?.get("videoRenderer")?.jsonObject
                                        ?: richContent?.get("reelItemRenderer")?.jsonObject
                                else -> null
                            } ?: return@forEach
                            parseVideoRenderer(renderer)?.let(videos::add)
                        }
                        // Continuation token sits either in the section or the grid.
                        collectContinuation(sectionEl)?.let { continuation = it }
                    }
                }

            return YouTubeChannelPage(
                channelId = channelId,
                title = title.orEmpty(),
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                subscriberCountText = subscriberCountText,
                videosCountText = videosCountText,
                description = description,
                videos = videos,
                continuation = continuation,
            )
        }

        private fun parseVideoRenderer(renderer: JsonObject): YouTubeVideoItem? {
            val videoId = renderer["videoId"]?.jsonPrimitive?.content ?: return null
            val title = renderer["title"]?.jsonObject?.let { textOf(it) } ?: return null
            val byline = renderer["longBylineText"]?.jsonObject
                ?: renderer["shortBylineText"]?.jsonObject
            val channelName = byline?.let { textOf(it) }.orEmpty()
            val channelId = byline?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject?.get("navigationEndpoint")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.content
            val viewCount = renderer["viewCountText"]?.jsonObject?.let { textOf(it) }
            val published = renderer["publishedTimeText"]?.jsonObject?.let { textOf(it) }
            val lengthText = renderer["lengthText"]?.jsonObject?.let { textOf(it) }
            val thumbnails = renderer["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                ?.mapNotNull { thumb ->
                    val url = thumb.jsonObject["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    Thumbnail(
                        url = url,
                        width = thumb.jsonObject["width"]?.jsonPrimitive?.content?.toIntOrNull(),
                        height = thumb.jsonObject["height"]?.jsonPrimitive?.content?.toIntOrNull(),
                    )
                }.orEmpty()
            val isLive = renderer["badges"]?.jsonArray?.any { badge ->
                badge.jsonObject["metadataBadgeRenderer"]?.jsonObject
                    ?.get("style")?.jsonPrimitive?.content == "BADGE_STYLE_TYPE_LIVE"
            } == true || viewCount?.contains("watching", ignoreCase = true) == true
            return YouTubeVideoItem(
                videoId = videoId,
                title = title,
                channelName = channelName,
                channelId = channelId,
                viewCountText = viewCount,
                publishedTimeText = published,
                durationText = lengthText,
                thumbnails = thumbnails,
                isLive = isLive,
            )
        }

        private fun textOf(obj: JsonObject): String? {
            obj["simpleText"]?.jsonPrimitive?.content?.let { return it }
            return obj["runs"]?.jsonArray
                ?.joinToString("") { run ->
                    run.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
                }?.takeIf { it.isNotBlank() }
        }

        private fun thumbnailUrlHelper(obj: JsonObject): String? =
            obj["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content

        private fun collectContinuation(element: JsonElement): String? {
            val direct = element.jsonObject["continuationItemRenderer"]?.jsonObject
                ?: element.jsonObject["itemSectionRenderer"]?.jsonObject?.get("contents")?.jsonArray
                    ?.firstOrNull { it.jsonObject["continuationItemRenderer"] != null }
                    ?.jsonObject?.get("continuationItemRenderer")?.jsonObject
                ?: return null
            return direct["continuationEndpoint"]?.jsonObject
                ?.get("continuationCommand")?.jsonObject
                ?.get("token")?.jsonPrimitive?.content
        }
    }
}
