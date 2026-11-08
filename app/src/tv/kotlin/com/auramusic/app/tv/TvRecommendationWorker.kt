package com.auramusic.app.tv

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import timber.log.Timber

internal class TvRecommendationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val immediate = TvRecommendationWorkScheduler.isImmediateRefresh(inputData)
        if (immediate) TvRecommendationWorkScheduler.onImmediateRefreshStarted()
        var succeeded = false
        val result = try {
            val queueTitle = TvRecommendationPublisher.publish(applicationContext)
            Timber.i("TV recommendation refresh completed queue=%s", queueTitle ?: "no programs")
            succeeded = true
            Result.success()
        } catch (e: CancellationException) {
            if (immediate) TvRecommendationWorkScheduler.onImmediateRefreshFinished(applicationContext)
            throw e
        } catch (e: Exception) {
            Timber.w(e, "TV recommendation refresh failed")
            Result.retry()
        }
        if (immediate && succeeded) {
            TvRecommendationWorkScheduler.onImmediateRefreshFinished(applicationContext)
        }
        return result
    }
}
