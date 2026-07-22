package com.auramusic.app.lyrics

import android.content.Context
import com.auramusic.app.constants.EnablePaxsenixKey
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.paxsenix.Paxsenix
import timber.log.Timber

object PaxsenixLyricsProvider : LyricsProvider {
    override val name = "Paxsenix"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnablePaxsenixKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        Timber.d("PaxsenixLyricsProvider: fetching lyrics for '$title' by '$artist'")
        return Paxsenix.getLyrics(title, artist, duration, album)
    }
}
