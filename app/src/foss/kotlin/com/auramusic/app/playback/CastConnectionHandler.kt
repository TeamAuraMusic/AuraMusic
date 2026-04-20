package com.auramusic.app.playback

import android.content.Context
import com.auramusic.app.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CastConnectionHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val listener: Any? = null
) {
    val isCasting: StateFlow<Boolean> = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = MutableStateFlow(false)
    val castIsPlaying: StateFlow<Boolean> = MutableStateFlow(false)
    val castPosition: StateFlow<Long> = MutableStateFlow(0L)
    val castDuration: StateFlow<Long> = MutableStateFlow(0L)
    val castVolume: StateFlow<Float> = MutableStateFlow(1f)
    val castDeviceName: StateFlow<String?> = MutableStateFlow(null)
    var isSyncingFromCast: Boolean = false
        private set

    fun initialize() {}

    fun play() {}

    fun pause() {}

    fun seekTo(position: Long) {}

    fun skipToNext() {}

    fun skipToPrevious() {}

    fun setVolume(volume: Float) {}

    fun release() {}

    fun closeRPC() {}

    fun navigateToMediaIfInQueue(mediaId: String): Boolean = false

    fun loadMedia(metadata: MediaMetadata) {}

    fun disconnect() {}
}