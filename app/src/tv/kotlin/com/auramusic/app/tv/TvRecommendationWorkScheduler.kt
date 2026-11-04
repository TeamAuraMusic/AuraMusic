package com.auramusic.app.tv

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object TvRecommendationWorkScheduler {
    private const val WORK_NAME = "tv_recommendation_refresh"
    private const val REFRESH_INTERVAL_HOURS = 4L

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
}
