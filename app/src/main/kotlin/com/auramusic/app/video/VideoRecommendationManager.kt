package com.auramusic.app.video

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.auramusic.app.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    // Shared at process level: the screen creates a fresh manager on every entry, and
    // keeping sections instance-scoped would throw the built feed away on each tab switch
    // and re-run every network search just to repaint the same rows.
    private val _sections = sharedSections
    val sections: StateFlow<List<FeedSection>> = _sections.asStateFlow()

    private val _isLoading = sharedLoading
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // How many videos have been watched since last refresh. Compose snapshot state, not
    // plain vars: VideosScreen observes shouldRefresh through snapshotFlow, which only
    // re-emits when the reads inside it touch snapshot-tracked state.
    private var watchCountSinceRefresh by mutableIntStateOf(0)
    private var lastRefreshWatchCount by mutableIntStateOf(0)
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
                coroutineScope {
                    // The four sections are independent of one another and three of
                    // them are a handful of round trips each, so they all start at
                    // once. What stays serial is only the waiting, and that happens
                    // in the order the page reads.
                    // Each one fails on its own. Running them together would
                    // otherwise mean a single failed search cancelling the other two
                    // as well, and taking the sections that had already been found
                    // down with it.
                    val channelVideos = async {
                        runCatching { buildChannelRecommendations(history) }.getOrDefault(emptyList())
                    }
                    val recommended = async {
                        runCatching { buildRecommended(history) }.getOrDefault(emptyList())
                    }
                    val subscriptions = async {
                        runCatching { buildSubscriptionsFeed() }.getOrDefault(emptyList())
                    }

                    val built = mutableListOf<FeedSection>()

                    // A first build fills the page in from the top as each section is
                    // ready - Continue Watching needs no network at all and is there
                    // within a frame or two, before the first search has answered.
                    // Waiting for all of them before showing any of them is what made
                    // the page sit blank while eleven requests ran in a row.
                    //
                    // A refresh does not do this. There the old rows are already on
                    // screen, and replacing the list after every section would empty
                    // the page and fill it again, one row at a time, in front of the
                    // user. Those are swapped once, when the whole new set is ready.
                    val fillAsItGoes = _sections.value.isEmpty()

                    fun add(
                        title: String,
                        videos: List<com.auramusic.innertube.models.YouTubeVideoItem>,
                    ) {
                        // Every section is passed through MusicContentFilter: these
                        // searches run on regular YouTube, so without it a
                        // "recommended" section happily fills up with vlogs.
                        if (videos.isEmpty()) return
                        built += FeedSection(title, videos)
                        if (fillAsItGoes) _sections.value = built.toList()
                    }

                    add("Continue Watching", buildContinueWatching(history))
                    add("From Channels You Watch", channelVideos.await())
                    add("Recommended For You", recommended.await())
                    add("Subscriptions", subscriptions.await())

                    built.toList()
                }
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
        // Anything the user started but did not finish. The thresholds are shared with
        // the player so the section, the card bars and "does this resume?" never disagree.
        val partial = history.filter { entry ->
            entry.durationMs > 0 &&
                entry.watchProgress > RESUME_MIN_PROGRESS &&
                entry.watchProgress < WATCHED_PROGRESS
        }.sortedByDescending { it.lastPlayedAt }
            .take(10)

        return MusicContentFilter.filter(partial.mapNotNull { entry ->
            com.auramusic.innertube.models.YouTubeVideoItem(
                videoId = entry.videoId,
                title = entry.title,
                channelName = entry.channelName,
                channelId = entry.channelId,
                // History rows used to arrive with no credited channel at all, so the
                // card showed a bare title with no avatar and no way into the channel.
                channels = if (entry.channelName.isNotBlank()) {
                    listOf(
                        com.auramusic.innertube.models.Artist(
                            name = entry.channelName,
                            id = entry.channelId,
                            avatarUrl = entry.channelAvatarUrl,
                        ),
                    )
                } else {
                    emptyList()
                },
                thumbnails = listOfNotNull(
                    entry.videoThumbnailUrl?.let { url ->
                        com.auramusic.innertube.models.Thumbnail(url = url, width = null, height = null)
                    }
                ),
            )
        })
    }

    /**
     * A channel a section draws from.
     *
     * [channelId] is preferred and is what makes the section reliable: the channel's
     * own tab is asked directly. Older history rows carry a name but no id, so the
     * name is kept as the way back to it.
     */
    private data class ChannelSource(val channelId: String?, val name: String)

    /**
     * Videos from the channels the user watches most.
     *
     * Each channel is asked for its own videos rather than searched for by name. A
     * search returns whatever YouTube finds relevant, which in practice was one busy
     * channel filling the whole quota and the loop stopping before it ever reached
     * the others - so a section titled with every channel showed one of them. Asking
     * the channel itself guarantees the rows belong to it, and taking a few from each
     * and interleaving them is what makes the section plural.
     */
    private suspend fun buildChannelRecommendations(
        history: List<VideoPlaybackManager.VideoHistoryEntry>,
    ): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        val sources = history
            .filter { it.channelName.isNotBlank() }
            .groupBy { it.channelId?.takeIf(String::isNotBlank) ?: it.channelName }
            .entries
            .sortedByDescending { it.value.size }
            .take(MAX_SECTION_CHANNELS)
            .map { (_, entries) ->
                val first = entries.first()
                ChannelSource(
                    channelId = first.channelId?.takeIf(String::isNotBlank),
                    name = first.channelName,
                )
            }

        if (sources.isEmpty()) return emptyList()

        // Recently watched videos are the one thing this section should not offer:
        // the point is the next thing from a channel, not the last one again.
        val recentlyWatched = history.take(20).mapTo(HashSet()) { it.videoId }
        return sectionFrom(sources, excluded = recentlyWatched)
    }

    /**
     * A few videos from each source, interleaved, music only.
     */
    private suspend fun sectionFrom(
        sources: List<ChannelSource>,
        excluded: Set<String>,
    ): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        val perChannel = coroutineScope {
            sources.map { source ->
                async {
                    videosFromChannel(source)
                        .filterNot { it.videoId in excluded }
                        .distinctBy { it.videoId }
                        .take(MAX_PER_CHANNEL)
                }
            }.awaitAll()
        }
        return MusicContentFilter.filter(interleave(perChannel)).take(MAX_SECTION_VIDEOS)
    }

    /**
     * One channel's own videos, newest first.
     */
    private suspend fun videosFromChannel(
        source: ChannelSource,
    ): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        if (source.channelId != null) {
            val videos = com.auramusic.innertube.YouTube.youtubeChannel(source.channelId)
                .getOrNull()
                ?.videos
                .orEmpty()
            if (videos.isNotEmpty()) return videos
        }
        if (source.name.isBlank()) return emptyList()
        // No id, so a search is the only way back to the channel - and only its own
        // rows are kept, which is the whole point of going per channel.
        return com.auramusic.innertube.YouTube.youtubeSearchFeed(
            query = source.name,
            continuation = null,
        ).getOrNull()
            ?.items
            ?.filter { it.channelName.equals(source.name, ignoreCase = true) }
            .orEmpty()
    }

    /**
     * Round-robin across the channels, so the first rows are not all one of them.
     */
    private fun interleave(
        perChannel: List<List<com.auramusic.innertube.models.YouTubeVideoItem>>,
    ): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        val seen = HashSet<String>()
        val rows = mutableListOf<com.auramusic.innertube.models.YouTubeVideoItem>()
        val longest = perChannel.maxOfOrNull { it.size } ?: 0
        for (index in 0 until longest) {
            for (videos in perChannel) {
                val video = videos.getOrNull(index) ?: continue
                if (seen.add(video.videoId)) rows += video
            }
        }
        return rows
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

        return MusicContentFilter.filter(allVideos.take(15))
    }

    /**
     * Videos from the channels the user is subscribed to.
     *
     * Same shape as the channels section, and for the same reason: it searched for
     * each channel by name and stopped as soon as fifteen videos had been found, so
     * the first busy channel filled it and the rest were never asked.
     */
    private suspend fun buildSubscriptionsFeed(): List<com.auramusic.innertube.models.YouTubeVideoItem> {
        val subs = try {
            val subsJson = context.dataStore.data.first()[com.auramusic.app.constants.VideoSubscribedChannelsKey]
            parseSubscribedChannelsPublic(subsJson)
        } catch (_: Exception) {
            emptyList<VideoPlaybackManager.VideoSubscribedChannel>()
        }

        if (subs.isEmpty()) return emptyList()

        val sources = subs
            .sortedByDescending { it.subscribedAt }
            .take(MAX_SECTION_CHANNELS)
            .map { ChannelSource(channelId = it.channelId.ifBlank { null }, name = it.name) }

        return sectionFrom(sources, excluded = emptySet())
    }

    // ------------------------------------------------------------------
    // History reader
    // ------------------------------------------------------------------
    private suspend fun readHistory(): List<VideoPlaybackManager.VideoHistoryEntry> {
        return try {
            val json = context.dataStore.data.first()[com.auramusic.app.constants.VideoHistoryKey]
            parseVideoHistoryPublic(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        /** Refresh the personalised feed after this many new video watches. */
        const val REFRESH_THRESHOLD = 3

        /**
         * How many channels a section draws from, and how many videos from each.
         *
         * A few from several channels rather than one channel's newest page: a
         * section named after channels should not be a single channel.
         */
        private const val MAX_SECTION_CHANNELS = 3
        private const val MAX_PER_CHANNEL = 6
        private const val MAX_SECTION_VIDEOS = 18

        internal val sharedSections = MutableStateFlow<List<FeedSection>>(emptyList())
        private val sharedLoading = MutableStateFlow(false)

        /**
         * Drops the built sections so the next screen entry rebuilds them. Used when the
         * history changed in a way the rebuild threshold would not notice (a video that
         * just finished and therefore left Continue Watching).
         */
        fun invalidateSections() {
            sharedSections.value = emptyList()
        }
    }
}
