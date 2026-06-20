package com.auramusic.app.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Triggered on device boot to refresh TV home screen recommendations.
 * This ensures the "Continue Listening" row is always up-to-date
 * when the user turns on their TV.
 */
class TvBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("Boot completed, refreshing TV recommendations")
            TvRecommendationService.start(context)
        }
    }
}
