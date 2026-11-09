package com.auramusic.app.tv

import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.ObjectInputStream
import kotlin.random.Random

/** Shared publication path for playback-triggered and periodic recommendation refreshes. */
internal object TvRecommendationPublisher {
    private const val MAX_RECOMMENDATIONS = 10
    private val publishMutex = Mutex()

    private data class PreviewAudio(
        val uri: Uri?,
        val mimeType: String,
        val revisionIdentity: String?,
    )
    private data class PersistQueueSnapshot(val queue: PersistQueue?, val available: Boolean)

    suspend fun publish(context: Context): String? = publishMutex.withLock {
        withContext(Dispatchers.IO) {
            publishLocked(context.applicationContext)
        }
    }

    private suspend fun publishLocked(context: Context): String? {
        val queueSnapshot = readPersistQueue(context)
        val persistQueue = queueSnapshot.queue
        val queueTitle = persistQueue?.title?.takeIf(String::isNotBlank) ?: "Continue Listening"
        val queueItems = persistQueue?.let { queue ->
            val currentIndex = queue.mediaItemIndex.coerceIn(0, queue.items.lastIndex.coerceAtLeast(0))
            queue.items.drop(currentIndex)
        }.orEmpty()
        val rotationSeed = tvRecommendationRotationSeed(System.currentTimeMillis())
        var quickPicksSnapshotAvailable = true
        val quickPickItems = try {
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            context.tvDatabase().quickPicks().first()
                .filterVideoSongs(hideVideoSongs)
                .shuffled(Random(rotationSeed))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            quickPicksSnapshotAvailable = false
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
            if (shouldRemoveStaleTvPrograms(
                    queueSnapshotAvailable = queueSnapshot.available,
                    quickPicksSnapshotAvailable = quickPicksSnapshotAvailable,
                    selectedProgramCount = selectedMediaIds.size,
                )
            ) {
                removeStalePrograms(context)
            } else {
                Timber.w("No TV recommendations selected from an incomplete source snapshot; keeping existing cards")
            }
            return null
        }

        val channelId = ensureChannel(context)
        if (channelId <= 0L) {
            Timber.w("Could not create TV recommendation channel; keeping existing cards")
            return null
        }
        val existingPrograms = readExistingPrograms(context, channelId)
        val previewAudioSources = resolvePreviewAudioUris(
            context,
            selectedMediaIds,
            existingPrograms,
        )
        val occurrenceByMediaId = mutableMapOf<String, Int>()
        val sourceArtworkByInternalId = linkedMapOf<String, String>()
        val programValues = selectedMediaIds.mapIndexedNotNull { index, mediaId ->
            val queueItem = queueItemsById[mediaId]
            val quickPick = quickPickItemsById[mediaId]
            val title = (quickPick?.title ?: queueItem?.title).orEmpty().ifBlank { "Unknown song" }
            val artist = quickPick?.artists?.firstOrNull()?.name
                ?: queueItem?.artists?.firstOrNull()?.name.orEmpty()
            val artworkUri = quickPick?.thumbnailUrl ?: queueItem?.thumbnailUrl.orEmpty()
            val previewAudio = previewAudioSources[mediaId]
            val durationMs = quickPick?.song?.duration?.coerceAtLeast(0)?.times(1000)
                ?: queueItem?.duration?.coerceAtLeast(0)
                ?: 0
            val isQuickPick = quickPick != null
            val occurrence = occurrenceByMediaId.getOrDefault(mediaId, 0)
            occurrenceByMediaId[mediaId] = occurrence + 1
            val internalProviderId = "auramusic:track:$mediaId:$occurrence"
            sourceArtworkByInternalId[internalProviderId] = artworkUri

            try {
                val launchIntent = tvRecommendationClickIntent(context, mediaId)

                PreviewProgram.Builder()
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
                        val revision = TvPreviewUri.revision(
                            mediaId,
                            artworkUri,
                            durationMs.toLong(),
                            previewAudio?.revisionIdentity,
                        )
                        setPreviewAudioUri(previewAudio?.uri)
                        if (artworkUri.isNotBlank()) {
                            // Projectivy plays audio-only MP4 quickly; the accessibility overlay
                            // restores the poster and draws its focus-driven waveform.
                            val previewUri = when (projectivyPreviewUriMode(previewAudio?.mimeType)) {
                                ProjectivyPreviewUriMode.AUDIO_ONLY_PROVIDER ->
                                    TvPreviewUri.forAudioOnlyProgram(context, internalProviderId, revision)
                                ProjectivyPreviewUriMode.GENERATED_VIDEO_PROVIDER ->
                                    TvPreviewUri.forProgram(context, internalProviderId, revision)
                            }
                            setPreviewVideoUri(
                                previewUri
                            )
                            setPosterArtUri(Uri.parse(artworkUri))
                            setThumbnailUri(Uri.parse(artworkUri))
                        }
                    }
                    .build()
                    .toContentValues()
            } catch (e: Exception) {
                Timber.w(e, "Failed to insert recommendation for $title")
                null
            }
        }

