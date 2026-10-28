package com.auramusic.app.video

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.auramusic.app.constants.LastPlayedMediaKindKey
import com.auramusic.app.constants.LikedVideosKey
import com.auramusic.app.constants.SavedVideoIdsKey
import com.auramusic.app.constants.SavedVideosKey
import com.auramusic.app.constants.VideoAutoplayEnabledKey
import com.auramusic.app.constants.VideoHistoryKey
import com.auramusic.app.constants.VideoQuality
import com.auramusic.app.constants.VideoCodec
import com.auramusic.app.constants.VideoCodecKey
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object VideoPlaybackManager {

    /** Values of [LastPlayedMediaKindKey]. */
    const val MEDIA_KIND_MUSIC = "music"
    const val MEDIA_KIND_VIDEO = "video"

    data class VideoSession(
        val videoId: String,
        val title: String,
        val channelName: String = "",
        val channelId: String? = null,
        val channelThumbnail: String? = null,
        val channelAvatarUrl: String? = null,
        /**
         * The video's own artwork. History rows used to be handed [channelThumbnail]
         * instead, which is why Continue Watching and the Library shelves showed a
         * channel avatar (or nothing at all) where a video thumbnail belongs.
         */
        val videoThumbnail: String? = null,
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
        val channelAvatarUrl: String? = null,
    ) {
        /**
         * Artwork for the row. Entries written before the video thumbnail was recorded
         * hold the channel avatar here (and some hold nothing at all), which is why
         * Continue Watching and the Library shelves had cards with the wrong image or
         * none - so anything that is not video artwork falls back to YouTube's own.
         */
        val videoThumbnailUrl: String?
            get() = thumbnailUrl
                ?.takeIf { it.isNotBlank() && !it.isChannelAvatarUrl() }
                ?: VideoThumbnails.highQuality(videoId)

        /**
         * How much of the video was actually watched, 0..1. Zero while the duration is
         * still unknown, which is what keeps a just-started video from drawing an empty
         * watch bar over its own thumbnail.
         */
        val watchProgress: Float
            get() = if (durationMs <= 0L) 0f
            else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

        /** Watched to the end (or close enough) - these start over instead of resuming. */
        val isWatched: Boolean
            get() = watchProgress >= WATCHED_PROGRESS ||
                (durationMs in 1 until MIN_RESUMABLE_DURATION_MS && watchProgress >= 0.5f)

        /** Partially watched: there is a meaningful place to come back to. */
        val shouldResume: Boolean
            get() = durationMs >= MIN_RESUMABLE_DURATION_MS &&
                watchProgress > RESUME_MIN_PROGRESS &&
                watchProgress < WATCHED_PROGRESS
    }

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
        /**
         * Chapters of the current video. Empty for most videos - they are not an error,
         * they simply have none - and only worth drawing a chapter rail for when there
         * are more than one.
         */
        val chapters: List<com.auramusic.innertube.VideoChapters.Chapter> = emptyList(),
        /** Storyboard template for scrub previews; null when the client gave none. */
        val storyboardSpec: String? = null,
        val queue: List<RecommendationItem> = emptyList(),
        /**
         * The order the queue had before it was shuffled, and null when it has not
         * been. So turning the shuffle off is not "shuffle again", which would give a
         * second different order, but the one that was there before.
         */
        val queueOrder: List<RecommendationItem>? = null,
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
        val videoCodec: VideoCodec = VideoCodec.CODEC_AUTO,
        val autoplayEnabled: Boolean = true,
        val suppressOverlay: Boolean = false,
        val isFullScreen: Boolean = false,
        /**
         * Surface fills the screen while the video keeps its own shape, centred.
         *
         * Not the same as landscape: the video is not stretched or cropped, only the
         * surface around it grows. Reached by pushing down on the video.
         */
        val zoomMode: Boolean = false,
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

    /** Guards the one cold-start restore per process. */
    @Volatile private var restoreAttempted = false
    private var tickerJob: Job? = null

    /**
     * Brings back the video that was on screen when the app last closed, as a paused
     * tile in the floating player.
     *
     * Music restores itself because the service persists its queue; a video had
     * nothing to come back to, so opening the app always showed the last song - even
     * when the last thing the listener was watching was a video. Where it stopped and
     * what it was come from the video history, which is written while a video plays.
     *
     * Only the first connection in a process is a cold start; reattaching after a
     * configuration change must not pull the video the listener is watching back into
     * the corner.
     */
    fun restoreLastPlayedVideo(context: Context) {
        if (restoreAttempted) return
        restoreAttempted = true
        val appContext = context.applicationContext
        scope.launch {
            val prefs = withContext(Dispatchers.IO) {
                runCatching { appContext.dataStore.data.first() }.getOrNull()
            } ?: return@launch
            if (prefs[LastPlayedMediaKindKey] != MEDIA_KIND_VIDEO) return@launch
            if (_uiState.value.session != null) return@launch

            val connection = sharedConnectionRef?.get() ?: return@launch
            runCatching { connection.service.isPlayerReady.first { it } }

            // MusicService restores its persisted queue onto this same player right
            // after the player is ready, and a video taking the player snapshots that
            // queue to disk first. Waiting for the queue keeps the snapshot from
            // overwriting the listener's music with an empty one. An empty queue is a
            // normal state, so the wait is short.
            val deadline = System.currentTimeMillis() + QUEUE_RESTORE_GRACE_MS
            while (System.currentTimeMillis() < deadline) {
                val items = runCatching { connection.player.mediaItemCount }.getOrDefault(0)
                if (items > 0) break
                delay(100)
            }
            if (_uiState.value.session != null) return@launch

            // Music may have started while the queue was settling; if it did, it is the
            // most recent thing that played and the video stays closed.
            val fresh = withContext(Dispatchers.IO) {
                runCatching { appContext.dataStore.data.first() }.getOrNull()
            } ?: return@launch
            if (fresh[LastPlayedMediaKindKey] != MEDIA_KIND_VIDEO) return@launch

            val entry = parseVideoHistory(fresh[VideoHistoryKey])
                .maxByOrNull { it.lastPlayedAt }
                ?.takeIf { it.videoId.isNotBlank() && !it.isWatched }
                ?: return@launch
            // A video finished to the end is not "where I left off"; it belongs to
            // history now, not to the corner of the screen.
            playWithDetails(
                context = context,
                videoId = entry.videoId,
                title = entry.title,
                channelName = entry.channelName,
                channelId = entry.channelId,
                channelThumbnail = entry.channelAvatarUrl,
                thumbnails = emptyList(),
                // A history entry with no duration is older than the field, not a
                // video that just started; its position is still the best answer.
                startPositionMs = if (entry.durationMs <= 0L || entry.shouldResume) {
                    entry.positionMs
                } else {
                    0L
                },
                startDurationMs = entry.durationMs,
                startMinimized = true,
                startPaused = true,
            )
        }
    }

    /** Records which side last played, so a cold start knows what to bring back. */
    private fun markLastPlayedMediaKind(kind: String) {
        val ctx = currentContext
            ?: sharedConnectionRef?.get()?.service?.applicationContext
            ?: return
        scope.launch {
            runCatching {
                ctx.dataStore.edit { it[LastPlayedMediaKindKey] = kind }
            }
        }
    }

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
     * Channels subscribed in this session, by id.
     *
     * The channel screen and the video overlay both show a subscribe control for the same
     * channel, and each used to answer from its own page load - so subscribing in one
     * left the other showing the old state until it happened to reload. One shared set
     * keeps them in step the moment either one is tapped.
     */
    val subscribedChannelIds: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    /**
     * Video ids liked so far in this session.
     *
     * A like is the user's own decision and lives on the device; the watch page is only
     * asked afterwards, and says nothing when the account is signed out. Reading the
     * like off that page alone is why a video replayed after being liked showed up
     * unliked again - and why the overlay's button could flicker between states.
     */
    private val likedVideoIds: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    fun isVideoLiked(videoId: String): Boolean = likedVideoIds.value.contains(videoId)

    private fun markVideoLiked(videoId: String, liked: Boolean) {
        likedVideoIds.value = likedVideoIds.value.let {
            if (liked) it + videoId else it - videoId
        }
    }

    /** Loads the stored likes once per process so replayed videos read as liked. */
    suspend fun primeLikedVideos(context: Context) {
        val stored = runCatching { parseVideoHistory(context.dataStore[LikedVideosKey]) }
            .getOrDefault(emptyList())
            .map { it.videoId }
            .toSet()
        likedVideoIds.value = stored
    }

    /** True when [channelId] is subscribed as far as this process knows. */
    fun isChannelSubscribed(channelId: String?): Boolean =
        channelId != null && subscribedChannelIds.value.contains(channelId)

    /** Records (or forgets) a subscription in the shared set. */
    fun markChannelSubscribed(channelId: String, subscribed: Boolean) {
        subscribedChannelIds.value = subscribedChannelIds.value.let {
            if (subscribed) it + channelId else it - channelId
        }
    }

    /**
     * Seeds the shared set from what is already stored, so a channel subscribed in a
     * previous session reads as subscribed before any page has loaded.
     */
    suspend fun primeSubscribedChannels(context: Context) {
        val stored = runCatching { parseSubscribedChannels(context.dataStore[VideoSubscribedChannelsKey]) }
            .getOrDefault(emptyList())
            .map { it.channelId }
            .toSet()
        subscribedChannelIds.value = stored
    }
    /**
     * Application context only. This used to hold the Activity, which leaked it for
     * the life of the process (it was never cleared, and was set before the
     * "player already exists" early return, so it also went stale across rotation).
     * Everything that starts/stops a service or reads DataStore is happy with the
     * application context; use [activityRef] for anything needing an Activity.
     */
    private var currentContext: Context? = null

    /**
     * Whether this is a television.
     *
     * Asked of the device rather than the build, so one answer covers every place
     * where the two behave differently. Resolved from whichever context is in hand at
     * the time and then remembered - a device does not change its mind about what it
     * is, and the answer is needed on paths that run before any context exists.
     */
    private var isTelevision: Boolean? = null
    private fun isTelevision(context: Context?): Boolean {
        isTelevision?.let { return it }
        val answer = (context ?: com.auramusic.app.utils.AuraPlayerUtils.currentContextForDownloads)
            ?.packageManager
            ?.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            ?: false
        isTelevision = answer
        return answer
    }

    /**
     * Weak Activity handle, only needed to flip the requested orientation for
     * fullscreen. Weak so a configuration change can collect the old Activity.
     */
    private var activityRef: java.lang.ref.WeakReference<android.app.Activity>? = null

    /**
     * Videos this session has already played, in the order they were played.
     *
     * A recommendation list is generated from what YouTube thinks is related to the
     * video on screen, which happily includes what came before it: without this the
     * same three videos came round again every time the queue ran out. It survives
     * closing the overlay on purpose - the queue is the listener's, not the window's -
     * and is capped so a long session cannot grow without end.
     */
    private val playedVideoIds = LinkedHashSet<String>()

    /** Remembers [videoId] as played, oldest first, and forgets the ones furthest back. */
    private fun markPlayed(videoId: String) {
        if (videoId.isBlank()) return
        playedVideoIds.remove(videoId)
        playedVideoIds.add(videoId)
        while (playedVideoIds.size > MAX_REMEMBERED_PLAYED) {
            val oldest = playedVideoIds.firstOrNull() ?: break
            playedVideoIds.remove(oldest)
        }
    }

    /**
     * The queue as it should read right now: what is playing stays on top, and nothing
     * already played or already in the list is offered a second time.
     *
     * The recommendations that arrive for a new video are a fresh page, so without this
     * the list the listener is looking at jumps about under them - the video playing
     * disappears off the top and videos that have been watched come back into it.
     */
    private fun rebuildQueue(
        candidates: List<RecommendationItem>,
        keepCurrentOnTop: Boolean = true,
    ): List<RecommendationItem> {
        val currentId = _uiState.value.session?.videoId
        val seen = mutableSetOf<String>()
        val kept = ArrayList<RecommendationItem>(candidates.size + 1)
        fun offer(item: RecommendationItem) {
            val id = item.videoId
            if (id.isBlank()) return
            // The video on screen belongs at the top whatever the new page says about
            // it; a repeat of it further down is the same video twice.
            if (id == currentId) {
                if (keepCurrentOnTop && kept.none { it.videoId == id }) {
                    kept.add(item)
                }
                return
            }
            // Already watched, and still here. A music queue keeps what it has played -
            // the song before this one does not vanish off the list when the next one
            // starts, and the video queue asked to behave like a music queue was doing
            // exactly that. What has been watched is kept out of what comes *next*,
            // which is a separate question and is asked where it is asked.
            if (!seen.add(id)) return
            kept.add(item)
        }
        candidates.forEach(::offer)
        if (keepCurrentOnTop && currentId != null && kept.none { it.videoId == currentId }) {
            val session = _uiState.value.session
            kept.add(
                0,
                RecommendationItem(
                    videoId = currentId,
                    title = session?.title.orEmpty(),
                    channelName = session?.channelName.orEmpty(),
                    thumbnail = session?.videoThumbnail,
                    durationText = null,
                ),
            )
        }
        return kept
    }

    /**
     * Bumped every time a different video is requested. Stream extraction is async,
     * so without this a slow load for an older video could land after a newer one and
     * overwrite the player (and stamp an error on the wrong video). The metadata
     * loaders already re-check the session; this covers the stream itself.
     */
    @Volatile private var loadGeneration = 0

    private var currentQuality: VideoQuality = VideoQuality.QUALITY_720P

    private var currentCodec: VideoCodec = VideoCodec.CODEC_AUTO
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
            if (isPlaying) {
                // Set while playback is actually running, not when a video is merely
                // selected: a load that never produces a frame must not claim the cold
                // start next time.
                markLastPlayedMediaKind(MEDIA_KIND_VIDEO)
            } else {
                // The playhead has stopped, so this is the most accurate position the
                // history will get before the process goes away.
                recordCurrentToHistory()
            }
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
                // Mark it watched before moving on. The 10s ticker only ever saw the
                // position from before the last stretch, so a finished video could be
                // left in Continue Watching at ~90% and resume into its final seconds.
                markCurrentWatched()
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

            // Repeat-all wrapped the single video round to its own start, which is the
            // end of it as far as the queue is concerned: there is nothing in a
            // playlist of one to repeat along to. The mode itself is left exactly as
            // the listener set it - it is the button that decides, not the queue - so
            // the repeat button keeps behaving the way it does for music.
            //
            // Repeat-one is not this: that is the listener asking for this video again.
            val sessionId = _uiState.value.session?.videoId
            val wrapped = reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
                sessionId != null &&
                mediaItem?.mediaId?.let { it == sessionId || it.startsWith("${sessionId}_") } == true
            if (wrapped && exo.repeatMode == Player.REPEAT_MODE_ALL) {
                markCurrentWatched()
                if (_uiState.value.autoplayEnabled) playNext()
                return
            }

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
                    chapters = emptyList(),
                    storyboardSpec = null,
                    queue = emptyList(),
                    recommendationsContinuation = null,
                    isLoadingRecommendations = false,
                    isLoadingMoreRecommendations = false,
                    isLiked = newVideoId?.let { id -> isVideoLiked(id) } ?: false,
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

    fun play(context: Context, videoId: String, title: String, channelName: String = "", startPositionMs: Long = 0L) {
        playWithDetails(context, videoId, title, channelName, startPositionMs = startPositionMs)
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
        startPositionMs: Long = 0L,
        startDurationMs: Long = 0L,
        startMinimized: Boolean = false,
        startPaused: Boolean = false,
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

        markPlayed(videoId)
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
        // The outgoing video comes off the player now, before anything is fetched.
        //
        // It used to stay there - still audible, still on screen - through the whole
        // network wait for the next one, so a video that had finished played on for
        // several seconds more before the next took over. It is not wanted from the
        // moment another one is chosen, so that is when it goes.
        stopOutgoingVideo()
        handOverFromMusic(context)
        if (exo == null) {
            // Which used to be ignored, leaving state that said playing with nothing
            // behind it and every control on it quietly doing nothing.
            failLoad("Playback is not available", videoId, generation)
            return
        }
        _uiState.value = UiState(
            session = VideoSession(
                videoId = videoId,
                title = title,
                channelName = channelName,
                channelId = channelId,
                channelThumbnail = channelThumbnail ?: bestThumbnail,
                videoThumbnail = bestThumbnail ?: VideoThumbnails.highQuality(videoId),
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
            // Restoring the last video opens in the floating player, where the
            // listener left it, rather than taking the whole screen again.
            minimized = startMinimized,
            // Not yet playing, because nothing is. This used to be asserted true
            // before a byte of video existed, so every load that gave up on the way
            // left the player claiming to play a video that was never going to move -
            // the one shape every one of these failures has taken.
            isPlaying = false,
            isBuffering = true,
            // A resumed video knows where it is before the stream does, so the tile's
            // progress strip and the history write are right during the load.
            positionMs = startPositionMs,
            durationMs = startDurationMs,
            videoQuality = currentQuality,
            autoplayEnabled = currentAutoplay,
            // Pre-set loading states so the UI shows spinners immediately. The async loaders
            // (loadRecommendations, loadComments) skip their own redundant initial-state writes
            // and go straight to writing final results, cutting two extra recompositions per load.
            isLoadingRecommendations = true,
            isLoadingComments = true,
            // A like is the user's own decision and is recorded on this device. Seeding it
            // here means a replayed video shows as liked straight away; the watch page that
            // loads afterwards can only confirm it, never take it back.
            isLiked = isVideoLiked(videoId),
            isSubscribed = isChannelSubscribed(channelId),
            // The queue belongs to the session, not to the video. It used to be reset
            // along with everything else here, so every video arrived with an empty
            // list and then refilled it from its own recommendations - which is why
            // moving to the next video reshuffled the whole list, and why the video
            // queue never behaved like the music queue, where one list is built once
            // and a pointer moves through it. What is already queued is carried over,
            // and whatever is about to be fetched goes behind it rather than in front.
            queue = current.queue,
            recommendations = current.recommendations,
            queueOrder = current.queueOrder,
        )
        // Always reset the high-frequency pair with the session. A video opened from
        // music used to inherit the song's progress for a beat, because the ticker
        // had been publishing the shared player's position all along.
        _positionState.value = startPositionMs to startDurationMs
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
            runVideoLoad(
                context = context,
                generation = generation,
                videoId = videoId,
                title = title,
                channelName = channelName,
                channelThumbnail = channelThumbnail ?: bestThumbnail,
                startPositionMs = startPositionMs,
                startPaused = startPaused,
            )
        }
        scope.launch {
            // Enrich metadata (fills gaps for views/description/channel avatar), then load content.
            // enrichSessionMetadata returns the fetched watch page so loadRecommendations can
            // reuse the already-cached response without locking the mutex a second time —
            // this removes one extra _uiState.update recomposition at the start of every load.
            val watchMetadata = enrichSessionMetadata(videoId)
            loadChaptersAndStoryboard(videoId)
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
                // The remote page cannot un-like anything it never saw, so a locally
                // recorded like is kept rather than being reset on every load.
                isLiked = likeState == WatchLikeState.LIKE || isVideoLiked(videoId),
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
    /**
     * Chapters and the scrub-preview template for the current video.
     *
     * Both are extras on top of playback, so neither is allowed to hold anything up: a
     * video that cannot answer plays exactly as it did before.
     */
    private fun loadChaptersAndStoryboard(videoId: String) {
        val generation = loadGeneration
        scope.launch {
            val chapters = withContext(Dispatchers.IO) {
                YouTube.videoChapters(videoId).getOrDefault(emptyList())
            }
            if (generation != loadGeneration || _uiState.value.session?.videoId != videoId) return@launch
            if (chapters.size > 1) {
                _uiState.update { it.copy(chapters = chapters) }
            }

            val spec = withContext(Dispatchers.IO) {
                YouTube.videoStoryboardSpec(videoId).getOrNull()
            }
            if (generation != loadGeneration || _uiState.value.session?.videoId != videoId) return@launch
            if (!spec.isNullOrBlank()) {
                _uiState.update { it.copy(storyboardSpec = spec) }
            }
        }
    }

    /** The chapter playing at [timeMs], or null when this video has no chapters. */
    fun chapterAt(timeMs: Long): com.auramusic.innertube.VideoChapters.Chapter? =
        com.auramusic.innertube.VideoChapters.chapterAt(_uiState.value.chapters, timeMs)

    /** Scrub-preview thumbnail for [timeMs], or null when this video has no storyboard. */
    fun storyboardUrlAt(timeMs: Long): String? {
        val state = _uiState.value
        val spec = state.storyboardSpec ?: return null
        val duration = state.durationMs
        if (duration <= 0) return null
        return com.auramusic.innertube.VideoChapters.storyboardUrl(spec, timeMs, duration)
    }

    /** Jumps to the start of a chapter. */
    fun seekToChapter(chapter: com.auramusic.innertube.VideoChapters.Chapter) {
        seekTo(chapter.startMs)
    }

    /**
     * Puts a recommendation straight after the current video.
     *
     * The queue is rebuilt around the request so the lookahead keeps a next item to
     * advance into, which is what makes this feel immediate rather than merely queued.
     */
    fun playNext(item: RecommendationItem) {
        val state = _uiState.value
        if (item.videoId == state.session?.videoId) return
        // rebuildQueue keeps the video on screen on top and drops anything already
        // watched, so the requested video lands immediately after it - which is what
        // "play next" asks for - and the rest of the list still describes what follows.
        _uiState.update {
            it.copy(queue = rebuildQueue(listOf(item) + it.queue))
        }
        queuedAheadVideoId = null
        queueUpcomingVideo()
    }

    /**
     * Starts a specific video from the list of what comes next, keeping the rest of
     * that list behind it.
     *
     * The list on a television is not the player's playlist - the playlist holds only
     * the video that is playing, because a television has one owner of its queue - so
     * choosing one from the queue is a deliberate start rather than a seek.
     */
    fun playFromQueue(item: RecommendationItem) {
        val ctx = currentContext ?: return
        if (item.videoId == _uiState.value.session?.videoId) {
            // Already playing: going through the pipeline again would reload it from
            // the start, which is the opposite of what picking it asked for.
            player?.play()
            return
        }
        _uiState.update {
            it.copy(queue = rebuildQueue(listOf(item) + it.queue))
        }
        queuedAheadVideoId = null
        playWithDetails(
            context = ctx,
            videoId = item.videoId,
            title = item.title,
            channelName = item.channelName,
            channelId = item.channelId,
            channelThumbnail = item.thumbnail,
        )
    }

    /**
     * Whether the queue is currently in a shuffled order.
     *
     * Read by the player so the button says what turning it will do, which is the
     * same thing the music queue's shuffle button does.
     */
    val isQueueShuffled: Boolean get() = _uiState.value.queueOrder != null

    /**
     * Shuffles what comes next, and keeps the order it replaced.
     *
     * The list that is already there is the session's, so this is the only thing
     * that reorders it - which is what makes turning it back off able to mean
     * "put it back" rather than "give me a different order again".
     */
    fun reshuffleQueue() {
        _uiState.update { state ->
            val session = state.session
            val ahead = state.queue.filter { it.videoId != session?.videoId }
            // One video behind the one playing is not a list to shuffle; shuffling it
            // would only make the next button land somewhere arbitrary.
            if (ahead.size < 2) return@update state
            val head = state.queue.filter { it.videoId == session?.videoId }
            state.copy(
                queue = rebuildQueue(head + ahead.shuffled()),
                queueOrder = state.queueOrder ?: ahead,
            )
        }
    }

    /** Puts the queue back the way it was before it was shuffled. */
    fun unreshuffleQueue() {
        _uiState.update { state ->
            val order = state.queueOrder ?: return@update state
            state.copy(queue = rebuildQueue(order), queueOrder = null)
        }
    }

    /**
     * Whether this video has already been played this session.
     *
     * Shown on the Up next list so a video that has been watched reads as watched,
     * rather than looking like something still to come.
     */
    fun hasPlayed(videoId: String): Boolean = videoId in playedVideoIds

    private fun queueUpcomingVideo() {
        val state = _uiState.value
        if (!state.autoplayEnabled) return
        val currentId = state.session?.videoId ?: return

        // Everything in the queue has been played - the page this video was recommended
        // from is finite, and the ones in front of it have all been watched. Rather than
        // let the session stop dead, the recommendations are topped up from what has
        // been seen: that is the same list the Up next panel shows, so the queue keeps
        // playing what the listener is already looking at.
        if (state.queue.none { it.videoId != currentId && it.videoId !in playedVideoIds }) {
            val topUp = state.recommendations
                .filter { it.videoId != currentId && it.videoId !in playedVideoIds }
            if (topUp.isNotEmpty()) {
                _uiState.update {
                    it.copy(queue = rebuildQueue(it.queue + topUp))
                }
            }
        }

        // Nothing is appended to the player's playlist, on anything.
        //
        // A playlist entry for the next video is a second copy of the queue, and a
        // second copy can disagree with the first: a video taken off the queue stays
        // on the playlist, is reached again later, and leaves the player holding
        // something it will not show - a black picture with both ends at zero and no
        // error to explain it. It can also land mid-load, while the video actually
        // being played is still preparing, and leave that one loaded and black too.
        //
        // A television never did this and works. A telephone did, and did not.
        if (!queuesVideosOnPlayer(currentContext)) return

        val refreshed = _uiState.value
        val nextItem = refreshed.queue.firstOrNull { it.videoId != currentId && it.videoId !in playedVideoIds }
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
        markPlayed(videoId)
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
                isLiked = isVideoLiked(item.videoId),
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
            _uiState.update {
                it.copy(
                    recommendations = items,
                    // Behind the list that is already queued, not instead of it. This
                    // video's recommendations join the session's queue at the end, the
                    // way more songs join a radio's queue, so what the listener is
                    // about to watch is not reordered underneath them by the fact that
                    // the video in front of them changed.
                    queue = rebuildQueue(it.queue + items),
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
            _uiState.update {
                it.copy(
                    recommendations = nextItems,
                    // Behind what is already queued, like its two sibling branches.
                    // This one replaced the list outright, so a video whose watch page
                    // failed to list related videos - which lands here, after a retry
                    // ladder, seconds after the next video started - quietly threw away
                    // the video just played and everything behind it.
                    queue = rebuildQueue(it.queue + nextItems),
                    isLoadingRecommendations = false,
                )
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
            _uiState.update {
                it.copy(
                    recommendations = relatedItems,
                    // Behind what is already queued, for the same reason: this list is
                    // the session's, and where it comes from must not decide its order.
                    queue = rebuildQueue(it.queue + relatedItems),
                    isLoadingRecommendations = false,
                )
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
                val fresh = items.filter { it.videoId !in existing }
                current.copy(
                    recommendations = current.recommendations + fresh,
                    // A page arriving later must not disturb what is already queued, so
                    // it is only added where it is not already in the list.
                    queue = rebuildQueue(
                        candidates = current.queue.filterNot { it.videoId == current.session?.videoId } + fresh,
                    ),
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
        val currentId = _uiState.value.session?.videoId

        // Repeat one means repeat this. Asking the player for its "next" item under
        // repeat one returns the item it is already playing, so skipping replayed the
        // same video instead of moving on - which is what the listener had asked it
        // to do. It is taken as the mode it is, not as a next item.
        if (exo != null && exo.repeatMode == Player.REPEAT_MODE_ONE) {
            exo.seekTo(0L)
            exo.play()
            return
        }

        // If a different video is queued on the player itself, moving onto it is a
        // seek rather than a load. Asked of the service, which owns the playlist and
        // knows what its ids mean - a merged source is two halves of one video, and
        // reading that here is how "the next item" came to be the video already
        // playing.
        val service = sharedConnectionRef?.get()?.service
        if (service?.seekToQueuedVideo(next = true) == true) return
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return
        // From just after the one playing, then around. Not "anything that is not this
        // one": the queue keeps what it has already played, so the first entry that
        // merely differs from the current video is usually the one *behind* it - which
        // is how pressing next went backwards, or nowhere at all.
        val from = queue.indexOfFirst { it.videoId == currentId }
        val nextItem = (
            queue.drop(from + 1) + queue.take(maxOf(from, 0))
            ).firstOrNull { it.videoId != currentId && it.videoId !in playedVideoIds }
            ?: return
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
        // Under repeat one the previous item is the one already playing, so going
        // there again is the same non-answer as it was for next: it read as the
        // button doing nothing. Restarts the video instead, which is what going
        // back from the start of a track has always meant everywhere else.
        if (exo != null && exo.repeatMode == Player.REPEAT_MODE_ONE) {
            exo.seekTo(0L)
            exo.play()
            return
        }
        if (sharedConnectionRef?.get()?.service?.seekToQueuedVideo(next = false) == true) {
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
            // Music videos only, for the same reason as saves: the account's library
            // should mirror this shelf, not every video opened from it.
            if (MusicContentFilter.isMusic(session.videoId)) {
                YouTube.likeVideo(session.videoId, newLiked)
            }
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
                        thumbnailUrl = session.videoThumbnail,
                channelAvatarUrl = session.channelAvatarUrl ?: session.channelThumbnail,
                        lastPlayedAt = System.currentTimeMillis(),
                    ),
                )
            }
            ctx.dataStore.edit { it[LikedVideosKey] = buildVideoHistoryJson(current.take(MAX_VIDEO_HISTORY_ENTRIES)) }
            markVideoLiked(session.videoId, liked)
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

    /**
     * Subscribes to (or unsubscribes from) a channel credited on the current video.
     *
     * [channelId] picks a specific credited channel - a collaboration credits two, and
     * each has to be followed in its own right. Flipped before the request goes out and
     * put back if it is rejected, because a control that only moves on success reads as
     * a dead button when the account cannot be reached.
     */
    fun toggleSubscribe(channelId: String? = null) {
        val session = _uiState.value.session ?: return
        val target = channelId ?: session.channelId ?: return
        val credited = session.channels.firstOrNull { it.id == target }
        val current = if (target == session.channelId) {
            _uiState.value.isSubscribed
        } else {
            isChannelSubscribed(target)
        }
        val newSubscribed = !current

        if (target == session.channelId) {
            _uiState.update { it.copy(isSubscribed = newSubscribed) }
        }
        markChannelSubscribed(target, newSubscribed)
        scope.launch {
            persistChannelSubscription(
                channelId = target,
                name = credited?.name ?: session.channelName,
                avatarUrl = credited?.avatarUrl ?: session.channelAvatarUrl ?: session.channelThumbnail,
                subscribe = newSubscribed,
            )
            val result = YouTube.subscribeChannel(target, newSubscribed)
            if (result.isFailure) {
                markChannelSubscribed(target, current)
                if (target == session.channelId) {
                    _uiState.update { it.copy(isSubscribed = current) }
                }
                persistChannelSubscription(
                    channelId = target,
                    name = credited?.name ?: session.channelName,
                    avatarUrl = credited?.avatarUrl ?: session.channelAvatarUrl ?: session.channelThumbnail,
                    subscribe = current,
                )
                subscribeErrorMessage = result.exceptionOrNull()?.message
            }
        }
    }

    /**
     * Pulls the account's Watch Later queue into the local Saved shelf.
     *
     * Called once after signing in. Only music videos are imported - this side is a
     * music shelf - and nothing already saved is removed, so the merge can only ever
     * add. Runs off the interaction path and never blocks it.
     */
    suspend fun syncSavedVideosFromAccount(context: Context) {
        if (!YouTube.isLoggedIn()) return
        withContext(Dispatchers.IO) {
            val page = YouTube.playlist(YOUTUBE_WATCH_LATER_ID).getOrNull() ?: return@withContext
            val candidates = page.songs
                .mapNotNull { it.id }
                .filter { it.isNotBlank() }
            if (candidates.isEmpty()) return@withContext

            // The filter is cached, so a queue already seen costs nothing here.
            val musicVideos = candidates.filter { MusicContentFilter.isMusic(it) }
            if (musicVideos.isEmpty()) return@withContext

            try {
                context.dataStore.edit { settings ->
                    val saved = settings[SavedVideoIdsKey]?.toMutableSet() ?: mutableSetOf()
                    saved.addAll(musicVideos)
                    settings[SavedVideoIdsKey] = saved
                }
            } catch (e: Exception) {
                // Best effort: the shelf keeps what it already had.
            }
        }
    }

    /** Why the last subscribe attempt failed, if it did. */
    @Volatile var subscribeErrorMessage: String? = null

    fun toggleSave() {
        val session = _uiState.value.session ?: return
        val newSaved = !_uiState.value.isSaved
        val ctx = currentContext ?: return
        // Flipped before the call: the shelf and the button follow immediately, and the
        // account call is the slow part.
        _uiState.update { it.copy(isSaved = newSaved) }
        scope.launch {
            // Local first, so the toggle is instant and survives whatever happens to the
            // account call below.
            ctx.dataStore.edit { settings ->
                val saved = settings[SavedVideoIdsKey]?.toMutableSet() ?: mutableSetOf()
                if (newSaved) saved.add(session.videoId) else saved.remove(session.videoId)
                settings[SavedVideoIdsKey] = saved
            }
            writeSavedVideo(session, newSaved)
            syncSavedVideoToAccount(session.videoId, newSaved)
        }
    }

    /**
     * Mirrors a save into the account's Watch Later queue.
     *
     * Save and Watch Later are the same idea on two sides, so a video saved here should
     * also be waiting on YouTube - and a video already there should not be added twice.
     * Signed out, nothing is sent and nothing is lost: the local shelf still has it, and
     * [syncSavedVideosFromAccount] picks it up when an account is there.
     */
    private suspend fun syncSavedVideoToAccount(videoId: String, saved: Boolean) {
        if (!YouTube.isLoggedIn()) return
        // Only music videos are mirrored to the account. The shelves on this side are a
        // music shelf, so a vlog or a gaming clip saved there is the user's own business
        // and has no place in their YouTube Watch Later.
        if (!MusicContentFilter.isMusic(videoId)) return
        runCatching {
            if (saved) {
                YouTube.addToPlaylist(YOUTUBE_WATCH_LATER_ID, videoId)
            } else {
                // Removing needs the set-video id of the entry in the queue, which only
                // the queue itself knows.
                val entry = YouTube.playlist(YOUTUBE_WATCH_LATER_ID).getOrNull()
                    ?.songs?.firstOrNull { it.id == videoId }
                entry?.setVideoId?.let { YouTube.removeFromPlaylist(YOUTUBE_WATCH_LATER_ID, videoId, it) }
            }
        }.onFailure { Timber.tag("VideoPlaybackManager").w(it, "Watch Later sync failed for %s", videoId) }
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
                        thumbnailUrl = session.videoThumbnail,
                channelAvatarUrl = session.channelAvatarUrl ?: session.channelThumbnail,
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
    fun setVideoQuality(quality: VideoQuality) = setVideoFormat(quality, currentCodec)

    /**
     * Changes resolution and codec together, since both are answered by the same stream
     * lookup and re-resolving for one of them alone would only be half a change.
     */
    fun setVideoFormat(quality: VideoQuality, codec: VideoCodec) {
        val session = _uiState.value.session ?: return
        val exo = player ?: return
        currentQuality = quality
        currentCodec = codec
        AuraPlayerUtils.setPreferredVideoFormat(quality, codec)
        val positionMs = exo.currentPosition
        _uiState.update { it.copy(videoQuality = quality, videoCodec = codec) }
        currentContext?.let { ctx ->
            scope.launch {
                ctx.dataStore.edit {
                    it[VideoQualityKey] = quality.name
                    it[VideoCodecKey] = codec.name
                }
            }
            scope.launch {
                val source = withContext(Dispatchers.IO) {
                    // Passed explicitly rather than read back from the global preference,
                    // which another caller may have changed while this was in flight.
                    AuraPlayerUtils.getVideoStreamSource(session.videoId, quality, codec).getOrNull()
                }
                // The user may have switched video (or quality) while we resolved.
                if (_uiState.value.session?.videoId != session.videoId ||
                    _uiState.value.videoQuality != quality ||
                    _uiState.value.videoCodec != codec
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
                    // The video already on the player has its own tracks attached; a
                    // fresh copy would drop them for the sake of a resolution change.
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
            // playing. The service owns the playlist; this only decides that it should
            // no longer describe one.
            queuedAheadVideoId = null
            sharedConnectionRef?.get()?.service?.clearVideoPlaylist()
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
        // Asked of the service, which owns the playlist and matches on the video
        // rather than on the item's id - the two halves of a merged source do not
        // carry it, so matching on the id removed nothing and the video was reached
        // again a moment later as though it had never been taken out.
        sharedConnectionRef?.get()?.service?.removeQueuedVideo(videoId)
    }

    /** Clears the pending playback queue (Up next stays visible). */
    fun clearQueue() {
        val currentId = _uiState.value.session?.videoId
        val current = _uiState.value.queue.firstOrNull { it.videoId == currentId }
        // What is playing stays on: a queue emptied from under the video on screen reads
        // as though playback has stopped, and the next thing to play would be nothing.
        _uiState.update { it.copy(queue = listOfNotNull(current)) }
        queuedAheadVideoId = null
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
        val item = video.toRecommendationItem()
        val state = _uiState.value
        // Already playing, already queued, or already watched: putting it back does
        // nothing except make the same video turn up twice in a row.
        if (item.videoId == state.session?.videoId) return
        if (state.queue.any { it.videoId == item.videoId }) return
        _uiState.update { state ->
            val existing = state.recommendations.any { it.videoId == item.videoId }
            state.copy(
                recommendations = if (existing) state.recommendations else listOf(item) + state.recommendations,
                // Straight after the video on screen, which rebuildQueue keeps on top.
                queue = rebuildQueue(listOf(item) + state.queue),
            )
        }
        if (_uiState.value.queue.any { it.videoId == item.videoId }) {
            queuedAheadVideoId = null
            queueUpcomingVideo()
        }
    }

    /** Appends a feed/list video to the end of the playback queue. */
    fun addToQueue(video: YouTubeVideoItem) {
        val item = video.toRecommendationItem()
        _uiState.update { state ->
            if (state.session?.videoId == item.videoId) return@update state
            if (state.queue.any { it.videoId == item.videoId }) return@update state
            state.copy(
                recommendations = if (state.recommendations.any { it.videoId == item.videoId }) {
                    state.recommendations
                } else {
                    state.recommendations + item
                },
                queue = rebuildQueue(state.queue + item),
            )
        }
        queuedAheadVideoId = null
        queueUpcomingVideo()
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
     * Steps back out of a video that is filling the window, whether that was full screen
     * or the pushed-down zoom.
     *
     * Back from either of those returns to the player rather than out of it: the surface
     * was grown deliberately, and leaving the video entirely would skip the state in
     * between. One step out, not two - which is what pressing back twice used to take.
     */
    fun exitFullScreen() {
        val state = _uiState.value
        if (!state.isFullScreen && !state.zoomMode) return
        _uiState.update { it.copy(isFullScreen = false, zoomMode = false) }
        applyOrientation(false)
    }

    /**
     * Grows the video surface to the whole screen while the video keeps its own aspect
     * and sits centred, so a portrait video stays portrait instead of being cropped or
     * stretched to fill.
     */
    fun toggleZoomMode() {
        val zoom = !_uiState.value.zoomMode
        _uiState.update { it.copy(zoomMode = zoom) }
        if (zoom) applyOrientation(false)
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
        // playedVideoIds deliberately survives this: the queue belongs to the listener
        // rather than to the window, and forgetting what has been watched when the
        // overlay is closed is what let the same videos come round again.
        queuedAheadVideoId = null
        // Closed on purpose: the video should not be waiting in the corner the next
        // time the app opens. Music (if the listener has any) takes the cold start.
        markLastPlayedMediaKind(MEDIA_KIND_MUSIC)
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
        currentCodec = VideoCodec.fromName(context.dataStore.get(VideoCodecKey, VideoCodec.CODEC_AUTO.name))
        currentAutoplay = context.dataStore.get(VideoAutoplayEnabledKey, true)
        AuraPlayerUtils.setPreferredVideoFormat(currentQuality, currentCodec)
        _uiState.update {
            it.copy(
                videoQuality = currentQuality,
                videoCodec = currentCodec,
                autoplayEnabled = currentAutoplay,
            )
        }
    }

    /**
     * The one way a video reaches the player.
     *
     * Resolve the stream, build the item, hand it over - and if any of that
     * cannot be done, say so. Every part of this used to be able to give up in
     * silence from four different places, and each one left the same thing behind: a
     * video with its details loaded, both ends of it at zero, nothing moving, and no
     * error anywhere. Giving up is still allowed here, but only when a later load has
     * taken over - in which case that one is answerable for the result.
     */
    private suspend fun runVideoLoad(
        context: Context,
        generation: Int,
        videoId: String,
        title: String,
        channelName: String,
        channelThumbnail: String?,
        startPositionMs: Long,
        startPaused: Boolean = false,
    ) {
        // True once a later load owns the session. This load's answer is no longer the
        // one on screen, so it has nothing to report.
        fun superseded() =
            generation != loadGeneration || _uiState.value.session?.videoId != videoId

        val service = sharedConnectionRef?.get()?.service
        if (service == null) {
            failLoad("Playback is not available", videoId, generation)
            return
        }

        // At the same time as nothing else, and waiting on nothing but itself.
        //
        // Captions used to sit in front of the picture here, and that is where the
        // video stopped starting: they are fetched a track at a time, each a round
        // trip, so every video waited behind a full caption download before a frame
        // appeared - and a slow caption server looked exactly like a video that would
        // not play. Loading a video now does the one thing it is for, and reaches the
        // player as soon as the stream is there. Captions are a separate concern with
        // their own pipeline, and that is where they belong.
        val source = withContext(Dispatchers.IO) {
            runCatching { AuraPlayerUtils.getVideoStreamSource(videoId).getOrNull() }.getOrNull()
        }
        if (superseded()) return
        if (source == null) {
            failLoad("Could not load this video", videoId, generation)
            return
        }

        loadMediaSourceInto(
            videoId = videoId,
            source = source,
            title = title,
            channelName = channelName,
            channelThumbnail = channelThumbnail,
            startPositionMs = startPositionMs,
            playWhenReady = !startPaused,
        )
    }

    /**
     * Records a load that could not be completed.
     *
     * The point of it is that the player stops saying it is playing. Without this a
     * load that fails looks exactly like one that is merely slow, which is how it was
     * mistaken for one for so long.
     */
    private fun failLoad(message: String, videoId: String, generation: Int) {
        if (generation != loadGeneration || _uiState.value.session?.videoId != videoId) return
        player?.pause()
        _uiState.update {
            it.copy(isPlaying = false, isBuffering = false, error = message)
        }
    }

    /**
     * Takes the outgoing video off the player.
     *
     * Done at the moment a new video is chosen rather than when its source is ready,
     * which is where it belonged - the old one is not wanted from the moment another
     * is chosen, and leaving it there meant it played on, audibly, through the whole
     * wait for the next.
     */
    private fun stopOutgoingVideo() {
        val service = sharedConnectionRef?.get()?.service ?: return
        if (!service.guestVideoActive.value) return
        player?.pause()
        _positionState.value = 0L to 0L
        _uiState.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    /**
     * Whether videos are queued onto the player's playlist.
     *
     * Never. The queue has one owner and that is this class; the playlist is the
     * player's, and a second copy of the queue on it can disagree with the first.
     *
     * It was a television-and-telephone difference once - a television kept its list
     * here and reached it from the player screen, while a telephone had no queue panel
     * and let the playlist carry it. The telephone's copy is the one that went wrong,
     * so the difference is gone and the answer is the same everywhere.
     *
     * What it costs is the end of a video being a load rather than a seek. That is
     * what a television has always done, and it works.
     */
    private fun queuesVideosOnPlayer(context: Context?): Boolean = false

    /**
     * Builds the [MergingMediaSource-like] source for [source], sets it on the
     * player, prepares, and replays — also rebuilding the media notification.
     * Used both for initial load and when the stream must be re-resolved
     * (e.g. quality change mid-playback).
     */
    private suspend fun loadMediaSourceInto(
        videoId: String,
        source: com.auramusic.auravideo.AuraVideo.VideoStreamSource,
        title: String,
        channelName: String,
        channelThumbnail: String?,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = true,
    ) {
        val service = sharedConnectionRef?.get()?.service ?: return
        // Another load has taken over since this one was checked. It is answerable for
        // the result, so there is nothing here to say.
        if (_uiState.value.session?.videoId != videoId) return
        val context = currentContext ?: return
        val mediaSource = buildMediaSource(
            context,
            videoId,
            source,
            title = title,
            channelName = channelName,
            channelThumbnail = channelThumbnail,
        )
        // The shared player swaps to the video as a guest: the music queue is snapshotted
        // on the first takeover and the session's notification shows the video's metadata.
        service.playGuestVideoSource(mediaSource, startPositionMs, playWhenReady)
        // A fresh single-item playlist replaced whatever the lookahead had queued, so
        // what is ahead on the player is no longer described by it either.
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
                thumbnailUrl = session.videoThumbnail,
                channelAvatarUrl = session.channelAvatarUrl ?: session.channelThumbnail,
                lastPlayedAt = System.currentTimeMillis(),
                positionMs = _positionState.value.first,
                durationMs = _positionState.value.second,
            )
        )
    }

    /**
     * Closes the current video out as fully watched, so it drops out of Continue Watching
     * and its card bar reads as finished instead of "almost done".
     */
    private fun markCurrentWatched() {
        val session = _uiState.value.session ?: return
        val duration = _positionState.value.second
            .takeIf { it > 0 }
            ?: _uiState.value.durationMs
        if (duration <= 0) return
        recordHistoryEntry(
            VideoHistoryEntry(
                videoId = session.videoId,
                title = session.title,
                channelName = session.channelName,
                channelId = session.channelId,
                thumbnailUrl = session.videoThumbnail,
                channelAvatarUrl = session.channelAvatarUrl ?: session.channelThumbnail,
                lastPlayedAt = System.currentTimeMillis(),
                positionMs = duration,
                durationMs = duration,
            ),
            refreshSections = true,
        )
    }

    private fun recordHistoryEntry(entry: VideoHistoryEntry, refreshSections: Boolean = false) {
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
            if (refreshSections) {
                // A video just finished: rebuild Continue Watching so the finished row is
                // gone instead of sitting there with a full bar waiting to be resumed.
                VideoRecommendationManager.invalidateSections()
                onVideoPlayed?.invoke()
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

        // The same declaration every other media item in the app makes, and the one
        // the whole app reads back through [com.auramusic.app.extensions.metadata].
        //
        // A video that carried only the player-facing metadata above was, to
        // everything that reads a media item's tag, an unnamed item - which is why a
        // playing video reported "no song playing" in the player, left the top bar
        // with nothing to draw, and had no details to show. The media source was
        // right the whole time; nothing was ever asked what it was.
        val appMetadata = com.auramusic.app.models.MediaMetadata(
            id = videoId,
            title = title.ifBlank { videoId },
            artists = listOfNotNull(
                channelName.takeIf { it.isNotBlank() }
                    ?.let { com.auramusic.app.models.MediaMetadata.Artist(null, it) },
            ),
            duration = 0,
            // The video's own picture. Only a channel avatar was on hand here, which
            // is a profile shot rather than the thing being watched, so the video's
            // own artwork stands in whenever there is one.
            thumbnailUrl = channelThumbnail?.takeIf { !it.isChannelAvatarUrl() }
                ?: VideoThumbnails.highQuality(videoId),
        )

        return when (source) {
            is com.auramusic.auravideo.AuraVideo.VideoStreamSource.Single -> {
                val mediaItem = MediaItem.Builder()
                    .setUri(source.url)
                    .setMimeType(source.mimeType)
                    .setMediaId(videoId)
                    .setTag(appMetadata)
                    .setMediaMetadata(mediaMetadata)
                    .build()
                factory.createMediaSource(mediaItem)
            }
            is com.auramusic.auravideo.AuraVideo.VideoStreamSource.Merged -> {
                val videoMediaItem = MediaItem.Builder()
                    .setUri(source.videoUrl)
                    .setMimeType(source.videoMimeType)
                    .setMediaId(videoId + "_v")
                    .setTag(appMetadata)
                    .setMediaMetadata(mediaMetadata)
                    .build()
                val videoSource = factory.createMediaSource(videoMediaItem)
                val audioMediaItem = MediaItem.Builder()
                    .setUri(source.audioUrl)
                    .setMimeType(source.audioMimeType)
                    .setMediaId(videoId + "_a")
                    .setTag(appMetadata)
                    .setMediaMetadata(mediaMetadata)
                    .build()
                val audioSource = factory.createMediaSource(audioMediaItem)
                MergingMediaSource(true, true, videoSource, audioSource)
            }
        }
    }
}

private const val MAX_VIDEO_HISTORY_ENTRIES = 100

/** How many played videos the queue remembers.
 *
 * Enough that a session cannot come round to the same recommendations twice over,
 * bounded so a long evening of videos does not grow a set without end.
 */
private const val MAX_REMEMBERED_PLAYED = 300


/**
 * Channel avatars are served from these hosts. Used to tell an artwork URL that is
 * actually a channel avatar apart from a real video thumbnail.
 */
private fun String.isChannelAvatarUrl(): Boolean =
    contains("yt3.googleusercontent.com") ||
        contains("yt3.ggpht.com") ||
        contains("ytimg.com/a/")

/**
 * Watch fractions shared by every surface that draws a watch bar or decides whether a
 * video resumes: below [RESUME_MIN_PROGRESS] a tap is a fresh start (the user only nudged
 * the playhead), at or above [WATCHED_PROGRESS] the video counts as watched.
 */
const val RESUME_MIN_PROGRESS = 0.02f
const val WATCHED_PROGRESS = 0.95f
/** Short videos are not worth resuming: under this a pause looks like a completed watch. */
const val MIN_RESUMABLE_DURATION_MS = 15_000L

/** How often playback progress is published while the playhead is actually moving. */
private const val ACTIVE_TICK_MS = 500L

/** YouTube's built-in Watch Later queue, and the other side of a saved video. */
private const val YOUTUBE_WATCH_LATER_ID = "WL"

/** How often the ticker wakes when there is nothing on screen to redraw for it. */
private const val IDLE_TICK_MS = 1_000L

/**
 * How long a cold start waits for the music service to put its persisted queue on
 * the shared player before restoring a video over it.
 */
private const val QUEUE_RESTORE_GRACE_MS = 2_000L

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
                    channelAvatarUrl = obj.optString("ca").takeIf { it.isNotEmpty() },
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
                put("ca", entry.channelAvatarUrl ?: "")
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
