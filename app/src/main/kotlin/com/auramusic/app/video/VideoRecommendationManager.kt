package com.auramusic.app.video

import android.content.Context
import com.auramusic.app.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Analyses video watch history and produces personalised feed sections:
 *  - Continue Watching  – partially watched videos
 *  - From Channels You Watch – videos from the user's most-watched channels
 *  - Recommended For You – related content derived from recent watch titles
 *  - Subscriptions – latest videos from subscribed channels
 */
class VideoRecommendationManager(private val context: Context) {

    // ------------------------------------------------------------------
    // Feed sections
    // ------------------------------------------------------------------
    data class FeedSection(
        val title: String,
        val videos: List<com.auramusic.innertube.models.YouTubeVideoItem>,
    )

    private val _sections = MutableStateFlow<List<FeedSection>>(emptyList())
    val sections: StateFlow<List<FeedSection>> = _sections.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // How many videos have been watched since last refresh
    private var watchCountSinceRefresh = 0
    private var lastRefreshWatchCount = 0
    val shouldRefresh: Boolean
        get() = watchCountSinceRefresh - lastRefreshWatchCount >= REFRESH_THRESHOLD

    fun onVideoWatched() {
        watchCountSinceRefresh++
    }

    fun markRefreshed() {
        lastRefreshWatchCount = watchCountSinceRefresh
    }

    // ------------------------------------------------------------------
    // Public entry point
    // ------------------------------------------------------------------
    suspend fun refresh() {
        _isLoading.value = true
        try {
            val history = readHistory()
            if (history.isEmpty()) {
                _sections.value = emptyList()
                return
            }

            val sections = withContext(Dispatchers.IO) {
                val result = mutableListOf<FeedSection>()

                // 1. Continue Watching
                val continueWatching = buildContinueWatching(history)
                if (continueWatching.isNotEmpty()) {
                    result.add(FeedSection("Continue Watching", continueWatching))
                }

                // 2. From Channels You Watch
                val channelVideos = buildChannelRecommendations(history)
                if (channelVideos.isNotEmpty()) {
                    result.add(FeedSection("From Channels You Watch", channelVideos))
                }

                // 3. Recommended For You
                val recommended = buildRecommended(history)
                if (recommended.isNotEmpty()) {
                    result.add(FeedSection("Recommended For You", recommended))
                }

                // 4. Subscriptions
                val subscriptions = buildSubscriptionsFeed()
                if (subscriptions.isNotEmpty()) {
                    result.add(FeedSection("Subscriptions", subscriptions))
                }

                result
            }
            _sections.value = sections
            markRefreshed()
        } finally {
            _isLoading.value = false
        }
    }

    // ------------------------------------------------------------------
    // Section builders
    // ------------------------------------------------------------------

    /**
     * Videos where the user watched 20%-80% but didn't finish.
     * Sorted by recency.
     */
    private suspend fun buildContinueWatching(
        history: List<VideoPlaybackManager.VideoHistoryEntry>,
    ): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        val partial = history.filter { entry ->
            entry.durationMs > 0 &&
                entry.positionMs > entry.durationMs * 0.2 &&
                entry.positionMs < entry.durationMs * 0.85
        }.sortedByDescending { it.lastPlayedAt }
            .take(10)

