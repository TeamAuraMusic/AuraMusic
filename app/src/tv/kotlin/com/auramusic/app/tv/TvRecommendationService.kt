package com.auramusic.app.tv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.auramusic.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Publishes "Continue Listening" recommendations in response to playback events. */
class TvRecommendationService : android.app.Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshState = TvRecommendationRefreshState()

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "TV Recommendations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continue Listening recommendations for Google TV home screen"
            }
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AuraMusic")
            .setContentText("Updating recommendations...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        if (refreshState.request(startId)) {
            refreshRecommendations(startId)
        }
        return START_NOT_STICKY
    }

    private fun refreshRecommendations(startId: Int) {
        scope.launch {
            try {
                updateRecommendations()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update TV recommendations")
            } finally {
                withContext(Dispatchers.Main.immediate) {
                    val nextStartId = refreshState.finish()
                    if (nextStartId != null) {
                        refreshRecommendations(nextStartId)
                    } else {
                        stopSelfResult(startId)
                    }
                }
            }
        }
    }

    private suspend fun updateRecommendations() {
        val queueTitle = TvRecommendationPublisher.publish(this) ?: return
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AuraMusic")
            .setContentText("Continue Listening • $queueTitle")
            .setOngoing(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "tv_recommendations"
        private const val NOTIFICATION_ID = 9999

        fun start(context: Context) {
            val intent = Intent(context, TvRecommendationService::class.java)
            context.startForegroundService(intent)
        }
    }
}
