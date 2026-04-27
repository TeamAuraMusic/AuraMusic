/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvSearchViewModel
@Inject
constructor(
    private val database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults

    fun updateQuery(newQuery: String) {
        query.value = newQuery
        if (newQuery.isNotBlank()) {
            performSearch(newQuery)
        } else {
            clearResults()
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Perform local search
            try {
                val results = mutableListOf<Any>()

                // Search songs
                val songs = database.searchSongs("%$query%", 10).collect { songList ->
                    results.addAll(songList)
                }

                // Search artists
                val artists = database.searchArtists("%$query%", 5).collect { artistList ->
                    results.addAll(artistList)
                }

                // Search albums
                val albums = database.searchAlbums("%$query%", 5).collect { albumList ->
                    results.addAll(albumList)
                }

                // Search playlists
                val playlists = database.searchPlaylists("%$query%", 5).collect { playlistList ->
                    results.addAll(playlistList)
                }

                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun clearResults() {
        _searchResults.value = emptyList()
    }
}
    }

    private fun performSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                // Local database search
                val localResults = performLocalSearch(query)
                updateLocalResults(localResults)

                // YouTube search
                val ytResults = performYouTubeSearch(query)
                updateYTResults(ytResults)

                // Update combined results
                updateCombinedResults()
            } finally {
                _isSearching.value = false
            }
        }
    }

    private suspend fun performLocalSearch(query: String): Map<String, List<LocalItem>> {
        val results = mutableMapOf<String, List<LocalItem>>()

        try {
            val songs = database.searchSongs("%$query%", 20).first()
            results["songs"] = songs
        } catch (e: Exception) {
            results["songs"] = emptyList()
        }

        try {
            val artists = database.searchArtists("%$query%", 10).first()
            results["artists"] = artists
        } catch (e: Exception) {
            results["artists"] = emptyList()
        }

        try {
            val albums = database.searchAlbums("%$query%", 10).first()
            results["albums"] = albums
        } catch (e: Exception) {
            results["albums"] = emptyList()
        }

        try {
            val playlists = database.searchPlaylists("%$query%", 10).first()
            results["playlists"] = playlists
        } catch (e: Exception) {
            results["playlists"] = emptyList()
        }

        return results
    }

    private suspend fun performYouTubeSearch(query: String): SearchResult? {
        return try {
            YouTube.search(query, YouTube.SearchFilter.SONG)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateLocalResults(results: Map<String, List<LocalItem>>) {
        localSongs.value = results["songs"]?.filterIsInstance<Song>() ?: emptyList()
        localArtists.value = results["artists"]?.filterIsInstance<Artist>() ?: emptyList()
        localAlbums.value = results["albums"]?.filterIsInstance<Album>() ?: emptyList()
        localPlaylists.value = results["playlists"]?.filterIsInstance<Playlist>() ?: emptyList()
    }

    private fun updateYTResults(result: SearchResult?) {
        ytSongs.value = result?.songs?.filterExplicit()?.filterVideoSongs() ?: emptyList()
        ytAlbums.value = result?.albums ?: emptyList()
        ytArtists.value = result?.artists ?: emptyList()
        ytPlaylists.value = result?.playlists ?: emptyList()
    }

    private fun updateCombinedResults() {
        val combined = CombinedSearchResult(
            localItems = (localSongs.value + localArtists.value + localAlbums.value + localPlaylists.value),
            ytItems = (ytSongs.value + ytAlbums.value + ytArtists.value + ytPlaylists.value),
            isLoading = false
        )
        _searchResults.value = combined
    }

    private fun clearResults() {
        localSongs.value = emptyList()
        localArtists.value = emptyList()
        localAlbums.value = emptyList()
        localPlaylists.value = emptyList()
        ytSongs.value = emptyList()
        ytAlbums.value = emptyList()
        ytArtists.value = emptyList()
        ytPlaylists.value = emptyList()
        _searchResults.value = CombinedSearchResult()
    }

    fun addToRecentSearches(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _recentSearches.value.toMutableList()
            current.remove(query) // Remove if exists
            current.add(0, query) // Add to front
            if (current.size > 10) current.removeAt(current.lastIndex)
            _recentSearches.value = current
        }
    }
}

    private fun loadRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[RecentSearchesKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList() }
                .distinctUntilChanged()
                .collect { searches ->
                    _recentSearches.value = searches
                }
        }
    }

    fun setQuery(newQuery: String) {
        query.value = newQuery
        if (newQuery.isNotEmpty()) {
            performSearch(newQuery)
        } else {
            clearResults()
        }
    }

    fun performSearch(searchQuery: String) {
        if (searchQuery.isBlank()) {
            clearResults()
            return
        }

        _isSearching.value = true
        query.value = searchQuery

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Save to recent searches
                saveRecentSearch(searchQuery)

                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                // Perform local database search
                val localResults = combine(
                    database.searchSongs(searchQuery),
                    database.searchAlbums(searchQuery),
                    database.searchArtists(searchQuery),
                    database.searchPlaylists(searchQuery),
                ) { songs, albums, artists, playlists ->
                    val filteredSongs = if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                    filteredSongs + albums + artists + playlists
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

                localSearchResults.value = localResults.value

                // Perform YouTube search if user has internet
                val ytResults = try {
                    YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterExplicit(hideExplicit)?.filterVideoSongs(hideVideoSongs).orEmpty()
                } catch (e: Exception) {
                    emptyList()
                }

                ytSearchResults.value = ytResults

                _searchResults.value = CombinedSearchResult(
                    localItems = localSearchResults.value,
                    ytItems = ytSearchResults.value,
                    isLoading = false
                )
            } catch (e: Exception) {
                _searchResults.value = CombinedSearchResult(
                    localItems = localSearchResults.value,
                    ytItems = ytSearchResults.value,
                    isLoading = false
                )
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun saveRecentSearch(searchQuery: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSearches = context.dataStore.data.first()[RecentSearchesKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val updatedSearches = (listOf(searchQuery) + currentSearches).distinct().take(10)
            context.dataStore.edit { settings ->
                settings[RecentSearchesKey] = updatedSearches.joinToString(",")
            }
        }
    }

    fun clearResults() {
        query.value = ""
        localSearchResults.value = emptyList()
        ytSearchResults.value = emptyList()
        _searchResults.value = CombinedSearchResult()
        _isSearching.value = false
    }

    fun clearRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { settings ->
                settings.remove(RecentSearchesKey)
            }
            _recentSearches.value = emptyList()
        }
    }

    companion object {
        private const val RecentSearchesKey = "recent_searches_tv"
    }
}
