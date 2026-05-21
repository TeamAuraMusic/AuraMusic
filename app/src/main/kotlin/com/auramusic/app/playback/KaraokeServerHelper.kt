package com.auramusic.app.playback

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object KaraokeServerHelper {
    // OkHttp engine: more reliable than CIO on Android, especially behind
    // Cloudflare. Generous timeouts because ML separation can take a while.
    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 5 * 60 * 1000L   // 5 min – separation is slow
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 5 * 60 * 1000L
        }
    }

    private const val SERVER_URL = "https://karaoke.auramusic.site/instrumental"

    /**
     * Uploads the given audio file to the ML karaoke server and returns the
     * instrumental version. Saves the result inside [outputDir] and returns
     * the [File], or null on failure.
     */
    suspend fun separateToInstrumental(
        inputAudio: File,
        outputDir: File = File(System.getProperty("java.io.tmpdir", "/tmp"))
    ): File? = withContext(Dispatchers.IO) {
        try {
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
            )

            if (!response.status.isSuccess()) return@withContext null

            val instrumentalFile = File(
                outputDir,
                "instrumental_${inputAudio.nameWithoutExtension}.wav"
            )
            response.bodyAsChannel().copyTo(instrumentalFile.writeChannel())
            instrumentalFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
