package com.auramusic.innertube

import com.auramusic.innertube.models.Artist
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cached lookup of every channel a video credits.
 *
 * The /next owner-dialog payload is the only place that names a collaboration's second
 * channel, so cards and the player resolve it lazily - once per video, with the result
 * remembered (and an empty answer remembered too, so a single-channel video never pays
 * for the same lookup twice in a session).
 */
object CollaboratorResolver {
    private val lookup = CollaboratorLookup(fetchCollaborators = YouTube::videoCollaborators)

    suspend fun resolve(videoId: String): List<Artist> = lookup.resolve(videoId)
}

/** Test seam: [timeSourceMillis] and [dispatcher] are injectable so cache behaviour is observable. */
internal class CollaboratorLookup(
    private val fetchCollaborators: suspend (String) -> Result<List<Artist>>,
    maxCacheSize: Int = 300,
    private val positiveCacheTtlMillis: Long = 6 * 60 * 60 * 1_000L,
    private val negativeCacheTtlMillis: Long = 30 * 60 * 1_000L,
    private val timeoutMillis: Long = 4_000L,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    timeSourceMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val cache = TimedCollaboratorCache(maxCacheSize, timeSourceMillis)
    private val requestLocks = List(REQUEST_LOCK_COUNT) { Mutex() }

    suspend fun resolve(videoId: String): List<Artist> {
        if (videoId.isBlank()) return emptyList()
        cache[videoId]?.let { return it }

        return withContext(dispatcher) {
            requestLock(videoId).withLock {
                cache[videoId]?.let { return@withLock it }

                val result = withTimeoutOrNull(timeoutMillis) {
                    fetchCollaborators(videoId)
                } ?: return@withLock emptyList()
                val collaborators = result.getOrElse { return@withLock emptyList() }
                val ttlMillis = if (collaborators.isEmpty()) {
                    negativeCacheTtlMillis
                } else {
                    positiveCacheTtlMillis
                }
                cache.put(videoId, collaborators, ttlMillis)
                collaborators
            }
        }
    }

    private fun requestLock(videoId: String): Mutex {
        val index = (videoId.hashCode() and Int.MAX_VALUE) % REQUEST_LOCK_COUNT
        return requestLocks[index]
    }

    private companion object {
        const val REQUEST_LOCK_COUNT = 64
    }
}

private class TimedCollaboratorCache(
    private val maxSize: Int,
    private val timeSourceMillis: () -> Long,
) {
    private data class Entry(
        val collaborators: List<Artist>,
        val expiresAtMillis: Long,
    )

    private val entries = object : LinkedHashMap<String, Entry>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, Entry>?,
        ): Boolean = size > maxSize
    }

    @Synchronized
    operator fun get(videoId: String): List<Artist>? {
        val entry = entries[videoId] ?: return null
        if (entry.expiresAtMillis <= timeSourceMillis()) {
            entries.remove(videoId)
            return null
        }
        return entry.collaborators
    }

    @Synchronized
    fun put(
        videoId: String,
        collaborators: List<Artist>,
        ttlMillis: Long,
    ) {
        entries[videoId] = Entry(
            collaborators = collaborators,
            expiresAtMillis = timeSourceMillis() + ttlMillis,
        )
    }
}
