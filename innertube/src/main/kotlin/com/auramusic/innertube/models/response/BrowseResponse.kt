package com.auramusic.innertube.models.response

import com.auramusic.innertube.models.Button
import com.auramusic.innertube.models.Continuation
import com.auramusic.innertube.models.GridRenderer
import com.auramusic.innertube.models.Menu
import com.auramusic.innertube.models.MusicDetailHeaderRenderer
import com.auramusic.innertube.models.MusicEditablePlaylistDetailHeaderRenderer
import com.auramusic.innertube.models.MusicShelfRenderer
import com.auramusic.innertube.models.ResponseContext
import com.auramusic.innertube.models.Runs
import com.auramusic.innertube.models.SectionListRenderer
import com.auramusic.innertube.models.SubscriptionButton
import com.auramusic.innertube.models.Tabs
import com.auramusic.innertube.models.ThumbnailRenderer
import kotlinx.serialization.Serializable

@Serializable
data class BrowseResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
    val onResponseReceivedActions: List<ResponseAction>?,
    val header: Header?,
    val microformat: Microformat?,
    val responseContext: ResponseContext,
    val background: ThumbnailRenderer?,
    val frameworkUpdates: FrameworkUpdates?,
) {
    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: Tabs?,
        val sectionListRenderer: SectionListRenderer?,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?,
    )

    @Serializable
    data class TwoColumnBrowseResultsRenderer(
        val tabs: List<Tabs.Tab?>?,
        val secondaryContents: SecondaryContents?,
    )
    @Serializable
    data class SecondaryContents(
        val sectionListRenderer: SectionListRenderer?,
    )

    @Serializable
    data class ContinuationContents(
        val sectionListContinuation: SectionListContinuation?,
        val musicPlaylistShelfContinuation: MusicPlaylistShelfContinuation?,
        val gridContinuation: GridContinuation?,
        val musicShelfContinuation: MusicShelfRenderer?,
    ) {
        @Serializable
        data class SectionListContinuation(
            val contents: List<SectionListRenderer.Content> = emptyList(),
            val continuations: List<Continuation>?,
        )

        @Serializable
        data class MusicPlaylistShelfContinuation(
            val contents: List<MusicShelfRenderer.Content> = emptyList(),
            val continuations: List<Continuation>?,
        )

        @Serializable
        data class GridContinuation(
            val items: List<GridRenderer.Item> = emptyList(),
            val continuations: List<Continuation>?,
        )
    }

    @Serializable
    data class ResponseAction(
        val appendContinuationItemsAction: ContinuationItems?,
    ) {
        @Serializable
        data class ContinuationItems(
            val continuationItems: List<MusicShelfRenderer.Content>?,
        )
    }

    @Serializable
    data class Header(
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?,
        val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer?,
        val musicVisualHeaderRenderer: MusicVisualHeaderRenderer?,
        val musicHeaderRenderer: MusicHeaderRenderer?,
    ) {
        @Serializable
        data class MusicImmersiveHeaderRenderer(
            val title: Runs,
            val description: Runs?,
            val thumbnail: ThumbnailRenderer?,
            val playButton: Button?,
            val startRadioButton: Button?,
            val subscriptionButton: SubscriptionButton?,
            val menu: Menu,
            val subscriptionButton2: SubscriptionButton2?,
            val monthlyListenerCount: Runs? = null,
        ) {
            @Serializable
            data class SubscriptionButton2(
                val subscribeButtonRenderer: SubscribeButtonRenderer?,
            ) {
                @Serializable
                data class SubscribeButtonRenderer(
                    val subscriberCountWithSubscribeText: Runs?,
                )
            }
        }

        @Serializable
        data class MusicVisualHeaderRenderer(
            val title: Runs,
            val foregroundThumbnail: ThumbnailRenderer,
            val thumbnail: ThumbnailRenderer?,
        )

        @Serializable
        data class Buttons(
            val menuRenderer: Menu.MenuRenderer?,
        )

        @Serializable
        data class MusicHeaderRenderer(
            val buttons: List<Buttons>?,
            val title: Runs?,
            val thumbnail: MusicThumbnailRenderer?,
            val subtitle: Runs?,
            val secondSubtitle: Runs?,
            val straplineTextOne: Runs?,
            val straplineThumbnail: MusicThumbnailRenderer?,
        )
        @Serializable
        data class MusicThumbnail(
            val url: String?,
        )
        @Serializable
        data class MusicThumbnailRenderer(
            val musicThumbnailRenderer: MusicThumbnailRenderer,
            val thumbnails: List<MusicThumbnail>?,
        )
    }

    @Serializable
    data class Microformat(
        val microformatDataRenderer: MicroformatDataRenderer?,
    ) {
        @Serializable
        data class MicroformatDataRenderer(
            val urlCanonical: String?,
        )
    }

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