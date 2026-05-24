package com.auramusic.app.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of a canvas lookup.
 */
data class AuraCanvasResult(
    val url: String,
    val source: String, // e.g. "manifest", "spotify_direct", "apple_music"
    val song: String? = null,
    val artist: String? = null,
    val album: String? = null
)

/**
 * Interface for different canvas sources.
 * All providers should be thread-safe and cache-friendly where possible.
 */
interface AuraCanvasProvider {

    /**
     * Try to find a canvas for a specific song + artist combination.
     */
    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null
    ): AuraCanvasResult?

    /**
     * Try to find a canvas for an album + artist.
     * Default implementation falls back to song-based search.
     */
    suspend fun getByAlbumArtist(
        album: String,
        artist: String
    ): AuraCanvasResult? {
        return getBySongArtist(album, artist, album)
    }
}
