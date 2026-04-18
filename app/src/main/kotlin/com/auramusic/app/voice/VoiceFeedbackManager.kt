package com.auramusic.app.voice

import android.app.Service
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false
    private var originalMusicVolume = 0
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Available English voices
    private var availableVoices: List<Voice> = emptyList()
    private var currentVoice: Voice? = null
    private var currentPitch = 1.0f
    private var currentSpeechRate = 1.0f
    
    companion object {
        private const val PREFS_NAME = "voice_feedback_prefs"
        private const val KEY_VOICE_NAME = "selected_voice"
        private const val KEY_PITCH = "pitch"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_ENABLED = "enabled"
    }

    fun initialize() {
        if (isInitialized) return
        
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Detect available English voices - limit to main 5 locales
                val mainEnglishLocales = listOf(Locale.US, Locale.UK, Locale.CANADA, Locale("en", "AU"), Locale("en", "IN"))
                availableVoices = tts?.voices?.filter { voice ->
                    voice.locale.language == "en" && mainEnglishLocales.any { it.language == voice.locale.language && it.country == voice.locale.country }
                }?.sortedBy { it.locale.displayName }
                    ?.distinctBy { it.locale.displayName }
                    ?: emptyList()
                
                // Load saved voice preference
                val prefs = context.getSharedPreferences(PREFS_NAME, Service.MODE_PRIVATE)
                val savedVoiceName = prefs.getString(KEY_VOICE_NAME, null)
                
                currentVoice = availableVoices.find { it.name == savedVoiceName }
                    ?: availableVoices.firstOrNull { it.locale == Locale.US }
                    ?: availableVoices.firstOrNull()
                
                currentVoice?.let { tts?.voice = it }

                // Route TTS output to a separate stream so it is not
                // affected by STREAM_MUSIC ducking/muting
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                }

                // Load pitch/speed
                currentPitch = prefs.getFloat(KEY_PITCH, 1.0f)
                currentSpeechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
                tts?.setPitch(currentPitch)
                tts?.setSpeechRate(currentSpeechRate)
                
                isInitialized = true
                android.util.Log.d("VoiceFeedbackManager", "TTS initialized with ${availableVoices.size} English voices. Selected: ${currentVoice?.name}")
            } else {
                android.util.Log.e("VoiceFeedbackManager", "TTS initialization failed: $status")
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized) {
            android.util.Log.w("VoiceFeedbackManager", "TTS not initialized, cannot speak")
            onComplete?.invoke()
            return
        }

        scope.launch {
            try {
                // Lower music volume during speech
                lowerMusicVolume()
                
                isSpeaking = true
                
                val utteranceId = UUID.randomUUID().toString()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    
                    // Set listener to know when done (API 21+)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // Started speaking
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                restoreMusicVolume()
                                isSpeaking = false
                                onComplete?.invoke()
                            }
                        }
                        
                        @Deprecated("Deprecated in Android API")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post {
                                android.util.Log.e("VoiceFeedbackManager", "TTS utterance error")
                                restoreMusicVolume()
                                isSpeaking = false
                                onComplete?.invoke()
                            }
                        }
                    })
                    
                    // Fallback restore after 5 seconds in case listener fails
                    launch {
                        delay(5000)
                        if (isSpeaking) {
                            restoreMusicVolume()
                            isSpeaking = false
                            onComplete?.invoke()
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                    // For older APIs, restore after a delay
                    launch {
                        delay(3000)
                        restoreMusicVolume()
                        isSpeaking = false
                        onComplete?.invoke()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VoiceFeedbackManager", "Speech error", e)
                isSpeaking = false
                restoreMusicVolume()
                onComplete?.invoke()
            }
        }
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        restoreMusicVolume()
    }

    fun isSpeaking(): Boolean = isSpeaking
    
    fun getAvailableVoices(): List<Voice> = availableVoices
    fun getCurrentVoice(): Voice? = currentVoice
    fun setVoice(voice: Voice) {
        currentVoice = voice
        tts?.voice = voice
        // Save preference
        context.getSharedPreferences(PREFS_NAME, Service.MODE_PRIVATE)
            .edit()
            .putString(KEY_VOICE_NAME, voice.name)
            .apply()
    }
    
    fun getPitch(): Float = currentPitch
    fun setPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(currentPitch)
        context.getSharedPreferences(PREFS_NAME, Service.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_PITCH, currentPitch)
            .apply()
    }
    
    fun getSpeechRate(): Float = currentSpeechRate
    fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(currentSpeechRate)
        context.getSharedPreferences(PREFS_NAME, Service.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_SPEECH_RATE, currentSpeechRate)
            .apply()
    }

    fun isEnabled(): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Service.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Service.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    private fun lowerMusicVolume() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            // Only duck if volume is meaningful; skip if stream is muted/very low
            // to avoid saving a muted state as the "original" volume
            if (currentVolume <= 1) {
                originalMusicVolume = -1 // sentinel: do not restore
                return
            }
            originalMusicVolume = currentVolume
            val reducedVolume = (originalMusicVolume * 0.3f).toInt().coerceAtLeast(1)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, reducedVolume, 0)
            android.util.Log.d("VoiceFeedbackManager", "Music volume lowered from $originalMusicVolume to $reducedVolume")
        } catch (e: Exception) {
            android.util.Log.e("VoiceFeedbackManager", "Failed to lower volume", e)
        }
    }

    private fun restoreMusicVolume() {
        try {
            if (originalMusicVolume < 0) return // was not ducked
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
            android.util.Log.d("VoiceFeedbackManager", "Music volume restored to $originalMusicVolume")
        } catch (e: Exception) {
            android.util.Log.e("VoiceFeedbackManager", "Failed to restore volume", e)
        }
    }
}
