package com.auramusic.app.video

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import com.auramusic.app.utils.AuraPlayerUtils
import com.auramusic.app.utils.VideoThumbnails
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.response.WatchCompactVideo
import com.auramusic.innertube.models.response.WatchMetadataResponse
import com.auramusic.innertube.models.response.channelAvatarUrl
import com.auramusic.innertube.models.response.channelId
import com.auramusic.innertube.models.response.channelName
import com.auramusic.innertube.models.response.commentCountText
import com.auramusic.innertube.models.response.comments
import com.auramusic.innertube.models.response.commentsContinuation
import com.auramusic.innertube.models.response.dateText
import com.auramusic.innertube.models.response.description
import com.auramusic.innertube.models.response.likeCountText
import com.auramusic.innertube.models.response.relatedContinuation
import com.auramusic.innertube.models.response.relatedVideos
import com.auramusic.innertube.models.response.subscriberCountText
import com.auramusic.innertube.models.response.title
import com.auramusic.innertube.models.response.viewCountText
import com.auramusic.innertube.models.response.YoutubeComment
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

object VideoPlaybackManager {

    data class VideoSession(
        val videoId: String,
        val title: String,
        val channelName: String = "",
        val channelId: String? = null,
        val channelThumbnail: String? = null,
        val description: String? = null,
        val viewCountText: String? = null,
        val publishedTimeText: String? = null,
        val subscriberCountText: String? = null,
        val commentCountText: String? = null,
        val likeCountText: String? = null,
    )

    data class RecommendationItem(
        val videoId: String,
        val title: String,
        val channelName: String,
        val channelId: String? = null,
        val thumbnail: String?,
        val durationText: String?,
        val viewCountText: String? = null,
        val publishedTimeText: String? = null,
    )

    data class CommentItem(
        val commentId: String,
        val authorName: String,
        val authorThumbnail: String? = null,
        val content: String,
        val publishedTime: String? = null,
        val likeCount: String? = null,
        val replyCount: String? = null,
        val isPinned: Boolean = false,
    )

