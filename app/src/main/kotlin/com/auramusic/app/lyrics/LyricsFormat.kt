/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.lyrics

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private val LRC_TIMESTAMP_HINT = Regex("""\[\d{1,2}:\d{2}""")

/**
 * Whether raw lyrics text appears to be time-synced (LRC-style), including when a BOM or
 * leading blank lines precede the first `[mm:ss.xx]` tag.
 */
fun lyricsTextLooksSynced(lyrics: String?): Boolean {
    if (lyrics.isNullOrBlank()) return false
    val t = lyrics.trim().removePrefix("\uFEFF").trimStart()
    if (t.startsWith('[')) return true
    return LRC_TIMESTAMP_HINT.containsMatchIn(t.take(4096))
}

/**
 * Returns the set of line indices that are currently active (being sung).
 * A line is active if playback position >= line.time AND position <= line end time.
 * Line end time = the last word's endTime if word timings exist, otherwise the next line's start time.
 * Supports simultaneous singers whose lines overlap in time.
 */
fun findActiveLineIndices(
    lines: List<LyricsEntry>,
    position: Long,
): Set<Int> {
    val active = mutableSetOf<Int>()
    val hasWordTimings = lines.any { !it.words.isNullOrEmpty() }

    for (index in lines.indices) {
        val line = lines[index]
        if (line.time > position) break

        val lineEndMs: Long = if (!line.words.isNullOrEmpty()) {
            (line.words.last().endTime * 1000).toLong()
        } else {
            if (index + 1 < lines.size) lines[index + 1].time else Long.MAX_VALUE
        }

        if (position <= lineEndMs) {
            active.add(index)
        }
    }

    if (!hasWordTimings && active.size > 1) {
        val mainActive = active.filter { !lines[it].isBackground }
        if (mainActive.size > 1) {
            val maxTime = mainActive.maxOf { lines[it].time }
            active.removeAll { it in mainActive && lines[it].time < maxTime }
        }
    }

    return active
}

/**
 * Triggers a resync of the lyrics view (auto-scroll jump back to the active line).
 */
object LyricsResyncHelper {
    private val _resyncTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resyncTrigger: SharedFlow<Unit> = _resyncTrigger.asSharedFlow()

    fun triggerResync() {
        _resyncTrigger.tryEmit(Unit)
    }
}