        return partial.mapNotNull { entry ->
            com.auramusic.innertube.models.YouTubeVideoItem(
                videoId = entry.videoId,
                title = entry.title,
                channelName = entry.channelName,
                channelId = entry.channelId,
                thumbnails = listOfNotNull(
                    entry.thumbnailUrl?.let { url ->
                        com.auramusic.innertube.models.Thumbnail(url = url)
                    }
                ),
            )
        }
    }

    /**
     * Search YouTube for videos from the user's top 3 most-watched channels.
     */
    private suspend fun buildChannelRecommendations(
        history: List<VideoPlaybackManager.VideoHistoryEntry>,
    ): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        val topChannels = history
            .filter { it.channelName.isNotBlank() }
            .groupBy { it.channelName }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { it.key }

        if (topChannels.isEmpty()) return emptyList()

        val allVideos = mutableListOf<com.auramusic.innertube.models.YouTubeVideoItem>()
        val seenIds = mutableSetOf<String>()
        val seenTitles = mutableSetOf<String>()

        // Also exclude recently watched video IDs
        val recentlyWatchedIds = history.take(20).map { it.videoId }.toSet()

        for (channelName in topChannels) {
            try {
                val result = com.auramusic.innertube.YouTube.youtubeSearchFeed(
                    query = channelName,
                    continuation = null,
                ).getOrNull()
                result?.items?.forEach { video ->
                    if (video.videoId !in seenIds &&
                        video.videoId !in recentlyWatchedIds &&
                        video.title !in seenTitles
                    ) {
                        seenIds.add(video.videoId)
                        seenTitles.add(video.title)
                        allVideos.add(video)
                    }
                }
            } catch (_: Exception) {
                // Skip failed channel searches
            }
            if (allVideos.size >= 15) break
        }

        return allVideos.take(15)
    }

    /**
     * Use recently watched video titles to find related content.
     * Takes the 3 most recent titles and searches for related videos.
     */
    private suspend fun buildRecommended(
        history: List<VideoPlaybackManager.VideoHistoryEntry>,
    ): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        val recentTitles = history
            .sortedByDescending { it.lastPlayedAt }
            .take(3)
            .map { it.title }

        if (recentTitles.isEmpty()) return emptyList()

        val allVideos = mutableListOf<com.auramusic.innertube.models.YouTubeVideoItem>()
        val seenIds = mutableSetOf<String>()
        val recentlyWatchedIds = history.take(15).map { it.videoId }.toSet()

        for (title in recentTitles) {
            try {
                // Extract a shorter search query from the title (first 4-5 words)
                val query = title.split(" ").take(5).joinToString(" ")
                val result = com.auramusic.innertube.YouTube.youtubeSearchFeed(
                    query = query,
                    continuation = null,
                ).getOrNull()
                result?.items?.forEach { video ->
                    if (video.videoId !in seenIds && video.videoId !in recentlyWatchedIds) {
                        seenIds.add(video.videoId)
                        allVideos.add(video)
                    }
                }
            } catch (_: Exception) {
                // Skip failed searches
            }
            if (allVideos.size >= 15) break
        }

        return allVideos.take(15)
    }

    /**
     * Videos from subscribed channels, sorted by recency.
     * Searches YouTube for each subscribed channel name.
     */
    private suspend fun buildSubscriptionsFeed(): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        val subs = try {
            val subsJson = context.dataStore.data.first()[com.auramusic.app.constants.VideoSubscribedChannelsKey]
            VideoPlaybackManager.parseSubscribedChannelsPublic(subsJson)
        } catch (_: Exception) {
            emptyList()
        }

        if (subs.isEmpty()) return emptyList()

        val allVideos = mutableListOf<com.auramusic.innertube.models.YouTubeVideoItem>()
        val seenIds = mutableSetOf<String>()

        // Search for videos from the most recently subscribed channels first
        val sortedSubs = subs.sortedByDescending { it.subscribedAt }.take(5)

        for (channel in sortedSubs) {
            try {
                val result = com.auramusic.innertube.YouTube.youtubeSearchFeed(
                    query = channel.name,
                    continuation = null,
                ).getOrNull()
                result?.items?.forEach { video ->
                    if (video.videoId !in seenIds) {
                        seenIds.add(video.videoId)
                        allVideos.add(video)
                    }
                }
            } catch (_: Exception) {
                // Skip failed channel searches
            }
            if (allVideos.size >= 15) break
        }

        return allVideos.take(15)
    }

    // ------------------------------------------------------------------
    // History reader
    // ------------------------------------------------------------------
    private suspend fun readHistory(): List<VideoPlaybackManager.VideoHistoryEntry> {
        return try {
            val json = context.dataStore.data.first()[com.auramusic.app.constants.VideoHistoryKey]
            VideoPlaybackManager.parseVideoHistoryPublic(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        /** Refresh the personalised feed after this many new video watches. */
        const val REFRESH_THRESHOLD = 3
    }
}
