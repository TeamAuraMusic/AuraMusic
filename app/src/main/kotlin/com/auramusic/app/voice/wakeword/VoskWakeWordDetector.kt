package com.auramusic.app.voice.wakeword

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

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
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 4096
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
        private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        private const val WAKE_WORD = "aura"
        // Grammar constrains VOSK to only recognize these phrases (+ [unk] for everything else)
        // Without this, VOSK does free-form recognition and may transcribe "aura" as "are a", "or a", etc.
        private const val WAKE_WORD_GRAMMAR = "[\"hey aura\", \"hello aura\", \"ok aura\", \"aura\", \"[unk]\"]"
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun setOnWakeWordDetectedListener(callback: () -> Unit) {
        this.wakeWordCallback = callback
    }

    override fun start() {
        if (isRunning.getAndSet(true)) return

        detectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelPath = ensureModel()
                android.util.Log.d("VoskWakeWordDetector", "Loading model from: $modelPath")
                model = Model(modelPath)
                // Use grammar mode to constrain recognition to wake word phrases only
                recognizer = Recognizer(model, SAMPLE_RATE.toFloat(), WAKE_WORD_GRAMMAR)
                android.util.Log.d("VoskWakeWordDetector", "Model loaded with grammar, starting audio")
                showToast("Aura wake word detection active")
                startAudioRecording()
            } catch (e: Exception) {
                android.util.Log.e("VoskWakeWordDetector", "Failed to start", e)
                showToast("Wake word failed: ${e.message}")
                isRunning.set(false)
            }
        }
    }

    private suspend fun ensureModel(): String {
        val modelDir = File(context.filesDir, MODEL_NAME)
        if (modelDir.exists()) {
            return modelDir.absolutePath
        }

        showToast("Downloading wake word model...")
        val zipFile = File(context.cacheDir, "$MODEL_NAME.zip")
        if (!zipFile.exists()) {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .build()
                val request = Request.Builder().url(MODEL_URL).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("Download failed: HTTP ${response.code}")
                }
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(zipFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Download failed: empty response")
            }
        }
        showToast("Unpacking wake word model...")
        unzip(zipFile.absolutePath, context.filesDir.absolutePath)
        zipFile.delete()

        if (!modelDir.exists()) {
            throw Exception("Model directory not found after unzip")
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
        isRunning.set(false)

        detectionJob?.cancel()
        detectionJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            android.util.Log.e("VoskWakeWordDetector", "Error stopping audio", e)
        }

        try {
            recognizer?.close()
            recognizer = null
            model?.close()
            model = null
        } catch (e: Exception) {
            android.util.Log.e("VoskWakeWordDetector", "Error stopping model", e)
        }
    }

    override fun close() = stop()

    private suspend fun startAudioRecording() {
        val buffer = ShortArray(BUFFER_SIZE)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                android.util.Log.e("VoskWakeWordDetector", "AudioRecord not initialized")
                isRunning.set(false)
                return
            }

            audioRecord?.startRecording()
            android.util.Log.d("VoskWakeWordDetector", "Recording started")

            while (isRunning.get()) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                if (read > 0) {
                    try {
                        val isFinal = recognizer?.acceptWaveForm(buffer, read)

                        // Check partial results for wake word (low-latency detection)
                        val partialJson = recognizer?.partialResult ?: ""
                        if (partialJson.isNotEmpty() && "[unk]" !in partialJson) {
                            android.util.Log.d("VoskWakeWordDetector", "Partial: $partialJson")
                        }
                        if (WAKE_WORD in partialJson.lowercase() && "[unk]" !in partialJson) {
                            android.util.Log.d("VoskWakeWordDetector", "DETECTED in partial: $partialJson")
                            showToast("Wake word detected!")
                            triggerWakeWord()
                            continue
                        }

                        // Check final results
                        if (isFinal == true) {
                            val finalJson = recognizer?.result ?: ""
                            if (WAKE_WORD in finalJson.lowercase() && "[unk]" !in finalJson) {
                                android.util.Log.d("VoskWakeWordDetector", "DETECTED in final: $finalJson")
                                showToast("Wake word detected!")
                                triggerWakeWord()
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("VoskWakeWordDetector", "Processing error", e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoskWakeWordDetector", "Audio recording failed", e)
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    private suspend fun triggerWakeWord() {
        // Stop recording immediately so the mic is released before SpeechRecognizer needs it
        android.util.Log.d("VoskWakeWordDetector", "triggerWakeWord: callbackNull=${wakeWordCallback == null}")
        isRunning.set(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            android.util.Log.e("VoskWakeWordDetector", "Error releasing audio on trigger", e)
        }

        withContext(Dispatchers.Main) {
            wakeWordCallback?.invoke()
        }
    }
}
