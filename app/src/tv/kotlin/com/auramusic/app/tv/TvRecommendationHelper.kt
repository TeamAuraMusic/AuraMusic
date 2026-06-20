package com.auramusic.app.tv

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper to trigger TV home screen recommendation updates.
 *
 * Updates are debounced so rapid queue changes (e.g., next track)
 * don't spam the system. The latest state is always what gets published.
 */
object TvRecommendationHelper {

    private var pendingJob: kotlinx.coroutines.Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Schedule a recommendation update. Multiple rapid calls are debounced —
     * only the last one within the debounce window actually fires.
     */
    fun scheduleUpdate(context: Context) {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(2000) // Debounce 2 seconds
            try {
                TvRecommendationService.start(context.applicationContext)
            } catch (_: Exception) {
                // Best effort — don't crash if the system rejects it
            }
        }
    }
}
