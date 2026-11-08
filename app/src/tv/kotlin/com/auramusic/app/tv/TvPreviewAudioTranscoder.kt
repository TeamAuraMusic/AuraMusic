package com.auramusic.app.tv

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Converts unsupported source containers to the AAC/MP4 audio-only format verified in Projectivy. */
internal object TvPreviewAudioTranscoder {
    suspend fun transcode(context: Context, source: File, output: File) {
        val partial = File(output.parentFile, "${output.name}.transcoding.mp4")
        partial.delete()
        try {
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    lateinit var transformer: Transformer
                    val listener = object : Transformer.Listener {
                        override fun onCompleted(composition: androidx.media3.transformer.Composition,
                                                 exportResult: ExportResult) {
                            transformer.removeListener(this)
                            if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun onError(composition: androidx.media3.transformer.Composition,
                                             exportResult: ExportResult,
                                             exportException: ExportException) {
                            transformer.removeListener(this)
                            if (continuation.isActive) continuation.resumeWithException(exportException)
                        }

                        override fun onFallbackApplied(
                            composition: androidx.media3.transformer.Composition,
                            originalTransformationRequest: TransformationRequest,
                            fallbackTransformationRequest: TransformationRequest,
                        ) {
                            Timber.w(
                                "TV preview audio transcode fallback requested=%s applied=%s",
                                originalTransformationRequest.audioMimeType,
                                fallbackTransformationRequest.audioMimeType,
                            )
                        }
                    }
                    transformer = Transformer.Builder(context.applicationContext)
                        .setLooper(Looper.getMainLooper())
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(listener)
                        .build()
                    continuation.invokeOnCancellation {
                        android.os.Handler(Looper.getMainLooper()).post {
                            runCatching { transformer.cancel() }
                            partial.delete()
                        }
                    }
                    try {
                        val editedItem = EditedMediaItem.Builder(
                            MediaItem.fromUri(Uri.fromFile(source)),
                        ).setRemoveVideo(true).build()
                        transformer.start(editedItem, partial.absolutePath)
                    } catch (error: Exception) {
                        transformer.removeListener(listener)
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }

            withContext(Dispatchers.IO) {
                require(partial.isFile && partial.length() > 0L) {
                    "Media3 produced an empty audio-only preview"
                }
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(partial.absolutePath)
                    val tracks = (0 until extractor.trackCount).map { index ->
                        extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
                    }
                    require(hasProjectivyCompatibleAudioOnlyTracks(tracks)) {
                        "Expected one AAC audio track and no video, got $tracks"
                    }
                } finally {
                    extractor.release()
                }
                if (output.exists()) {
                    check(output.delete()) { "Could not replace incomplete normalized preview audio" }
                }
                check(partial.renameTo(output)) { "Could not atomically publish normalized preview audio" }
            }
        } catch (error: Exception) {
            partial.delete()
            output.delete()
            throw error
        }
    }
}
