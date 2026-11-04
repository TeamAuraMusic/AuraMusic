package com.auramusic.app.tv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContentProviderOperation
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.auramusic.app.R
import com.auramusic.app.constants.HideVideoSongsKey
import com.auramusic.app.extensions.filterVideoSongs
import com.auramusic.app.models.PersistQueue
import com.auramusic.app.ui.tv.tvDatabase
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.ObjectInputStream
import kotlin.random.Random

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
        // Create notification channel (required for Android 8+)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "TV Recommendations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continue Listening recommendations for Google TV home screen"
            }
        )

        // Android 14 requires startForeground() within 5 seconds
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AuraMusic")
            .setContentText("Updating recommendations...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

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

        val persistQueue = readPersistQueue(ctx)
        val queueTitle = persistQueue?.title?.takeIf(String::isNotBlank) ?: "Continue Listening"
        val queueItems = persistQueue?.let { queue ->
            val currentIndex = queue.mediaItemIndex.coerceIn(0, queue.items.lastIndex.coerceAtLeast(0))
            queue.items.drop(currentIndex)
        }.orEmpty()
        val rotationSeed = tvRecommendationRotationSeed(System.currentTimeMillis())
        val quickPickItems = try {
            val hideVideoSongs = ctx.dataStore.get(HideVideoSongsKey, false)
            ctx.tvDatabase().quickPicks().first()
                .filterVideoSongs(hideVideoSongs)
                .shuffled(Random(rotationSeed))
        } catch (e: Exception) {
            Timber.w(e, "Failed to load TV Quick Picks; publishing the saved queue only")
            emptyList()
        }
        val queueItemsById = queueItems.associateBy { it.id }
        val quickPickItemsById = quickPickItems.associateBy { it.id }
        val selectedMediaIds = selectTvRecommendationMediaIds(
            queueMediaIds = queueItems.map { it.id },
            quickPickMediaIds = quickPickItems.map { it.id },
            limit = MAX_RECOMMENDATIONS,
        )

        if (selectedMediaIds.isEmpty()) {
            Timber.d("No queue or Quick Picks available for TV recommendations")
            return
        }

        val channelId = ensureChannel(ctx)

        val occurrenceByMediaId = mutableMapOf<String, Int>()
        val programValues = selectedMediaIds.mapIndexedNotNull { index, mediaId ->
            val queueItem = queueItemsById[mediaId]
            val quickPick = quickPickItemsById[mediaId]
            val title = (quickPick?.title ?: queueItem?.title).orEmpty().ifBlank { "Unknown song" }
            val artist = quickPick?.artists?.firstOrNull()?.name
                ?: queueItem?.artists?.firstOrNull()?.name.orEmpty()
            val artworkUri = quickPick?.thumbnailUrl ?: queueItem?.thumbnailUrl.orEmpty()
            val durationMs = quickPick?.song?.duration?.coerceAtLeast(0)?.times(1000)
                ?: queueItem?.duration?.coerceAtLeast(0)
                ?: 0
            val isQuickPick = quickPick != null
            val occurrence = occurrenceByMediaId.getOrDefault(mediaId, 0)
            occurrenceByMediaId[mediaId] = occurrence + 1
            val internalProviderId = "auramusic:track:$mediaId:$occurrence"

            try {
                val launchIntent = Intent(Intent.ACTION_VIEW).apply {
                    setClassName(ctx, "com.auramusic.app.TvMainActivity")
                    putExtra("media_id", mediaId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val program = PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setType(TvContractCompat.PreviewProgramColumns.TYPE_TRACK)
                    .setTitle(title)
                    .setDescription("$artist • ${if (isQuickPick) "Quick Picks" else queueTitle}")
                    .setLongDescription(
                        if (isQuickPick) "Recommended from your listening history"
                        else "Continue listening from $queueTitle"
                    )
                    .setIntent(launchIntent)
                    .setContentId(mediaId)
                    .setInternalProviderId(internalProviderId)
                    .setWeight(selectedMediaIds.size - index)
                    .setDurationMillis(durationMs)
                    .setSearchable(true)
                    .apply {
                        if (artworkUri.isNotBlank()) {
                            setPosterArtUri(Uri.parse(artworkUri))
                            setThumbnailUri(Uri.parse(artworkUri))
                        }
                    }
                    .build()

                program.toContentValues()
            } catch (e: Exception) {
                Timber.w(e, "Failed to insert recommendation for $title")
                null
            }
        }

        if (programValues.isEmpty()) {
            Timber.w("No valid TV recommendations to publish; keeping existing cards")
            return
        }

        if (channelId <= 0L) {
            Timber.w("Could not create TV recommendation channel; keeping existing cards")
            return
        }

        val existingPrograms = readExistingPrograms(ctx, channelId)
        val internalIds = programValues.map { values ->
            values.getAsString(TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID)
        }
        val plan = planPreviewProgramReconciliation(existingPrograms, internalIds)
        val valuesByInternalId = programValues.associateBy { values ->
            values.getAsString(TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID)
        }
        val operations = ArrayList<ContentProviderOperation>(
            plan.updates.size + plan.inserts.size + plan.deletes.size
        )
        plan.updates.forEach { (programId, internalId) ->
            operations += ContentProviderOperation.newUpdate(
                TvContractCompat.buildPreviewProgramUri(programId)
            ).withValues(valuesByInternalId.getValue(internalId)).build()
        }
        plan.inserts.forEach { internalId ->
            operations += ContentProviderOperation.newInsert(
                TvContractCompat.PreviewPrograms.CONTENT_URI
            ).withValues(valuesByInternalId.getValue(internalId)).build()
        }
        plan.deletes.forEach { programId ->
            operations += ContentProviderOperation.newDelete(
                TvContractCompat.buildPreviewProgramUri(programId)
            ).build()
        }
        if (operations.isNotEmpty()) {
            ctx.contentResolver.applyBatch(TvContractCompat.AUTHORITY, operations)
        }

        val quickPickCount = selectedMediaIds.count(quickPickItemsById::containsKey)
        Timber.d(
            "Published ${programValues.size} TV recommendations for '$queueTitle' " +
                "($quickPickCount Quick Picks; ${plan.inserts.size} added, ${plan.updates.size} updated, " +
                "${plan.deletes.size} removed)"
        )

        // Update the foreground notification with final info
        val notification: Notification = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AuraMusic")
            .setContentText("Continue Listening • $queueTitle")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun readExistingPrograms(context: Context, channelId: Long): List<ExistingPreviewProgram> {
        val uri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId)
        val projection = arrayOf(
            TvContractCompat.PreviewPrograms._ID,
            TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
        )
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
            ?: error("TV provider returned no preview-program cursor")

        return cursor.use {
            val idColumn = it.getColumnIndexOrThrow(TvContractCompat.PreviewPrograms._ID)
            val internalIdColumn = it.getColumnIndexOrThrow(
                TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID
            )
            buildList {
                while (it.moveToNext()) {
                    add(
                        ExistingPreviewProgram(
                            programId = it.getLong(idColumn),
                            internalProviderId = it.getString(internalIdColumn),
                        )
                    )
                }
            }
        }
    }

    private fun ensureChannel(ctx: Context): Long {
        // Look for existing channel for this package
        ctx.contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            arrayOf(
                TvContractCompat.Channels._ID,
                TvContractCompat.Channels.COLUMN_PACKAGE_NAME,
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val packageNameColumn = cursor.getColumnIndexOrThrow(TvContractCompat.Channels.COLUMN_PACKAGE_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(packageNameColumn) == ctx.packageName) {
                    return cursor.getLong(0).also {
                        ensureChannelLogo(ctx, it)
                        requestChannelVisibility(ctx, it)
                    }
                }
            }
        }

        // Create a new channel
        val channel = PreviewChannel.Builder()
            .setDisplayName("AuraMusic")
            .setDescription("Continue listening recommendations")
            .setAppLinkIntent(Intent(ctx, com.auramusic.app.TvMainActivity::class.java))
            .build()

        val channelUri = ctx.contentResolver.insert(
            TvContractCompat.Channels.CONTENT_URI,
            channel.toContentValues()
        )

        return channelUri?.lastPathSegment?.toLongOrNull()?.also {
            ensureChannelLogo(ctx, it)
            requestChannelVisibility(ctx, it)
        } ?: 0L
    }

    private fun ensureChannelLogo(ctx: Context, channelId: Long) {
        val logoUri = Uri.parse("android.resource://${ctx.packageName}/${R.mipmap.ic_launcher_foreground}")
        if (ChannelLogoUtils.loadChannelLogo(ctx, channelId) == null) {
            ChannelLogoUtils.storeChannelLogo(ctx, channelId, logoUri)
        }
    }

    private fun requestChannelVisibility(ctx: Context, channelId: Long) {
        if (channelId <= 0L) return
        try {
            TvContractCompat.requestChannelBrowsable(ctx, channelId)
        } catch (e: Exception) {
            Timber.w(e, "TV launcher rejected recommendation channel visibility request")
        }
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
        private const val MAX_RECOMMENDATIONS = 10

        fun start(context: Context) {
            val intent = Intent(context, TvRecommendationService::class.java)
            context.startForegroundService(intent)
        }
    }
}
