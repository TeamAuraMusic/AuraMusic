package com.auramusic.app.video

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import com.auramusic.app.utils.FlowPlayerUtils
import com.auramusic.flow.FlowVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the standalone video playback session for the Videos experience.
 *
 * The player lives at the app shell level (not in a route) so that the same
 * ExoPlayer surface can be shown full-screen and float as a minimized tile
 * while the user keeps browsing.
 */
object VideoPlaybackManager {

    data class VideoSession(
        val videoId: String,
        val title: String,
        val channelName: String = "",
    )

    data class UiState(
        val session: VideoSession? = null,
        val minimized: Boolean = false,
        val isPlaying: Boolean = false,
        val isBuffering: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val error: String? = null,
    ) {
        val isEmpty: Boolean get() = session == null
        val progress: Float
            get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null
    private var tickerJob: Job? = null

    /** The active player instance, if any. UI attaches PlayerView surfaces to it. */
    fun playerOrNull(): ExoPlayer? = player

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update {
                it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
            }
            if (playbackState == Player.STATE_READY) {
                val exo = player ?: return
                _uiState.update {
                    it.copy(
                        durationMs = exo.duration.takeIf { d -> d > 0 } ?: 0,
                        isBuffering = false,
                        error = null,
                    )
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update { it.copy(isBuffering = false, error = error.message ?: "Playback error") }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val exo = player ?: return
            _uiState.update {
                it.copy(
                    positionMs = 0,
                    durationMs = exo.duration.takeIf { d -> d > 0 } ?: 0,
                    isBuffering = true,
                    error = null,
                )
            }
        }
    }

    /** Start (or resume) a video. Same videoId just expands/resumes. */
    fun play(context: Context, videoId: String, title: String, channelName: String = "") {
        val current = _uiState.value
        if (current.session?.videoId == videoId) {
            current.error?.let {
                _uiState.update { s -> s.copy(error = null) }
            }
            _uiState.update { it.copy(minimized = false) }
            player?.play()
            return
        }

        val exo = getOrCreatePlayer(context)
        _uiState.value = UiState(
            session = VideoSession(videoId, title, channelName),
            minimized = false,
            isPlaying = true,
            isBuffering = true,
        )
        scope.launch {
            val source = withContext(Dispatchers.IO) {
                FlowPlayerUtils.getVideoStreamSource(videoId).getOrNull()
            }
            if (source == null) {
                _uiState.update { it.copy(isBuffering = false, error = "Could not load video") }
                return@launch
            }
            val mediaSource = buildMediaSource(videoId, source)
            exo.setMediaSource(mediaSource)
            exo.prepare()
            exo.play()
        }
    }

    fun togglePlayPause() {
        val exo = player ?: return
        if (_uiState.value.isPlaying) exo.pause() else exo.play()
    }

    fun seekTo(positionMs: Long) {
        val exo = player ?: return
        _uiState.update { it.copy(positionMs = positionMs) }
        exo.seekTo(positionMs)
    }

    fun collapse() {
        _uiState.update { it.copy(minimized = true) }
    }

    fun expand() {
        _uiState.update { it.copy(minimized = false) }
    }

    fun toggleMinimized() {
        _uiState.update { it.copy(minimized = !it.minimized) }
    }

    fun close() {
        player?.let { exo ->
            exo.removeListener(playerListener)
            exo.stop()
            exo.release()
        }
        player = null
        tickerJob?.cancel()
        tickerJob = null
        _uiState.value = UiState()
    }

    /** Release any active session without touching state observers. */
    fun release() {
        player?.removeListener(playerListener)
        player?.release()
        player = null
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        player?.let { return it }
        return ExoPlayer.Builder(context).build().also {
            it.addListener(playerListener)
            it.playWhenReady = true
            player = it
            startTicker()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val exo = player
                if (exo != null && _uiState.value.session != null) {
                    val pos = exo.currentPosition
                    val dur = if (exo.duration > 0) exo.duration else 0
                    _uiState.update { it.copy(positionMs = pos, durationMs = dur) }
                }
                delay(250)
            }
        }
    }

    private fun buildMediaSource(
        videoId: String,
        source: FlowVideo.VideoStreamSource,
    ): androidx.media3.exoplayer.source.MediaSource {
        val factory = ProgressiveMediaSource.Factory(
            DefaultHttpDataSource.Factory(),
            ExtractorsFactory {
                arrayOf(
                    MatroskaExtractor(),
                    FragmentedMp4Extractor(),
                    Mp4Extractor()
                )
            }
        )
        return when (source) {
            is FlowVideo.VideoStreamSource.Single -> {
                val mediaItem = MediaItem.Builder()
                    .setUri(source.url)
                    .setMimeType(source.mimeType)
                    .setMediaId(videoId)
                    .build()
                factory.createMediaSource(mediaItem)
            }
            is FlowVideo.VideoStreamSource.Merged -> {
                val videoMediaItem = MediaItem.Builder()
                    .setUri(source.videoUrl)
                    .setMimeType(source.videoMimeType)
                    .setMediaId(videoId + "_v")
                    .build()
                val videoSource = factory.createMediaSource(videoMediaItem)
                val audioMediaItem = MediaItem.Builder()
                    .setUri(source.audioUrl)
                    .setMimeType(source.audioMimeType)
                    .setMediaId(videoId + "_a")
                    .build()
                val audioSource = factory.createMediaSource(audioMediaItem)
                MergingMediaSource(true, true, videoSource, audioSource)
            }
        }
    }
}