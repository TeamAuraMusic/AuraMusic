package com.auramusic.innertube.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YouTubeSearchResponse(
    val contents: Contents?,
) {
    @Serializable
    data class Contents(
        @SerialName("twoColumnSearchResultsRenderer")
        val twoColumnSearchResultsRenderer: TwoColumnSearchResultsRenderer?,
    )

    @Serializable
    data class TwoColumnSearchResultsRenderer(
        val primaryContents: PrimaryContents?,
    )

    @Serializable
    data class PrimaryContents(
        val sectionListRenderer: SectionListRenderer?,
    )

    @Serializable
    data class SectionListRenderer(
        val contents: List<Content>?,
    )

    @Serializable
    data class Content(
        val itemSectionRenderer: ItemSectionContent?,
        @SerialName("continuationItemRenderer")
        val continuationItemRenderer: ContinuationItemRendererData?,
    )

    @Serializable
    data class ItemSectionContent(
        val contents: List<Item>?,
    )

    @Serializable
    data class Item(
        @SerialName("videoRenderer")
        val videoRenderer: VideoRenderer?,
        @SerialName("channelRenderer")
        val channelRenderer: ChannelRenderer?,
        @SerialName("playlistRenderer")
        val playlistRenderer: PlaylistRendererData?,
        @SerialName("lockupViewModel")
        val lockupViewModel: JsonElement?,
    )

    @Serializable
    data class VideoRenderer(
        val videoId: String?,
        val title: TextRuns?,
        val longBylineText: TextRuns?,
        val viewCountText: ViewCountText?,
        val publishedTimeText: SimpleText?,
        val lengthText: SimpleText?,
        val thumbnail: Thumbnails?,
        val ownerText: TextRuns?,
        val descriptionSnippet: TextRuns?,
    )

    @Serializable
    data class ChannelRenderer(
        val channelId: String?,
        val title: SimpleText?,
        val thumbnail: Thumbnails?,
        val subscriberCountText: SimpleText?,
        val videoCountText: SimpleText?,
        val descriptionSnippet: TextRuns?,
    )

    @Serializable
    data class PlaylistRendererData(
        val playlistId: String?,
        val title: SimpleText?,
        val thumbnail: Thumbnails?,
        val videoCountText: SimpleText?,
        val longBylineText: TextRuns?,
    )

    @Serializable
    data class TextRuns(
        val runs: List<TextRun>?,
    )

    @Serializable
    data class TextRun(
        val text: String?,
        val navigationEndpoint: TextRunNavigationEndpoint?,
    )

    @Serializable
    data class TextRunNavigationEndpoint(
        val browseEndpoint: TextRunBrowseEndpoint?,
    )

    @Serializable
    data class TextRunBrowseEndpoint(
        val browseId: String?,
    )

    @Serializable
    data class SimpleText(
        val simpleText: String?,
    )

    @Serializable
    data class ViewCountText(
        val simpleText: String?,
        val runs: List<TextRun>?,
    )

    @Serializable
    data class Thumbnails(
        val thumbnails: List<Thumbnail>?,
    )

    @Serializable
    data class Thumbnail(
        val url: String?,
        val width: Int?,
        val height: Int?,
    )

    @Serializable
    data class ContinuationItemRendererData(
        val continuationEndpoint: ContinuationEndpointData?,
    )

    @Serializable
    data class ContinuationEndpointData(
        val continuationCommand: ContinuationCommandData?,
    )

    @Serializable
    data class ContinuationCommandData(
        val token: String?,
    )
}
