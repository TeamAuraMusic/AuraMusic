package com.auramusic.app.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Direct Spotify canvas provider using sp_dc cookie.
 * 
 * This should eventually call the local Spotify-Canvas-API tool
 * (or a direct implementation) using the user's sp_dc token.
 */
class SpotifyDirectCanvasProvider(
    private val fetchCanvas: suspend (song: String, artist: String) -> String? = { _, _ -> null }
) : AuraCanvasProvider {

    override suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String?
    ): AuraCanvasResult? = withContext(Dispatchers.IO) {
        val url = fetchCanvas(song, artist)
        if (!url.isNullOrBlank()) {
            AuraCanvasResult(
                url = url,
                source = "spotify_direct",
                song = song,
                artist = artist,
                album = album
            )
        } else {
            null
        }
    }
}
