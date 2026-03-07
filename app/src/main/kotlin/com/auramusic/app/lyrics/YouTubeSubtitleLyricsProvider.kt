/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.lyrics

import android.content.Context
import com.auramusic.innertube.YouTube

object YouTubeSubtitleLyricsProvider : LyricsProvider {
    override val name = "YouTube Subtitle"

    override fun isEnabled(context: Context) = true

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
        return YouTube.transcript(videoId)
    }
}
