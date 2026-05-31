/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.auramusic.app.MainActivity
import com.auramusic.app.R
import com.auramusic.app.constants.LastSeenSubscribedReleaseIdsKey
import com.auramusic.app.constants.NewReleaseNotificationsEnabledKey
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.app.utils.reportException
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.AlbumItem
import kotlinx.coroutines.flow.first

object NewReleaseNotificationChecker {
    const val CHANNEL_ID = "new_releases"

    private const val NOTIFICATION_ID = 2304
    private const val MAX_SEEN_RELEASE_IDS = 300

    suspend fun check(context: Context, database: MusicDatabase) {
        if (!context.dataStore.get(NewReleaseNotificationsEnabledKey, true)) return

        val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
        val subscribedIds = subscribedArtists
            .flatMap { artist -> listOfNotNull(artist.id, artist.artist.channelId) }
            .toSet()
        if (subscribedIds.isEmpty()) return

        val matchingAlbums = YouTube.newReleaseAlbums()
            .getOrElse { throwable ->
                reportException(throwable)
                return
            }
            .filter { album ->
                album.artists.orEmpty().any { it.id in subscribedIds }
            }
            .distinctBy { it.id }
        val currentIds = matchingAlbums.map { it.id }.toSet()

        val preferences = context.dataStore.data.first()
        val seenRaw = preferences[LastSeenSubscribedReleaseIdsKey]
        if (seenRaw == null) {
            saveSeenReleaseIds(context, currentIds)
            return
        }

        val seenIds = seenRaw.split(',').filter { it.isNotBlank() }.toSet()
        val newAlbums = matchingAlbums.filter { it.id !in seenIds }
        if (newAlbums.isNotEmpty()) {
            showNotification(context, newAlbums)
        }

        saveSeenReleaseIds(context, currentIds + seenIds)
    }

    private suspend fun saveSeenReleaseIds(context: Context, ids: Set<String>) {
        context.dataStore.edit { settings ->
            settings[LastSeenSubscribedReleaseIdsKey] = ids.take(MAX_SEEN_RELEASE_IDS).joinToString(",")
        }
    }

    private fun showNotification(context: Context, albums: List<AlbumItem>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = if (albums.size == 1) {
            context.getString(R.string.new_release_notification_title_single)
        } else {
            context.getString(R.string.new_release_notification_title_multiple, albums.size)
        }
        val content = albums.firstOrNull()?.let { album ->
            val artist = album.artists.orEmpty().joinToString { it.name }
            if (artist.isBlank()) album.title else "${album.title} • $artist"
        } ?: context.getString(R.string.new_releases)

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    albums.take(5).forEach { album ->
                        val artist = album.artists.orEmpty().joinToString { it.name }
                        style.addLine(if (artist.isBlank()) album.title else "${album.title} • $artist")
                    }
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
