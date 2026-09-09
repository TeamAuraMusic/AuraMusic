package com.auramusic.innertube.pages

import com.auramusic.innertube.models.Thumbnail
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parser for YouTube's newer `lockupViewModel` renderer. Since 2025 YouTube moved
 * channel tabs, some search results and the home feed to this structure instead of
 * `videoRenderer`/`playlistRenderer`. A single lockup can represent a video, a
 * playlist, a channel or a shelf, distinguished by [Lockup.contentType].
 */
object Lockup {
    const val TYPE_VIDEO = "LOCKUP_CONTENT_TYPE_VIDEO"
    const val TYPE_PLAYLIST = "LOCKUP_CONTENT_TYPE_PLAYLIST"
    const val TYPE_CHANNEL = "LOCKUP_CONTENT_TYPE_CHANNEL"

    data class Lockup(
        val contentId: String?,
        val contentType: String?,
        val title: String?,
        val thumbnails: List<Thumbnail>,
        val durationText: String?,
        val metadataText: List<String>,
        val videoId: String?,
        val playlistId: String?,
        val channelId: String?,
    )

    fun parse(element: JsonElement): Lockup? {
        val obj = element.jsonObjectOrNull() ?: return null
        val contentId = obj["contentId"]?.jsonPrimitive?.contentOrNull
        val contentType = obj["contentType"]?.jsonPrimitive?.contentOrNull
            ?: return null

        val metadata = obj["metadata"]?.jsonObjectOrNull()
            ?.get("lockupMetadataViewModel")?.jsonObjectOrNull()
        val title = metadata?.get("title")?.jsonObjectOrNull()
            ?.get("content")?.jsonPrimitive?.contentOrNull

        val thumbnails = obj["contentImage"]?.jsonObjectOrNull()
            ?.get("thumbnailViewModel")?.jsonObjectOrNull()
            ?.get("image")?.jsonObjectOrNull()
            ?.get("sources")?.jsonArray
            ?.mapNotNull { source ->
                val url = source.jsonObjectOrNull()?.get("url")?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                Thumbnail(
                    url = url,
                    width = source.jsonObjectOrNull()?.get("width")?.jsonPrimitive?.content?.toIntOrNull(),
                    height = source.jsonObjectOrNull()?.get("height")?.jsonPrimitive?.content?.toIntOrNull(),
                )
            }.orEmpty()

        // Duration comes from the bottom overlay badge text (e.g. "23:28").
        val durationText = obj["contentImage"]?.jsonObjectOrNull()
            ?.get("thumbnailViewModel")?.jsonObjectOrNull()
            ?.get("overlays")?.jsonArray
            ?.firstNotNullOfOrNull { overlay ->
                overlay.jsonObjectOrNull()?.get("thumbnailBottomOverlayViewModel")?.jsonObjectOrNull()
                    ?.get("badges")?.jsonArray
                    ?.firstNotNullOfOrNull { badge ->
                        badge.jsonObjectOrNull()?.get("thumbnailBadgeViewModel")?.jsonObjectOrNull()
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                            ?.takeIf { it.contains(":") }
                    }
            }

        val metadataText = metadata?.get("metadata")?.jsonObjectOrNull()
            ?.get("contentMetadataViewModel")?.jsonObjectOrNull()
            ?.get("metadataRows")?.jsonArray
            ?.flatMap { row ->
                row.jsonObjectOrNull()?.get("metadataParts")?.jsonArray
                    ?.mapNotNull { part ->
                        part.jsonObjectOrNull()?.get("text")?.jsonObjectOrNull()
                            ?.get("content")?.jsonPrimitive?.contentOrNull
                    }.orEmpty()
            }.orEmpty()

        // Navigation: watchEndpoint for videos, watchPlaylistEndpoint for playlists,
        // browseEndpoint for channels.
        val endpoint = obj["onTap"]?.jsonObjectOrNull()
            ?.get("navigationEndpoint")?.jsonObjectOrNull()
        val videoId = endpoint?.get("watchEndpoint")?.jsonObjectOrNull()
            ?.get("videoId")?.jsonPrimitive?.contentOrNull

        return Lockup(
            contentId = contentId,
            contentType = contentType,
            title = title,
            thumbnails = thumbnails,
            durationText = durationText,
            metadataText = metadataText,
            videoId = videoId ?: if (contentType == TYPE_VIDEO) contentId else null,
            playlistId = if (contentType == TYPE_PLAYLIST) contentId else null,
            channelId = if (contentType == TYPE_CHANNEL) contentId else null,
        )
    }

    /** Builds a [com.auramusic.innertube.models.YouTubeVideoItem] from a video lockup. */
    fun toVideoItem(lockup: Lockup): com.auramusic.innertube.models.YouTubeVideoItem? {
        val videoId = lockup.videoId ?: return null
        val title = lockup.title ?: return null
        return com.auramusic.innertube.models.YouTubeVideoItem(
            videoId = videoId,
            title = title,
            channelName = "",
            viewCountText = lockup.metadataText.firstOrNull(),
            publishedTimeText = lockup.metadataText.getOrNull(1),
            durationText = lockup.durationText,
            thumbnails = lockup.thumbnails,
        )
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject
}
