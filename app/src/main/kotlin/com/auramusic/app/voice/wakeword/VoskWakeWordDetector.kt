package com.auramusic.app.voice.wakeword

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VOSK-based wake word detector.
 * Continuously listens and triggers when "aura" is detected.
 */
@Singleton
class VoskWakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) : WakeWordDetector {

    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null
    private var recognizer: Recognizer? = null
    private var model: Model? = null
    private val isRunning = AtomicBoolean(false)
    private var wakeWordCallback: (() -> Unit)? = null

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 4096
        private const val MODEL_NAME = "vosk-model-small-en-0.15"
        private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-0.15.zip"
        private const val WAKE_WORD = "aura"
    }

    override fun setOnWakeWordDetectedListener(callback: () -> Unit) {
        this.wakeWordCallback = callback
    }

    override fun start() {
        if (isRunning.getAndSet(true)) return

        detectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Download model if not present
                val modelPath = ensureModel()
                model = Model(modelPath)
                recognizer = Recognizer(model, SAMPLE_RATE.toFloat())

                // Start audio loop
                startAudioRecording()
            } catch (e: Exception) {
                e.printStackTrace()
                isRunning.set(false)
            }
        }
    }

    private suspend fun ensureModel(): String {
        val modelDir = File(context.filesDir, MODEL_NAME)
        if (!modelDir.exists()) {
            val zipFile = File(context.cacheDir, "$MODEL_NAME.zip")
            if (!zipFile.exists()) {
                withContext(Dispatchers.IO) {
                    URL(MODEL_URL).openStream().use { input ->
                        FileOutputStream(zipFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            unzip(zipFile.absolutePath, context.filesDir.absolutePath)
            zipFile.delete()
        }
        return modelDir.absolutePath
    }

    private fun unzip(zipPath: String, destDir: String) {
        java.util.zip.ZipInputStream(FileInputStream(File(zipPath))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    override fun stop() {
        if (isRunning.getAndSet(false).not()) return

        detectionJob?.cancel()
        detectionJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recognizer?.close()
            recognizer = null
            model?.close()
            model = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun close() = stop()

    private suspend fun startAudioRecording() {
        val buffer = ShortArray(BUFFER_SIZE)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            BUFFER_SIZE * 2
        )

        audioRecord?.startRecording()

        while (isRunning.get()) {
            val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
            if (read > 0) {
                try {
                    // VOSK accepts short[] buffer directly with number of samples
                    val isFinal = recognizer?.acceptWaveForm(buffer, read)

                    // Check partial result (real-time)
                    val partialJson = recognizer?.partialResult ?: ""
                    if (WAKE_WORD in partialJson.lowercase()) {
                        triggerWakeWord()
                        continue
                    }

                    // Check final result if utterance ended
                    if (isFinal == true) {
                        val finalJson = recognizer?.result ?: ""
                        if (WAKE_WORD in finalJson.lowercase()) {
                            triggerWakeWord()
                            continue
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun triggerWakeWord() {
        withContext(Dispatchers.Main) {
            wakeWordCallback?.invoke()
        }
        // Cooldown to avoid rapid re-trigger
        delay(1500)
        recognizer?.reset()
    }
}
