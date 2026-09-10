package com.auramusic.innertube.pages

import com.auramusic.innertube.models.YouTubeChannelPost
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A channel's community posts, parsed from the WEB /youtubei/v1/browse response
 * (browseId = channelId, params = [POSTS_PARAMS]) or a subsequent continuation page.
 *
 * The first page puts backstagePostThreadRenderer items inside a sectionListRenderer;
 * continuation pages append them via onResponseReceivedEndpoints. Both shapes are
 * gathered with the same recursive walk.
 */
data class YouTubeChannelPostsPage(
    val posts: List<YouTubeChannelPost>,
    val continuation: String? = null,
) {
    companion object {
        const val POSTS_PARAMS = "EgVwb3N0c_IGBAoCSgA%3D"

        fun fromJson(element: JsonElement): YouTubeChannelPostsPage {
            val posts = mutableListOf<YouTubeChannelPost>()
            var continuation: String? = null
            walk(element.jsonObject) { node ->
                node["backstagePostRenderer"]?.jsonObject
                    ?.let(::parsePost)
                    ?.let(posts::add)
                val token = node["continuationCommand"]?.jsonObject
                    ?.get("token")?.jsonPrimitive?.content
                if (token != null) continuation = token
            }
            return YouTubeChannelPostsPage(posts = posts, continuation = continuation)
        }

        private fun walk(node: JsonObject, visit: (JsonObject) -> Unit) {
            visit(node)
            node.values.forEach { value ->
                when (value) {
                    is JsonObject -> walk(value, visit)
                    is JsonArray -> value.forEach { child ->
                        if (child is JsonObject) walk(child, visit)
                    }
                    else -> { /* primitive, ignore */ }
                }
            }
        }

        private fun parsePost(renderer: JsonObject): YouTubeChannelPost? {
            val postId = renderer["postId"]?.jsonPrimitive?.content ?: return null
            val authorName = renderer["authorText"]?.jsonObject?.let { textOf(it) }.orEmpty()
            val authorThumbnail = renderer["authorThumbnail"]?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
            val published = renderer["publishedTimeText"]?.jsonObject?.let { textOf(it) }
            val votes = renderer["voteCount"]?.jsonObject
                ?.get("simpleText")?.jsonPrimitive?.content
            val content = renderer["contentText"]?.jsonObject?.let { textOf(it) }.orEmpty()
            val images = mutableListOf<String>()
            renderer["backstageAttachment"]?.jsonObject?.let { attachment ->
                attachment["backstageImageRenderer"]?.jsonObject
                    ?.get("image")?.jsonObject
                    ?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                    ?.let(images::add)
                attachment["postMultiImageRenderer"]?.jsonObject
                    ?.get("images")?.jsonArray
                    ?.forEach { img ->
                        img.jsonObject["backstageImageRenderer"]?.jsonObject
                            ?.get("image")?.jsonObject
                            ?.get("thumbnails")?.jsonArray
                            ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                            ?.let(images::add)
                    }
            }
            return YouTubeChannelPost(
                postId = postId,
                authorName = authorName,
                authorThumbnail = authorThumbnail,
                publishedTimeText = published,
                voteCountText = votes,
                contentText = content,
                imageUrls = images,
            )
        }

        private fun textOf(obj: JsonObject): String? {
            obj["simpleText"]?.jsonPrimitive?.content?.let { return it }
            return obj["runs"]?.jsonArray
                ?.joinToString("") { run ->
                    run.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
                }?.takeIf { it.isNotBlank() }
        }
    }
}