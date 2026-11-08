package com.auramusic.app.tv

/** Main-thread polling loop. Schedules refreshes only while its focused-card predicate stays true. */
internal class FocusedPlaybackRefreshLoop(
    private val intervalMs: Long,
    private val isFocused: () -> Boolean,
    private val refresh: () -> Unit,
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
) {
    private var queued = false
    private val tick = Runnable {
        queued = false
        if (!isFocused()) return@Runnable
        refresh()
        scheduleNext()
    }

    fun start() {
        scheduleNext()
    }

    fun stop() {
        if (queued) removeCallbacks(tick)
        queued = false
    }

    private fun scheduleNext() {
        if (queued || !isFocused()) return
        queued = true
        postDelayed(tick, intervalMs)
    }
}
