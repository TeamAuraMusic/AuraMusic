package com.auramusic.app.tv

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.OpenableColumns
import android.system.ErrnoException
import android.system.OsConstants
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.tvprovider.media.tv.TvContractCompat
import com.auramusic.auravideo.AuraVideo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Serves Projectivy preview audio, artwork, and generated video content from bounded caches. */
@OptIn(UnstableApi::class)
class TvPreviewContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: return false
        File(File(appContext.cacheDir, AUDIO_CACHE_DIRECTORY), "source-tmp").deleteRecursively()
        return true
    }

    /** Debug-only device-test hook: atomically inject a clip for the exact published program URI. */
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method !in setOf("debugImportPreview", "debugListPrograms", "debugUseAudioOnlyPreview")) {
            return super.call(method, arg, extras)
        }
        val appContext = requireContext()
        check(appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            "Preview diagnostics are available only in debuggable builds"
        }
        if (method == "debugListPrograms") {
            val channels = appContext.contentResolver.query(
                TvContractCompat.Channels.CONTENT_URI,
                arrayOf(TvContractCompat.Channels._ID, TvContractCompat.Channels.COLUMN_PACKAGE_NAME,
                    TvContractCompat.Channels.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(TvContractCompat.Channels._ID)
                val packageColumn = cursor.getColumnIndexOrThrow(TvContractCompat.Channels.COLUMN_PACKAGE_NAME)
                val nameColumn = cursor.getColumnIndexOrThrow(TvContractCompat.Channels.COLUMN_DISPLAY_NAME)
                buildList {
                    while (cursor.moveToNext()) {
                        if (cursor.getString(packageColumn) == appContext.packageName) {
                            add(cursor.getLong(idColumn) to cursor.getString(nameColumn))
                        }
                    }
                }
            }.orEmpty()
            val report = channels.map { (channelId, channelName) ->
                val titles = appContext.contentResolver.query(
                    TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                    arrayOf("title"), null, null, null,
                )?.use { cursor ->
                    val titleColumn = cursor.getColumnIndexOrThrow("title")
                    buildList { while (cursor.moveToNext()) add(cursor.getString(titleColumn).orEmpty()) }
                }.orEmpty()
                "$channelName (id=$channelId, programs=${titles.size}): ${titles.joinToString(" | ")}"
            }.joinToString("\n")
            return Bundle().apply { putString("result", report.ifBlank { "No AuraMusic TV channels published" }) }
        }
        if (method == "debugUseAudioOnlyPreview") {
            val title = requireNotNull(arg) { "Program title is required" }
            val key = findProgramKeyByTitle(appContext, title)
                ?: error("Program $title is not published")
            val preferenceKey = DEBUG_AUDIO_ONLY_PREFIX + key
            val preferences = appContext.getSharedPreferences(DEBUG_PREFERENCES, Context.MODE_PRIVATE)
            val enabled = extras?.getBoolean("enabled", true) ?: true
            if (enabled) preferences.edit().putBoolean(preferenceKey, true).commit()
            else preferences.edit().remove(preferenceKey).commit()
            Timber.i("TV preview debug audio-only mode enabled=%s key=%s", enabled, safeKey(key))
            return Bundle().apply { putString("result", "audio-only=$enabled title=$title") }
        }
        val title = requireNotNull(arg) { "Program title is required" }
        val key = appContext.contentResolver.query(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            arrayOf(
                TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
                "title",
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(
                TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
            )
            val titleColumn = cursor.getColumnIndexOrThrow("title")
            var match: String? = null
            while (cursor.moveToNext()) {
                if (cursor.getString(titleColumn) == title) match = cursor.getString(idColumn)
            }
            match
        } ?: error("Program $title is not published")
        val record = readProgram(appContext, key)
        val input = File(requireNotNull(appContext.getExternalFilesDir(null)), "preview-import.mp4")
        check(input.isFile && input.length() > 0L) { "External preview-import.mp4 is missing" }
        val destination = cacheFile(appContext, key, record)
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "${destination.name}.importing")
        FileInputStream(input).use { source -> FileOutputStream(temporary).use { target -> source.copyTo(target) } }
        check(temporary.renameTo(destination)) { "Could not atomically install test preview" }
        return Bundle().apply {
            putString("path", destination.absolutePath)
            putLong("bytes", destination.length())
        }
    }

    override fun getType(uri: Uri): String {
        if (isWaveformUri(uri)) return "image/gif"
        val appContext = context?.applicationContext
        val audioOnly = if (appContext != null) {
            runCatching { isAudioOnlyPreview(appContext, programKey(uri), uri) }
                .getOrDefault(isAudioOnlyPreview(uri))
        } else isAudioOnlyPreview(uri)
        return if (audioOnly) "audio/mp4" else "video/mp4"
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val key = programKey(uri)
        val context = appContext()
        val row = findProgram(context, key)
        val size = runCatching {
            val program = readProgram(context, key)
            val file = when {
                isWaveformUri(uri) -> waveformFile(context, key, program)
                isAudioOnlyPreview(context, key, uri) -> audioFile(context, key, program)
                else -> cacheFile(context, key, program)
            }
            file.takeIf(File::isFile)?.length()
        }.getOrNull()
        Timber.d("TV preview query key=%s size=%s", safeKey(key), size)
        val requested = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return android.database.MatrixCursor(requested).apply {
            addRow(requested.map { column ->
                when (column) {
                    OpenableColumns.DISPLAY_NAME -> when {
                        isWaveformUri(uri) -> "${programKey(uri)}.gif"
                        isAudioOnlyPreview(context, key, uri) -> "${programKey(uri)}.m4a"
                        else -> "${programKey(uri)}.mp4"
                    }
                    OpenableColumns.SIZE -> size
                    else -> row?.get(column)
                }
            })
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Read-only preview provider")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        error("Read-only preview provider")
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = error("Read-only preview provider")

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r") { "Preview stream is read-only" }
        val appContext = requireContext()
        val programKey = programKey(uri)
        if (isWaveformUri(uri)) {
            val file = ensureWaveformFile(appContext, programKey)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        val requestStartedMs = SystemClock.elapsedRealtime()
        val requestId = requestStartedMs.toString(16)
        Timber.i(
            "TV preview open request=%s key=%s callerUid=%d",
            requestId, safeKey(programKey), android.os.Binder.getCallingUid(),
        )
        val cachedRecord = readProgram(appContext, programKey)
        if (isAudioOnlyPreview(appContext, programKey, uri)) {
            val expectedFile = audioFile(appContext, programKey, cachedRecord)
            pinAudioCacheFile(expectedFile)
            try {
                val audioFile = ensureAudioFile(appContext, programKey, cachedRecord)
                Timber.i(
                    "TV preview audio-only open request=%s key=%s bytes=%d",
                    requestId, safeKey(programKey), audioFile.length(),
                )
                return ParcelFileDescriptor.open(audioFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } finally {
                unpinAudioCacheFile(expectedFile)
            }
        }
        val cachedFile = cacheFile(appContext, programKey, cachedRecord)
        if (cachedFile.isFile && cachedFile.length() > 0L) {
            cachedFile.setLastModified(System.currentTimeMillis())
            Timber.i(
                "TV preview seekable cache hit request=%s key=%s bytes=%d",
                requestId, safeKey(programKey), cachedFile.length(),
            )
            return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        val generation = ensureGeneration(appContext, programKey)
        val readyFile = try {
            runBlocking(Dispatchers.IO) {
                withTimeout(OPEN_WAIT_TIMEOUT_MS) { generation.result.await() }
            }
        } catch (error: Exception) {
            Timber.w(error, "TV preview preparation failed before open request=%s key=%s", requestId, safeKey(programKey))
            throw FileNotFoundException("Preview video is not ready").apply { initCause(error) }
        }
        Timber.i(
            "TV preview seekable prepared open request=%s key=%s elapsedMs=%d bytes=%d",
            requestId, safeKey(programKey), SystemClock.elapsedRealtime() - requestStartedMs, readyFile.length(),
        )
        return ParcelFileDescriptor.open(readyFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun appContext(): Context = context?.applicationContext
        ?: error("Preview provider is not attached")

    private fun findProgramKeyByTitle(context: Context, title: String): String? =
        context.contentResolver.query(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            arrayOf(
                TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
                COLUMN_TITLE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(
                TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
            )
            val titleColumn = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
            var result: String? = null
            while (cursor.moveToNext()) {
                if (cursor.getString(titleColumn) == title) result = cursor.getString(idColumn)
            }
            result
        }

    private fun isDebugAudioOnlyEnabled(context: Context, key: String): Boolean =
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 &&
            context.getSharedPreferences(DEBUG_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(DEBUG_AUDIO_ONLY_PREFIX + key, false)

    private fun isAudioOnlyPreview(uri: Uri): Boolean =
        uri.getQueryParameter(AUDIO_ONLY_QUERY) == AUDIO_ONLY_VALUE &&
            uri.getQueryParameter(MIME_QUERY).equals(AUDIO_MP4_MIME, ignoreCase = true)

    private fun isAudioOnlyPreview(context: Context, key: String, uri: Uri): Boolean =
        isAudioOnlyPreview(uri) || isDebugAudioOnlyEnabled(context, key)

    /** Prepare a Projectivy-compatible audio-only file without encoding a full-length video. */
    private fun ensureAudioFile(context: Context, key: String, record: ProgramRecord): File {
        val output = audioFile(context, key, record)
        val outputDirectory = output.parentFile!!
        outputDirectory.mkdirs()
        if (output.isFile && output.length() > 0L) {
            output.setLastModified(System.currentTimeMillis())
            return output
        }

        val acquisition = runBlocking(Dispatchers.IO) {
            withTimeout(OPEN_WAIT_TIMEOUT_MS) {
                audioGenerations.acquire(output.absolutePath) { output.isFile && output.length() > 0L }
            }
        } ?: run {
            output.setLastModified(System.currentTimeMillis())
            return output
        }
        val activeGeneration = acquisition.generation
        if (acquisition.isNew) {
            val producer = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                var downloaded: File? = null
                try {
                    val (audio, audioFile) = audioDownloadGate.runWithSource(
                        resolve = { resolveAudio(record) },
                        refresh = { resolveAudio(record, forceRefresh = true) },
                        shouldRefresh = ::shouldRefreshPreviewAudioSource,
                    ) { source ->
                        prepareAudioOnlyFile(context, source, output)
                    }
                    downloaded = audioFile
                    check(downloaded == output || downloaded.absolutePath == output.absolutePath) {
                        "Audio downloader returned an unexpected cache path"
                    }
                    evictAudioCache(outputDirectory, output)
                    output.setLastModified(System.currentTimeMillis())
                    activeGeneration.result.complete(output)
                    Timber.i(
                        "TV preview audio-only source ready key=%s mime=%s bytes=%d",
                        safeKey(key), audio.mimeType, output.length(),
                    )
                } catch (cancelled: CancellationException) {
                    activeGeneration.result.cancel(cancelled)
                    throw cancelled
                } catch (error: Exception) {
                    downloaded?.delete()
                    activeGeneration.markFailed()
                    activeGeneration.result.completeExceptionally(error)
                    Timber.w(error, "TV preview debug audio-only source failed key=%s", safeKey(key))
                } finally {
                    audioGenerations.remove(output.absolutePath, activeGeneration)
                    activeGeneration.markCleanupComplete()
                }
            }
            activeGeneration.attachProducer(producer)
            producer.start()
        }
        try {
            return runBlocking(Dispatchers.IO) {
                withTimeout(OPEN_WAIT_TIMEOUT_MS) { activeGeneration.result.await() }
            }
        } finally {
            activeGeneration.releaseWaiter()
        }
    }

    private suspend fun prepareAudioOnlyFile(
        context: Context,
        source: AuraVideo.AudioPreviewSource,
        output: File,
    ): File {
        return when (projectivyAudioPreviewPreparation(source.mimeType)) {
            ProjectivyAudioPreviewPreparation.DIRECT_MP4 -> TvPreviewAudioDownloader.download(
                source.url,
                output.parentFile!!,
                output.nameWithoutExtension,
                source.requestHeaders,
            )
            ProjectivyAudioPreviewPreparation.TRANSCODE_TO_MP4 -> {
                val sourceDirectory = File(
                    File(output.parentFile!!, "source-tmp"),
                    output.nameWithoutExtension,
                )
                check(sourceDirectory.isDirectory || sourceDirectory.mkdirs()) {
                    "Could not create temporary audio source directory"
                }
                val sourceFile = TvPreviewAudioDownloader.download(
                    source.url,
                    sourceDirectory,
                    output.nameWithoutExtension,
                    source.requestHeaders,
                )
                try {
                    exportSemaphore.withPermit {
                        TvPreviewAudioTranscoder.transcode(context, sourceFile, output)
                    }
                    Timber.i(
                        "TV preview source normalized to audio/mp4 sourceMime=%s sourceBytes=%d outputBytes=%d",
                        source.mimeType, sourceFile.length(), output.length(),
                    )
                    output
                } finally {
                    sourceFile.delete()
                    sourceDirectory.deleteRecursively()
                }
            }
            ProjectivyAudioPreviewPreparation.UNSUPPORTED ->
                throw IllegalArgumentException("Unsupported audio preview MIME type ${source.mimeType}")
        }
    }

    private fun evictAudioCache(directory: File, justWritten: File? = null) {
        synchronized(audioCacheLock) {
            val completed = directory.listFiles { file -> file.isFile && file.extension == "audio" }
                .orEmpty()
            val entries = completed.map {
                PreviewAudioCacheEntry(it.absolutePath, it.length(), it.lastModified())
            }
            val protectedPaths = audioCachePins.keys + listOfNotNull(justWritten?.absolutePath)
            planPreviewAudioCacheEvictions(entries, AUDIO_CACHE_BYTES, protectedPaths)
                .forEach { path -> File(path).delete() }
        }
    }

    private fun pinAudioCacheFile(file: File) {
        synchronized(audioCacheLock) {
            val path = file.absolutePath
            audioCachePins[path] = (audioCachePins[path] ?: 0) + 1
        }
    }

    private fun unpinAudioCacheFile(file: File) {
        synchronized(audioCacheLock) {
            val path = file.absolutePath
            val count = audioCachePins[path] ?: return
            if (count <= 1) audioCachePins.remove(path) else audioCachePins[path] = count - 1
        }
        file.parentFile?.let(::evictAudioCache)
    }

    private fun programKey(uri: Uri): String =
        uri.pathSegments.getOrNull(1)?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Missing internal program ID")

    private fun isWaveformUri(uri: Uri): Boolean = uri.lastPathSegment == "waveform.gif"

    private fun ensureWaveformFile(context: Context, key: String): File {
        val record = readProgram(context, key)
        val file = waveformFile(context, key, record)
        synchronized(waveformGenerationLock) {
            if (file.isFile && file.length() > 0L) return file
            file.parentFile?.mkdirs()
            val partial = File(file.parentFile, "${file.name}.partial")
            partial.delete()
            val artworkFile = runBlocking(Dispatchers.IO) {
                withTimeout(OPEN_WAIT_TIMEOUT_MS) {
                    downloadArtwork(context, record.artworkUri, file.parentFile!!)
                }
            }
            try {
                val bitmap = BitmapFactory.decodeFile(artworkFile.absolutePath)
                    ?: throw FileNotFoundException("Waveform artwork could not be decoded")
                try {
                    FileOutputStream(partial).use { TvPreviewGif.encode(bitmap, it) }
                } finally {
                    bitmap.recycle()
                }
                check(partial.length() > 0L && partial.renameTo(file)) {
                    "Could not atomically publish waveform artwork"
                }
                Timber.i("TV waveform poster ready key=%s bytes=%d", safeKey(key), file.length())
                return file
            } finally {
                artworkFile.delete()
                partial.delete()
            }
        }
    }

    @Synchronized
    private fun ensureGeneration(context: Context, key: String): Generation {
        val record = readProgram(context, key)
        val cacheFile = cacheFile(context, key, record)
        val generationKey = cacheFile.name
        cacheFile.parentFile?.mkdirs()
        if (cacheFile.isFile && cacheFile.length() > 0L) {
            cacheFile.setLastModified(System.currentTimeMillis())
            Timber.i(
                "TV preview cache hit key=%s title=%s bytes=%d",
                safeKey(key), record.title, cacheFile.length(),
            )
            return Generation(cacheFile, cacheFile, completed = true)
        }

        val existing = generations[generationKey]
        if (existing != null) return existing
        val partialFile = File(cacheFile.parentFile, "${cacheFile.name}.partial")
        partialFile.delete()
        partialFile.createNewFile()
        val candidate = Generation(partialFile, cacheFile)
        val raced = generations.putIfAbsent(generationKey, candidate)
        if (raced != null) {
            partialFile.delete()
            return raced
        }
        val generationJob = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            val startedAt = SystemClock.elapsedRealtime()
            try {
                Timber.i("TV preview generation start key=%s", safeKey(key))
                Timber.i(
                    "TV preview selected program title=%s contentIdHash=%s",
                    record.title, safeKey(record.mediaId),
                )
                val artworkFile = downloadArtwork(context, record.artworkUri, partialFile.parentFile!!)
                Timber.i("TV preview artwork ready key=%s bytes=%d", safeKey(key), artworkFile.length())
                try {
                    // Resolve the potentially expiring source only after obtaining a bounded
                    // preparation slot, then hold it through duration probing and download.
                    val (audio, prepared) = audioDownloadGate.runWithSource(
                        resolve = { resolveAudio(record) },
                        refresh = { resolveAudio(record, forceRefresh = true) },
                        shouldRefresh = ::shouldRefreshPreviewAudioSource,
                    ) { source ->
                        val durationMs = record.durationMs.takeIf { it > 0L }
                            ?: source.durationMs.takeIf { it > 0L }
                            ?: runCatching { probeAudioDuration(context, source.url, source.requestHeaders) }
                                .getOrNull()
                                ?.takeIf { it > 0L }
                            ?: UNKNOWN_TRACK_DURATION_FALLBACK_MS
                        require(durationMs in 1L..MAX_TRACK_DURATION_MS) { "Track has no usable duration" }
                        val downloadedAudio = TvPreviewAudioDownloader.download(
                            source.url,
                            partialFile.parentFile!!,
                            partialFile.name,
                            source.requestHeaders,
                        )
                        durationMs to downloadedAudio
                    }
                    val (trackDurationMs, audioFile) = prepared
                    // Keep the in-card indicator alive for the full focused preview. Projectivy
                    // 4.73 does not loop previewVideoUri: an 8-second composition falls back to poster art.
                    val visualDurationMs = trackDurationMs
                    Timber.i(
                        "TV preview source ready key=%s trackDurationMs=%d visualDurationMs=%d",
                        safeKey(key), trackDurationMs, visualDurationMs,
                    )
                    try {
                        Timber.i(
                            "TV preview audio ready key=%s mime=%s bytes=%d",
                            safeKey(key), audio.mimeType, audioFile.length(),
                        )
                        transform(artworkFile, audioFile, visualDurationMs, candidate)
                    } finally {
                        audioFile.delete()
                    }
                    require(partialFile.length() > 0L) { "MediaCodec wrote an empty preview" }
                    if (cacheFile.exists()) cacheFile.delete()
                    check(partialFile.renameTo(cacheFile)) { "Could not atomically publish preview cache" }
                    cacheFile.setLastModified(System.currentTimeMillis())
                    evictCache(cacheFile.parentFile!!, cacheFile)
                    candidate.result.complete(cacheFile)
                    Timber.i(
                        "TV preview ready key=%s visualDurationMs=%d bytes=%d elapsedMs=%d",
                        safeKey(key), visualDurationMs, cacheFile.length(), SystemClock.elapsedRealtime() - startedAt,
                    )
                } finally {
                    artworkFile.delete()
                }
            } catch (cancelled: CancellationException) {
                partialFile.delete()
                candidate.result.cancel(cancelled)
                throw cancelled
            } catch (error: Exception) {
                partialFile.delete()
                candidate.result.completeExceptionally(error)
                Timber.w(error, "TV preview generation failed key=%s", safeKey(key))
            } finally {
                generations.remove(generationKey, candidate)
            }
        }
        candidate.job = generationJob
        generationJob.start()
        return candidate
    }

    private suspend fun transform(
        artworkFile: File,
        audioFile: File,
        durationMs: Long,
        generation: Generation,
    ) = exportSemaphore.withPermit {
        Timber.i("TV preview direct MediaCodec generation starting durationMs=%d", durationMs)
        TvPreviewMediaCodecEncoder.encode(artworkFile, audioFile, durationMs, generation.partialFile)
    }

    private suspend fun resolveAudio(
        record: ProgramRecord,
        forceRefresh: Boolean = false,
    ): AuraVideo.AudioPreviewSource {
        val existing = record.audioUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (!forceRefresh && existing != null && canReuseTvAudioPreview(
                scheme = existing.scheme,
                mimeType = existing.getQueryParameter("mime"),
                expiresAtSeconds = existing.getQueryParameter("expire"),
                nowMillis = System.currentTimeMillis(),
            )
        ) {
            return AuraVideo.AudioPreviewSource(
                url = existing.toString(),
                mimeType = existing.getQueryParameter("mime").orEmpty(),
                durationMs = record.durationMs,
            )
        }
        return withTimeout(RESOLVE_TIMEOUT_MS) {
            TvPreviewSourceResolver.resolve(requireContext(), record.mediaId)
        }
    }

    /** Reads the remote container's duration without playing audio; Quick Picks often omit it. */
    private suspend fun probeAudioDuration(
        context: Context,
        audioUrl: String,
        requestHeaders: Map<String, String> = emptyMap(),
    ): Long =
        withTimeout(DURATION_PROBE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val finished = java.util.concurrent.atomic.AtomicBoolean(false)
                var player: ExoPlayer? = null
                val startProbe = Runnable {
                    if (!continuation.isActive || finished.get()) return@Runnable
                    try {
                        val dataSourceFactory = DefaultDataSource.Factory(
                            context,
                            DefaultHttpDataSource.Factory().setDefaultRequestProperties(requestHeaders),
                        )
                        val mediaSourceFactory = DefaultMediaSourceFactory(context)
                            .setDataSourceFactory(dataSourceFactory)
                        val probe = ExoPlayer.Builder(context)
                            .setMediaSourceFactory(mediaSourceFactory)
                            .build()
                        player = probe
                        val listener = object : Player.Listener {
                            private fun readDuration(timeline: Timeline = probe.currentTimeline) {
                                if (timeline.windowCount == 0) return
                                val durationMs = timeline.getWindow(0, Timeline.Window()).durationMs
                                if (durationMs > 0L && durationMs != androidx.media3.common.C.TIME_UNSET) {
                                    finish(Result.success(durationMs))
                                }
                            }

                            private fun finish(result: Result<Long>) {
                                if (!finished.compareAndSet(false, true)) return
                                probe.removeListener(this)
                                probe.release()
                                if (continuation.isActive) result.fold(
                                    onSuccess = { continuation.resume(it) },
                                    onFailure = { continuation.resumeWithException(it) },
                                )
                            }

                            override fun onTimelineChanged(timeline: Timeline, reason: Int) =
                                readDuration(timeline)

                            override fun onPlaybackStateChanged(playbackState: Int) = readDuration()

                            override fun onPlayerError(error: PlaybackException) {
                                finish(Result.failure(error))
                            }
                        }
                        probe.addListener(listener)
                        continuation.invokeOnCancellation {
                            mainHandler.post {
                                if (finished.compareAndSet(false, true)) probe.release()
                            }
                        }
                        probe.setMediaItem(MediaItem.fromUri(audioUrl))
                        probe.prepare()
                    } catch (error: Exception) {
                        if (finished.compareAndSet(false, true) && continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                }
                mainHandler.post(startProbe)
            }
        }

    private fun readProgram(context: Context, key: String): ProgramRecord {
        val columns = arrayOf(
            TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
            TvContractCompat.PreviewProgramColumns.COLUMN_CONTENT_ID,
            TvContractCompat.PreviewProgramColumns.COLUMN_PREVIEW_AUDIO_URI,
            COLUMN_THUMBNAIL_URI,
            TvContractCompat.PreviewProgramColumns.COLUMN_DURATION_MILLIS,
            COLUMN_TITLE,
        )
        val cursor = context.contentResolver.query(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            columns,
            null,
            null,
            null,
        ) ?: error("TvProvider returned no program cursor")
        val currentRecord = cursor.use {
            val providerIdColumn = it.getColumnIndexOrThrow(
                TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID
            )
            var rowFound: Cursor? = null
            while (it.moveToNext()) {
                if (it.getString(providerIdColumn) == key) {
                    rowFound = it
                    break
                }
            }
            val row = rowFound ?: return@use null
            ProgramRecord(
                mediaId = row.getString(1)?.takeIf(String::isNotBlank)
                    ?: error("Preview program has no content ID"),
                audioUri = row.getString(2),
                artworkUri = Uri.parse(
                    sourceArtworkUri(context, key, row.getString(3).orEmpty())
                ),
                durationMs = row.getLong(4).coerceAtLeast(0L),
                title = row.getString(5).orEmpty(),
            )
        }
        return currentRecord ?: readStaleProgram(context, key)
            ?: throw FileNotFoundException("Preview program no longer exists")
    }

    private fun findProgram(context: Context, key: String): Map<String, Any?>? =
        runCatching {
            val cursor = context.contentResolver.query(
                TvContractCompat.PreviewPrograms.CONTENT_URI,
                arrayOf(
                    TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
                    COLUMN_TITLE,
                    TvContractCompat.PreviewProgramColumns.COLUMN_DURATION_MILLIS,
                ),
                null,
                null,
                null,
            ) ?: return null
            cursor.use {
                val providerIdColumn = it.getColumnIndexOrThrow(
                    TvContractCompat.PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID
                )
                while (it.moveToNext()) {
                    if (it.getString(providerIdColumn) == key) {
                        return mapOf(
                            "title" to it.getString(1),
                            "duration" to it.getLong(2),
                        )
                    }
                }
                null
            }
        }.getOrNull() ?: readStaleProgram(context, key)?.let {
            mapOf("title" to it.title, "duration" to it.durationMs)
        }

    private fun readStaleProgram(context: Context, key: String): ProgramRecord? {
        val preferenceKey = staleProgramPreferenceKey(key)
        val preferences = context.getSharedPreferences(STALE_PROGRAM_PREFERENCES, Context.MODE_PRIVATE)
        val encoded = preferences.getString(preferenceKey, null) ?: return null
        return try {
            val snapshot = JSONObject(encoded)
            val retained = RetainedTvProgramMetadata(
                internalProviderId = key,
                contentId = snapshot.getString("mediaId"),
                previewAudioUri = snapshot.optString("audioUri")
                    .takeUnless { it.isBlank() || it == "null" },
                posterArtUri = snapshot.getString("artworkUri"),
                durationMs = snapshot.optLong("durationMs").coerceAtLeast(0L),
                title = snapshot.optString("title"),
                expiresAtMs = snapshot.optLong("expiresAtMs"),
            )
            val current = findRecentlyDeletedTvProgram(listOf(retained), key, System.currentTimeMillis())
            if (current == null) {
                preferences.edit().remove(preferenceKey).apply()
                return null
            }
            Timber.i("TV preview using retained metadata for removed program key=%s", safeKey(key))
            ProgramRecord(
                mediaId = current.contentId,
                audioUri = current.previewAudioUri,
                artworkUri = Uri.parse(current.posterArtUri),
                durationMs = current.durationMs,
                title = current.title,
            )
        } catch (error: Exception) {
            preferences.edit().remove(preferenceKey).apply()
            Timber.w(error, "Invalid retained TV preview metadata key=%s", safeKey(key))
            null
        }
    }

    private suspend fun downloadArtwork(context: Context, uri: Uri, directory: File): File =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val input = openArtwork(context, uri)
            val bytes = input.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_ARTWORK_BYTES) { "Artwork exceeds size limit" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Artwork is not a supported image" }
            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_ARTWORK_EDGE)
            val bitmap = BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: error("Artwork decode failed")
            val scaled = if (maxOf(bitmap.width, bitmap.height) > MAX_ARTWORK_EDGE) {
                val factor = MAX_ARTWORK_EDGE / maxOf(bitmap.width, bitmap.height).toFloat()
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * factor).toInt().coerceAtLeast(1),
                    (bitmap.height * factor).toInt().coerceAtLeast(1),
                    true,
                ).also { if (it !== bitmap) bitmap.recycle() }
            } else bitmap
            val file = File(directory, "art-${System.nanoTime()}.jpg")
            try {
                file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            } finally {
                scaled.recycle()
            }
            file
        }

    private fun openArtwork(context: Context, uri: Uri): InputStream = when (uri.scheme?.lowercase()) {
        "http", "https" -> {
            val connection = URL(uri.toString()).openConnection() as HttpURLConnection
            connection.connectTimeout = NETWORK_TIMEOUT_MS
            connection.readTimeout = NETWORK_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            check(connection.responseCode in 200..299) { "Artwork HTTP ${connection.responseCode}" }
            connection.inputStream
        }
        "content", "android.resource" -> context.contentResolver.openInputStream(uri)
            ?: error("Artwork URI is unavailable")
        else -> error("Unsupported artwork URI scheme")
    }

    private fun cacheFile(context: Context, key: String, record: ProgramRecord): File {
        val cacheIdentity = listOf(
            key,
            tvPreviewSourceRevision(
                record.mediaId,
                record.artworkUri.toString(),
                record.durationMs,
                record.audioUri,
            ),
        ).joinToString("\n")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(cacheIdentity.toByteArray(Charsets.UTF_8))
            .take(16)
            .joinToString("") { "%02x".format(it) }
        return File(File(context.cacheDir, CACHE_DIRECTORY), "$digest.mp4")
    }

    private fun waveformFile(context: Context, key: String, record: ProgramRecord): File {
        val identity = listOf(
            key,
            tvPreviewSourceRevision(
                record.mediaId,
                record.artworkUri.toString(),
                record.durationMs,
                record.audioUri,
            ),
        ).joinToString("\n")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .take(16)
            .joinToString("") { "%02x".format(it) }
        return File(File(context.cacheDir, WAVEFORM_CACHE_DIRECTORY), "$digest.gif")
    }

    private fun audioFile(context: Context, key: String, record: ProgramRecord): File {
        val identity = listOf(
            key,
            tvPreviewSourceRevision(
                record.mediaId,
                record.artworkUri.toString(),
                record.durationMs,
                record.audioUri,
            ),
        ).joinToString("\n")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .take(16)
            .joinToString("") { "%02x".format(it) }
        return File(File(context.cacheDir, AUDIO_CACHE_DIRECTORY), "$digest.audio")
    }

    private fun evictCache(directory: File, justWritten: File) {
        val completed = directory.listFiles { file -> file.isFile && file.extension == "mp4" }
            ?.sortedBy(File::lastModified)
            .orEmpty()
        var bytes = completed.sumOf(File::length)
        for (file in completed) {
            if (bytes <= MAX_CACHE_BYTES) break
            if (file == justWritten) continue
            val size = file.length()
            if (file.delete()) bytes -= size
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (maxOf(width / sample, height / sample) > maxEdge * 2) sample *= 2
        return sample
    }

    private fun safeKey(key: String): String = key.hashCode().toUInt().toString(16)

    private fun Exception.isBrokenPipe(): Boolean =
        generateSequence<Throwable>(this) { it.cause }
            .filterIsInstance<ErrnoException>()
            .any { it.errno == OsConstants.EPIPE }

    private data class ProgramRecord(
        val mediaId: String,
        val audioUri: String?,
        val artworkUri: Uri,
        val durationMs: Long,
        val title: String,
    )

    private class Generation(
        val partialFile: File,
        val finalFile: File,
        completed: Boolean = false,
    ) {
        val result = CompletableDeferred<File>().apply {
            if (completed) complete(finalFile)
        }
        val readers = AtomicInteger(0)
        @Volatile var job: Job? = null
    }

    companion object {
        private const val CACHE_DIRECTORY = "tv_preview_video"
        private const val AUDIO_CACHE_DIRECTORY = "tv_preview_audio"
        private const val AUDIO_ONLY_QUERY = "mode"
        private const val AUDIO_ONLY_VALUE = "audio"
        private const val MIME_QUERY = "mime"
        private const val AUDIO_MP4_MIME = "audio/mp4"
        private const val DEBUG_PREFERENCES = "tv_preview_debug"
        private const val DEBUG_AUDIO_ONLY_PREFIX = "audio_only:"
        private const val AUDIO_CACHE_BYTES = 96L * 1024L * 1024L
        private const val WAVEFORM_CACHE_DIRECTORY = "tv_preview_gif"
        private const val MAX_CACHE_BYTES = 256L * 1024L * 1024L
        private const val MAX_ARTWORK_EDGE = 640
        private const val MAX_ARTWORK_BYTES = 12 * 1024 * 1024
        private const val MAX_TRACK_DURATION_MS = 6L * 60L * 60L * 1000L
        private const val UNKNOWN_TRACK_DURATION_FALLBACK_MS = 5L * 60L * 1000L
        private const val OPEN_WAIT_TIMEOUT_MS = 300_000L
        private const val RESOLVE_TIMEOUT_MS = 30_000L
        private const val DURATION_PROBE_TIMEOUT_MS = 12_000L
        private const val NETWORK_TIMEOUT_MS = 15_000
        private const val PREVIEW_VIDEO_WIDTH = 320
        private const val PREVIEW_VIDEO_HEIGHT = 180
        private const val COLUMN_POSTER_ART_URI = "poster_art_uri"
        private const val COLUMN_THUMBNAIL_URI = "thumbnail_uri"
        private const val COLUMN_TITLE = "title"
        private const val STALE_PROGRAM_PREFERENCES = "tv_preview_stale_programs"
        private const val SOURCE_ARTWORK_PREFERENCES = "tv_preview_source_artwork"
        private const val SOURCE_ARTWORK_JSON = "programs"
        private val waveformGenerationLock = Any()

        internal fun rememberSourceArtwork(context: Context, artworkByProgram: Map<String, String>) {
            val values = JSONObject()
            artworkByProgram.forEach { (key, uri) ->
                if (uri.isNotBlank()) values.put(key, uri)
            }
            if (!context.getSharedPreferences(SOURCE_ARTWORK_PREFERENCES, Context.MODE_PRIVATE)
                    .edit().putString(SOURCE_ARTWORK_JSON, values.toString()).commit()
            ) {
                Timber.w("Could not persist TV preview source artwork metadata")
            }
        }

        internal fun sourceArtworkUri(context: Context, key: String, fallback: String): String =
            runCatching {
                val encoded = context.getSharedPreferences(SOURCE_ARTWORK_PREFERENCES, Context.MODE_PRIVATE)
                    .getString(SOURCE_ARTWORK_JSON, null) ?: return@runCatching fallback
                JSONObject(encoded).optString(key).takeIf(String::isNotBlank) ?: fallback
            }.getOrDefault(fallback)
        private const val STALE_PROGRAM_PREFIX = "program:"
        private const val STALE_PROGRAM_TTL_MS = 5 * 60 * 1000L
        private const val MAX_STALE_PROGRAMS = 20
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mainHandler = Handler(Looper.getMainLooper())
        private val exportSemaphore = Semaphore(1)
        private val audioDownloadGate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 2)
        private val audioCacheLock = Any()
        private val audioCachePins = mutableMapOf<String, Int>()
        private val generations = ConcurrentHashMap<String, Generation>()
        private val audioGenerations = SharedPreviewGenerationRegistry<String, File>()

        internal fun rememberDeletedPrograms(context: Context, programs: List<ExistingPreviewProgram>) {
            synchronized(staleProgramLock) {
                val preferences = context.getSharedPreferences(STALE_PROGRAM_PREFERENCES, Context.MODE_PRIVATE)
                val now = System.currentTimeMillis()
                val existing = preferences.all.mapNotNull { (preferenceKey, value) ->
                    if (!preferenceKey.startsWith(STALE_PROGRAM_PREFIX)) return@mapNotNull null
                    val internalId = preferenceKey.removePrefix(STALE_PROGRAM_PREFIX)
                    runCatching {
                        val snapshot = JSONObject(value as? String ?: return@runCatching null)
                        RetainedTvProgramMetadata(
                            internalProviderId = internalId,
                            contentId = snapshot.getString("mediaId"),
                            previewAudioUri = snapshot.optString("audioUri")
                                .takeUnless { it.isBlank() || it == "null" },
                            posterArtUri = snapshot.getString("artworkUri"),
                            durationMs = snapshot.optLong("durationMs").coerceAtLeast(0L),
                            title = snapshot.optString("title"),
                            expiresAtMs = snapshot.optLong("expiresAtMs"),
                        )
                    }.getOrNull()
                }
                val retained = retainRecentlyDeletedTvPrograms(
                    existing = existing,
                    deleted = programs,
                    nowMillis = now,
                    ttlMillis = STALE_PROGRAM_TTL_MS,
                    maxEntries = MAX_STALE_PROGRAMS,
                )
                val retainedKeys = retained.mapTo(mutableSetOf()) {
                    staleProgramPreferenceKey(it.internalProviderId)
                }
                val editor = preferences.edit()
                preferences.all.keys
                    .filter { it.startsWith(STALE_PROGRAM_PREFIX) && it !in retainedKeys }
                    .forEach(editor::remove)
                retained.forEach { snapshot ->
                    editor.putString(
                        staleProgramPreferenceKey(snapshot.internalProviderId),
                        JSONObject()
                            .put("expiresAtMs", snapshot.expiresAtMs)
                            .put("mediaId", snapshot.contentId)
                            .put("audioUri", snapshot.previewAudioUri)
                            .put("artworkUri", snapshot.posterArtUri)
                            .put("durationMs", snapshot.durationMs)
                            .put("title", snapshot.title)
                            .toString(),
                    )
                }
                if (!editor.commit()) {
                    Timber.w("Could not persist recently removed TV preview metadata")
                }
            }
        }

        private fun staleProgramPreferenceKey(internalProviderId: String) =
            "$STALE_PROGRAM_PREFIX$internalProviderId"

        private val staleProgramLock = Any()
    }
}
