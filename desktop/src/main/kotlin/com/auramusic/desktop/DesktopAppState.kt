package com.auramusic.desktop

import com.auramusic.desktop.mpv.MpvPlayer
import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI state for the desktop spike. Kept plain so it can later move to :core. */
class DesktopAppState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val player = MpvPlayer()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun onQueryChange(value: String) {
        _query.value = value
    }

    private val _results = MutableStateFlow<List<SongItem>>(emptyList())
    val results: StateFlow<List<SongItem>> = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _resolving = MutableStateFlow(false)
    val resolving: StateFlow<Boolean> = _resolving.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun search() {
        val term = _query.value.trim()
        if (term.isEmpty()) return
        _searching.value = true
        _notice.value = null
        scope.launch {
            val response = withContext(Dispatchers.IO) {
                YouTube.search(term, YouTube.SearchFilter.FILTER_SONG)
            }
            response.onSuccess { result ->
                _results.value = result.items.filterIsInstance<SongItem>().take(50)
                if (_results.value.isEmpty()) _notice.value = "No songs found for “$term”"
            }.onFailure {
                _notice.value = "Search failed: ${it.message}"
            }
            _searching.value = false
        }
    }

    fun play(song: SongItem) {
        _resolving.value = true
        _notice.value = null
        scope.launch {
            val url = withContext(Dispatchers.IO) {
                runCatching { NewPipeExtractor.getStreamInfo(song.id) }
                    .getOrNull()
                    ?.audioStreams
                    ?.maxByOrNull { it.bitrate }
                    ?.content
            }
            _resolving.value = false
            if (url.isNullOrBlank()) {
                _notice.value = "Could not resolve an audio stream for “${song.title}”"
            } else {
                player.play(url, song.title)
            }
        }
    }

    fun close() {
        player.close()
        scope.cancel()
    }
}
