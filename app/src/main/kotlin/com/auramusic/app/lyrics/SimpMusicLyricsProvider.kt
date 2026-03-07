/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.lyrics

import android.content.Context
import com.auramusic.app.constants.EnableSimpMusicKey
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.simpmusic.SimpMusicLyrics

object SimpMusicLyricsProvider : LyricsProvider {
    override val name = "SimpMusic"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSimpMusicKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        setVideoId: String?,
    ): Result<String> {
        // Prefer setVideoId (YouTube video ID) if available, otherwise use id
        val videoId = setVideoId ?: id
        return SimpMusicLyrics.getLyrics(videoId, duration)
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        setVideoId: String?,
        callback: (String) -> Unit,
    ) {
        // Prefer setVideoId (YouTube video ID) if available, otherwise use id
        val videoId = setVideoId ?: id
        SimpMusicLyrics.getAllLyrics(videoId, duration, callback)
    }
}
