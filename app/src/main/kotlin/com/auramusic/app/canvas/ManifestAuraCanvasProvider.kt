package com.auramusic.app.canvas

import com.auramusic.app.playback.AuraCanvasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Canvas provider backed by the local/remote manifest (canvas.json).
 * This is the primary source for song-based canvases.
 */
class ManifestAuraCanvasProvider : AuraCanvasProvider {

    override suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String?
    ): AuraCanvasResult? = withContext(Dispatchers.IO) {
        val url = AuraCanvasRepository.findCanvasUrl(song, artist)
        if (url != null) {
            AuraCanvasResult(
                url = url,
                source = "manifest",
                song = song,
                artist = artist,
                album = album
            )
        } else {
            null
        }
    }
}
