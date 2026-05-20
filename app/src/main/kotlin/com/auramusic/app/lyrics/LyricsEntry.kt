/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.lyrics

import kotlinx.coroutines.flow.MutableStateFlow

data class WordTimestamp(
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val hasTrailingSpace: Boolean = true
)

data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<WordTimestamp>? = null,
    val romanizedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null),
    val translatedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null),
    val agent: String? = null,
    val isBackground: Boolean = false,
    val isInstrumental: Boolean = false,
    val endTime: Long = -1L,
    val isIntervalIndicator: Boolean = false,
    val intervalGapStart: Long = -1L,
    val intervalGapEnd: Long = -1L
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    /**
     * Returns the effective end time for this entry. If [endTime] is set use it; otherwise
     * fall back to the last word timestamp (in ms) or estimate based on a default duration.
     * For word-synced lyrics, this accurately reflects when the line finishes playing.
     * For standard LRC lines, returns time + estimatedDurationMs (defaults to 3 seconds).
     */
    fun effectiveEndTime(estimatedDurationMs: Long = 3000L): Long {
        if (endTime > 0L) return endTime
        val lastWordEndMs = words?.maxOfOrNull { (it.endTime * 1000).toLong() } ?: -1L
        if (lastWordEndMs > 0L) return lastWordEndMs
        // For standard LRC without word timings, estimate based on next line distance or default
        return time + estimatedDurationMs
    }

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}
