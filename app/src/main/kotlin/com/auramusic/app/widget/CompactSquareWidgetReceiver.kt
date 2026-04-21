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
import android.widget.RemoteViews
import com.auramusic.app.MainActivity
import com.auramusic.app.R
import com.auramusic.app.playback.MusicService

class CompactSquareWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_COMPACT_SQUARE_UPDATE
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
                val views = RemoteViews(context.packageName, R.layout.widget_compact_square)
                views.setImageViewResource(R.id.widget_compact_play_pause, R.drawable.ic_widget_play)
                views.setOnClickPendingIntent(R.id.widget_compact_album_art, getOpenAppIntent(context))
                views.setOnClickPendingIntent(R.id.widget_compact_play_container, getActionIntent(context, ACTION_COMPACT_SQUARE_PLAY_PAUSE))
                views.setOnClickPendingIntent(R.id.widget_compact_previous, getActionIntent(context, ACTION_COMPACT_SQUARE_PREVIOUS))
                views.setOnClickPendingIntent(R.id.widget_compact_next, getActionIntent(context, ACTION_COMPACT_SQUARE_NEXT))
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_COMPACT_SQUARE_PLAY_PAUSE -> {
                sendAction(context, MusicWidgetReceiver.ACTION_PLAY_PAUSE)
            }
            ACTION_COMPACT_SQUARE_PREVIOUS -> {
                sendAction(context, MusicWidgetReceiver.ACTION_PREVIOUS)
            }
            ACTION_COMPACT_SQUARE_NEXT -> {
                sendAction(context, MusicWidgetReceiver.ACTION_NEXT)
            }
        }
    }

    private fun sendAction(context: Context, action: String) {
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            this.action = action
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

    companion object {
        const val ACTION_COMPACT_SQUARE_PLAY_PAUSE = "com.auramusic.app.widget.COMPACT_SQUARE_PLAY_PAUSE"
        const val ACTION_COMPACT_SQUARE_UPDATE = "com.auramusic.app.widget.COMPACT_SQUARE_UPDATE"
        const val ACTION_COMPACT_SQUARE_PREVIOUS = "com.auramusic.app.widget.COMPACT_SQUARE_PREVIOUS"
        const val ACTION_COMPACT_SQUARE_NEXT = "com.auramusic.app.widget.COMPACT_SQUARE_NEXT"
    }
}