package com.auramusic.innertube.pages

import com.auramusic.innertube.models.Thumbnail
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class YouTubeFeedResult(
    val items: List<YouTubeVideoItem>,
    val continuation: String? = null,
)

/**
 * Parses a regular YouTube (WEB) browse response - used for the home feed
 * (FEwhat_to_watch) and trending (FEtrending). The response structure varies
 * between rich grids, shelves and item sections, so we walk the JSON tree and
 * collect every [YouTubeVideoItem] plus the most recent continuation token.
 */
object YouTubeFeedPage {
    fun fromBrowseResponse(text: String): YouTubeFeedResult {
        val items = mutableListOf<YouTubeVideoItem>()
        val continuationToken = arrayOfNulls<String>(1)
        val root = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
        walk(root, items, continuationToken)
        return YouTubeFeedResult(items.distinctBy { it.videoId }, continuationToken[0])
    }

    private fun walk(element: JsonElement, items: MutableList<YouTubeVideoItem>, continuation: Array<String?>) {
        when (element) {
            is JsonObject -> {
                element["videoRenderer"]?.jsonObject?.let { renderer ->
                    fromVideoRenderer(renderer)?.let { items.add(it) }
                }
                element["continuationItemRenderer"]?.jsonObject
                    ?.get("continuationEndpoint")?.jsonObject
                    ?.get("continuationCommand")?.jsonObject
                    ?.get("token")?.jsonPrimitive?.contentOrNull
                    ?.let { continuation[0] = it }
                element.values.forEach { value -> walk(value, items, continuation) }
            }
            is JsonArray -> element.forEach { walk(it, items, continuation) }
            is JsonPrimitive -> Unit
        }
    }

    private fun fromVideoRenderer(renderer: JsonObject): YouTubeVideoItem? {
        val videoId = renderer.getString("videoId") ?: return null
        val title = renderer["title"]?.asRunsText() ?: return null
        val byline = renderer["longBylineText"]?.asRunsText()
        val channelName = byline ?: renderer["ownerText"]?.asRunsText().orEmpty()
        val channelId = renderer["longBylineText"]?.firstBrowseId()

        val viewCount = renderer["viewCountText"]?.asSimpleOrRunsText()
        val publishedTime = renderer["publishedTimeText"]?.asSimpleText()
        val duration = renderer["lengthText"]?.asSimpleText()
        val description = renderer["descriptionSnippet"]?.asRunsText()

        val thumbnails = renderer["thumbnail"]?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.mapNotNull { it.jsonObject }
            ?.mapNotNull { thumb ->
                thumb.getString("url")?.let { url ->
                    Thumbnail(url = url, width = thumb.getInt("width"), height = thumb.getInt("height"))
                }
            }.orEmpty()

        val isLive = viewCount?.contains("watching") == true || renderer["badges"] != null

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

    private fun JsonElement?.asSimpleText(): String? =
        this?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull

    private fun JsonElement?.asRunsText(): String? =
        this?.jsonObject?.get("runs")?.jsonArray
            ?.mapNotNull { it.jsonObject.getString("text") }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("")

    private fun JsonElement?.asSimpleOrRunsText(): String? =
        asSimpleText() ?: asRunsText()

    private fun JsonElement?.firstBrowseId(): String? =
        this?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("navigationEndpoint")?.jsonObject
            ?.get("browseEndpoint")?.jsonObject
            ?.getString("browseId")

    private fun JsonObject.getString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.getInt(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull
}