package com.auramusic.app.video

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.datastore.preferences.core.edit
import com.auramusic.app.R
import com.auramusic.app.constants.LikedVideosKey
import com.auramusic.app.constants.SavedVideoIdsKey
import com.auramusic.app.constants.SavedVideosKey
import com.auramusic.app.constants.VideoAutoplayEnabledKey
import com.auramusic.app.constants.VideoHistoryKey
import com.auramusic.app.constants.VideoQuality
import com.auramusic.app.constants.VideoQualityKey
import com.auramusic.app.constants.VideoSubscribedChannelsKey
import com.auramusic.app.utils.AuraPlayerUtils
import com.auramusic.app.utils.VideoThumbnails
import com.auramusic.app.utils.compactViewCount
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.YouTubeVideoItem
import com.auramusic.innertube.models.response.WatchCompactVideo
import com.auramusic.innertube.models.response.WatchMetadataResponse
import com.auramusic.innertube.models.response.channelAvatarUrl
import com.auramusic.innertube.models.response.channelId
import com.auramusic.innertube.models.response.channelName
import com.auramusic.innertube.models.response.commentCountText
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
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
        /**
         * Every channel the video credits. Most videos have one; collaborative and music
         * videos can have two, and [channelId] only ever names the first.
         */
        val channels: List<com.auramusic.innertube.models.Artist> = emptyList(),
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
        val replies: List<CommentItem> = emptyList(),
        val isLoadingReplies: Boolean = false,
        val repliesExhausted: Boolean = false,
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
        val commentsContinuation: NewPipeExtractor.NextPage? = null,
        val isLoadingMoreComments: Boolean = false,
        val showSettings: Boolean = false,
        val playbackSpeed: Float = 1.0f,
        val resizeMode: Int = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
        val videoQuality: VideoQuality = VideoQuality.QUALITY_720P,
        val autoplayEnabled: Boolean = true,
        val suppressOverlay: Boolean = false,
        val isFullScreen: Boolean = false,
        // Display aspect ratio of the playing video, used to size the picture-in-picture
        // window. Null until the decoder reports it.
        val videoAspectRatio: Float? = null,
        // True while the Activity is showing the video in picture-in-picture.
        val inPictureInPicture: Boolean = false,
    ) {
        val isEmpty: Boolean get() = session == null
        val progress: Float
            get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Separate flow for position updates — only the progress UI observes this,
     *  so the rest of the overlay doesn't recompose every 500ms. */
    private val _positionState = MutableStateFlow(0L to 0L)
    val positionState: StateFlow<Pair<Long, Long>> = _positionState.asStateFlow()

    /**
     * Whether the video overlay is on screen at all.
     *
     * The overlay covers the whole screen while expanded and is simply gone once minimised -
     * the video then lives in a floating window or behind the app - but either way the music
     * mini player has to stay out of the way. Derived and distinct so a transition produces
     * exactly one layout pass instead of the two surfaces moving the bottom inset in
     * different frames.
     */
    val isOverlayVisible: StateFlow<Boolean> = _uiState
        .map { it.session != null && !it.hiddenByMusic }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    private var player: ExoPlayer? = null

    /**
     * The app's shared player connection. The video side does not own a player: audio and
     * video share the music player's ExoPlayer, session and notification, so the two can
     * never fight over the surface, the shade, or the CPU. Registered by MainActivity as
     * soon as the connection exists.
     */
    private var sharedConnectionRef: java.lang.ref.WeakReference<com.auramusic.app.playback.PlayerConnection>? = null

    fun attachSharedPlayer(connection: com.auramusic.app.playback.PlayerConnection) {
        sharedConnectionRef = java.lang.ref.WeakReference(connection)
    }
    private var tickerJob: Job? = null

    /**
     * Bumped whenever video takes the screen back from music.
     *
     * [giveWayToMusic] defers its service teardown by a frame so the visible state change gets to
     * draw first. If playback is restarted before that runs, the queued teardown would kill the
     * service the new playback just brought up, so the deferred block checks this first.
     */
    private val handoffGeneration = AtomicLong()

    // SponsorBlock integration for video playback
    var sponsorBlockManager: com.auramusic.app.sponsorblock.SponsorBlockManager? = null
        private set

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

    /**
     * Reply pagination tokens keyed by comment id. These stay out of [CommentItem]
     * because they are extractor types the UI layer should never have to know about.
     * Cleared whenever a new video starts so a stale token is never reused.
     */
    private val replyTokens = ConcurrentHashMap<String, NewPipeExtractor.NextPage?>()

    /** Channel refs for comments still missing an avatar, filled in on the side. */
    private val missingAvatars = ConcurrentHashMap<String, String>()

    /**
     * Resolved avatars by channel ref, kept for the life of the process. Pagination
     * pages often omit the avatar entirely, but the same handful of authors recur
     * across a comment thread (and across videos) — a cache hit paints those rows
     * immediately instead of leaving a placeholder until the channel page round-trip
     * lands.
     */
    private val avatarByChannelRef = ConcurrentHashMap<String, String>()
    /**
     * Application context only. This used to hold the Activity, which leaked it for
     * the life of the process (it was never cleared, and was set before the
     * "player already exists" early return, so it also went stale across rotation).
     * Everything that starts/stops a service or reads DataStore is happy with the
     * application context; use [activityRef] for anything needing an Activity.
     */
    private var currentContext: Context? = null
    /**
     * Weak Activity handle, only needed to flip the requested orientation for
     * fullscreen. Weak so a configuration change can collect the old Activity.
     */
    private var activityRef: java.lang.ref.WeakReference<android.app.Activity>? = null
    private val playedVideoIds = mutableSetOf<String>()

    /**
     * Bumped every time a different video is requested. Stream extraction is async,
     * so without this a slow load for an older video could land after a newer one and
     * overwrite the player (and stamp an error on the wrong video). The metadata
     * loaders already re-check the session; this covers the stream itself.
     */
    @Volatile private var loadGeneration = 0

    private var currentQuality: VideoQuality = VideoQuality.QUALITY_720P
    private var currentAutoplay: Boolean = true

    // Callback for VideoRecommendationManager to track watches
    var onVideoPlayed: (() -> Unit)? = null

    fun playerOrNull(): ExoPlayer? = player

    private val playerListener = object : Player.Listener {
        // The listener lives on the shared music player: while the video is not the guest
        // on it, every callback belongs to music and must not touch video state.
        private fun isGuestActive(): Boolean =
            sharedConnectionRef?.get()?.service?.guestVideoActive?.value == true

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isGuestActive()) return
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (!isGuestActive()) return
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
                        videoAspectRatio = exo.videoSize.toDisplayAspectRatioOrNull(),
                    )
                }
            }
            if (playbackState == Player.STATE_ENDED) {
                if (_uiState.value.autoplayEnabled) playNext()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (!isGuestActive()) return
            _uiState.update { it.copy(isBuffering = false, error = error.message ?: "Playback error") }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (!isGuestActive()) return
            // The PiP window is sized from this, and a stale ratio makes the window letterbox
            // the next video until it is re-entered.
            val ratio = videoSize.toDisplayAspectRatioOrNull()
            if (ratio != null) {
                _uiState.update { it.copy(videoAspectRatio = ratio) }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (!isGuestActive()) return
            val exo = player ?: return
            // A merged audio+video source reports its video-only child's "<id>_v" id
            // (the merged timeline takes the first child's media item), so strip the
            // stream suffix before comparing against the session and queue. Without
            // this, every 1080p+ start and every queued advance looked foreign: the
            // reset branch below wiped the just-loaded recommendations/comments while
            // the previous video's title stayed on screen.
            val rawMediaId = mediaItem?.mediaId
            val newVideoId = when {
                rawMediaId.isNullOrEmpty() -> null
                rawMediaId.length > 11 && rawMediaId.endsWith("_v") -> rawMediaId.dropLast(2)
                rawMediaId.length > 11 && rawMediaId.endsWith("_a") -> rawMediaId.dropLast(2)
                else -> rawMediaId
            }
            // playWithDetails already resets the per-video state before loading, so a
            // transition for the current session's own video (fired when exo.prepare()
            // lands) must NOT wipe recommendations/comments — the async loaders may have
            // just filled them in, and erasing them here made the Up Next list and
            // comments vanish (or never appear) for fast-loading videos.
            if (newVideoId == _uiState.value.session?.videoId) return
            // A native playlist advance to a video the lookahead queued: the session
            // follows the item instead of being reloaded.
            if (newVideoId != null && _uiState.value.queue.any { it.videoId == newVideoId }) {
                advanceSessionTo(newVideoId)
                return
            }
            // An item without a usable id (merged placeholder, external injection):
            // there is nothing to key a reload on, so keep the current session rather
            // than tearing down a video that may still be the one playing.
            if (newVideoId == null) return
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

    /**
     * Puts video playback in charge of the screen. The shared player swaps to the video's
     * media source when it arrives, so all this has to do is keep preferences fresh and
     * invalidate any stale give-way teardown from an earlier music takeover.
     */
    private fun handOverFromMusic(context: Context) {
        // Video is taking the screen back, so any teardown queued by an earlier giveWayToMusic
        // is now stale and must not run against the playback this function is about to start.
        handoffGeneration.incrementAndGet()
        applyStoredPreferences(context.applicationContext)
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
        channels: List<com.auramusic.innertube.models.Artist> = emptyList(),
    ) {
        val current = _uiState.value
        if (current.session?.videoId == videoId) {
            current.error?.let {
                _uiState.update { s -> s.copy(error = null) }
            }
            // Resuming the same video still has to take the screen back from music.
            // This path used to just player?.play(), so if music had reclaimed the
            // shade (giveWayToMusic) the music kept playing and video played on top
            // of it - two players audible at once, with no video notification.
            handOverFromMusic(context)
            _uiState.update { it.copy(minimized = false, hiddenByMusic = false) }
            if (sharedConnectionRef?.get()?.service?.guestVideoActive?.value == true) {
                // The guest source is still on the shared player: just resume it.
                player?.play()
                return
            }
            // Music took the shared player while the session slept; fall through and
            // run the full load again so the video source is put back on the player.
        }

        playedVideoIds.add(videoId)
        // A new video invalidates the previous one's cached watch page.
        watchMetadataCache = null
        watchMetadataCacheVideoId = null
        // Reply tokens and avatar backfills belong to the previous video's comments.
        replyTokens.clear()
        missingAvatars.clear()
        val bestThumbnail = thumbnails.maxByOrNull { it.width ?: 0 }?.url
        // Claim a generation for this load; older in-flight loads become no-ops.
        val generation = ++loadGeneration

        val exo = attachToSharedPlayer(context)
        handOverFromMusic(context)
        _uiState.value = UiState(
            session = VideoSession(
                videoId = videoId,
                title = title,
                channelName = channelName,
                channelId = channelId,
                channelThumbnail = channelThumbnail ?: bestThumbnail,
                channels = channels.ifEmpty {
                    channelId?.let { listOf(com.auramusic.innertube.models.Artist(channelName, it)) }
                        .orEmpty()
                }.mapIndexed { index, channel ->
                    // The primary channel's art travels with the video; hand it to the
                    // credited list so the avatar stack renders immediately instead of
                    // waiting on a channel-page lookup.
                    if (index == 0 && channel.avatarUrl == null) {
                        channel.copy(avatarUrl = channelThumbnail ?: bestThumbnail)
                    } else {
                        channel
                    }
                },
                description = description,
                viewCountText = viewCountText,
                publishedTimeText = publishedTimeText,
            ),
            minimized = false,
            isPlaying = true,
            isBuffering = true,
            videoQuality = currentQuality,
            autoplayEnabled = currentAutoplay,
            // Pre-set loading states so the UI shows spinners immediately. The async loaders
            // (loadRecommendations, loadComments) skip their own redundant initial-state writes
            // and go straight to writing final results, cutting two extra recompositions per load.
            isLoadingRecommendations = true,
            isLoadingComments = true,
        )
        // Rebuild the media notification immediately with the NEW video's metadata.
        // This closes the window where the shade would otherwise keep showing the
        // previous video (or a placeholder) until the stream resolves and
        // loadMediaSourceInto triggers its own rebuild — the cause of the media
        // notification feeling non-persistent / out of sync when switching videos.
        // Notify recommendation manager that a video was played
        onVideoPlayed?.invoke()
        // The session is live: hand it to the widget, Discord presence and scrobbler.
        sharedConnectionRef?.get()?.service?.syncVideoIntegrations(sessionChanged = true)
        scope.launch {
            val source = withContext(Dispatchers.IO) {
                AuraPlayerUtils.getVideoStreamSource(videoId).getOrNull()
            }
            // Drop this result if the user moved on while we were extracting.
            if (generation != loadGeneration || _uiState.value.session?.videoId != videoId) {
                return@launch
            }
            if (source == null) {
                _uiState.update { it.copy(isBuffering = false, error = "Could not load video") }
                return@launch
            }
            loadMediaSourceInto(videoId, source, title, channelName, channelThumbnail ?: bestThumbnail)
        }
        scope.launch {
            // Enrich metadata (fills gaps for views/description/channel avatar), then load content.
            // enrichSessionMetadata returns the fetched watch page so loadRecommendations can
            // reuse the already-cached response without locking the mutex a second time —
            // this removes one extra _uiState.update recomposition at the start of every load.
            val watchMetadata = enrichSessionMetadata(videoId)
            resolveCollaboratorChannels(videoId)
            loadRecommendations(videoId, prefetchedMetadata = watchMetadata)
            loadComments(videoId)
            // Load SponsorBlock segments for this video
            try {
                if (sponsorBlockManager == null) {
                    sponsorBlockManager = com.auramusic.app.sponsorblock.SponsorBlockManager(
                        context.applicationContext, scope
                    )
                    sponsorBlockManager?.loadPreferences()
                }
                val durationMs = player?.duration?.takeIf { it > 0 }?.toLong() ?: 0L
                sponsorBlockManager?.loadSegments(videoId, durationMs)
            } catch (e: Exception) {
                // SponsorBlock is optional - don't break video playback
            }
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
            watchMetadataCache = metadata
            watchMetadataCacheVideoId = videoId
            metadata
        }
    }

    /**
     * Fills missing session fields from the WEB watch page. Known metadata from the
     * list item the user tapped wins; only blank fields get overwritten. Also seeds
     * the real channel avatar (separate from the video artwork), the current
     * like/dislike state from the watch button icon, and the locally-saved state.
     *
     * Returns the fetched WatchMetadataResponse so callers that also need it (e.g.
     * loadRecommendations, which must run next) can reuse the already-cached value
     * instead of locking the mutex again.
     */
    private suspend fun enrichSessionMetadata(videoId: String): WatchMetadataResponse? {
        if (_uiState.value.session?.videoId != videoId) return null
        val maxAttempts = 3
        var attempt = 0
        var metadata: WatchMetadataResponse? = null
        while (attempt < maxAttempts) {
            attempt++
            metadata = fetchWatchMetadata(videoId)
            if (metadata != null) break
            delay(1000L * attempt)
        }
        if (metadata == null) return null
        val savedIds = withContext(Dispatchers.IO) {
            currentContext?.let { it.dataStore[SavedVideoIdsKey] }.orEmpty()
        }
        if (_uiState.value.session?.videoId != videoId) return metadata
        val likeState = metadata.likeState() ?: WatchLikeState.NONE
        _uiState.update { state ->
            val session = state.session?.takeIf { it.videoId == videoId } ?: return@update state
            state.copy(
                session = session.copy(
                    title = session.title.ifBlank { metadata.title().orEmpty() },
                    channelName = session.channelName.ifBlank { metadata.channelName().orEmpty() },
                    channelId = session.channelId ?: metadata.channelId(),
                    channelAvatarUrl = session.channelAvatarUrl ?: metadata.channelAvatarUrl(),
                    description = session.description?.takeIf { it.isNotBlank() }
                        ?: metadata.description(),
                    viewCountText = session.viewCountText ?: metadata.viewCountText(),
                    publishedTimeText = session.publishedTimeText ?: metadata.dateText(),
                    // Keep already-resolved values: the youtubei owner/comment blocks are
                    // routinely empty now, so an unconditional copy would wipe what the
                    // channel-page backfill had just resolved.
                    subscriberCountText = session.subscriberCountText
                        ?: metadata.subscriberCountText(),
                    commentCountText = session.commentCountText ?: metadata.commentCountText(),
                    likeCountText = session.likeCountText ?: metadata.likeCountText(),
                ),
                isLiked = likeState == WatchLikeState.LIKE,
                isDisliked = likeState == WatchLikeState.DISLIKE,
                isSaved = videoId in savedIds,
            )
        }
        recordCurrentToHistory()
        // The watch-page owner block (name, avatar, subscriber count) is the least stable
        // part of the youtubei response, so top it up from the channel page when it came
        // back empty.
        fillMissingChannelMetadata(videoId, metadata.channelId())
        // Enrichment refined the session (title, artwork, like state): repaint the
        // widget and presence without restarting the scrobble timer.
        sharedConnectionRef?.get()?.service?.syncVideoIntegrations()
        return metadata
    }

    /** The video the lookahead most recently appended to the shared playlist, if any. */
    private var queuedAheadVideoId: String? = null

    /**
     * Resolves the next unplayed recommendation and appends it to the shared playlist behind
     * the current video. With the next item already queued, the end of a video is a native
     * playlist advance - no reload gap - and the Up next sheet shows the real video queue.
     */
    private fun queueUpcomingVideo() {
        val state = _uiState.value
        if (!state.autoplayEnabled) return
        val currentId = state.session?.videoId ?: return
        val nextItem = state.queue.firstOrNull { it.videoId != currentId && it.videoId !in playedVideoIds }
            ?: return
        if (queuedAheadVideoId == nextItem.videoId) return
        val ctx = currentContext ?: return
        val service = sharedConnectionRef?.get()?.service ?: return
        queuedAheadVideoId = nextItem.videoId
        scope.launch {
            val source = withContext(Dispatchers.IO) {
                AuraPlayerUtils.getVideoStreamSource(nextItem.videoId).getOrNull()
            }
            if (source == null || _uiState.value.session?.videoId != currentId) {
                if (queuedAheadVideoId == nextItem.videoId) queuedAheadVideoId = null
                return@launch
            }
            val mediaSource = buildMediaSource(
                ctx,
                nextItem.videoId,
                source,
                title = nextItem.title,
                channelName = nextItem.channelName,
                channelThumbnail = nextItem.thumbnail,
            )
            service.appendGuestVideoSource(mediaSource)
        }
    }

    /**
     * The shared player advanced to an item that was queued behind the current video. The
     * session follows the item: metadata, comments, recommendations and the next lookahead
     * all reload for the new video, while playback itself never stops.
     */
    private fun advanceSessionTo(videoId: String) {
        val item = _uiState.value.queue.firstOrNull { it.videoId == videoId } ?: return
        playedVideoIds.add(videoId)
        queuedAheadVideoId = null
        // A new video invalidates the previous one's cached watch page.
        watchMetadataCache = null
        watchMetadataCacheVideoId = null
        // Reply tokens and avatar backfills belong to the previous video's comments.
        replyTokens.clear()
        missingAvatars.clear()
        _uiState.update {
            it.copy(
                session = VideoSession(
                    videoId = item.videoId,
                    title = item.title,
                    channelName = item.channelName,
                    channelId = item.channelId,
                    channelThumbnail = item.thumbnail,
                ),
                positionMs = 0,
                error = null,
                isLiked = false,
                isDisliked = false,
                isSubscribed = false,
                isSaved = false,
                expandedDescription = false,
                // The Up Next list and comments belong to the video that just ended.
                // Clearing them up front (and lighting the loaders) means the new
                // video shows spinners instead of the old video's rows while the
                // enrich/recommendation/comment fetches are still in flight.
                recommendations = emptyList(),
                recommendationsContinuation = null,
                isLoadingRecommendations = true,
                isLoadingMoreRecommendations = false,
                comments = emptyList(),
                commentsContinuation = null,
                commentsError = null,
                isLoadingComments = true,
                isLoadingMoreComments = false,
            )
        }
        onVideoPlayed?.invoke()
        // Queue advanced to a new video while still playing: no IS_PLAYING event will
        // fire, so the integrations are pushed from here instead.
        sharedConnectionRef?.get()?.service?.syncVideoIntegrations(sessionChanged = true)
        scope.launch {
            val watchMetadata = enrichSessionMetadata(videoId)
            resolveCollaboratorChannels(videoId)
            loadRecommendations(videoId, prefetchedMetadata = watchMetadata)
            loadComments(videoId)
            queueUpcomingVideo()
        }
    }

    private suspend fun loadRecommendations(videoId: String, prefetchedMetadata: WatchMetadataResponse? = null) {
        // The isLoadingRecommendations=true + empty list state is already written by
        // playWithDetails at the point it resets UiState, so skipping a redundant update
        // here removes one spurious recomposition at the start of every video load.

        // Primary source: WEB watch page related videos (works for all YouTube videos).
        // Accept prefetched metadata from enrichSessionMetadata to avoid a second mutex lock.
        val metadata = prefetchedMetadata ?: fetchWatchMetadata(videoId)
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
            queueUpcomingVideo()
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
            queueUpcomingVideo()
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
            queueUpcomingVideo()
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

    private suspend fun loadComments(videoId: String, resetState: Boolean = false) {
        if (_uiState.value.session?.videoId != videoId) return
        if (resetState) {
            _uiState.update { it.copy(isLoadingComments = true, comments = emptyList(), commentsError = null, commentsContinuation = null) }
        }
        val maxAttempts = 3
        var attempt = 0
        var lastError: String? = null
        while (attempt < maxAttempts) {
            attempt++
            val result = withContext(Dispatchers.IO) {
                try {
                    NewPipeExtractor.getComments(videoId)
                } catch (e: Exception) {
                    Timber.tag("VideoPlaybackManager").w(e, "loadComments attempt $attempt failed")
                    null
                }
            }
            if (_uiState.value.session?.videoId != videoId) return
            if (result != null && (result.comments.isNotEmpty() || result.nextPage != null)) {
                val comments = result.comments.map { it.toCommentItem() }
                _uiState.update { state ->
                    state.copy(
                        comments = comments,
                        isLoadingComments = false,
                        commentsContinuation = result.nextPage,
                        commentsError = if (comments.isEmpty()) "Comments are unavailable for this video" else null,
                    )
                }
                backfillMissingAvatars(videoId)
                return
            }
            lastError = "Could not load comments"
            if (attempt < maxAttempts) {
                delay(1000L * attempt)
            }
        }
        _uiState.update { it.copy(isLoadingComments = false, commentsError = lastError ?: "Could not load comments") }
    }

    /** Re-runs the comments fetch for the current video (drives the Retry button). */
    fun retryLoadingComments() {
        val videoId = _uiState.value.session?.videoId ?: return
        scope.launch { loadComments(videoId, resetState = true) }
    }

    /** Infinite scroll for the comments list. */
    fun loadMoreComments() {
        val state = _uiState.value
        val videoId = state.session?.videoId ?: return
        val page = state.commentsContinuation ?: return
        if (state.isLoadingMoreComments) return
        _uiState.update { it.copy(isLoadingMoreComments = true) }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                NewPipeExtractor.getMoreComments(videoId, page)
            }
            if (_uiState.value.session?.videoId != videoId) return@launch
            val newComments = result.comments.map { it.toCommentItem() }
            _uiState.update { current ->
                val existing = current.comments.map { it.commentId }.toSet()
                current.copy(
                    comments = current.comments + newComments.filter { it.commentId !in existing },
                    commentsContinuation = result.nextPage,
                    isLoadingMoreComments = false,
                )
            }
            backfillMissingAvatars(videoId)
        }
    }

    /**
     * Loads the replies for one comment into that comment's own [CommentItem.replies].
     * [append] is false for the first page (replaces) and true for subsequent ones.
     */
    fun loadCommentReplies(commentId: String, append: Boolean = false) {
        val state = _uiState.value
        val videoId = state.session?.videoId ?: return
        val target = state.comments.firstOrNull { it.commentId == commentId } ?: return
        if (target.isLoadingReplies) return
        val token = replyTokens[commentId] ?: return

        markRepliesLoading(commentId, append)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                NewPipeExtractor.getCommentReplies(videoId, token)
            }
            if (_uiState.value.session?.videoId != videoId) return@launch
            val mapped = result.comments.map { it.toCommentItem() }
            if (result.nextPage != null) replyTokens[commentId] = result.nextPage else replyTokens.remove(commentId)
            _uiState.update { current ->
                val cleaned = current.comments.map { it.copy(isLoadingReplies = false) }
                current.copy(
                    comments = cleaned.map { comment ->
                        if (comment.commentId != commentId) return@map comment
                        mergeReplies(comment, mapped, result.nextPage != null)
                    },
                )
            }
            // Replies land without avatars nearly every time; the backfill patches the
            // nested rows too, so the chain does not stay a wall of initials.
            backfillMissingAvatars(videoId)
        }
    }

    private fun markRepliesLoading(commentId: String, append: Boolean) {
        _uiState.update { current ->
            current.copy(
                comments = current.comments.map { comment ->
                    if (comment.commentId != commentId) {
                        comment
                    } else {
                        comment.copy(
                            isLoadingReplies = true,
                            // A fresh load shows nothing until the page lands, so the
                            // row never flickers between old and new content.
                            replies = if (append) comment.replies else emptyList(),
                        )
                    }
                },
            )
        }
    }

    private fun mergeReplies(
        comment: CommentItem,
        incoming: List<CommentItem>,
        hasMore: Boolean,
    ): CommentItem {
        val existing = comment.replies.mapTo(HashSet()) { it.commentId }
        val merged = comment.replies + incoming.filter { it.commentId !in existing }
        return comment.copy(
            replies = merged,
            // Keep the spinner lit while pages are still outstanding, so a
            // multi-page reply chain shows continuous progress instead of
            // flickering between loaded and not.
            isLoadingReplies = hasMore && merged.size < (comment.replyCount?.toIntOrNull() ?: 0),
            repliesExhausted = !hasMore,
        )
    }

    /**
     * Comments ship without an author avatar more often than not — pagination pages
     * and replies almost never carry one — so the ones still blanking get fetched
     * from the author's channel page in batches. Known authors are painted straight
     * from [avatarByChannelRef] (no request), successful lookups are remembered for
     * later pages and later videos, and a transient failure gets one retry before
     * the row is left to its initial.
     */
    private suspend fun backfillMissingAvatars(videoId: String) {
        var pending = missingAvatars.toMap()
        missingAvatars.clear()
        if (pending.isEmpty()) return
        val resolved = mutableMapOf<String, String>()
        pending = pending.filter { (commentId, ref) ->
            avatarByChannelRef[ref]?.let { avatar ->
                resolved[commentId] = avatar
                false
            } ?: true
        }
        var attempts = 0
        while (pending.isNotEmpty() && attempts < 2) {
            attempts++
            val failed = mutableMapOf<String, String>()
            coroutineScope {
                // Four lookups in flight at once: a page rarely credits fewer authors
                // than that, and serialising them is what made new rows sit on their
                // initial for seconds after a load-more landed.
                pending.entries.chunked(4).forEach { batch ->
                    batch
                        .map { (commentId, ref) ->
                            async(Dispatchers.IO) {
                                Triple(commentId, ref, NewPipeExtractor.fetchCommentAvatar(ref))
                            }
                        }.awaitAll()
                        .forEach { (commentId, ref, avatar) ->
                            if (avatar.isNullOrBlank()) {
                                failed[commentId] = ref
                            } else {
                                avatarByChannelRef[ref] = avatar
                                resolved[commentId] = avatar
                            }
                        }
                }
            }
            pending = failed
            if (pending.isNotEmpty() && attempts < 2) delay(400L)
        }
        if (_uiState.value.session?.videoId != videoId) return
        if (resolved.isEmpty()) return
        fun applyAvatar(item: CommentItem): CommentItem {
            val avatar = resolved[item.commentId] ?: return item
            if (!item.authorThumbnail.isNullOrBlank()) return item
            return item.copy(authorThumbnail = avatar)
        }
        _uiState.update { current ->
            current.copy(
                comments = current.comments.map { comment ->
                    val patched = applyAvatar(comment)
                    val replies = patched.replies
                    if (replies.isEmpty() || replies.none { resolved.containsKey(it.commentId) }) {
                        patched
                    } else {
                        patched.copy(replies = replies.map(::applyAvatar))
                    }
                },
            )
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

    private fun NewPipeExtractor.VideoComment.toCommentItem(): CommentItem {
        // Copy to a local: a public API property from another module cannot be
        // smart-cast, so the null check has to be on a value we own.
        val channelRef = authorChannelRef
        val thumbnail = authorThumbnail?.takeIf { it.isNotBlank() }
            ?: channelRef?.let { avatarByChannelRef[it] }
        if (thumbnail == null && !channelRef.isNullOrBlank()) {
            missingAvatars[commentId] = channelRef
        }
        if (repliesToken != null) {
            replyTokens[commentId] = repliesToken
        }
        return CommentItem(
            commentId = commentId,
            authorName = authorName,
            authorThumbnail = thumbnail,
            content = content,
            publishedTime = publishedTime.takeIf { it.isNotBlank() },
            likeCount = likeCount.takeIf { it > 0 }?.toString(),
            replyCount = replyCount.takeIf { it > 0 }?.toString(),
            isPinned = isPinned,
        )
    }

    /**
     * Backstops the channel row against the youtubei watch response, which
     * reparents the owner block between renderers almost every version. Only
     * fills fields that are still blank, and only when a channel id is known.
     */
    private suspend fun fillMissingChannelMetadata(videoId: String, channelId: String?) {
        val session = _uiState.value.session?.takeIf { it.videoId == videoId } ?: return
        val id = session.channelId ?: channelId ?: return
        val needsName = session.channelName.isBlank()
        val needsAvatar = session.channelAvatarUrl.isNullOrBlank()
        val needsSubs = session.subscriberCountText.isNullOrBlank()
        if (!needsName && !needsAvatar && !needsSubs) return

        val info = withContext(Dispatchers.IO) { NewPipeExtractor.getChannelMetadata(id) } ?: return
        if (_uiState.value.session?.videoId != videoId) return
        val avatar = info.avatarUrl
        val subs = info.subscriberCount.takeIf { it >= 0 }
            ?.let { compactViewCount(it.toString()) }
        _uiState.update { current ->
            val cur = current.session?.takeIf { it.videoId == videoId } ?: return@update current
            current.copy(
                session = cur.copy(
                    channelName = cur.channelName.ifBlank { info.name },
                    channelAvatarUrl = cur.channelAvatarUrl
                        ?.takeIf { it.isNotBlank() }
                        ?: avatar,
                    subscriberCountText = cur.subscriberCountText ?: subs,
                ),
            )
        }
    }

    /**
     * Collaborative uploads credit a second channel only inside the owner dialog of the
     * watch-next payload; the byline-derived list usually carries just the first channel.
     * That left the avatar stack one short and the channel chooser with nothing to choose,
     * so the cached resolver swaps in the full credit list once it knows it.
     */
    private suspend fun resolveCollaboratorChannels(videoId: String) {
        val session = _uiState.value.session?.takeIf { it.videoId == videoId } ?: return
        if (session.channels.size > 1) return
        val collaborators = com.auramusic.innertube.CollaboratorResolver.resolve(videoId)
        if (_uiState.value.session?.videoId != videoId) return
        if (collaborators.size <= 1) return
        _uiState.update { current ->
            val cur = current.session?.takeIf { it.videoId == videoId } ?: return@update current
            current.copy(session = cur.copy(channels = collaborators))
        }
    }

    fun playNext() {
        // The lookahead keeps the next video queued on the shared playlist, so skipping is a
        // native advance with no reload gap. Only a drained queue falls back to a full load.
        val exo = player
        if (exo != null && exo.hasNextMediaItem()) {
            exo.seekToNextMediaItem()
            return
        }
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
        val exo = player
        if (exo != null && exo.hasPreviousMediaItem()) {
            exo.seekToPreviousMediaItem()
            return
        }
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
            // Record locally first: the Library shelf mirrors the optimistic button state,
            // so a failed remote call (signed-out account, endpoint hiccup) must not leave
            // the two out of sync - the button would read "liked" while the shelf stayed empty.
            writeLikedVideo(session, newLiked)
            YouTube.likeVideo(session.videoId, newLiked)
            _uiState.update {
                it.copy(
                    isLiked = newLiked,
                    isDisliked = if (newLiked) false else it.isDisliked
                )
            }
            // The widget shows the video's like state; repaint it now instead of
            // waiting for the next player event.
            sharedConnectionRef?.get()?.service?.syncVideoIntegrations()
        }
    }

    /** Keeps the Library's "Liked videos" shelf in step with the like toggle. */
    private suspend fun writeLikedVideo(session: VideoSession, liked: Boolean) {
        val ctx = currentContext ?: return
        withContext(Dispatchers.IO) {
            val current = parseVideoHistory(ctx.dataStore[LikedVideosKey]).toMutableList()
            current.removeAll { it.videoId == session.videoId }
            if (liked) {
                current.add(
                    0,
                    VideoHistoryEntry(
                        videoId = session.videoId,
                        title = session.title,
                        channelName = session.channelName,
                        channelId = session.channelId,
                        thumbnailUrl = session.channelThumbnail,
                        lastPlayedAt = System.currentTimeMillis(),
                    ),
                )
            }
            ctx.dataStore.edit { it[LikedVideosKey] = buildVideoHistoryJson(current.take(MAX_VIDEO_HISTORY_ENTRIES)) }
        }
    }

    /** The Library's "Liked videos" shelf, most recently liked first. */
    suspend fun readLikedVideos(context: Context): List<VideoHistoryEntry> =
        withContext(Dispatchers.IO) {
            runCatching { parseVideoHistory(context.dataStore[LikedVideosKey]) }.getOrDefault(emptyList())
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
            writeSavedVideo(session, newSaved)
            _uiState.update { it.copy(isSaved = newSaved) }
        }
    }

    /** Keeps the Library's "Saved videos" shelf in step with the save toggle. */
    private suspend fun writeSavedVideo(session: VideoSession, saved: Boolean) {
        val ctx = currentContext ?: return
        withContext(Dispatchers.IO) {
            val current = parseVideoHistory(ctx.dataStore[SavedVideosKey]).toMutableList()
            current.removeAll { it.videoId == session.videoId }
            if (saved) {
                current.add(
                    0,
                    VideoHistoryEntry(
                        videoId = session.videoId,
                        title = session.title,
                        channelName = session.channelName,
                        channelId = session.channelId,
                        thumbnailUrl = session.channelThumbnail,
                        lastPlayedAt = System.currentTimeMillis(),
                    ),
                )
            }
            ctx.dataStore.edit { it[SavedVideosKey] = buildVideoHistoryJson(current.take(MAX_VIDEO_HISTORY_ENTRIES)) }
        }
    }

    /** The Library's "Saved videos" shelf, most recently saved first. */
    suspend fun readSavedVideos(context: Context): List<VideoHistoryEntry> =
        withContext(Dispatchers.IO) {
            runCatching { parseVideoHistory(context.dataStore[SavedVideosKey]) }.getOrDefault(emptyList())
        }

    /**
     * Live views of the Library video shelves. Unlike the one-shot readers above, these keep
     * emitting as DataStore changes, so liking or saving while the Library is already on
     * screen (e.g. through the floating player) refreshes the shelf without waiting for the
     * screen to be rebuilt.
     */
    fun likedVideosFlow(context: Context) =
        context.dataStore.data
            .map { parseVideoHistory(it[LikedVideosKey]) }
            .distinctUntilChanged()

    fun savedVideosFlow(context: Context) =
        context.dataStore.data
            .map { parseVideoHistory(it[SavedVideosKey]) }
            .distinctUntilChanged()

    fun videoHistoryFlow(context: Context) =
        context.dataStore.data
            .map { parseVideoHistory(it[VideoHistoryKey]) }
            .distinctUntilChanged()

    fun subscribedChannelsFlow(context: Context) =
        context.dataStore.data
            .map { parseSubscribedChannels(it[VideoSubscribedChannelsKey]) }
            .distinctUntilChanged()

    fun toggleExpandedDescription() {
        _uiState.update { it.copy(expandedDescription = !it.expandedDescription) }
    }

    fun togglePlayPause() {
        val exo = player ?: return
        if (_uiState.value.isPlaying) {
            exo.pause()
            return
        }
        if (sharedConnectionRef?.get()?.service?.guestVideoActive?.value == true) {
            // The guest source is still on the shared player: just resume it.
            exo.play()
            return
        }
        // Music took the shared player while the video was hidden, so the source is
        // gone: run the full load again rather than resuming a stale frame.
        val session = _uiState.value.session ?: return
        val ctx = currentContext ?: return
        playWithDetails(
            context = ctx,
            videoId = session.videoId,
            title = session.title,
            channelName = session.channelName,
            channelId = session.channelId,
            channelThumbnail = session.channelThumbnail,
            description = session.description,
            viewCountText = session.viewCountText,
            publishedTimeText = session.publishedTimeText,
            channels = session.channels,
        )
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
            // No active video to demote; nothing to clean up.
            return
        }
        try {
            if (player?.isPlaying == true) player?.pause()
        } catch (e: Exception) {
            Timber.tag("VideoPlaybackManager").w(e, "giveWayToMusic: pause failed")
        }
        _uiState.update { it.copy(isPlaying = false, minimized = true, hiddenByMusic = true) }
        // Music owns the shared player now, so the guest session is over: no queue restore,
        // the incoming queue is the music the user just asked for. The session metadata is
        // kept alive so the video can be reloaded if the user comes back to it.
        sharedConnectionRef?.get()?.service?.endGuestVideo(restoreQueue = false)
        handoffGeneration.incrementAndGet()
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
                // The user may have switched video (or quality) while we resolved.
                if (_uiState.value.session?.videoId != session.videoId ||
                    _uiState.value.videoQuality != quality
                ) {
                    return@launch
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
        if (enabled) {
            queueUpcomingVideo()
        } else {
            // Anything the lookahead queued behind the current video would still advance
            // at the end of the item, so the playlist is trimmed back to just what is
            // playing.
            queuedAheadVideoId = null
            player?.let { exo ->
                val currentIndex = exo.currentMediaItemIndex
                for (index in exo.mediaItemCount - 1 downTo 0) {
                    if (index != currentIndex) exo.removeMediaItem(index)
                }
            }
        }
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
        if (queuedAheadVideoId == videoId) queuedAheadVideoId = null
        // Keep the shared playlist in step: a removed video must not come up next.
        val exo = player ?: return
        for (index in 0 until exo.mediaItemCount) {
            if (index != exo.currentMediaItemIndex && exo.getMediaItemAt(index).mediaId == videoId) {
                exo.removeMediaItem(index)
                break
            }
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

    /**
     * While the Shorts pager owns the screen it suppresses the global overlay (the expanded
     * player) so it does not render over the pager. The pager renders
     * [androidx.media3.ui.PlayerView] itself.
     */
    fun setOverlaySuppressed(suppressed: Boolean) {
        _uiState.update { if (it.suppressOverlay == suppressed) it else it.copy(suppressOverlay = suppressed) }
    }

    /**
     * Applies the requested orientation for the current fullscreen state. Kept as
     * its own function so collapse()/close() can put the Activity back the way they
     * found it - previously nothing ever restored the orientation, so backing out
     * of a fullscreen video left the whole app stuck in landscape.
     */
    private fun applyOrientation(isFullScreen: Boolean) {
        val activity = activityRef?.get() ?: return
        activity.requestedOrientation = if (isFullScreen) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun toggleFullScreen() {
        val newFullScreen = !_uiState.value.isFullScreen
        _uiState.update { it.copy(isFullScreen = newFullScreen) }
        applyOrientation(newFullScreen)
    }

    /**
     * Drops the player into the small in-app floating player.
     *
     * The video keeps playing in a corner of this Activity while the rest of the app stays
     * usable. The system's picture-in-picture window is deliberately not used here: it shrinks
     * the whole Activity, so the app the user was looking at disappears with it. The activity
     * only enters it when the user leaves (MainActivity.onUserLeaveHint).
     */
    fun collapse() {
        // Leaving fullscreen must also hand the orientation back to the system,
        // otherwise the Activity stays locked to landscape after the player hides.
        if (_uiState.value.isFullScreen) {
            _uiState.update { it.copy(isFullScreen = false) }
            applyOrientation(false)
        }
        _uiState.update { it.copy(minimized = true) }
    }

    fun expand() {
        _uiState.update { it.copy(minimized = false, hiddenByMusic = false) }
    }

    /**
     * Records that the Activity has entered or left picture-in-picture.
     *
     * While set, the in-app overlay unmounts: the platform is already drawing the video into the
     * PiP window, and a second PlayerView attached to the same player competes for frames.
     */
    fun setPictureInPicture(active: Boolean) {
        _uiState.update {
            if (it.inPictureInPicture == active) it
            else it.copy(inPictureInPicture = active)
        }
    }

    /**
     * Brings the overlay back once picture-in-picture ends. Without it the player would stay
     * minimised, because the overlay is unmounted for as long as the window is a PiP.
     */
    fun exitPictureInPicture() {
        _uiState.update {
            if (!it.inPictureInPicture && !it.minimized) it
            else it.copy(inPictureInPicture = false, minimized = false, hiddenByMusic = false)
        }
    }

    /** True while a video session is live and eligible to be shown in picture-in-picture. */
    fun isPictureInPictureEligible(): Boolean {
        val state = _uiState.value
        return state.session != null &&
            !state.hiddenByMusic &&
            !state.suppressOverlay &&
            playerOrNull()?.isPlaying == true
    }

    fun close() {
        // Persist the final watch position before tearing the session down so the
        // Library's "Recently watched" history reflects exactly where it stopped.
        recordCurrentToHistory()
        sponsorBlockManager?.reset()
        // Hand the orientation back before the player goes away, otherwise a
        // fullscreen video leaves the Activity locked in landscape.
        if (_uiState.value.isFullScreen) applyOrientation(false)
        tickerJob?.cancel()
        tickerJob = null
        // The shared player belongs to music: detach the listener, end the guest
        // session, and hand the player back with the listener's queue restored - but
        // never stop or release it.
        player?.removeListener(playerListener)
        player = null
        sharedConnectionRef?.get()?.service?.endGuestVideo(restoreQueue = true)
        // Ensure the display-on flag is cleared even when the composable tree is not
        // recomposed (e.g. close() called from a notification action while the overlay
        // is off-screen). The DisposableEffect in VideoPlayerOverlay handles it when the
        // composable is alive; this is the safety net for every other code path.
        activityRef?.get()?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        playedVideoIds.clear()
        queuedAheadVideoId = null
        // Drop our handles so we aren't pinning a Context/Activity any longer.
        currentContext = null
        activityRef = null
        _uiState.value = UiState()
    }

    fun release() {
        player?.removeListener(playerListener)
        player = null
        sharedConnectionRef?.get()?.service?.endGuestVideo(restoreQueue = false)
        tickerJob?.cancel()
        tickerJob = null
        playedVideoIds.clear()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun attachToSharedPlayer(context: Context): ExoPlayer? {
        // Always refresh both handles, even when the player is already attached: the
        // Activity may have been recreated by a configuration change, and the old
        // code returned early without updating them, leaving a dead Activity behind
        // for the fullscreen/orientation path.
        currentContext = context.applicationContext
        (context as? android.app.Activity)?.let { activityRef = java.lang.ref.WeakReference(it) }
        player?.let { return it }
        // Apply the quality + autoplay preference chosen in Settings/settings-overlay
        // so freshly attached players start with them.
        applyStoredPreferences(context)
        val connection = sharedConnectionRef?.get() ?: return null
        val exo = runCatching { connection.player }.getOrNull() ?: return null
        exo.addListener(playerListener)
        player = exo
        startTicker()
        return exo
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
        val service = sharedConnectionRef?.get()?.service ?: return
        val mediaSource = buildMediaSource(
            currentContext ?: return,
            videoId,
            source,
            title = title,
            channelName = channelName,
            channelThumbnail = channelThumbnail,
        )
        // The shared player swaps to the video as a guest: the music queue is snapshotted
        // on the first takeover and the session's notification shows the video's metadata.
        service.playGuestVideoSource(mediaSource, startPositionMs)
        // A fresh single-item playlist replaced whatever the lookahead had queued.
        queuedAheadVideoId = null
        queueUpcomingVideo()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var ticks = 0
            while (isActive) {
                val exo = player
                val state = _uiState.value
                if (exo == null || state.session == null) {
                    delay(IDLE_TICK_MS)
                    continue
                }
                // While music owns the screen the overlay is unmounted, so nobody reads
                // progress, and the video is paused so the playhead is not moving. Polling
                // here would only wake the main thread for a value nobody observes.
                if (state.hiddenByMusic) {
                    delay(IDLE_TICK_MS)
                    continue
                }

                val pos = exo.currentPosition
                val dur = if (exo.duration > 0) exo.duration else 0
                // Progress is published through _positionState only. It used to be
                // copied into uiState every ~2s as well, but uiState is collected
                // wholesale by MainActivity, the video overlay and the shorts
                // pager, so every copy invalidated the entire composition while a
                // video was playing - that is what made scrolling the pager and
                // moving between screens stutter. _positionState is the
                // high-frequency channel the UI reads for smooth progress.
                _positionState.value = pos to dur

                if (state.isPlaying) {
                    // Persist watch position to the video history every ~10s while
                    // actually playing (ticker fires every 500ms).
                    if (++ticks % 20 == 0) {
                        recordCurrentToHistory()
                    }
                    // SponsorBlock: auto-skip segments during video playback
                    if (dur > 0) {
                        val skipTo =
                            sponsorBlockManager?.findSkipTarget(pos, exo.playbackParameters.speed)
                        if (skipTo != null && skipTo > pos) {
                            exo.seekTo(skipTo)
                        }
                    }
                    delay(ACTIVE_TICK_MS)
                } else {
                    // Paused: the playhead is not moving, so there is nothing to redraw
                    // faster than once a second.
                    delay(IDLE_TICK_MS)
                }
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
                positionMs = _positionState.value.first,
                durationMs = _positionState.value.second,
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
        context: Context,
        videoId: String,
        source: com.auramusic.auravideo.AuraVideo.VideoStreamSource,
        title: String,
        channelName: String,
        channelThumbnail: String?,
    ): androidx.media3.exoplayer.source.MediaSource {
        val factory = ProgressiveMediaSource.Factory(
            VideoPlayerSupport.createDataSourceFactory(context),
            ExtractorsFactory {
                arrayOf(
                    MatroskaExtractor(),
                    FragmentedMp4Extractor(),
                    androidx.media3.extractor.mp4.Mp4Extractor()
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

/** How often playback progress is published while the playhead is actually moving. */
private const val ACTIVE_TICK_MS = 500L

/** How often the ticker wakes when there is nothing on screen to redraw for it. */
private const val IDLE_TICK_MS = 1_000L

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

// Public wrappers for VideoRecommendationManager
fun parseVideoHistoryPublic(json: String?): List<VideoPlaybackManager.VideoHistoryEntry> =
    parseVideoHistory(json)

fun parseSubscribedChannelsPublic(json: String?): List<VideoPlaybackManager.VideoSubscribedChannel> =
    parseSubscribedChannels(json)
