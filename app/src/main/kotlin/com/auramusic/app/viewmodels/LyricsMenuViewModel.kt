/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.LyricsEntity
import com.auramusic.app.db.entities.Song
import com.auramusic.app.lyrics.LyricsHelper
import com.auramusic.app.lyrics.LyricsResult
import com.auramusic.app.models.MediaMetadata
import com.auramusic.app.utils.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    private val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
    private val networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {
    private var job: Job? = null
    val results = MutableStateFlow(emptyList<LyricsResult>())
    val isLoading = MutableStateFlow(false)

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    init {
        viewModelScope.launch {
            networkConnectivity.networkStatus.collect { isConnected ->
                _isNetworkAvailable.value = isConnected
            }
        }

        _isNetworkAvailable.value = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true // Assume connected as fallback
        }
    }

    fun setCurrentSong(song: Song) {
        _currentSong.value = song
    }

    fun search(
        mediaId: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ) {
        isLoading.value = true
        results.value = emptyList()
        job?.cancel()
        job =
            viewModelScope.launch(Dispatchers.IO) {
                lyricsHelper.getAllLyrics(mediaId, title, artist, duration, album) { result ->
                    results.update {
                        it + result
                    }
                }
                isLoading.value = false
            }
    }

    fun cancelSearch() {
        job?.cancel()
        job = null
    }

    fun refetchLyrics(
        mediaMetadata: MediaMetadata,
        lyricsEntity: LyricsEntity?,
    ) {
        // The previous implementation called `runBlocking { lyricsHelper.getLyrics(...) }`
        // *inside* a Room transaction. That blocked the Room writer thread for the
        // entire ~25s of the lyrics pipeline, made the UI feel like the retry button
        // had done nothing, and (because LyricsHelper has an in-memory cache) it
        // simply returned the same lyrics it had cached — so retry was a no-op.
        //
        // Now we: cancel any running fetch, flip `isLoading` so the UI can show
        // feedback, invalidate the cached entry so the next call actually hits
        // the network, then write the result back to Room from a normal coroutine.
        job?.cancel()
        isLoading.value = true
        lyricsHelper.invalidateCache(mediaMetadata.id)
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (lyricsEntity != null) {
                    database.query { delete(lyricsEntity) }
                }
                val lyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            mediaMetadata.id,
                            lyricsWithProvider.lyrics,
                            lyricsWithProvider.provider,
                        ),
                    )
                }
            } finally {
                isLoading.value = false
            }
        }
    }
}
