package com.auramusic.app.tv

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Bounds simultaneous source resolution, duration probes, and full-song fetches. */
internal class TvPreviewAudioDownloadGate(maxConcurrentDownloads: Int) {
    private val permits = Semaphore(maxConcurrentDownloads.also { require(it > 0) })

    suspend fun <T> run(block: suspend () -> T): T = permits.withPermit { block() }

    suspend fun <Source, Result> runWithSource(
        resolve: suspend () -> Source,
        refresh: (suspend () -> Source)? = null,
        shouldRefresh: (Throwable) -> Boolean = { false },
        block: suspend (Source) -> Result,
    ): Pair<Source, Result> = permits.withPermit {
        val source = resolve()
        try {
            source to block(source)
        } catch (error: Exception) {
            if (refresh == null || !shouldRefresh(error)) throw error
            val refreshedSource = refresh()
            refreshedSource to block(refreshedSource)
        }
    }
}
