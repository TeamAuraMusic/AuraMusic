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
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSessionService
import com.auramusic.app.MainActivity
import com.auramusic.app.R
import com.google.common.collect.ImmutableList
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

    override fun onCreate() {
        super.onCreate()

        val exo = VideoPlaybackManager.playerOrNull()
        if (exo == null) {
            stopSelf()
            return
        }

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
    }

    private fun buildSession(exo: ExoPlayer, sessionId: String): MediaSession =
        MediaSession.Builder(this, exo)
            .setId(sessionId)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()

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
        try {
            mediaSession?.release()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "onDestroy: session release failed")
        }
        mediaSession = null
        super.onDestroy()
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

    private fun buildPlaceholderNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.video_player))
            .setContentText("")
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

    companion object {
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