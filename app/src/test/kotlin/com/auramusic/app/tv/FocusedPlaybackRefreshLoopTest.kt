package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusedPlaybackRefreshLoopTest {
    @Test
    fun refreshesEveryIntervalWhileFocusedAndStopsWhenFocusLeaves() {
        val scheduler = ManualScheduler()
        var focused = true
        var refreshes = 0
        val loop = FocusedPlaybackRefreshLoop(
            intervalMs = 500,
            isFocused = { focused },
            refresh = { refreshes++ },
            postDelayed = scheduler::post,
            removeCallbacks = scheduler::remove,
        )

        loop.start()
        loop.start()
        assertEquals(listOf(500L), scheduler.delays)

        scheduler.runNext()
        assertEquals(1, refreshes)
        assertEquals(listOf(500L, 500L), scheduler.delays)

        focused = false
        loop.stop()
        assertTrue(scheduler.pending.isEmpty())
        assertEquals(1, refreshes)
    }

    @Test
    fun focusLossBeforeTickSkipsRefreshAndDoesNotReschedule() {
        val scheduler = ManualScheduler()
        var focused = true
        var refreshes = 0
        val loop = FocusedPlaybackRefreshLoop(
            intervalMs = 500,
            isFocused = { focused },
            refresh = { refreshes++ },
            postDelayed = scheduler::post,
            removeCallbacks = scheduler::remove,
        )

        loop.start()
        focused = false
        scheduler.runNext()

        assertEquals(0, refreshes)
        assertTrue(scheduler.pending.isEmpty())
    }

    private class ManualScheduler {
        val pending = mutableListOf<Runnable>()
        val delays = mutableListOf<Long>()

        fun post(runnable: Runnable, delayMs: Long) {
            pending += runnable
            delays += delayMs
        }

        fun remove(runnable: Runnable) {
            pending.removeAll { it === runnable }
        }

        fun runNext() {
            pending.removeAt(0).run()
        }
    }
}
