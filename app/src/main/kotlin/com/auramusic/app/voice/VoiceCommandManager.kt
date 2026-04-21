package com.auramusic.app.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface VoiceRecognitionEvent {
    data object Ready : VoiceRecognitionEvent
    data class Rms(val value: Float) : VoiceRecognitionEvent
    data class PartialText(val text: String) : VoiceRecognitionEvent
    data class FinalText(val text: String) : VoiceRecognitionEvent
    data class Error(val code: Int, val message: String, val recoverable: Boolean) : VoiceRecognitionEvent
    data object EndOfSpeech : VoiceRecognitionEvent
    data object WakeWordDetected : VoiceRecognitionEvent // Emitted by Porcupine detector
}

enum class RecognitionMode { WAKE_WORD, COMMAND }

@Singleton
class VoiceCommandManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _events = MutableSharedFlow<VoiceRecognitionEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<VoiceRecognitionEvent> = _events.asSharedFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var currentMode: RecognitionMode = RecognitionMode.WAKE_WORD

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Called by WakeWordService when Porcupine detects the wake word.
     * Triggers a WakeWordDetected event for ViewModel to handle.
     */
    fun onWakeWordDetected() {
        val emitted = _events.tryEmit(VoiceRecognitionEvent.WakeWordDetected)
        android.util.Log.d("VoiceCommandManager", "WakeWordDetected emitted=$emitted")
    }

fun startListening(mode: RecognitionMode) {
        if (!isAvailable()) {
            _events.tryEmit(VoiceRecognitionEvent.Error(-1, "Speech recognition not available", false))
            return
        }

        currentMode = mode
        
        // Destroy previous recognizer first, then delay before creating a new one
        // to prevent recognizer busy errors from rapid restarts (avoid Thread.sleep on main thread)
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
        mainHandler.postDelayed({
            try {
                // Disable system sound effects (like Google mic click sounds)
                disableSystemSoundEffects()

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    // Don't request audio focus to avoid pausing playback
                    putExtra("android.speech.extra.AUDIO_FOCUS", false)
                    when (mode) {
                        RecognitionMode.WAKE_WORD -> {
                            // Longer wake word window: keep listening for up to 10 seconds of silence
                            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 7000L)
                        }
                        RecognitionMode.COMMAND -> {
                            // Longer silence timeouts so SpeechRecognizer waits for user to actually speak
                            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                        }
                    }
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                android.util.Log.e("VoiceCommandManager", "Failed to start listening", e)
                _events.tryEmit(VoiceRecognitionEvent.Error(-1, "Failed to start: ${e.message}", true))
                _isListening.value = false
            }
        }, 150)
    }

    private fun disableSystemSoundEffects() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Mute only STREAM_MUSIC to suppress the SpeechRecognizer mic-access beep
            // without affecting notification sounds (STREAM_NOTIFICATION / STREAM_RING)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    0
                )
            }
        } catch (e: Exception) {
            // Silently ignore; if it fails we'll just have sound effects
        }
    }

    fun enableSystemSoundEffects() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
            }
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
            _isListening.value = false
            enableSystemSoundEffects()
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            _isListening.value = false
            enableSystemSoundEffects()
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _events.tryEmit(VoiceRecognitionEvent.Ready)
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            _events.tryEmit(VoiceRecognitionEvent.Rms(rmsdB))
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _events.tryEmit(VoiceRecognitionEvent.EndOfSpeech)
        }

        override fun onError(error: Int) {
            _isListening.value = false
            enableSystemSoundEffects()
            val recoverable = error in listOf(
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_CLIENT
            )
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error"
            }
            _events.tryEmit(VoiceRecognitionEvent.Error(error, message, recoverable))
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            enableSystemSoundEffects()
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            _events.tryEmit(VoiceRecognitionEvent.FinalText(text))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotEmpty()) {
                _events.tryEmit(VoiceRecognitionEvent.PartialText(text))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
