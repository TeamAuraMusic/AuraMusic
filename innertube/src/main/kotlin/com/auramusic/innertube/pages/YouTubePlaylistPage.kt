package com.auramusic.innertube.pages

import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A slice of a public YouTube playlist.
 *
 * Only the first few entries are ever needed: deciding whether a playlist is music
 * only requires sampling a handful of its videos, so this deliberately stops early
 * instead of parsing a full listing that can run to thousands of items.
 */
data class YouTubePlaylistPage(
    val playlistId: String,
    val title: String?,
    val videos: List<YouTubeVideoItem>,
    val continuation: String? = null,
) {
    companion object {
        fun fromJson(playlistId: String, element: JsonElement, limit: Int): YouTubePlaylistPage {
            val root = element.jsonObject
            val browse =
                root["contents"]?.jsonObject?.get("twoColumnBrowseResultsRenderer")?.jsonObject
                    ?: return YouTubePlaylistPage(playlistId, null, emptyList())

            val tabs = browse["tabs"]?.jsonArray.orEmpty()
            val sections = tabs.mapNotNull { tab ->
                tab.jsonObject["tabRenderer"]?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("sectionListRenderer")?.jsonObject
            }

            val title = sections.firstNotNullOfOrNull { section ->
                section["header"]?.jsonObject
                    ?.get("playlistHeaderRenderer")?.jsonObject
                    ?.get("title")?.jsonObject?.let { textOf(it) }            }

            val videos = mutableListOf<YouTubeVideoItem>()
            var continuation: String? = null

            for (section in sections) {
                val contents = section["contents"]?.jsonArray.orEmpty()
                for (item in contents) {
                    val itemRenderer =
                        item.jsonObject["itemSectionRenderer"]?.jsonObject
                            ?: item.jsonObject["playlistVideoListRenderer"]?.jsonObject
                            ?: continue
                    val listRenderer = itemRenderer["contents"]?.jsonArray

                    if (listRenderer != null) {
                        for (entry in listRenderer) {
                            val renderer =
                                entry.jsonObject["playlistVideoRenderer"]?.jsonObject
                                    ?: entry.jsonObject["richItemRenderer"]?.jsonObject
                                        ?.get("content")?.jsonObject
                                        ?.get("videoRenderer")?.jsonObject
                                    ?: continue
                            // Playlist entries are individually unavailable, not music.
                            if (renderer["isPlayable"]?.jsonPrimitive?.content == "false") continue
                            YouTubeChannelPage.parseVideoRenderer(renderer)?.let {
                                if (videos.none { existing -> existing.videoId == it.videoId }) {
                                    videos.add(it)
                                }
                            }
                            if (videos.size >= limit) break
                        }
                    }

                    if (continuation == null) {
                        continuation = itemRenderer["continuations"]?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("nextContinuationData")?.jsonObject
                            ?.get("continuation")?.jsonPrimitive?.contentOrNull
                    }

                    if (videos.size >= limit) break
                }
                if (videos.size >= limit) break
            }

            return YouTubePlaylistPage(
                playlistId = playlistId,
                title = title,
                videos = videos.take(limit),
                continuation = continuation,
            )
        }

        private fun textOf(obj: JsonObject): String? {
            (obj["simpleText"] as? JsonPrimitive)?.let { return it.contentOrNull }
            (obj["runs"] as? JsonArray)?.let { runs ->
                val text = runs.joinToString("") { run ->
                    (run.jsonObject["text"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                }
                if (text.isNotBlank()) return text
            }
            return null
        }
    }
}
