package com.auramusic.innertube.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * WEB client /next response for regular YouTube videos. The YT Music (WEB_REMIX) /next
 * response only contains music queues and an ATV-only related tab, so regular video
 * metadata (related videos, channel info, comments continuation) must be parsed from
 * the two-column watch-next layout.
 */
@Serializable
data class WatchMetadataResponse(
    val contents: WatchContents? = null,
)

@Serializable
data class WatchContents(
    @SerialName("twoColumnWatchNextResults")
    val twoColumnWatchNextResults: WatchTwoColumn? = null,
)

@Serializable
data class WatchTwoColumn(
    val results: WatchResultsWrapper? = null,
    val secondaryResults: WatchSecondaryWrapper? = null,
)

// ---------------------------------------------------------------------------
// Primary results (title, channel, description, comments entry)
// ---------------------------------------------------------------------------

@Serializable
data class WatchResultsWrapper(
    val results: WatchResults? = null,
)

@Serializable
data class WatchResults(
    val contents: List<WatchResultContent?>? = null,
)

@Serializable
data class WatchResultContent(
    val videoPrimaryInfoRenderer: WatchVideoPrimaryInfoRenderer? = null,
    val videoSecondaryInfoRenderer: WatchVideoSecondaryInfoRenderer? = null,
    val itemSectionRenderer: WatchItemSectionRenderer? = null,
)

@Serializable
data class WatchVideoPrimaryInfoRenderer(
    val title: WatchText? = null,
    val viewCount: WatchViewCount? = null,
    val dateText: WatchSimpleText? = null,
)

@Serializable
data class WatchViewCount(
    val videoViewCountRenderer: WatchViewCountRenderer? = null,
)

@Serializable
data class WatchViewCountRenderer(
    val viewCount: WatchSimpleText? = null,
)

@Serializable
data class WatchVideoSecondaryInfoRenderer(
    val owner: WatchOwner? = null,
    val attributedDescription: WatchAttributedDescription? = null,
    val description: WatchText? = null,
)

@Serializable
data class WatchAttributedDescription(
    val content: String? = null,
)

@Serializable
data class WatchOwner(
    val videoOwnerRenderer: WatchVideoOwnerRenderer? = null,
)

@Serializable
data class WatchVideoOwnerRenderer(
    val thumbnail: WatchThumbnails? = null,
    val subscriberCountText: WatchSimpleText? = null,
    val title: WatchText? = null,
    val navigationEndpoint: WatchOwnerNavigationEndpoint? = null,
)

@Serializable
data class WatchOwnerNavigationEndpoint(
    val browseEndpoint: WatchBrowseEndpoint? = null,
)

@Serializable
data class WatchBrowseEndpoint(
    val browseId: String? = null,
)

@Serializable
data class WatchItemSectionRenderer(
    val contents: List<WatchItemSectionContent?>? = null,
)

@Serializable
data class WatchItemSectionContent(
    val continuationItemRenderer: WatchContinuationItemRenderer? = null,
    val commentsEntryPointHeaderRenderer: WatchCommentsEntryPointHeader? = null,
)

@Serializable
data class WatchCommentsEntryPointHeader(
    val commentCount: WatchSimpleText? = null,
)

@Serializable
data class WatchContinuationItemRenderer(
    val continuationEndpoint: WatchContinuationEndpoint? = null,
)

@Serializable
data class WatchContinuationEndpoint(
    val continuationCommand: WatchContinuationCommand? = null,
)

@Serializable
data class WatchContinuationCommand(
    val token: String? = null,
)

// ---------------------------------------------------------------------------
// Secondary results (related videos)
// ---------------------------------------------------------------------------

@Serializable
data class WatchSecondaryWrapper(
    val secondaryResults: WatchSecondaryResults? = null,
)

@Serializable
data class WatchSecondaryResults(
    val results: List<WatchSecondaryContent?>? = null,
    val continuations: List<WatchContinuationData>? = null,
)

@Serializable
data class WatchSecondaryContent(
    val compactVideoRenderer: WatchCompactVideoRenderer? = null,
    val continuationItemRenderer: WatchContinuationItemRenderer? = null,
)

@Serializable
data class WatchCompactVideoRenderer(
    val videoId: String? = null,
    val title: WatchText? = null,
    val longBylineText: WatchText? = null,
    val shortBylineText: WatchText? = null,
    val viewCountText: WatchSimpleText? = null,
    val publishedTimeText: WatchSimpleText? = null,
    val lengthText: WatchSimpleText? = null,
    val thumbnail: WatchThumbnails? = null,
    val thumbnailOverlays: List<WatchThumbnailOverlay?>? = null,
)

@Serializable
data class WatchThumbnailOverlay(
    val thumbnailOverlayTimeStatusRenderer: WatchTimeStatusRenderer? = null,
)

