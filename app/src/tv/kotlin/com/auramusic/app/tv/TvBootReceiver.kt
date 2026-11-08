package com.auramusic.app.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Refresh TV home screen recommendations after boot and app replacement.
 */
class TvBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Timber.d("Boot completed, refreshing TV recommendations")
                TvRecommendationWorkScheduler.ensureScheduled(context.applicationContext)
                TvRecommendationWorkScheduler.enqueueBootRefresh(context.applicationContext)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.d("AuraMusic updated, refreshing TV recommendations")
                TvRecommendationWorkScheduler.ensureScheduled(context.applicationContext)
                TvRecommendationWorkScheduler.enqueueRefresh(context.applicationContext)
            }
        }
    }
}
