/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.YTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NewReleasesViewModel @Inject constructor() : ViewModel() {
    private val _albumReleases = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albumReleases = _albumReleases.asStateFlow()

    private val _songReleases = MutableStateFlow<List<SongItem>>(emptyList())
    val songReleases = _songReleases.asStateFlow()

    private val _videoReleases = MutableStateFlow<List<YTItem>>(emptyList())
    val videoReleases = _videoReleases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadNewReleases()
    }

    fun loadNewReleases() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                // Get new release albums using the specific endpoint
                YouTube.newReleaseAlbums()
                    .onSuccess { albums ->
                        Timber.d("New release albums: ${albums.size}")
                        _albumReleases.value = albums
                    }
                    .onFailure { throwable ->
                        Timber.e(throwable, "Failed to load new release albums")
                        _error.value = throwable.message
                    }
                
                // Also try the general new releases browse endpoint for songs and videos
                YouTube.browse("FEmusic_new_releases", null)
                    .onSuccess { browseResult ->
                        Timber.d("New releases browse result: title=${browseResult.title}, sections=${browseResult.items.size}")
                        
                        val songs = mutableListOf<SongItem>()
                        val videos = mutableListOf<YTItem>()

                        // Parse the browse result and separate by type
                        browseResult.items.forEach { section ->
                            Timber.d("Section: ${section.title}, items=${section.items.size}")
                            section.items.forEach { item ->
                                when (item) {
                                    is SongItem -> {
                                        Timber.d("Song: ${item.title}")
                                        songs.add(item)
                                    }
                                    else -> {
                                        Timber.d("Video/Other: ${item.title}, type=${item::class.simpleName}")
                                        videos.add(item)
                                    }
                                }
                            }
                        }

                        Timber.d("Parsed results - Songs: ${songs.size}, Videos: ${videos.size}")
                        
                        // Only update if we have data, otherwise keep existing
                        if (songs.isNotEmpty()) {
                            _songReleases.value = songs
                        }
                        if (videos.isNotEmpty()) {
                            _videoReleases.value = videos
                        }
                    }
                    .onFailure { throwable ->
                        Timber.e(throwable, "Failed to load new releases browse")
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                Timber.e(e, "Error loading new releases")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
