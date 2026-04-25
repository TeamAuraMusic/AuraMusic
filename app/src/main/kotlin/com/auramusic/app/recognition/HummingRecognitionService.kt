/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.SearchFilter
import com.auramusic.shazamkit.models.RecognitionResult
import com.auramusic.shazamkit.models.RecognitionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Service for recognizing songs from humming, singing, or whistling.
 * Uses Android's SpeechRecognizer to capture sung/hummed melody text,
 * then searches YouTube Music to find matching songs.
 * Falls back to Shazam fingerprint recognition if speech recognition fails.
 */
object HummingRecognitionService {

    private val _recognitionStatus = MutableStateFlow<RecognitionStatus>(RecognitionStatus.Ready)
    val recognitionStatus: StateFlow<RecognitionStatus> = _recognitionStatus.asStateFlow()

    fun hasRecordPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start humming recognition.
     * Records audio and attempts to identify the song via speech-to-text + YouTube Music search,
     * falling back to Shazam fingerprint if speech recognition yields no results.
     */
    @SuppressLint("MissingPermission")
    suspend fun recognize(context: Context): RecognitionStatus = withContext(Dispatchers.IO) {
        if (!hasRecordPermission(context)) {
            return@withContext RecognitionStatus.Error("Microphone permission not granted")
        }

        _recognitionStatus.value = RecognitionStatus.Listening

        try {
            // Try speech recognition first (captures sung lyrics)
            val spokenText = withContext(Dispatchers.Main) {
                recognizeSpeech(context)
            }

            _recognitionStatus.value = RecognitionStatus.Processing

            if (!spokenText.isNullOrBlank() && spokenText.length >= 3) {
                // Search YouTube Music with the transcribed text
                val searchResult = searchYouTubeMusic(spokenText)
                if (searchResult != null) {
                    _recognitionStatus.value = RecognitionStatus.Success(searchResult)
                    return@withContext _recognitionStatus.value
                }
            }

            // Fallback: try Shazam fingerprint recognition
            val shazamResult = MusicRecognitionService.recognize(context)
            _recognitionStatus.value = when (shazamResult) {
                is RecognitionStatus.Success -> shazamResult
                is RecognitionStatus.NoMatch -> RecognitionStatus.NoMatch(
                    "Couldn't identify the melody. Try humming more clearly or singing some lyrics."
                )
                else -> RecognitionStatus.NoMatch(
                    "Couldn't identify the melody. Try humming more clearly or singing some lyrics."
                )
            }

            _recognitionStatus.value
        } catch (e: Exception) {
            _recognitionStatus.value = RecognitionStatus.Error(e.message ?: "Recognition failed")
            _recognitionStatus.value
        }
    }

    /**
     * Use Android SpeechRecognizer to capture sung/hummed text.
     * Must be called from Main thread.
     */
    private suspend fun recognizeSpeech(context: Context): String? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return null
        }

        val deferred = CompletableDeferred<String?>()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                recognizer.destroy()
                deferred.complete(matches?.firstOrNull())
            }

            override fun onError(error: Int) {
                recognizer.destroy()
                deferred.complete(null)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)

        // Wait up to 12 seconds for speech recognition
        return withTimeoutOrNull(12000L) {
            deferred.await()
        } ?: run {
            try { recognizer.cancel(); recognizer.destroy() } catch (_: Exception) {}
            null
        }
    }

    /**
     * Search YouTube Music for a song matching the transcribed text.
     */
    private suspend fun searchYouTubeMusic(query: String): RecognitionResult? {
        return withContext(Dispatchers.IO) {
            try {
                val searchResult = YouTube.search(query, SearchFilter.FILTER_SONG).getOrNull()
                val firstSong = searchResult?.items?.firstOrNull() ?: return@withContext null

                // Get more details if possible
                val songTitle = firstSong.title
                val songArtist = (firstSong as? com.auramusic.innertube.models.SongItem)
                    ?.artists?.joinToString(", ") { it.name } ?: ""
                val thumbnail = firstSong.thumbnail

                RecognitionResult(
                    trackId = firstSong.id,
                    title = songTitle,
                    artist = songArtist,
                    album = null,
                    coverArtUrl = thumbnail,
                    coverArtHqUrl = thumbnail,
                    genre = null,
                    releaseDate = null,
                    label = null,
                    lyrics = null,
                    shazamUrl = null,
                    appleMusicUrl = null,
                    spotifyUrl = null,
                    isrc = null,
                    youtubeVideoId = firstSong.id
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun reset() {
        _recognitionStatus.value = RecognitionStatus.Ready
    }
}
