package com.auramusic.innertube.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * WEB /next response used for comment threads. The same endpoint returns comments
 * when called with the comments continuation token extracted from the watch page.
 * Initial pages nest threads under twoColumnWatchNextResults; continuation pages
 * under continuationContents.itemSectionContinuation.
 */
@Serializable
data class YoutubeCommentResponse(
    val contents: CommentContents? = null,
    val continuationContents: CommentContinuationContents? = null,
)

@Serializable
data class CommentContents(
    @SerialName("twoColumnWatchNextResults")
    val twoColumnWatchNextResults: CommentTwoColumnWatchNextResults? = null,
)

@Serializable
data class CommentTwoColumnWatchNextResults(
    val results: CommentResultsWrapper? = null,
)

@Serializable
data class CommentResultsWrapper(
    val results: CommentResults? = null,
)

@Serializable
data class CommentResults(
    val contents: List<CommentResultContent?>? = null,
)

@Serializable
data class CommentResultContent(
    val itemSectionRenderer: CommentItemSectionRenderer? = null,
)

@Serializable
data class CommentItemSectionRenderer(
    val sectionIdentifier: String? = null,
    val contents: List<CommentItemSectionContent?>? = null,
)

@Serializable
data class CommentItemSectionContent(
    val commentThreadRenderer: CommentThreadRenderer? = null,
    val continuationItemRenderer: CommentContinuationItemRenderer? = null,
)

@Serializable
data class CommentThreadRenderer(
    val comment: CommentWrapper? = null,
)

@Serializable
data class CommentWrapper(
    val commentRenderer: CommentRenderer? = null,
)

@Serializable
data class CommentContinuationItemRenderer(
    val continuationEndpoint: CommentContinuationEndpoint? = null,
)

@Serializable
data class CommentContinuationEndpoint(
    val continuationCommand: CommentContinuationCommand? = null,
)

@Serializable
data class CommentContinuationCommand(
    val token: String? = null,
)

@Serializable
data class CommentContinuationContents(
    val itemSectionContinuation: CommentItemSectionContinuation? = null,
)

@Serializable
data class CommentItemSectionContinuation(
    val contents: List<CommentContinuationContent?>? = null,
    val header: CommentHeader? = null,
)

@Serializable
data class CommentContinuationContent(
    val commentThreadRenderer: CommentThreadRenderer? = null,
    val continuationItemRenderer: CommentContinuationItemRenderer? = null,
)

@Serializable
data class CommentHeader(
    val commentsHeaderRenderer: CommentHeaderRenderer? = null,
)

@Serializable
data class CommentHeaderRenderer(
    val commentsCount: CommentText? = null,
)

@Serializable
data class CommentText(
    val runs: List<CommentTextRun>? = null,
    val simpleText: String? = null,
) {
    fun text(): String = runs?.joinToString("") { it.text.orEmpty() } ?: simpleText.orEmpty()
}

@Serializable
data class CommentTextRun(
    val text: String? = null,
)

@Serializable
data class CommentRenderer(
    val commentId: String? = null,
    val authorText: CommentText? = null,
    val authorThumbnail: CommentThumbnails? = null,
    val contentText: CommentText? = null,
    val publishedTimeText: CommentText? = null,
    val voteCount: CommentText? = null,
    val replyCount: CommentText? = null,
    val authorIsChannelOwner: Boolean? = null,
    val pinnedText: CommentText? = null,
)

@Serializable
data class CommentThumbnails(
    val thumbnails: List<CommentThumbnailData>? = null,
)

@Serializable
data class CommentThumbnailData(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

/** Parsed comment threads with the next-page token. */
fun YoutubeCommentResponse.comments(): List<YoutubeComment> {
    val threadRenderers = mutableListOf<CommentThreadRenderer>()
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.forEach { content ->
            content?.itemSectionRenderer?.contents?.forEach { item ->
                item?.commentThreadRenderer?.let(threadRenderers::add)
            }
        }
    continuationContents?.itemSectionContinuation?.contents?.forEach { content ->
        content?.commentThreadRenderer?.let(threadRenderers::add)
    }
    return threadRenderers.mapNotNull { thread ->
        val r = thread.comment?.commentRenderer ?: return@mapNotNull null
        YoutubeComment(
            commentId = r.commentId ?: return@mapNotNull null,
            authorName = r.authorText?.text().orEmpty(),
            authorThumbnail = r.authorThumbnail?.thumbnails?.maxByOrNull { it.width ?: 0 }?.url,
            content = r.contentText?.text().orEmpty(),
            publishedTime = r.publishedTimeText?.text()?.takeIf { s -> s.isNotBlank() },
            likeCount = r.voteCount?.text()?.takeIf { s -> s.isNotBlank() },
            replyCount = r.replyCount?.text()?.takeIf { s -> s.isNotBlank() },
            isAuthorPinned = r.pinnedText?.text()?.isNotBlank() == true || r.authorIsChannelOwner == true,
        )
    }
}

fun YoutubeCommentResponse.commentsContinuation(): String? {
    // Initial page: the "load more comments" token lives at the end of the item section.
    contents?.twoColumnWatchNextResults?.results?.results?.contents
        ?.forEach { content ->
            content?.itemSectionRenderer?.contents?.forEach { item ->
                item?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
                    ?.let { return it }
            }
        }
    // Continuation pages: token at the end of the item section continuation.
    return continuationContents?.itemSectionContinuation?.contents
        ?.lastOrNull { it?.continuationItemRenderer != null }
        ?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
}

fun YoutubeCommentResponse.commentsCountText(): String? =
    continuationContents?.itemSectionContinuation?.header?.commentsHeaderRenderer?.commentsCount
        ?.text()?.takeIf { s -> s.isNotBlank() }

data class YoutubeComment(
    val commentId: String,
    val authorName: String,
    val authorThumbnail: String?,
    val content: String,
    val publishedTime: String?,
    val likeCount: String?,
    val replyCount: String?,
    val isAuthorPinned: Boolean,
)
