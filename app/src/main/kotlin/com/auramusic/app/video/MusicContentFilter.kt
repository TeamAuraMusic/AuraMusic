package com.auramusic.app.video

import android.util.LruCache
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Decides whether YouTube content is music, by asking YouTube.
 *
 * YouTube's player response tags every video it recognises as music with a
 * `videoDetails.musicVideoType` (official music video, audio-track video, user
 * upload of a track). A video with no such tag is an ordinary YouTube video - vlog,
 * gaming, interview, podcast. That answer comes from YouTube itself, so this is not
 * a keyword guess that can be fooled by a video titled "song".
 *
 * Every check costs a network call, so results are layered:
 *  1. [TOPIC_CHANNEL_SUFFIX] channels only ever contain music - no call needed.
 *  2. Previously answered videos come from [cache]; the verdict cannot change.
 *  3. Only what is left is asked over the network, concurrently and deduplicated.
 */
object MusicContentFilter {

    /** Auto-generated music channels. These are music by YouTube's own construction. */
    private const val TOPIC_CHANNEL_SUFFIX = " - Topic"

    /** Small enough to stay cheap, large enough to cover a browsing session. */
    private const val CACHE_SIZE = 4000

    /** Cap on simultaneous player calls, so a big page cannot flood the network. */
    private const val MAX_PARALLEL_CHECKS = 6

    /**
     * How many videos of a channel or playlist we are willing to check before
     * judging the whole thing. Sampling keeps the cost bounded.
     */
    const val SAMPLE_SIZE = 5

    /** A playlist/channel passes when at least this share of the sample is music. */
    private const val SAMPLE_PASS_RATIO = 0.6

    private val cache = LruCache<String, Boolean>(CACHE_SIZE)

    /** De-duplicates concurrent checks for the same video. */
    private val inFlight = mutableMapOf<String, Mutex>()
    private val inFlightLock = Mutex()

    /** Verdict for a single video. Cheap when known, authoritative otherwise. */
    fun isTopicChannel(channelName: String?): Boolean =
        channelName?.trim()?.endsWith(TOPIC_CHANNEL_SUFFIX, ignoreCase = true) == true

    fun isKnown(videoId: String): Boolean = cache.get(videoId) != null

    /** True when [channelName] is an auto-generated music channel (no network call). */
    fun isDefinitelyMusic(item: YouTubeVideoItem): Boolean = isTopicChannel(item.channelName)

    suspend fun isMusic(videoId: String): Boolean {
        cache.get(videoId)?.let { return it }
        return mutexFor(videoId).withLock {
            // Another coroutine may have resolved it while we waited for the lock.
            cache.get(videoId)?.let { return@withLock it }
            val verdict = YouTube.musicVideoType(videoId).getOrNull() != null
            cache.put(videoId, verdict)
            verdict
        }
    }

    suspend fun isMusic(item: YouTubeVideoItem): Boolean {
        if (isDefinitelyMusic(item)) return true
        return isMusic(item.videoId)
    }

    /**
     * Keeps only music videos. Results stream in as answers arrive so the UI can fill
     * progressively instead of blocking on the slowest check.
     *
     * @param onEmit called with each newly confirmed music video, in arrival order.
     */
    suspend fun filterStream(
        items: List<YouTubeVideoItem>,
        onEmit: suspend (List<YouTubeVideoItem>) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val alreadyMusic = items.filter { isDefinitelyMusic(it) }
        if (alreadyMusic.isNotEmpty()) onEmit(alreadyMusic)

        val remaining = items.filterNot { isDefinitelyMusic(it) }
        if (remaining.isEmpty()) return@withContext

        // Checked in bounded chunks so one page cannot fire dozens of player calls
        // at once, and each chunk is handed to the caller as soon as it resolves.
        remaining.chunked(MAX_PARALLEL_CHECKS).forEach { chunk ->
            val verdicts = coroutineScope {
                chunk.map { item -> async { item to isMusic(item) } }.awaitAll()
            }
            val confirmed = verdicts.filter { it.second }.map { it.first }
            if (confirmed.isNotEmpty()) onEmit(confirmed)
        }
    }

    /** Blocking variant: returns the music videos once every check has resolved. */
    suspend fun filter(items: List<YouTubeVideoItem>): List<YouTubeVideoItem> {
        val kept = mutableListOf<YouTubeVideoItem>()
        filterStream(items) { batch ->
            batch.forEach { item ->
                if (kept.none { it.videoId == item.videoId }) kept.add(item)
            }
        }
        return kept
    }

    /**
     * Judges a channel by sampling its videos. Auto-generated music channels pass
     * instantly; otherwise enough of the sample must be music.
     */
    suspend fun isMusicChannel(
        channelName: String?,
        sampleVideos: List<YouTubeVideoItem>,
    ): Boolean {
        if (isTopicChannel(channelName)) return true
        return passesSample(sampleVideos)
    }

    /**
     * Judges a playlist by sampling its videos. A playlist needs no name check - its
     * contents decide.
     */
    suspend fun isMusicPlaylist(sampleVideos: List<YouTubeVideoItem>): Boolean =
        passesSample(sampleVideos)

    private suspend fun passesSample(sample: List<YouTubeVideoItem>): Boolean {
        if (sample.isEmpty()) return false
        val probe = sample.take(SAMPLE_SIZE)
        var music = 0
        probe.forEach { item ->
            if (isMusic(item)) music++
        }
        return music >= (probe.size * SAMPLE_PASS_RATIO).toInt().coerceAtLeast(1)
    }

    private suspend fun mutexFor(videoId: String): Mutex = inFlightLock.withLock {
        inFlight.getOrPut(videoId) { Mutex() }
    }

    /** Test/maintenance hook: forget everything we have learned. */
    fun clearCache() = cache.evictAll()
}
