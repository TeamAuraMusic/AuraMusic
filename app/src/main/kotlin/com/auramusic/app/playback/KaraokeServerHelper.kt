package com.auramusic.app.playback

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
    private val client = HttpClient(CIO)

    private const val SERVER_URL = "https://karaoke.auramusic.site/instrumental"

    /**
     * Uploads the given audio file to the ML karaoke server and returns the instrumental version.
     * The result is saved to app cache and returned as a File.
     */
    suspend fun separateToInstrumental(
        inputAudio: File,
        outputDir: File = File(System.getProperty("java.io.tmpdir", "/tmp"))
    ): File? = withContext(Dispatchers.IO) {
        try {
            val response = client.submitFormWithBinaryData(
                url = SERVER_URL,
                formData = formData {
                    append(
                        "file",
                        inputAudio.readBytes(),
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"${inputAudio.name}\"")
                        }
                    )
                }
            )

            if (!response.status.isSuccess()) return@withContext null

            val instrumentalFile = File(outputDir, "instrumental_${inputAudio.nameWithoutExtension}.wav")
            response.bodyAsChannel().copyTo(instrumentalFile.writeChannel())
            instrumentalFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