    data class UiState(
        val session: VideoSession? = null,
        val minimized: Boolean = false,
        val isPlaying: Boolean = false,
        val isBuffering: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val error: String? = null,
        val recommendations: List<RecommendationItem> = emptyList(),
        val queue: List<RecommendationItem> = emptyList(),
        val recommendationsContinuation: String? = null,
        val isLoadingRecommendations: Boolean = false,
        val isLoadingMoreRecommendations: Boolean = false,
        val isLiked: Boolean = false,
        val isDisliked: Boolean = false,
        val isSubscribed: Boolean = false,
        val isSaved: Boolean = false,
        val expandedDescription: Boolean = false,
        val comments: List<CommentItem> = emptyList(),
        val isLoadingComments: Boolean = false,
        val commentsError: String? = null,
        val commentsContinuation: String? = null,
        val isLoadingMoreComments: Boolean = false,
        val showSettings: Boolean = false,
        val playbackSpeed: Float = 1.0f,
        val resizeMode: Int = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
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
    private var currentContext: Context? = null
    private val playedVideoIds = mutableSetOf<String>()

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
            if (playbackState == Player.STATE_ENDED) {
                playNext()
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
                    recommendations = emptyList(),
                    queue = emptyList(),
                    recommendationsContinuation = null,
                    isLoadingRecommendations = false,
                    isLoadingMoreRecommendations = false,
                    isLiked = false,
                    isDisliked = false,
                    isSubscribed = false,
                    isSaved = false,
                    expandedDescription = false,
                    comments = emptyList(),
                    isLoadingComments = false,
                    commentsError = null,
                    commentsContinuation = null,
                    isLoadingMoreComments = false,
                )
            }
        }
    }

    fun play(context: Context, videoId: String, title: String, channelName: String = "") {
        playWithDetails(context, videoId, title, channelName)
    }

    fun playWithDetails(
        context: Context,
        videoId: String,
        title: String,
        channelName: String,
        channelId: String? = null,
        channelThumbnail: String? = null,
        description: String? = null,
        viewCountText: String? = null,
        publishedTimeText: String? = null,
        thumbnails: List<com.auramusic.innertube.models.Thumbnail> = emptyList(),
    ) {
        val current = _uiState.value
        if (current.session?.videoId == videoId) {
            current.error?.let {
                _uiState.update { s -> s.copy(error = null) }
            }
            _uiState.update { it.copy(minimized = false) }
            player?.play()
            return
        }

        playedVideoIds.add(videoId)
        val bestThumbnail = thumbnails.maxByOrNull { it.width ?: 0 }?.url

        val exo = getOrCreatePlayer(context)
        _uiState.value = UiState(
            session = VideoSession(
                videoId = videoId,
                title = title,
                channelName = channelName,
                channelId = channelId,
                channelThumbnail = channelThumbnail ?: bestThumbnail,
                description = description,
                viewCountText = viewCountText,
                publishedTimeText = publishedTimeText,
            ),
            minimized = false,
            isPlaying = true,
            isBuffering = true,
        )
        scope.launch {
            val source = withContext(Dispatchers.IO) {
                AuraPlayerUtils.getVideoStreamSource(videoId).getOrNull()
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
        scope.launch {
            // Enrich metadata (fills gaps for views/description/channel avatar), then load content.
            enrichSessionMetadata(videoId)
            loadRecommendations(videoId)
            loadComments(videoId)
        }
    }

    /**
     * Fills missing session fields from the WEB watch page. Known metadata from the
     * list item the user tapped wins; only blank fields get overwritten.
     */
    private suspend fun enrichSessionMetadata(videoId: String) {
        if (_uiState.value.session?.videoId != videoId) return
        val metadata = withContext(Dispatchers.IO) {
            YouTube.watchMetadata(videoId).getOrNull()
        } ?: return
        if (_uiState.value.session?.videoId != videoId) return
        _uiState.update { state ->
            val session = state.session?.takeIf { it.videoId == videoId } ?: return@update state
            state.copy(
                session = session.copy(
                    title = session.title.ifBlank { metadata.title().orEmpty() },
                    channelName = session.channelName.ifBlank { metadata.channelName().orEmpty() },
                    channelId = session.channelId ?: metadata.channelId(),
                    channelThumbnail = session.channelThumbnail ?: metadata.channelAvatarUrl(),
                    description = session.description?.takeIf { it.isNotBlank() }
                        ?: metadata.description(),
                    viewCountText = session.viewCountText ?: metadata.viewCountText(),
                    publishedTimeText = session.publishedTimeText ?: metadata.dateText(),
                    subscriberCountText = metadata.subscriberCountText(),
                    commentCountText = metadata.commentCountText(),
                    likeCountText = session.likeCountText ?: metadata.likeCountText(),
                ),
            )
        }
    }

    private suspend fun loadRecommendations(videoId: String) {
        _uiState.update {
            it.copy(isLoadingRecommendations = true, recommendations = emptyList(), queue = emptyList(), recommendationsContinuation = null)
        }

        // Primary source: WEB watch page related videos (works for all YouTube videos).
        val metadata = withContext(Dispatchers.IO) {
            YouTube.watchMetadata(videoId).getOrNull()
        }
        val related = metadata?.relatedVideos().orEmpty()
        if (related.isNotEmpty()) {
            if (_uiState.value.session?.videoId != videoId) return
            val items = related
                .filter { it.videoId.isNotBlank() && it.videoId != videoId }
                .map { it.toRecommendationItem() }
            val queue = items.filter { it.videoId !in playedVideoIds }
            _uiState.update {
                it.copy(
                    recommendations = items,
                    queue = queue,
                    recommendationsContinuation = metadata?.relatedContinuation(),
                    isLoadingRecommendations = false,
                )
            }
            return
        }

        // Fallback 1: YT Music next/related (music content).
        val nextResult = withContext(Dispatchers.IO) {
            YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
        }
        val nextItems = nextResult?.items.orEmpty().map { song ->
            RecommendationItem(
                videoId = song.id,
                title = song.title,
                channelName = song.artists.joinToString { it.name },
                thumbnail = song.thumbnail,
                durationText = song.duration?.let { dur ->
                    val minutes = dur / 60
                    val seconds = dur % 60
                    "$minutes:${seconds.toString().padStart(2, '0')}"
                },
            )
        }
        if (nextItems.isNotEmpty()) {
            if (_uiState.value.session?.videoId != videoId) return
            val queue = nextItems.filter { it.videoId !in playedVideoIds }
            _uiState.update {
                it.copy(recommendations = nextItems, queue = queue, isLoadingRecommendations = false)
            }
            return
        }

        val relatedEndpoint = nextResult?.relatedEndpoint
        if (relatedEndpoint != null) {
            val relatedResult = withContext(Dispatchers.IO) {
                YouTube.related(relatedEndpoint).getOrNull()
            }
            val relatedItems = relatedResult?.songs.orEmpty().map { song ->
                RecommendationItem(
                    videoId = song.id,
                    title = song.title,
                    channelName = song.artists.joinToString { it.name },
                    thumbnail = song.thumbnail,
                    durationText = song.duration?.let { dur ->
                        val minutes = dur / 60
                        val seconds = dur % 60
                        "$minutes:${seconds.toString().padStart(2, '0')}"
                    },
                )
            }
            if (_uiState.value.session?.videoId != videoId) return
            val queue = relatedItems.filter { it.videoId !in playedVideoIds }
            _uiState.update {
                it.copy(recommendations = relatedItems, queue = queue, isLoadingRecommendations = false)
            }
        } else {
            _uiState.update { it.copy(isLoadingRecommendations = false) }
        }
    }

    /** Infinite scroll for the Up next list. */
    fun loadMoreRecommendations() {
        val state = _uiState.value
        val videoId = state.session?.videoId ?: return
        val continuation = state.recommendationsContinuation ?: return
        if (state.isLoadingMoreRecommendations) return
        _uiState.update { it.copy(isLoadingMoreRecommendations = true) }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                YouTube.watchMetadataRelatedContinuation(videoId, continuation).getOrNull()
            }
            if (_uiState.value.session?.videoId != videoId) return@launch
            val (videos, nextContinuation) = result ?: Pair(emptyList(), null)
            val items = videos
                .filter { it.videoId.isNotBlank() && it.videoId != videoId }
                .map { it.toRecommendationItem() }
            _uiState.update { current ->
                val existing = current.recommendations.map { it.videoId }.toSet()
                current.copy(
                    recommendations = current.recommendations + items.filter { it.videoId !in existing },
                    queue = current.queue + items.filter { it.videoId !in existing && it.videoId !in playedVideoIds },
                    recommendationsContinuation = nextContinuation,
                    isLoadingMoreRecommendations = false,
                )
            }
        }
    }

    private suspend fun loadComments(videoId: String) {
        _uiState.update { it.copy(isLoadingComments = true, comments = emptyList(), commentsError = null, commentsContinuation = null) }
        val result = withContext(Dispatchers.IO) {
            // The WEB comment feed is only reachable through the watch page's comments
            // continuation token, so resolve it first.
            val token = YouTube.watchMetadata(videoId).getOrNull()?.commentsContinuation()
            token?.let { YouTube.videoComments(videoId, it).getOrNull() }
        }
        if (_uiState.value.session?.videoId != videoId) return
        if (result == null) {
            _uiState.update { it.copy(isLoadingComments = false, commentsError = "Could not load comments") }
            return
        }
        val comments = result.comments().map { it.toCommentItem() }
        _uiState.update {
            it.copy(
                comments = comments,
                isLoadingComments = false,
                commentsContinuation = result.commentsContinuation(),
                commentsError = if (comments.isEmpty()) "Comments are unavailable for this video" else null,
            )
        }
    }

    /** Infinite scroll for the comments list. */
    fun loadMoreComments() {
        val state = _uiState.value
        val videoId = state.session?.videoId ?: return
        val continuation = state.commentsContinuation ?: return
        if (state.isLoadingMoreComments) return
        _uiState.update { it.copy(isLoadingMoreComments = true) }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                YouTube.videoComments(videoId, continuation).getOrNull()
            }
            if (_uiState.value.session?.videoId != videoId) return@launch
            val newComments = result?.comments().orEmpty().map { it.toCommentItem() }
            _uiState.update { current ->
                val existing = current.comments.map { it.commentId }.toSet()
                current.copy(
                    comments = current.comments + newComments.filter { it.commentId !in existing },
                    commentsContinuation = result?.commentsContinuation(),
                    isLoadingMoreComments = false,
                )
            }
        }
    }

    private fun WatchCompactVideo.toRecommendationItem() = RecommendationItem(
        videoId = videoId,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnail = thumbnailUrl ?: VideoThumbnails.highQuality(videoId),
        durationText = durationText,
        viewCountText = viewCountText,
        publishedTimeText = publishedTimeText,
    )

    private fun YoutubeComment.toCommentItem() = CommentItem(
        commentId = commentId,
        authorName = authorName,
        authorThumbnail = authorThumbnail,
        content = content,
        publishedTime = publishedTime,
        likeCount = likeCount,
        replyCount = replyCount,
        isPinned = isAuthorPinned,
    )

    fun playNext() {
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return
        val nextItem = queue.firstOrNull { it.videoId != _uiState.value.session?.videoId } ?: return
        val ctx = currentContext ?: return
        playWithDetails(
            context = ctx,
            videoId = nextItem.videoId,
            title = nextItem.title,
            channelName = nextItem.channelName,
            channelId = nextItem.channelId,
            channelThumbnail = nextItem.thumbnail,
        )
    }

    fun playPrevious() {
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return
        val currentId = _uiState.value.session?.videoId ?: return
        val currentIndex = queue.indexOfFirst { it.videoId == currentId }
        if (currentIndex > 0) {
            val prevItem = queue[currentIndex - 1]
            val ctx = currentContext ?: return
            playWithDetails(
                context = ctx,
                videoId = prevItem.videoId,
                title = prevItem.title,
                channelName = prevItem.channelName,
                channelId = prevItem.channelId,
                channelThumbnail = prevItem.thumbnail,
            )
        }
    }

    fun toggleLike() {
        val session = _uiState.value.session ?: return
        val newLiked = !_uiState.value.isLiked
        scope.launch {
            YouTube.likeVideo(session.videoId, newLiked)
            _uiState.update {
                it.copy(
                    isLiked = newLiked,
                    isDisliked = if (newLiked) false else it.isDisliked
                )
            }
        }
    }

    fun toggleDislike() {
        val session = _uiState.value.session ?: return
        val newDisliked = !_uiState.value.isDisliked
        _uiState.update {
            it.copy(
                isDisliked = newDisliked,
                isLiked = if (newDisliked) false else it.isLiked
            )
        }
    }

    fun toggleSubscribe() {
        val channelId = _uiState.value.session?.channelId ?: return
        val newSubscribed = !_uiState.value.isSubscribed
        scope.launch {
            YouTube.subscribeChannel(channelId, newSubscribed)
            _uiState.update { it.copy(isSubscribed = newSubscribed) }
        }
    }

    fun toggleSave() {
        val session = _uiState.value.session ?: return
        val newSaved = !_uiState.value.isSaved
        scope.launch {
            if (newSaved) {
                YouTube.addSongToLibrary(session.videoId)
            } else {
                YouTube.removeSongFromLibrary(session.videoId)
            }
            _uiState.update { it.copy(isSaved = newSaved) }
        }
    }

    fun toggleExpandedDescription() {
        _uiState.update { it.copy(expandedDescription = !it.expandedDescription) }
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

    fun setPlaybackSpeed(speed: Float) {
        val exo = player ?: return
        exo.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setResizeMode(mode: Int) {
        _uiState.update { it.copy(resizeMode = mode) }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
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
        playedVideoIds.clear()
        _uiState.value = UiState()
    }

    fun release() {
        player?.removeListener(playerListener)
        player?.release()
        player = null
        tickerJob?.cancel()
        tickerJob = null
        playedVideoIds.clear()
    }

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        currentContext = context
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

    @OptIn(UnstableApi::class)
    private fun buildMediaSource(
        videoId: String,
        source: com.auramusic.auravideo.AuraVideo.VideoStreamSource,
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
            is com.auramusic.auravideo.AuraVideo.VideoStreamSource.Single -> {
                val mediaItem = MediaItem.Builder()
                    .setUri(source.url)
                    .setMimeType(source.mimeType)
                    .setMediaId(videoId)
                    .build()
                factory.createMediaSource(mediaItem)
            }
            is com.auramusic.auravideo.AuraVideo.VideoStreamSource.Merged -> {
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