        if (programValues.isEmpty()) {
            Timber.w("No valid TV recommendations to publish; keeping existing cards")
            return null
        }
        val internalIds = programValues.map { values ->
            values.getAsString(TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID)
        }
        val plan = planPreviewProgramReconciliation(existingPrograms, internalIds)
        if (plan.deletes.isNotEmpty()) {
            val deletedIds = plan.deletes.toSet()
            TvPreviewContentProvider.rememberDeletedPrograms(
                context,
                existingPrograms.filter { it.programId in deletedIds },
            )
        }
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
        TvPreviewContentProvider.rememberSourceArtwork(context, sourceArtworkByInternalId)
        if (operations.isNotEmpty()) {
            context.contentResolver.applyBatch(TvContractCompat.AUTHORITY, operations)
        }
        val quickPickCount = selectedMediaIds.count(quickPickItemsById::containsKey)
        Timber.d(
            "Published ${programValues.size} TV recommendations for '$queueTitle' " +
                "($quickPickCount Quick Picks; ${plan.inserts.size} added, ${plan.updates.size} updated, " +
                "${plan.deletes.size} removed; " +
                "${previewAudioSources.size} audio sources resolved; previews resolve on focus)"
        )
        return queueTitle
    }

    private fun readExistingPrograms(context: Context, channelId: Long): List<ExistingPreviewProgram> {
        val uri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId)
        val projection = arrayOf(
            TvContractCompat.PreviewPrograms._ID,
            TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
            TvContractCompat.PreviewProgramColumns.COLUMN_CONTENT_ID,
            TvContractCompat.PreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI,
            TvContractCompat.PreviewProgramColumns.COLUMN_PREVIEW_AUDIO_URI,
            COLUMN_POSTER_ART_URI,
            COLUMN_TITLE,
            TvContractCompat.PreviewProgramColumns.COLUMN_DURATION_MILLIS,
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
                            contentId = it.getString(
                                it.getColumnIndexOrThrow(
                                    TvContractCompat.PreviewProgramColumns.COLUMN_CONTENT_ID
                                )
                            ),
                            previewVideoUri = it.getString(
                                it.getColumnIndexOrThrow(
                                    TvContractCompat.PreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI
                                )
                            ),
                            previewAudioUri = it.getString(
                                it.getColumnIndexOrThrow(
                                    TvContractCompat.PreviewProgramColumns.COLUMN_PREVIEW_AUDIO_URI
                                )
                            ),
                            posterArtUri = TvPreviewContentProvider.sourceArtworkUri(
                                context,
                                it.getString(internalIdColumn),
                                it.getString(it.getColumnIndexOrThrow(COLUMN_POSTER_ART_URI)).orEmpty(),
                            ),
                            title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                            durationMs = it.getLong(
                                it.getColumnIndexOrThrow(
                                    TvContractCompat.PreviewProgramColumns.COLUMN_DURATION_MILLIS
                                )
                            ).coerceAtLeast(0L),
                        )
                    )
                }
            }
        }
    }

    private suspend fun resolvePreviewAudioUris(
        context: Context,
        mediaIds: List<String>,
        existingPrograms: List<ExistingPreviewProgram>,
    ): Map<String, PreviewAudio> = coroutineScope {
        val existingByContentId = existingPrograms
            .filter { !it.contentId.isNullOrBlank() }
            .associateBy { it.contentId!! }
        val now = System.currentTimeMillis()
        val resolveLimit = Semaphore(2)
        mediaIds.distinct().map { mediaId ->
            async {
                val previousUri = (existingByContentId[mediaId]?.previewAudioUri
                    ?: existingByContentId[mediaId]?.previewVideoUri)
                    ?.let { rawUri -> runCatching { Uri.parse(rawUri) }.getOrNull() }
                if (previousUri != null && canReuseTvAudioPreview(
                        scheme = previousUri.scheme,
                        mimeType = previousUri.getQueryParameter("mime"),
                        expiresAtSeconds = previousUri.getQueryParameter("expire"),
                        nowMillis = now,
                )
                ) {
                    return@async mediaId to PreviewAudio(
                        previousUri,
                        previousUri.getQueryParameter("mime").orEmpty(),
                        previousUri.toString(),
                    )
                }

                val source = resolveLimit.withPermit {
                    try {
                        TvPreviewSourceResolver.resolve(context, mediaId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to resolve TV preview audio source")
                        null
                    }
                } ?: return@async null
                val sourceUri = runCatching { Uri.parse(source.url) }.getOrNull()
                    ?.takeIf {
                        canPublishTvPreview(
                            scheme = it.scheme,
                            host = it.host,
                            expiresAtSeconds = it.getQueryParameter("expire"),
                            nowMillis = System.currentTimeMillis(),
                        )
                    } ?: return@async null
                // Projectivy cannot attach the browser headers required by some resolver
                // clients. In that case the provider streams its locally prepared audio.
                val uri = sourceUri.takeIf { source.requestHeaders.isEmpty() }
                mediaId to PreviewAudio(uri, source.mimeType, source.url)
            }
        }.awaitAll().filterNotNull().toMap()
    }

    private fun ensureChannel(context: Context): Long {
        findExistingChannelId(context).takeIf { it > 0L }?.let { channelId ->
            ensureChannelLogo(context, channelId)
            requestChannelVisibility(context, channelId)
            return channelId
        }

        val channel = PreviewChannel.Builder()
            .setDisplayName("AuraMusic")
            .setDescription("Continue listening recommendations")
            .setAppLinkIntent(Intent(context, com.auramusic.app.TvMainActivity::class.java))
            .build()
        val channelUri = context.contentResolver.insert(
            TvContractCompat.Channels.CONTENT_URI,
            channel.toContentValues()
        )

        return channelUri?.lastPathSegment?.toLongOrNull()?.also { channelId ->
            ensureChannelLogo(context, channelId)
            requestChannelVisibility(context, channelId)
        } ?: 0L
    }

    private fun findExistingChannelId(context: Context): Long =
        context.contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            arrayOf(TvContractCompat.Channels._ID, TvContractCompat.Channels.COLUMN_PACKAGE_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(TvContractCompat.Channels._ID)
            val packageNameColumn = cursor.getColumnIndexOrThrow(
                TvContractCompat.Channels.COLUMN_PACKAGE_NAME
            )
            var channelId = 0L
            while (cursor.moveToNext()) {
                if (cursor.getString(packageNameColumn) == context.packageName) {
                    channelId = cursor.getLong(idColumn)
                    break
                }
            }
            channelId
        } ?: 0L

    private fun removeStalePrograms(context: Context) {
        val channelId = findExistingChannelId(context)
        if (channelId <= 0L) return
        val existing = readExistingPrograms(context, channelId)
        val plan = planPreviewProgramReconciliation(existing, emptyList())
        if (plan.deletes.isEmpty()) return
        val deletedIds = plan.deletes.toSet()
        TvPreviewContentProvider.rememberDeletedPrograms(
            context,
            existing.filter { it.programId in deletedIds },
        )
        val operations = plan.deletes.map { programId ->
            ContentProviderOperation.newDelete(
                TvContractCompat.buildPreviewProgramUri(programId)
            ).build()
        }
        context.contentResolver.applyBatch(TvContractCompat.AUTHORITY, ArrayList(operations))
        Timber.i("Removed %d stale AuraMusic TV programs after a confirmed empty selection", plan.deletes.size)
    }

    private fun ensureChannelLogo(context: Context, channelId: Long) {
        val logoUri = Uri.parse("android.resource://${context.packageName}/${R.mipmap.ic_launcher_foreground}")
        if (ChannelLogoUtils.loadChannelLogo(context, channelId) == null) {
            ChannelLogoUtils.storeChannelLogo(context, channelId, logoUri)
        }
    }

    private fun requestChannelVisibility(context: Context, channelId: Long) {
        if (channelId <= 0L) return
        try {
            TvContractCompat.requestChannelBrowsable(context, channelId)
        } catch (e: Exception) {
            Timber.w(e, "TV launcher rejected recommendation channel visibility request")
        }
    }

    private fun readPersistQueue(context: Context): PersistQueueSnapshot {
        val file = File(context.filesDir, "persistent_queue.data")
        if (!file.exists()) return PersistQueueSnapshot(queue = null, available = true)
        return try {
            val queue = ObjectInputStream(file.inputStream()).use { ois ->
                ois.readObject() as? PersistQueue
            }
            PersistQueueSnapshot(queue = queue, available = queue != null)
        } catch (e: Exception) {
            Timber.w(e, "Failed to read persisted queue")
            PersistQueueSnapshot(queue = null, available = false)
        }
    }

    private const val COLUMN_POSTER_ART_URI = "poster_art_uri"
    private const val COLUMN_TITLE = "title"
}
