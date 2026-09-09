package com.auramusic.innertube.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * WEB comment-thread response.
 *
 * Modern WEB client (2025+) no longer returns comment data inside `commentRenderer`.
 * Instead a comment page is split into two parts:
 *  - [onResponseReceivedEndpoints]: a `reloadContinuationItemsCommand` whose
 *    `continuationItems` are lightweight `commentThreadRenderer`s that only carry a
 *    view-model (commentId, entity key, optional pinned text) plus a trailing
 *    `continuationItemRenderer` with the next-page token.
 *  - [frameworkUpdates]: an `entityBatchUpdate` whose mutations carry the actual
 *    comment `commentEntityPayload` objects (author, content, published time, like/
 *    reply counts) keyed by the same comment id.
 *
 * The first page is fetched by passing the comments continuation token obtained from
 * [WatchMetadataResponse.commentsContinuation]; later pages pass the token returned
 * by each page.
 */
@Serializable
data class YoutubeCommentResponse(
    val onResponseReceivedEndpoints: List<CommentResponseEndpoint?>? = null,
    val frameworkUpdates: CommentFrameworkUpdates? = null,
)

@Serializable
data class CommentResponseEndpoint(
    val reloadContinuationItemsCommand: CommentReloadContinuationItemsCommand? = null,
)

@Serializable
data class CommentReloadContinuationItemsCommand(
    val targetId: String? = null,
    val continuationItems: List<CommentReloadItem?>? = null,
)

@Serializable
data class CommentReloadItem(
    val commentThreadRenderer: CommentThreadRenderer? = null,
    val continuationItemRenderer: CommentContinuationItemRenderer? = null,
    val commentsHeaderRenderer: CommentHeaderRenderer? = null,
)

@Serializable
data class CommentThreadRenderer(
    val commentViewModel: CommentThreadViewModelWrapper? = null,
)

@Serializable
data class CommentThreadViewModelWrapper(
    val commentViewModel: CommentThreadViewModel? = null,
)

@Serializable
data class CommentThreadViewModel(
    val commentId: String? = null,
    val commentKey: String? = null,
    val pinnedText: String? = null,
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
data class CommentHeaderRenderer(
    val countText: CommentText? = null,
    val commentsCount: CommentText? = null,
)

@Serializable
data class CommentFrameworkUpdates(
    val entityBatchUpdate: CommentEntityBatchUpdate? = null,
)

@Serializable
data class CommentEntityBatchUpdate(
    val mutations: List<CommentEntityMutation?>? = null,
)

@Serializable
data class CommentEntityMutation(
    val entityKey: String? = null,
    val payload: CommentEntityPayload? = null,
)

@Serializable
data class CommentEntityPayload(
    val key: String? = null,
    val properties: CommentEntityProperties? = null,
    val author: CommentEntityAuthor? = null,
    val toolbar: CommentEntityToolbar? = null,
)

@Serializable
data class CommentEntityProperties(
    @SerialName("commentId")
    val commentId: String? = null,
    val content: CommentEntityContent? = null,
    val publishedTime: String? = null,
)

@Serializable
data class CommentEntityContent(
    val content: String? = null,
)

@Serializable
data class CommentEntityAuthor(
    @SerialName("displayName")
    val displayName: String? = null,
    @SerialName("channelId")
    val channelId: String? = null,
    @SerialName("avatarThumbnailUrl")
    val avatarThumbnailUrl: String? = null,
    val isVerified: Boolean? = null,
)

@Serializable
data class CommentEntityToolbar(
    @SerialName("likeCountLiked")
    val likeCountLiked: String? = null,
    @SerialName("likeCountNotliked")
    val likeCountNotliked: String? = null,
    @SerialName("replyCount")
    val replyCount: String? = null,
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

/** Parsed comment threads with the next-page token. */
fun YoutubeCommentResponse.comments(): List<YoutubeComment> {
    val entities = buildMap<String, CommentEntityPayload> {
        frameworkUpdates?.entityBatchUpdate?.mutations.orEmpty().forEach { mutation ->
            mutation?.let { m ->
                val id = m.payload?.properties?.commentId
                if (id != null) put(id, m.payload)
            }
        }
    }
    val threads = onResponseReceivedEndpoints.orEmpty()
        .mapNotNull { it?.reloadContinuationItemsCommand }
        .flatMap { command -> command.continuationItems.orEmpty() }
        .mapNotNull { it?.commentThreadRenderer }
    return threads.mapNotNull { thread ->
        val viewModel = thread.commentViewModel?.commentViewModel ?: return@mapNotNull null
        val commentId = viewModel.commentId ?: return@mapNotNull null
        val entity = entities[commentId] ?: return@mapNotNull null
        val toolbar = entity.toolbar
        YoutubeComment(
            commentId = commentId,
            authorName = entity.author?.displayName.orEmpty(),
            authorThumbnail = entity.author?.avatarThumbnailUrl,
            content = entity.properties?.content?.content.orEmpty(),
            publishedTime = entity.properties?.publishedTime?.takeIf { it.isNotBlank() },
            likeCount = (toolbar?.likeCountNotliked ?: toolbar?.likeCountLiked)
                ?.takeIf { it.isNotBlank() },
            replyCount = toolbar?.replyCount?.takeIf { it.isNotBlank() },
            isAuthorPinned = viewModel.pinnedText?.isNotBlank() == true,
        )
    }
}

fun YoutubeCommentResponse.commentsContinuation(): String? =
    onResponseReceivedEndpoints.orEmpty()
        .mapNotNull { it?.reloadContinuationItemsCommand }
        .mapNotNull { command ->
            command.continuationItems.orEmpty()
                .lastOrNull { it?.continuationItemRenderer != null }
        }
        .firstNotNullOfOrNull {
            it?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
        }

fun YoutubeCommentResponse.commentsCountText(): String? =
    onResponseReceivedEndpoints.orEmpty()
        .mapNotNull { it?.reloadContinuationItemsCommand }
        .mapNotNull { command ->
            command.continuationItems.orEmpty()
                .firstNotNullOfOrNull { it?.commentsHeaderRenderer }
        }
        .firstNotNullOfOrNull { renderer ->
            renderer.countText?.text()?.takeIf { it.isNotBlank() }
        }

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
