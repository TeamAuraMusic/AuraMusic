/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.auramusic.app.MainActivity
import com.auramusic.app.R
import com.auramusic.app.playback.MusicService

class MusicWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Only trigger update through MusicService if it's already running
        // This prevents BackgroundServiceStartNotAllowedException on Android 14+
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Service might be restricted in background
            }
        } else {
            // Even when service isn't running, show default content when user adds widget
            // This ensures widget displays properly before user opens the app
            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_music_player)
                views.setTextViewText(R.id.widget_song_title, context.getString(R.string.no_song_playing))
                views.setTextViewText(R.id.widget_artist_name, context.getString(R.string.tap_to_open))
                views.setImageViewResource(R.id.widget_play_pause, R.drawable.ic_widget_play)
                views.setImageViewResource(R.id.widget_like_button, R.drawable.ic_widget_heart_outline_nav)
                views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent(context))
                views.setOnClickPendingIntent(R.id.widget_play_pause, getActionIntent(context, ACTION_PLAY_PAUSE))
                views.setOnClickPendingIntent(R.id.widget_previous, getActionIntent(context, ACTION_PREVIOUS))
                views.setOnClickPendingIntent(R.id.widget_next, getActionIntent(context, ACTION_NEXT))
                views.setOnClickPendingIntent(R.id.widget_like_button, getActionIntent(context, ACTION_LIKE))
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun getOpenAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getActionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Trigger widget update when size changes
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Service might be restricted in background
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_LIKE, ACTION_NEXT, ACTION_PREVIOUS -> {
                // User interactions from widget buttons can start the service
                // Android allows starting FGS from widget PendingIntent clicks
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    // Service might be restricted in background
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.auramusic.app.widget.PLAY_PAUSE"
        const val ACTION_LIKE = "com.auramusic.app.widget.LIKE"
        const val ACTION_NEXT = "com.auramusic.app.widget.NEXT"
        const val ACTION_PREVIOUS = "com.auramusic.app.widget.PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "com.auramusic.app.widget.UPDATE_WIDGET"
    }
}
