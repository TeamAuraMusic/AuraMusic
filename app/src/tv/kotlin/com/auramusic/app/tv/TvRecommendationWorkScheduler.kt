package com.auramusic.app.tv

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal object TvRecommendationWorkScheduler {
    private const val WORK_NAME = "tv_recommendation_refresh"
    private const val IMMEDIATE_WORK_NAME = "tv_recommendation_refresh_now"
    private const val REFRESH_INTERVAL_HOURS = 4L
    private const val IMMEDIATE_REFRESH_INPUT = "tv_immediate_refresh"
    private val refreshState = TvRecommendationRefreshState()
    private val refreshLock = Any()
    private var nextRefreshId = 0

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<TvRecommendationWorker>(
            REFRESH_INTERVAL_HOURS,
            TimeUnit.HOURS,
        ).build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueBootRefresh(context: Context) {
        enqueueRefresh(context)
    }

    fun enqueueRefresh(context: Context) {
        synchronized(refreshLock) {
            val requestId = ++nextRefreshId
            if (!refreshState.request(requestId)) {
                Timber.d("Coalesced TV recommendation refresh request id=%d", requestId)
                return
            }
            enqueueImmediateWork(context)
        }
    }

    internal fun onImmediateRefreshStarted() {
        synchronized(refreshLock) {
            // Covers a WorkManager retry or process recreation. This pass reads current state.
            refreshState.markStarted()
        }
    }

    internal fun onImmediateRefreshFinished(context: Context) {
        synchronized(refreshLock) {
            if (refreshState.finish() != null) enqueueImmediateWork(context)
        }
    }

    private fun enqueueImmediateWork(context: Context) {
        val request = OneTimeWorkRequestBuilder<TvRecommendationWorker>()
            .setInputData(workDataOf(IMMEDIATE_REFRESH_INPUT to true))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            // The in-process state queues at most one latest-state follow-up. APPEND keeps
            // the current request alive; OR_REPLACE recovers if an old chain was cancelled.
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    internal fun isImmediateRefresh(inputData: androidx.work.Data): Boolean =
        inputData.getBoolean(IMMEDIATE_REFRESH_INPUT, false)
}
