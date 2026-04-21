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

class CompactWideWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_COMPACT_WIDE_UPDATE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
            }
        } else {
            // Show default content when service isn't running
            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_compact_wide)
                views.setTextViewText(R.id.widget_wide_song_title, context.getString(R.string.no_song_playing))
                views.setTextViewText(R.id.widget_wide_artist_name, context.getString(R.string.tap_to_open))
                views.setImageViewResource(R.id.widget_wide_play_pause, R.drawable.ic_widget_play_low)
                views.setImageViewResource(R.id.widget_wide_like_button, R.drawable.ic_widget_heart_outline_nav)
                views.setOnClickPendingIntent(R.id.widget_wide_album_art, getOpenAppIntent(context))
                views.setOnClickPendingIntent(R.id.widget_wide_play_pause, getActionIntent(context, ACTION_COMPACT_WIDE_PLAY_PAUSE))
                views.setOnClickPendingIntent(R.id.widget_wide_like_button, getActionIntent(context, ACTION_COMPACT_WIDE_LIKE))
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
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_COMPACT_WIDE_UPDATE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_COMPACT_WIDE_PLAY_PAUSE, ACTION_COMPACT_WIDE_LIKE -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = when (intent.action) {
                        ACTION_COMPACT_WIDE_PLAY_PAUSE -> MusicWidgetReceiver.ACTION_PLAY_PAUSE
                        ACTION_COMPACT_WIDE_LIKE -> MusicWidgetReceiver.ACTION_LIKE
                        else -> ""
                    }
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    companion object {
        const val ACTION_COMPACT_WIDE_PLAY_PAUSE = "com.auramusic.app.widget.COMPACT_WIDE_PLAY_PAUSE"
        const val ACTION_COMPACT_WIDE_LIKE = "com.auramusic.app.widget.COMPACT_WIDE_LIKE"
        const val ACTION_COMPACT_WIDE_UPDATE = "com.auramusic.app.widget.COMPACT_WIDE_UPDATE"
    }
}