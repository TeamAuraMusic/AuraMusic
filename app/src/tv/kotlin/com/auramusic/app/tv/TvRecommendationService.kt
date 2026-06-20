package com.auramusic.app.tv

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.auramusic.app.R
import com.auramusic.app.models.PersistQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.ObjectInputStream

/**
 * Android TV Recommendations service.
 *
 * Publishes "Continue Listening" recommendations to the Google TV home screen,
 * similar to how Netflix shows "Continue Watching".
 */
class TvRecommendationService : android.app.Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            try {
                updateRecommendations()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update TV recommendations")
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun updateRecommendations() {
        val ctx = this

        val persistQueue = readPersistQueue(ctx) ?: run {
            Timber.d("No persisted queue for recommendations")
            return
        }

        val queueTitle = persistQueue.title ?: "Continue Listening"
        val currentIndex = persistQueue.mediaItemIndex.coerceIn(0, (persistQueue.items.lastIndex).coerceAtLeast(0))
        val items = persistQueue.items.drop(currentIndex).take(MAX_RECOMMENDATIONS)

        if (items.isEmpty()) {
            Timber.d("Empty queue, skipping recommendations")
            return
        }

        val channelId = ensureChannel(ctx)

        // Remove old programs for this channel
        try {
            ctx.contentResolver.delete(
                TvContractCompat.buildProgramsUriForChannel(channelId),
                null, null
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to clean old programs")
        }

        var count = 0
        for (mediaMeta in items) {
            val title = mediaMeta.title.ifBlank { "Unknown song" }
            val artist = mediaMeta.artists.firstOrNull()?.name.orEmpty()
            val artworkUri = mediaMeta.thumbnailUrl.orEmpty()
            val durationMs = mediaMeta.duration.coerceAtLeast(0)
            val mediaId = mediaMeta.id

            if (mediaId.isBlank()) continue

            try {
                val launchIntent = Intent(Intent.ACTION_VIEW).apply {
                    setClassName(ctx, "com.auramusic.app.TvMainActivity")
                    putExtra("media_id", mediaId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val program = PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setTitle(title)
                    .setDescription("$artist • $queueTitle")
                    .setLongDescription("Continue listening from $queueTitle")
                    .setIntent(launchIntent)
                    .setContentId(mediaId)
                    .setDurationMillis(durationMs)
                    .setSearchable(true)
                    .apply {
                        if (artworkUri.isNotBlank()) {
                            setPosterArtUri(Uri.parse(artworkUri))
                            setThumbnailUri(Uri.parse(artworkUri))
                        }
                    }
                    .build()

                ctx.contentResolver.insert(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    program.toContentValues()
                )
                count++
            } catch (e: Exception) {
                Timber.w(e, "Failed to insert recommendation for $title")
            }
        }

        Timber.d("Published $count TV recommendations for '$queueTitle'")

        // Foreground notification required by Android for services
        val notification: Notification = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AuraMusic")
            .setContentText("Continue Listening • $queueTitle")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(ctx: Context): Long {
        // Look for existing channel for this package
        ctx.contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            arrayOf(TvContractCompat.Channels._ID),
            "${TvContractCompat.Channels.COLUMN_PACKAGE_NAME} = ?",
            arrayOf(ctx.packageName),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        // Create a new channel
        val channel = PreviewChannel.Builder()
            .setDisplayName("AuraMusic")
            .setDescription("Continue listening recommendations")
            .build()

        val channelUri = ctx.contentResolver.insert(
            TvContractCompat.Channels.CONTENT_URI,
            channel.toContentValues()
        )

        return channelUri?.lastPathSegment?.toLongOrNull() ?: 0L
    }

    private fun readPersistQueue(context: Context): PersistQueue? {
        val file = File(context.filesDir, "persistent_queue.data")
        if (!file.exists()) return null
        return try {
            ObjectInputStream(file.inputStream()).use { ois ->
                ois.readObject() as? PersistQueue
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to read persisted queue")
            null
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "tv_recommendations"
        private const val NOTIFICATION_ID = 9999
        private const val MAX_RECOMMENDATIONS = 5

        fun start(context: Context) {
            val intent = Intent(context, TvRecommendationService::class.java)
            context.startForegroundService(intent)
        }
    }
}