@Serializable
data class WatchTimeStatusRenderer(
    val text: WatchText? = null,
    val style: String? = null,
)

@Serializable
data class WatchContinuationData(
    val nextContinuationData: WatchNextContinuationData? = null,
)

@Serializable
data class WatchNextContinuationData(
    val continuation: String? = null,
)

// ---------------------------------------------------------------------------
// Shared primitives
// ---------------------------------------------------------------------------

@Serializable
data class WatchText(
    val runs: List<WatchTextRun>? = null,
    val simpleText: String? = null,
) {
    fun text(): String = runs?.joinToString("") { it.text.orEmpty() } ?: simpleText.orEmpty()
}

@Serializable
data class WatchTextRun(
    val text: String? = null,
    val navigationEndpoint: WatchOwnerNavigationEndpoint? = null,
)

@Serializable
data class WatchSimpleText(
    val simpleText: String? = null,
)

@Serializable
data class WatchThumbnails(
    val thumbnails: List<WatchThumbnailData>? = null,
)

@Serializable
data class WatchThumbnailData(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

// ---------------------------------------------------------------------------
// Parsed accessors
// ---------------------------------------------------------------------------

data class WatchCompactVideo(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String?,
    val viewCountText: String?,
    val publishedTimeText: String?,
    val durationText: String?,
    val thumbnailUrl: String?,
    val isLive: Boolean,
)

fun WatchMetadataResponse.relatedVideos(): List<WatchCompactVideo> =
    contents?.twoColumnWatchNextResults
        ?.secondaryResults?.secondaryResults?.results
        ?.mapNotNull { it?.compactVideoRenderer }
        ?.map { r ->
            val lengthText = r.lengthText?.simpleText
            // LIVE badge style comes through the overlay; fall back to a missing duration.
            val overlayLive =
                r.thumbnailOverlays?.any { it?.thumbnailOverlayTimeStatusRenderer?.style == "LIVE" } == true
            val overlayDuration =
                r.thumbnailOverlays
                    ?.firstNotNullOfOrNull { it?.thumbnailOverlayTimeStatusRenderer?.text?.text() }
            WatchCompactVideo(
                videoId = r.videoId ?: "",
                title = r.title?.text().orEmpty(),
                channelName = (r.longBylineText ?: r.shortBylineText)?.text().orEmpty(),
                channelId = (r.longBylineText ?: r.shortBylineText)?.runs
                    ?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.browseId,
                viewCountText = r.viewCountText?.simpleText,
                publishedTimeText = r.publishedTimeText?.simpleText,
                durationText = lengthText ?: overlayDuration,
                thumbnailUrl = r.thumbnail?.thumbnails?.maxByOrNull { it.width ?: 0 }?.url,
                isLive = overlayLive || lengthText == null,
            )
        }
        .orEmpty()

fun WatchMetadataResponse.relatedContinuation(): String? {
    val results = contents?.twoColumnWatchNextResults
        ?.secondaryResults?.secondaryResults?.results.orEmpty()
    return results
        .lastOrNull { it?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token != null }
        ?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
        ?: contents?.twoColumnWatchNextResults
            ?.secondaryResults?.secondaryResults?.continuations
            ?.firstOrNull()?.nextContinuationData?.continuation
}


fun WatchMetadataResponse.commentCountText(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull { content ->
            content?.itemSectionRenderer?.contents
                ?.firstNotNullOfOrNull { it?.commentsEntryPointHeaderRenderer?.commentCount?.simpleText }
        }

fun WatchMetadataResponse.commentsContinuation(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull { content ->
            content?.itemSectionRenderer?.contents
                ?.firstNotNullOfOrNull { item ->
                    item?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                }
        }

fun WatchMetadataResponse.title(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull { it?.videoPrimaryInfoRenderer?.title?.text() }

fun WatchMetadataResponse.viewCountText(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull {
            it?.videoPrimaryInfoRenderer?.viewCount?.videoViewCountRenderer?.viewCount?.simpleText
        }

fun WatchMetadataResponse.dateText(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull { it?.videoPrimaryInfoRenderer?.dateText?.simpleText }

fun WatchMetadataResponse.channelName(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull { it?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.title?.text() }

fun WatchMetadataResponse.channelId(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull {
            it?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.navigationEndpoint
                ?.browseEndpoint?.browseId
        }

fun WatchMetadataResponse.channelAvatarUrl(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull {
            it?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.thumbnail?.thumbnails
                ?.maxByOrNull { t -> t.width ?: 0 }?.url
        }

fun WatchMetadataResponse.subscriberCountText(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull {
            it?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.subscriberCountText?.simpleText
        }

fun WatchMetadataResponse.description(): String? =
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.firstNotNullOfOrNull { content ->
            content?.videoSecondaryInfoRenderer?.let {
                it.attributedDescription?.content
                    ?: it.description?.text()
            }
        }
