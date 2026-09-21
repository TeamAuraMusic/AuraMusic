package com.auramusic.app.video

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.datastore.preferences.core.edit
import com.auramusic.app.R
import com.auramusic.app.constants.SavedVideoIdsKey
import com.auramusic.app.constants.VideoAutoplayEnabledKey
import com.auramusic.app.constants.VideoHistoryKey
import com.auramusic.app.constants.VideoQuality
import com.auramusic.app.constants.VideoQualityKey
import com.auramusic.app.constants.VideoSubscribedChannelsKey
import com.auramusic.app.utils.AuraPlayerUtils
import com.auramusic.app.utils.VideoThumbnails
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.app.playback.MusicService
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.YouTubeVideoItem
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
import com.auramusic.innertube.models.response.likeState
import com.auramusic.innertube.models.response.relatedContinuation
import com.auramusic.innertube.models.response.relatedVideos
import com.auramusic.innertube.models.response.subscriberCountText
import com.auramusic.innertube.models.response.title
import com.auramusic.innertube.models.response.viewCountText
import com.auramusic.innertube.models.response.WatchLikeState
import com.auramusic.innertube.models.response.YoutubeComment
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber
import com.google.common.util.concurrent.MoreExecutors
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object VideoPlaybackManager {

    data class VideoSession(
        val videoId: String,
        val title: String,
        val channelName: String = "",
        val channelId: String? = null,
        val channelThumbnail: String? = null,
        val channelAvatarUrl: String? = null,
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

    /** One watched-video entry shown in the Library "Recently watched" row. */
    data class VideoHistoryEntry(
        val videoId: String,
        val title: String,
        val channelName: String,
        val channelId: String?,
        val thumbnailUrl: String?,
        val lastPlayedAt: Long,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
    )

    /** A channel the user subscribed to from a video/channel screen. */
    data class VideoSubscribedChannel(
        val channelId: String,
        val name: String,
        val avatarUrl: String?,
        val subscribedAt: Long,
    )

    data class UiState(
        val session: VideoSession? = null,
        val minimized: Boolean = false,
        val hiddenByMusic: Boolean = false,
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
        val videoQuality: VideoQuality = VideoQuality.QUALITY_720P,
        val autoplayEnabled: Boolean = true,
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

    /**
     * The last successful WEB watch-page response, keyed by video id. Shipped to
     * enrichSessionMetadata / loadRecommendations / loadComments so the three
     * loaders share ONE /next request instead of firing three near-identical calls,
     * which on flaky networks is how comments or recommendations silently end up
     * blank. Cleared whenever a new video starts.
     */
    private var watchMetadataCache: WatchMetadataResponse? = null
    private var watchMetadataCacheVideoId: String? = null
    private val watchMetadataMutex = Mutex()
    private var currentContext: Context? = null
    private var videoControllerFuture: ListenableFuture<MediaController>? = null
    private val playedVideoIds = mutableSetOf<String>()

    private var currentQuality: VideoQuality = VideoQuality.QUALITY_720P
    private var currentAutoplay: Boolean = true

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
                if (_uiState.value.autoplayEnabled) playNext()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update { it.copy(isBuffering = false, error = error.message ?: "Playback error") }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val exo = player ?: return
            // playWithDetails already resets the per-video state before loading, so a
            // transition for the current session's own video (fired when exo.prepare()
            // lands) must NOT wipe recommendations/comments — the async loaders may have
            // just filled them in, and erasing them here made the Up Next list and
            // comments vanish (or never appear) for fast-loading videos.
            if (mediaItem?.mediaId == _uiState.value.session?.videoId) return
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
            _uiState.update { it.copy(minimized = false, hiddenByMusic = false) }
            player?.play()
            return
        }

        playedVideoIds.add(videoId)
        // A new video invalidates the previous one's cached watch page.
        watchMetadataCache = null
        watchMetadataCacheVideoId = null
        val bestThumbnail = thumbnails.maxByOrNull { it.width ?: 0 }?.url

        val exo = getOrCreatePlayer(context)
        // The video mini player takes over from the music player: tell the music
        // service to pause and drop its miniplayer notification so the video
        // notification (artwork + controls) becomes the one shown in the shade.
        // MusicService is usually still bound by the activity, so stopping it won't
        // tear it down - an explicit action removes the notification instead.
        if (MusicService.isRunning) {
            try {
                context.applicationContext.startService(
                    Intent(context.applicationContext, MusicService::class.java)
                        .setAction(MusicService.ACTION_PAUSE_FOR_VIDEO)
                )
            } catch (e: Exception) {
                // Ignore: music service may not be started.
            }
        }
        VideoPlaybackService.start(context.applicationContext)
        connectServiceController(context.applicationContext)
        applyStoredPreferences(context.applicationContext)
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
            videoQuality = currentQuality,
            autoplayEnabled = currentAutoplay,
        )
        // Rebuild the media notification immediately with the NEW video's metadata.
        // This closes the window where the shade would otherwise keep showing the
        // previous video (or a placeholder) until the stream resolves and
        // loadMediaSourceInto triggers its own rebuild — the cause of the media
        // notification feeling non-persistent / out of sync when switching videos.
        context.applicationContext.let { VideoPlaybackService.notifySessionChanged(it) }
        scope.launch {
            val source = withContext(Dispatchers.IO) {
                AuraPlayerUtils.getVideoStreamSource(videoId).getOrNull()
            }
            if (source == null) {
                _uiState.update { it.copy(isBuffering = false, error = "Could not load video") }
                return@launch
            }
            loadMediaSourceInto(videoId, source, title, channelName, channelThumbnail ?: bestThumbnail)
        }
        scope.launch {
            // Enrich metadata (fills gaps for views/description/channel avatar), then load content.
            enrichSessionMetadata(videoId)
            loadRecommendations(videoId)
            loadComments(videoId)
        }
    }

    /**
     * Fetches the WEB watch page for a video exactly once and caches it, so the
     * enrichment, recommendations and comments loaders all read the SAME response
     * (one /next round-trip per video instead of up to three).
     */
    private suspend fun fetchWatchMetadata(videoId: String): WatchMetadataResponse? {
        return watchMetadataMutex.withLock {
            val cached = watchMetadataCache.takeIf { watchMetadataCacheVideoId == videoId }
            if (cached != null) return@withLock cached
            val metadata = withContext(Dispatchers.IO) {
                YouTube.watchMetadata(videoId).getOrNull()
            }
            // Do not cache failures. A transient /next failure would otherwise
            // permanently disable comments and channel enrichment for this session.
            if (metadata != null) {
                watchMetadataCache = metadata
                watchMetadataCacheVideoId = videoId
            }
            metadata
        }
    }

    /**
     * Fills missing session fields from the WEB watch page. Known metadata from the
     * list item the user tapped wins; only blank fields get overwritten. Also seeds
     * the real channel avatar (separate from the video artwork), the current
     * like/dislike state from the watch button icon, and the locally-saved state.
     */
    private suspend fun enrichSessionMetadata(videoId: String) {
        if (_uiState.value.session?.videoId != videoId) return
        val metadata = fetchWatchMetadata(videoId)
        val savedIds = withContext(Dispatchers.IO) {
            currentContext?.let { it.dataStore[SavedVideoIdsKey] }.orEmpty()
        }
        if (_uiState.value.session?.videoId != videoId) return
        val likeState = metadata?.likeState() ?: WatchLikeState.NONE
        _uiState.update { state ->
            val session = state.session?.takeIf { it.videoId == videoId } ?: return@update state
            state.copy(
                session = session.copy(
                    title = session.title.ifBlank { metadata?.title().orEmpty() },
                    channelName = session.channelName.ifBlank { metadata?.channelName().orEmpty() },
                    channelId = session.channelId ?: metadata?.channelId(),
                    // The real channel avatar; session.channelThumbnail stays as the
                    // video artwork used by the media notification.
                    channelAvatarUrl = session.channelAvatarUrl ?: metadata?.channelAvatarUrl(),
                    description = session.description?.takeIf { it.isNotBlank() }
                        ?: metadata?.description(),
                    viewCountText = session.viewCountText ?: metadata?.viewCountText(),
                    publishedTimeText = session.publishedTimeText ?: metadata?.dateText(),
                    subscriberCountText = metadata?.subscriberCountText(),
                    commentCountText = metadata?.commentCountText(),
                    likeCountText = session.likeCountText ?: metadata?.likeCountText(),
                ),
                isLiked = likeState == WatchLikeState.LIKE,
                isDisliked = likeState == WatchLikeState.DISLIKE,
                isSaved = videoId in savedIds,
            )
        }
        // Notify the service that session metadata has been enriched (artwork URL,
        // channel name etc.) so the media notification is rebuilt with the updated
        // title, artist, and artwork — the same metadata that the MediaControlsPlayer
        // surfaces via getMediaMetadata().
        currentContext?.let { VideoPlaybackService.notifySessionChanged(it) }
        // Record the enriched session into the local watch history (dedupes by id
        // and bumps it to the top so the Library mirrors YouTube's ordering).
        recordCurrentToHistory()
    }

    private suspend fun loadRecommendations(videoId: String) {
        _uiState.update {
            it.copy(isLoadingRecommendations = true, recommendations = emptyList(), queue = emptyList(), recommendationsContinuation = null)
        }

        // Primary source: WEB watch page related videos (works for all YouTube videos).
        val metadata = fetchWatchMetadata(videoId)
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
            // continuation token, so resolve it first (reusing the cached watch page
            // fetched during metadata enrichment where possible).
            val token = fetchWatchMetadata(videoId)?.commentsContinuation()
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

    /** Re-runs the comments fetch for the current video (drives the Retry button). */
    fun retryLoadingComments() {
        val videoId = _uiState.value.session?.videoId ?: return
        scope.launch { loadComments(videoId) }
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

    private fun YouTubeVideoItem.toRecommendationItem() = RecommendationItem(
        videoId = videoId,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnail = thumbnails.maxByOrNull { it.width ?: 0 }?.url ?: VideoThumbnails.highQuality(videoId),
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
        val session = _uiState.value.session ?: return
        val channelId = session.channelId ?: return
        val newSubscribed = !_uiState.value.isSubscribed
        scope.launch {
            persistChannelSubscription(
                channelId = channelId,
                name = session.channelName,
                avatarUrl = session.channelAvatarUrl ?: session.channelThumbnail,
                subscribe = newSubscribed,
            )
            YouTube.subscribeChannel(channelId, newSubscribed)
            _uiState.update { it.copy(isSubscribed = newSubscribed) }
        }
    }

    fun toggleSave() {
        val session = _uiState.value.session ?: return
        val newSaved = !_uiState.value.isSaved
        val ctx = currentContext ?: return
        scope.launch {
            // "Save" for a video is a local watch-later bookmark. The YouTube Music
            // library add/remove endpoints only apply to songs, not arbitrary YouTube
            // videos, so calling them here would mutate the music library (or fail).
            // Persisting to DataStore keeps the toggle instant and consistent.
            ctx.dataStore.edit { settings ->
                val saved = settings[SavedVideoIdsKey]?.toMutableSet() ?: mutableSetOf()
                if (newSaved) saved.add(session.videoId) else saved.remove(session.videoId)
                settings[SavedVideoIdsKey] = saved
            }
            _uiState.update { it.copy(isSaved = newSaved) }
        }
    }

    fun toggleExpandedDescription() {
        _uiState.update { it.copy(expandedDescription = !it.expandedDescription) }
    }

    fun togglePlayPause() {
        val exo = player ?: return
        if (_uiState.value.isPlaying) {
            exo.pause()
        } else {
            // Resuming after the video yielded the shade to the music player
            // (giveWayToMusic): re-establish the video's foreground media
            // notification, re-pause the music player, and reconnect the
            // controller so the video notification is the one shown again.
            currentContext?.applicationContext?.let { ctx ->
                if (MusicService.isRunning) {
                    try {
                        ctx.startService(
                            Intent(ctx, MusicService::class.java)
                                .setAction(MusicService.ACTION_PAUSE_FOR_VIDEO)
                        )
                    } catch (_: Exception) { /* music service may not be running */ }
                }
                VideoPlaybackService.start(ctx)
                connectServiceController(ctx)
                VideoPlaybackService.notifySessionChanged(ctx)
            }
            exo.play()
        }
    }

    /**
     * Called when the music player actually starts playing. The video player (and
     * miniplayer) gives way to the music player: video audio is paused, the tile
     * collapses, the video's media notification is removed from the shade, and the
     * music service's notification-suppression guard is released so the music
     * notification can be posted again.
     */
    fun giveWayToMusic(context: Context) {
        if (_uiState.value.session == null) {
            // No active video to demote. (The takeover guard, if any, is released on
            // close(); there's nothing to clean up here.)
            return
        }
        try {
            if (player?.isPlaying == true) player?.pause()
        } catch (e: Exception) {
            Timber.tag("VideoPlaybackManager").w(e, "giveWayToMusic: pause failed")
        }
        _uiState.update { it.copy(isPlaying = false, minimized = true, hiddenByMusic = true) }
        val ctx = context.applicationContext
        // Remove the video notification and drop the connected controller (mirroring
        // close()) so a later resume reconnects and brings the notification back.
        VideoPlaybackService.stop(ctx)
        releaseServiceController()
        // Lift the music service's takeover guard so it can post its notification.
        if (MusicService.isRunning) MusicService.resumeFromVideo(ctx)
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

    /**
     * Switches the rendering quality of the currently playing video. Because the
     * stream URL depends on the quality selection, the current video is re-resolved
     * and reloaded (keeping the same position). The choice is persisted so future
     * videos start at it too.
     */
    fun setVideoQuality(quality: VideoQuality) {
        val session = _uiState.value.session ?: return
        val exo = player ?: return
        currentQuality = quality
        AuraPlayerUtils.setPreferredVideoQuality(quality)
        val positionMs = exo.currentPosition
        _uiState.update { it.copy(videoQuality = quality) }
        currentContext?.let { ctx ->
            scope.launch {
                ctx.dataStore.edit { it[VideoQualityKey] = quality.name }
            }
            scope.launch {
                val source = withContext(Dispatchers.IO) {
                    AuraPlayerUtils.getVideoStreamSource(session.videoId).getOrNull()
                }
                if (source == null) {
                    // Keep old stream playing if the new quality can't be resolved.
                    return@launch
                }
                loadMediaSourceInto(
                    videoId = session.videoId,
                    source = source,
                    title = session.title,
                    channelName = session.channelName,
                    channelThumbnail = session.channelThumbnail,
                    startPositionMs = positionMs.coerceAtLeast(0L),
                )
            }
        }
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        currentAutoplay = enabled
        _uiState.update { it.copy(autoplayEnabled = enabled) }
        currentContext?.let { ctx ->
            scope.launch {
                ctx.dataStore.edit { it[VideoAutoplayEnabledKey] = enabled }
            }
        }
    }

    /** Removes a recommendation from both the Up-next list and the playback queue. */
    fun removeFromQueue(videoId: String) {
        _uiState.update {
            it.copy(
                recommendations = it.recommendations.filterNot { r -> r.videoId == videoId },
                queue = it.queue.filterNot { q -> q.videoId == videoId },
            )
        }
    }

    /** Clears the pending playback queue (Up next stays visible). */
    fun clearQueue() {
        _uiState.update { it.copy(queue = emptyList()) }
    }

    fun shareVideo(context: Context) {
        val session = _uiState.value.session ?: return
        shareVideo(context, session.videoId, session.title)
    }

    fun copyVideoLink(context: Context) {
        val session = _uiState.value.session ?: return
        copyVideoLink(context, session.videoId)
    }

    /** Adds the current video to the given playlist id. */
    fun addToPlaylist(playlistId: String) {
        val session = _uiState.value.session ?: return
        scope.launch {
            YouTube.addToPlaylist(playlistId, session.videoId)
            currentContext?.let { ctx ->
                Toast.makeText(ctx, R.string.video_player_added_to_playlist, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Queues a feed/list video (not necessarily the current one) for playback as
     * the next item. The item is prepended to the Up Next list and the queue.
     */
    fun queueNext(video: YouTubeVideoItem) {
        _uiState.update { state ->
            val existing = state.recommendations.any { it.videoId == video.videoId }
            val currentId = state.session?.videoId
            val item = video.toRecommendationItem()
            state.copy(
                recommendations = if (existing) state.recommendations else listOf(item) + state.recommendations,
                queue = listOf(item) + state.queue.filterNot { it.videoId == currentId },
            )
        }
    }

    /** Appends a feed/list video to the end of the playback queue. */
    fun addToQueue(video: YouTubeVideoItem) {
        _uiState.update { state ->
            if (state.queue.any { it.videoId == video.videoId }) return@update state
            state.copy(
                recommendations = if (state.recommendations.any { it.videoId == video.videoId }) state.recommendations
                else state.recommendations + video.toRecommendationItem(),
                queue = state.queue + video.toRecommendationItem(),
            )
        }
    }

    fun shareVideo(context: Context, videoId: String, title: String) {
        val url = "https://youtu.be/$videoId"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun copyVideoLink(context: Context, videoId: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("video_link", "https://youtu.be/$videoId"))
        Toast.makeText(context, R.string.video_player_copy_link, Toast.LENGTH_SHORT).show()
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun collapse() {
        _uiState.update { it.copy(minimized = true) }
    }

    fun expand() {
        _uiState.update { it.copy(minimized = false, hiddenByMusic = false) }
    }

    fun toggleMinimized() {
        _uiState.update { it.copy(minimized = !it.minimized) }
    }

    fun close() {
        // Persist the final watch position before tearing the session down so the
        // Library's "Recently watched" history reflects exactly where it stopped.
        recordCurrentToHistory()
        player?.let { exo ->
            exo.removeListener(playerListener)
            exo.stop()
            exo.release()
        }
        player = null
        tickerJob?.cancel()
        tickerJob = null
        playedVideoIds.clear()
        releaseServiceController()
        currentContext?.applicationContext?.let { ctx ->
            VideoPlaybackService.stop(ctx)
            // Lift the music notification transparency guard so the music player's
            // notification can return once music resumes.
            if (MusicService.isRunning) MusicService.resumeFromVideo(ctx)
        }
        _uiState.value = UiState()
    }

    fun release() {
        player?.removeListener(playerListener)
        player?.release()
        player = null
        tickerJob?.cancel()
        tickerJob = null
        playedVideoIds.clear()
        releaseServiceController()
    }

    /**
     * Keep a MediaController connected to the video MediaSession from app scope.
     * The connected controller is what makes the session "active", which is what
     * causes Media3 to render (and keep updated) the media notification with the
     * artwork + transport controls, mirroring how the music player's notification
     * is driven by its own connected controller.
     */
    private fun connectServiceController(context: Context) {
        if (videoControllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, VideoPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        videoControllerFuture = future
        future.addListener(
            {
                try {
                    future.get()
                } catch (e: Exception) {
                    Timber.tag("VideoPlaybackManager").w(e, "Failed to connect video service controller")
                    if (videoControllerFuture === future) videoControllerFuture = null
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun releaseServiceController() {
        videoControllerFuture?.let { future ->
            try {
                MediaController.releaseFuture(future)
            } catch (e: Exception) {
                Timber.tag("VideoPlaybackManager").w(e, "Failed to release video service controller")
            }
        }
        videoControllerFuture = null
    }

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        currentContext = context
        player?.let { return it }
        // Apply the quality + autoplay preference chosen in Settings/settings-overlay
        // so freshly created players start with them.
        applyStoredPreferences(context)
        return ExoPlayer.Builder(context).build().also {
            it.addListener(playerListener)
            it.playWhenReady = true
            player = it
            startTicker()
        }
    }

    /** Reads videoQuality/autoplay from DataStore and pushes them to AuraVideo + state. */
    private fun applyStoredPreferences(context: Context) {
        val storedQuality = context.dataStore.get(VideoQualityKey, "QUALITY_720P")
        currentQuality = runCatching { VideoQuality.valueOf(storedQuality) }.getOrDefault(VideoQuality.QUALITY_720P)
        currentAutoplay = context.dataStore.get(VideoAutoplayEnabledKey, true)
        AuraPlayerUtils.setPreferredVideoQuality(currentQuality)
        _uiState.update { it.copy(videoQuality = currentQuality, autoplayEnabled = currentAutoplay) }
    }

    /**
     * Builds the [MergingMediaSource-like] source for [source], sets it on the
     * player, prepares, and replays — also rebuilding the media notification.
     * Used both for initial load and when the stream must be re-resolved
     * (e.g. quality change mid-playback).
     */
    private fun loadMediaSourceInto(
        videoId: String,
        source: com.auramusic.auravideo.AuraVideo.VideoStreamSource,
        title: String,
        channelName: String,
        channelThumbnail: String?,
        startPositionMs: Long = 0L,
    ) {
        val exo = player ?: return
        val mediaSource = buildMediaSource(
            videoId,
            source,
            title = title,
            channelName = channelName,
            channelThumbnail = channelThumbnail,
        )
        val wasPlaying = exo.playWhenReady || _uiState.value.isPlaying
        exo.setMediaSource(mediaSource)
        exo.prepare()
        if (startPositionMs > 0) exo.seekTo(startPositionMs)
        if (wasPlaying) exo.play()
        // Force the video service notification to rebuild immediately with
        // the media metadata (title, artist, artwork) so the MediaStyle
        // notification shows full artwork + transport controls instead of
        // the initial text-only placeholder.
        currentContext?.let { VideoPlaybackService.notifySessionChanged(it) }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var ticks = 0
            while (isActive) {
                val exo = player
                if (exo != null && _uiState.value.session != null) {
                    val pos = exo.currentPosition
                    val dur = if (exo.duration > 0) exo.duration else 0
                    _uiState.update { it.copy(positionMs = pos, durationMs = dur) }
                    // Persist watch position to the video history every ~10s while
                    // actually playing (ticker fires every 250ms).
                    if (_uiState.value.isPlaying && ++ticks % 40 == 0) {
                        recordCurrentToHistory()
                    }
                }
                delay(250)
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Video history + channel subscriptions (persisted in DataStore)
    // ---------------------------------------------------------------------------
    // JSON layout for history: [{"id","t","c","cid","th","ts","pos","dur"}]
    // JSON layout for subscriptions: [{"cid","n","a","ts"}]

    suspend fun getVideoHistory(): List<VideoHistoryEntry> =
        currentContext?.let { readVideoHistory(it) }.orEmpty()

    suspend fun readVideoHistory(context: Context): List<VideoHistoryEntry> =
        withContext(Dispatchers.IO) {
            try {
                parseVideoHistory(context.dataStore[VideoHistoryKey])
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun getSubscribedChannels(): List<VideoSubscribedChannel> =
        currentContext?.let { readSubscribedChannels(it) }.orEmpty()

    suspend fun readSubscribedChannels(context: Context): List<VideoSubscribedChannel> =
        withContext(Dispatchers.IO) {
            try {
                parseSubscribedChannels(context.dataStore[VideoSubscribedChannelsKey])
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun recordCurrentToHistory() {
        val session = _uiState.value.session ?: return
        recordHistoryEntry(
            VideoHistoryEntry(
                videoId = session.videoId,
                title = session.title,
                channelName = session.channelName,
                channelId = session.channelId,
                thumbnailUrl = session.channelThumbnail,
                lastPlayedAt = System.currentTimeMillis(),
                positionMs = _uiState.value.positionMs,
                durationMs = _uiState.value.durationMs,
            )
        )
    }

    private fun recordHistoryEntry(entry: VideoHistoryEntry) {
        val ctx = currentContext ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val current = parseVideoHistory(ctx.dataStore[VideoHistoryKey]).toMutableList()
                    current.removeAll { it.videoId == entry.videoId }
                    current.add(0, entry)
                    ctx.dataStore.edit {
                        it[VideoHistoryKey] = buildVideoHistoryJson(current.take(MAX_VIDEO_HISTORY_ENTRIES))
                    }
                } catch (e: Exception) {
                    // History is best-effort; never let persistence break playback.
                }
            }
        }
    }

    /** Persists a channel subscription change so the Library can list channels. */
    suspend fun persistChannelSubscription(
        channelId: String,
        name: String,
        avatarUrl: String?,
        subscribe: Boolean,
    ) {
        currentContext?.let {
            persistChannelSubscription(it, channelId, name, avatarUrl, subscribe)
        }
    }

    suspend fun persistChannelSubscription(
        context: Context,
        channelId: String,
        name: String,
        avatarUrl: String?,
        subscribe: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val current = parseSubscribedChannels(context.dataStore[VideoSubscribedChannelsKey]).toMutableList()
                current.removeAll { it.channelId == channelId }
                if (subscribe) {
                    current.add(0, VideoSubscribedChannel(channelId, name, avatarUrl, System.currentTimeMillis()))
                }
                context.dataStore.edit {
                    it[VideoSubscribedChannelsKey] = buildSubscribedChannelsJson(current)
                }
            } catch (e: Exception) {
                // Best-effort persistence.
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildMediaSource(
        videoId: String,
        source: com.auramusic.auravideo.AuraVideo.VideoStreamSource,
        title: String,
        channelName: String,
        channelThumbnail: String?,
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
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title.ifBlank { videoId })
            .setArtist(channelName.ifBlank { null })
            .apply {
                channelThumbnail?.let { setArtworkUri(android.net.Uri.parse(it)) }
            }
            .build()
        return when (source) {
            is com.auramusic.auravideo.AuraVideo.VideoStreamSource.Single -> {
                val mediaItem = MediaItem.Builder()
                    .setUri(source.url)
                    .setMimeType(source.mimeType)
                    .setMediaId(videoId)
                    .setMediaMetadata(mediaMetadata)
                    .build()
                factory.createMediaSource(mediaItem)
            }
            is com.auramusic.auravideo.AuraVideo.VideoStreamSource.Merged -> {
                val videoMediaItem = MediaItem.Builder()
                    .setUri(source.videoUrl)
                    .setMimeType(source.videoMimeType)
                    .setMediaId(videoId + "_v")
                    .setMediaMetadata(mediaMetadata)
                    .build()
                val videoSource = factory.createMediaSource(videoMediaItem)
                val audioMediaItem = MediaItem.Builder()
                    .setUri(source.audioUrl)
                    .setMimeType(source.audioMimeType)
                    .setMediaId(videoId + "_a")
                    .setMediaMetadata(mediaMetadata)
                    .build()
                val audioSource = factory.createMediaSource(audioMediaItem)
                MergingMediaSource(true, true, videoSource, audioSource)
            }
        }
    }
}

private const val MAX_VIDEO_HISTORY_ENTRIES = 100

private fun parseVideoHistory(json: String?): List<VideoPlaybackManager.VideoHistoryEntry> {
    if (json.isNullOrBlank()) return emptyList()
    val result = mutableListOf<VideoPlaybackManager.VideoHistoryEntry>()
    try {
        val array = org.json.JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                VideoPlaybackManager.VideoHistoryEntry(
                    videoId = obj.optString("id"),
                    title = obj.optString("t"),
                    channelName = obj.optString("c"),
                    channelId = obj.optString("cid").takeIf { it.isNotEmpty() },
                    thumbnailUrl = obj.optString("th").takeIf { it.isNotEmpty() },
                    lastPlayedAt = obj.optLong("ts"),
                    positionMs = obj.optLong("pos"),
                    durationMs = obj.optLong("dur"),
                )
            )
        }
    } catch (_: Exception) {
        // Corrupt history is ignored and simply overwritten on the next write.
    }
    return result
}

private fun buildVideoHistoryJson(entries: List<VideoPlaybackManager.VideoHistoryEntry>): String {
    val array = org.json.JSONArray()
    entries.forEach { entry ->
        array.put(
            org.json.JSONObject().apply {
                put("id", entry.videoId)
                put("t", entry.title)
                put("c", entry.channelName)
                put("cid", entry.channelId ?: "")
                put("th", entry.thumbnailUrl ?: "")
                put("ts", entry.lastPlayedAt)
                put("pos", entry.positionMs)
                put("dur", entry.durationMs)
            }
        )
    }
    return array.toString()
}

private fun parseSubscribedChannels(json: String?): List<VideoPlaybackManager.VideoSubscribedChannel> {
    if (json.isNullOrBlank()) return emptyList()
    val result = mutableListOf<VideoPlaybackManager.VideoSubscribedChannel>()
    try {
        val array = org.json.JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                VideoPlaybackManager.VideoSubscribedChannel(
                    channelId = obj.optString("cid"),
                    name = obj.optString("n"),
                    avatarUrl = obj.optString("a").takeIf { it.isNotEmpty() },
                    subscribedAt = obj.optLong("ts"),
                )
            )
        }
    } catch (_: Exception) {
        // Corrupt data is ignored and overwritten on the next write.
    }
    return result
}

private fun buildSubscribedChannelsJson(channels: List<VideoPlaybackManager.VideoSubscribedChannel>): String {
    val array = org.json.JSONArray()
    channels.forEach { channel ->
        array.put(
            org.json.JSONObject().apply {
                put("cid", channel.channelId)
                put("n", channel.name)
                put("a", channel.avatarUrl ?: "")
                put("ts", channel.subscribedAt)
            }
        )
    }
    return array.toString()
}
