/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ForegroundServiceStartNotAllowedException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.auramusic.app.MainActivity
import com.auramusic.app.R
import com.auramusic.app.constants.MediaSessionConstants
import com.auramusic.app.constants.MediaSessionConstants.CommandToggleLike
import com.auramusic.app.utils.CoilBitmapLoader
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import timber.log.Timber

/**
 * Foreground media service that surfaces a MediaSession + notification for the
 * in-app video player, mirroring the music player's notification behavior so
 * videos keep showing playback controls in the notification panel.
 */
class VideoPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var notificationProvider: DefaultMediaNotificationProvider? = null
    private var latestMediaNotification: Notification? = null
    private var scope = CoroutineScope(Dispatchers.Main + Job())

    /**
     * Listener attached to the underlying ExoPlayer that explicitly triggers
     * notification rebuilds whenever the media item changes or playback state
     * shifts. Without this, MediaSessionService's auto-update path can miss
     * state transitions through the ForwardingPlayer wrapper, leaving the
     * notification stuck on the initial placeholder (title/artist only, no
     * artwork or transport controls).
     */
    private val playerNotificationListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            triggerNotificationUpdate()
            updateCustomLayout()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                triggerNotificationUpdate()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            triggerNotificationUpdate()
        }

        override fun onPlayerError(error: PlaybackException) {
            triggerNotificationUpdate()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            configureService()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "VideoPlaybackService.onCreate failed")
            stopSelf()
        }
    }

    private fun configureService() {
        val exo = VideoPlaybackManager.playerOrNull()
        if (exo == null) {
            stopSelf()
            return
        }

        // Attach a listener that forces notification rebuilds on every meaningful
        // player state change. This is the critical link that makes the media
        // notification show artwork + transport controls instead of just text.
        exo.addListener(playerNotificationListener)

        setListener(
            object : MediaSessionService.Listener {
                override fun onForegroundServiceStartNotAllowedException() {
                    Timber.w("VideoPlaybackService: FGS start not allowed, re-promoting")
                    promoteToForegroundWithLatestNotification()
                }
            },
        )

        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.video_player),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to create notification channel")
        }

        // Media title/artist/artwork live on each MediaItem's MediaMetadata (set when the
        // media source is built), so the session can render the full media notification.
        // The session ID must be unique per process; without an explicit ID the default
        // collides with another live session (e.g. after an abnormal teardown), crashing
        // onCreate with "Session ID must be unique". If a stale session still lingers,
        // retry with a per-instance unique ID so the service always comes up.
        mediaSession = try {
            buildSession(exo, SESSION_ID)
        } catch (e: IllegalStateException) {
            Timber.tag(TAG).w(e, "MediaSession ID collision, retrying with unique ID")
            buildSession(exo, "$SESSION_ID-${System.nanoTime()}")
        }

        // Keep a connected controller so the media notification with transport
        // controls and artwork is rendered and updated, mirroring MusicService.
        try {
            val sessionToken = SessionToken(this, ComponentName(this, VideoPlaybackService::class.java))
            val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
            controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to connect MediaController")
        }

        notificationProvider = DefaultMediaNotificationProvider(
            this,
            { NOTIFICATION_ID },
            CHANNEL_ID,
            R.string.video_player,
        ).apply {
            setSmallIcon(R.drawable.ic_notification_icon)
        }

        setMediaNotificationProvider(
            object : MediaNotification.Provider {
                override fun createNotification(
                    mediaSession: MediaSession,
                    mediaButtonPreferences: ImmutableList<CommandButton>,
                    actionFactory: MediaNotification.ActionFactory,
                    onNotificationChangedCallback: MediaNotification.Provider.Callback,
                ): MediaNotification {
                    val trackingCallback = MediaNotification.Provider.Callback { notification ->
                        latestMediaNotification = notification.notification
                        onNotificationChangedCallback.onNotificationChanged(notification)
                    }
                    return notificationProvider
                        ?.createNotification(
                            mediaSession,
                            mediaButtonPreferences,
                            actionFactory,
                            trackingCallback,
                        )
                        ?: MediaNotification(
                            NOTIFICATION_ID,
                            buildPlaceholderNotification(),
                        )
                }

                override fun handleCustomCommand(
                    session: MediaSession,
                    action: String,
                    extras: Bundle,
                ): Boolean = notificationProvider?.handleCustomCommand(session, action, extras) == true

                override fun getNotificationChannelInfo() = notificationProvider?.notificationChannelInfo
                    ?: MediaNotification.Provider.NotificationChannelInfo(
                        CHANNEL_ID,
                        getString(R.string.video_player),
                    )
            },
        )

        // Surface a notification immediately (even before media3 renders the media
        // controls on the first playable frame) so the video always appears in the
        // notification panel, exactly like the music player does.
        promoteToForegroundWithLatestNotification()

        // Seed the custom layout (like button etc.) so the notification renders
        // custom action buttons from the very first frame.
        updateCustomLayout()
    }

    private fun buildSession(exo: ExoPlayer, sessionId: String): MediaSession {
        val session = MediaSession.Builder(this, MediaControlsPlayer(exo))
            .setId(sessionId)
            .setBitmapLoader(CoilBitmapLoader(this, scope))
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()

        // Declare which transport actions the notification should display.
        // Without this, the notification may only show a subset of controls
        // or fall back to a text-only layout on some Android versions.
        session.setMediaButtonPreferences(
            ImmutableList.of(
                CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build(),
                CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .build(),
                CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build(),
            ),
        )

        return session
    }

    /**
     * Exposes seek-to-next/previous in the MediaSession so the media notification
     * always shows skip buttons for the up-next queue (a fresh video plays on a
     * single-item timeline, which by itself gives the notification no skip button).
     */
    @OptIn(UnstableApi::class)
    private inner class MediaControlsPlayer(delegate: ExoPlayer) : ForwardingPlayer(delegate) {
        private val queueCommands: Player.Commands =
            super.getAvailableCommands()
                .buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()

        override fun getAvailableCommands(): Player.Commands = queueCommands

        override fun isCommandAvailable(command: Int): Boolean = queueCommands.contains(command)

        /**
         * Surface the current video's title/artist/artwork on the MediaSession even
         * before the underlying player has loaded a MediaItem (and its metadata), so
         * the media notification always renders the video details + transport
         * controls instead of falling back to a plain placeholder.
         */
        override fun getMediaMetadata(): MediaMetadata {
            val session = VideoPlaybackManager.uiState.value.session
                ?: return super.getMediaMetadata()
            val thumbnailUrl = session.channelThumbnail?.takeIf { it.isNotBlank() }
            return super.getMediaMetadata()
                .buildUpon()
                .setTitle(session.title)
                .setArtist(session.channelName.ifBlank { null })
                .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
                .build()
        }

        override fun seekToNext() = VideoPlaybackManager.playNext()

        override fun seekToNextMediaItem() = VideoPlaybackManager.playNext()

        override fun seekToPrevious() = VideoPlaybackManager.playPrevious()

        override fun seekToPreviousMediaItem() = VideoPlaybackManager.playPrevious()

        override fun hasNextMediaItem(): Boolean =
            VideoPlaybackManager.uiState.value.queue.any {
                it.videoId != VideoPlaybackManager.uiState.value.session?.videoId
            }

        override fun hasPreviousMediaItem(): Boolean = false
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForegroundWithLatestNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            handleForegroundServiceStartNotAllowed(e)
        } catch (e: IllegalStateException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e.javaClass.name == ForegroundServiceStartNotAllowedException::class.java.name
            ) {
                handleForegroundServiceStartNotAllowed(e)
            } else {
                throw e
            }
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "onUpdateNotification: suppressed SecurityException from FGS path")
        }
    }

    override fun onDestroy() {
        // Detach the player listener to prevent leaked callbacks after teardown.
        VideoPlaybackManager.playerOrNull()?.removeListener(playerNotificationListener)
        try {
            mediaSession?.release()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "onDestroy: session release failed")
        }
        mediaSession = null
        super.onDestroy()
    }

    /**
     * Explicitly triggers a notification rebuild via the MediaSessionService.
     * The DefaultMediaNotificationProvider reads the session's current
     * MediaMetadata (title, artist, artwork) and renders a full MediaStyle
     * notification with artwork, transport controls, and custom buttons.
     */
    private fun triggerNotificationUpdate() {
        val session = mediaSession ?: return
        try {
            onUpdateNotification(session, true)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "triggerNotificationUpdate failed")
        }
    }

    /**
     * Sets a custom notification layout with action buttons (like), mirroring
     * how MusicService decorates its media notification. The custom layout
     * ensures the notification shows more than just the default transport
     * controls — it adds app-specific actions like the like/unlike toggle.
     */
    private fun updateCustomLayout() {
        val session = mediaSession ?: return
        val isLiked = VideoPlaybackManager.uiState.value.isLiked
        session.setCustomLayout(
            listOf(
                CommandButton.Builder()
                    .setDisplayName(
                        getString(
                            if (isLiked) R.string.action_remove_like
                            else R.string.action_like
                        ),
                    )
                    .setIconResId(
                        if (isLiked) R.drawable.ic_heart
                        else R.drawable.ic_heart_outline
                    )
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(VideoPlaybackManager.uiState.value.session != null)
                    .build(),
            ),
        )
    }

    private fun handleForegroundServiceStartNotAllowed(error: Throwable?) {
        Timber.tag(TAG).w(error, "Foreground service start denied during notification update")
        promoteToForegroundWithLatestNotification()
    }

    private fun promoteToForegroundWithLatestNotification() {
        val notification = latestMediaNotification ?: buildPlaceholderNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Timber.tag(TAG).w(e, "startForeground: FGS start not allowed")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "startForeground: failed")
        }
    }

    private fun buildPlaceholderNotification(): Notification {
        val session = VideoPlaybackManager.uiState.value.session
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(session?.title?.ifBlank { null } ?: getString(R.string.video_player))
            .setContentText(session?.channelName.orEmpty())
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    1,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }

    companion object {
        /**
         * Force a full notification rebuild from outside the service (e.g. after
         * VideoPlaybackManager enriches session metadata with artwork URL).
         */
        fun notifySessionChanged(context: Context) {
            // The service is already running; triggering an onStartCommand with
            // no special action causes onUpdateNotification to fire, which
            // rebuilds the notification with the latest MediaMetadata.
            try {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, VideoPlaybackService::class.java),
                )
            } catch (_: Exception) { /* service may not be started */ }
        }
        private const val TAG = "VideoPlaybackService"
        const val SESSION_ID = "aura_video_playback"
        const val CHANNEL_ID = "video_channel_01"
        const val NOTIFICATION_ID = 889

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, VideoPlaybackService::class.java),
            )
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, VideoPlaybackService::class.java),
            )
        }
    }
}