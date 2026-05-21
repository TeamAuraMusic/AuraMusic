package com.auramusic.app.playback

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full lifecycle of an ML-karaoke request, observable by the UI so the
 * connection sheet can show what's happening at every step.
 */
sealed class KaraokeProgress {
    object Idle : KaraokeProgress()
    object Connecting : KaraokeProgress()
    object Connected : KaraokeProgress()
    data class Uploading(val percent: Int) : KaraokeProgress()
    object Processing : KaraokeProgress()
    data class Downloading(val percent: Int) : KaraokeProgress()
    object Preparing : KaraokeProgress()
    object Playing : KaraokeProgress()
    data class Failed(val reason: String) : KaraokeProgress()
}

object KaraokeServerHelper {

    private val _progress = MutableStateFlow<KaraokeProgress>(KaraokeProgress.Idle)
    val progress: StateFlow<KaraokeProgress> = _progress.asStateFlow()

    /** Allow callers (e.g. MusicService) to drive the later pipeline stages. */
    fun setProgress(state: KaraokeProgress) {
        _progress.value = state
    }

    // OkHttp engine: more reliable than CIO on Android, especially behind
    // Cloudflare. Generous timeouts because ML separation can take a while
    // and Render's free tier may cold-start (up to ~90 s).
    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 5 * 60 * 1000L   // 5 min – separation is slow
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 5 * 60 * 1000L
        }
    }

    private const val SERVER_URL = "https://karaoke.auramusic.site/instrumental"

    /**
     * Uploads the given audio file to the ML karaoke server and returns the
     * instrumental version. Emits progress events to [progress] throughout.
     */
    suspend fun separateToInstrumental(
        inputAudio: File,
        outputDir: File = File(System.getProperty("java.io.tmpdir", "/tmp"))
    ): File? = withContext(Dispatchers.IO) {
        try {
            _progress.value = KaraokeProgress.Uploading(0)
            val response = client.submitFormWithBinaryData(
                url = SERVER_URL,
                formData = formData {
                    // Form field name MUST be "file" – matches FastAPI:
                    //   async def get_instrumental(file: UploadFile = File(...))
                    append(
                        "file",
                        inputAudio.readBytes(),
                        Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=\"${inputAudio.name}\""
                            )
                        }
                    )
                }
            ) {
                onUpload { bytesSent, contentLength ->
                    val total = contentLength ?: 0L
                    val pct = if (total > 0L) ((bytesSent * 100) / total).toInt() else 0
                    _progress.value = KaraokeProgress.Uploading(pct.coerceIn(0, 100))
                    if (bytesSent >= total && total > 0L) {
                        _progress.value = KaraokeProgress.Processing
                    }
                }
                onDownload { bytesReceived, contentLength ->
                    val total = contentLength ?: 0L
                    val pct = if (total > 0L) ((bytesReceived * 100) / total).toInt() else 0
                    _progress.value = KaraokeProgress.Downloading(pct.coerceIn(0, 100))
                }
            }

            if (!response.status.isSuccess()) {
                _progress.value = KaraokeProgress.Failed("Server returned HTTP ${response.status.value}")
                return@withContext null
            }

            val instrumentalFile = File(
                outputDir,
                "instrumental_${inputAudio.nameWithoutExtension}.wav"
            )
            response.bodyAsChannel().copyTo(instrumentalFile.writeChannel())
            instrumentalFile
        } catch (e: Exception) {
            e.printStackTrace()
            _progress.value = KaraokeProgress.Failed(e.message ?: e::class.java.simpleName)
            null
        }
    }
}
