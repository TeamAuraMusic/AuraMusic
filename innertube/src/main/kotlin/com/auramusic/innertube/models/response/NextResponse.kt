package com.auramusic.innertube.models.response

import com.auramusic.innertube.models.NavigationEndpoint
import com.auramusic.innertube.models.PlaylistPanelRenderer
import com.auramusic.innertube.models.Tabs
import com.auramusic.innertube.models.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents,
    val continuationContents: ContinuationContents?,
    val currentVideoEndpoint: NavigationEndpoint?,
    val frameworkUpdates: FrameworkUpdates?,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?,
        val twoColumnWatchNextResults: YouTubeDataPage.Contents.TwoColumnWatchNextResults?,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer?,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab>,
                )
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer,
    )

    @Serializable
    data class FrameworkUpdates(
        val entityBatchUpdate: EntityBatchUpdate?,
    ) {
        @Serializable
        data class EntityBatchUpdate(
            val mutations: List<Mutation>,
        ) {
            @Serializable
            data class Mutation(
                val entityKey: String?,
                val payload: MutationPayload?,
            ) {
                @Serializable
                data class MutationPayload(
                    val commentEntityPayload: CommentEntityPayload?,
                ) {
                    @Serializable
                    data class CommentEntityPayload(
                        val properties: CommentProperties?,
                    ) {
                        @Serializable
                        data class CommentProperties(
                            val commentId: String?,
                            val content: Content?,
                            val publishedTime: String?,
                        ) {
                            @Serializable
                            data class Content(
                                val content: String?,
                            )
                        }
                    }
                }
            }
        }
    }
}
