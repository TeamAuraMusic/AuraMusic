package com.auramusic.app.voice.wakeword

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Porcupine-based wake word detector.
 * Runs continuously in a background coroutine, listening for the configured keyword.
 */
@Singleton
class PorcupineWakeWordDetector @Inject constructor(
    private val context: Context,
) : WakeWordDetector {

    private var porcupine: Porcupine? = null
    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    private var wakeWordCallback: (() -> Unit)? = null

    companion object {
        private const val TAG = "PorcupineDetector"
        private const val SAMPLE_RATE = 16000
        private const val FRAME_LENGTH = 512
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val KEYWORD_PATH = "aura.ppn" // .ppn file placed in assets/
    }

    fun setOnWakeWordDetectedListener(callback: () -> Unit) {
        this.wakeWordCallback = callback
    }

    override fun start() {
        if (isRunning.getAndSet(true)) return

        try {
            // Initialize Porcupine with the keyword model from assets
            porcupine = Porcupine.Builder()
                .setKeywordPath(KEYWORD_PATH)
                .setSensitivity(0.5f)
                .build(context.assets)

            // Start audio recording and detection loop
            detectionJob = CoroutineScope(Dispatchers.IO).launch {
                startAudioRecording()
            }
        } catch (e: PorcupineException) {
            e.printStackTrace()
            isRunning.set(false)
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
            porcupine?.delete()
            porcupine = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun close() = stop()

    private suspend fun startAudioRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * 2

        val buffer = ShortArray(FRAME_LENGTH)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        audioRecord?.startRecording()

        while (isRunning.get()) {
            val read = audioRecord?.read(buffer, 0, FRAME_LENGTH) ?: -1
            if (read > 0) {
                try {
                    val result = porcupine?.process(buffer.copyOf(read))
                    if (result == Porcupine.PORCUPINE_PROCESSED_PPN) {
                        // Wake word detected!
                        withContext(Dispatchers.Main) {
                            wakeWordCallback?.invoke()
                        }
                    }
                } catch (e: PorcupineException) {
                    e.printStackTrace()
                    break
                }
                    }
                } catch (e: PorcupineException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
}
