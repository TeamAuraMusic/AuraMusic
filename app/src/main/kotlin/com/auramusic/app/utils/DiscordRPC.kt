/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import android.content.Context
import com.auramusic.app.R
import com.auramusic.app.db.entities.Song
import com.auramusic.kizzy.rpc.KizzyRPC
import com.auramusic.kizzy.rpc.RpcImage

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(
    token = token,
    os = "Android",
    browser = "Discord Android",
    device = android.os.Build.DEVICE,
    userAgent = SuperProperties.userAgent,
    superPropertiesBase64 = SuperProperties.superPropertiesBase64
) {
    suspend fun updateSong(song: Song, currentPlaybackTimeMillis: Long, playbackSpeed: Float = 1.0f, useDetails: Boolean = false) = runCatching {
        val currentTime = System.currentTimeMillis()

        val safeSpeed = playbackSpeed.takeIf { it > 0f } ?: 1.0f
        val adjustedPlaybackTime = (currentPlaybackTimeMillis / safeSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime

        val songTitleWithRate = if (safeSpeed != 1.0f) {
            "${song.song.title} [${String.format("%.2fx", safeSpeed)}]"
        } else {
            song.song.title
        }

        val endTime = song.song.duration
            .takeIf { it > 0 }
            ?.times(1000L)
            ?.let { durationMillis -> durationMillis - currentPlaybackTimeMillis }
            ?.takeIf { it > 0 }
            ?.let { remainingDuration -> currentTime + (remainingDuration / safeSpeed).toLong() }

        setActivity(
            name = context.getString(R.string.app_name).removeSuffix(" Debug"),
            details = songTitleWithRate,
            state = song.artists.joinToString { it.name },
            detailsUrl = "https://music.youtube.com/watch?v=${song.song.id}",
            largeImage = song.song.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            smallImage = song.artists.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            largeText = song.album?.title,
            smallText = song.artists.firstOrNull()?.name,
            buttons = listOf(
                "Listen on YouTube Music" to "https://music.youtube.com/watch?v=${song.song.id}",
                "Visit AuraMusic" to "https://github.com/TeamAuraMusic/AuraMusic"
            ),
            type = Type.LISTENING,
            statusDisplayType = if (useDetails) StatusDisplayType.DETAILS else StatusDisplayType.STATE,
            startTime = calculatedStartTime,
            endTime = endTime,
            applicationId = APPLICATION_ID
        )
    }

    override suspend fun close() {
        super.close()
    }
    companion object {
        private const val APPLICATION_ID = "1411019391843172514"
    }
}
