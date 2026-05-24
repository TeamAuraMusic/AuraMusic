package com.auramusic.app.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Placeholder for Apple Music canvas provider.
 * Can be implemented later using Apple Music API or scraping.
 */
class AppleMusicCanvasProvider : AuraCanvasProvider {

    override suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String?
    ): AuraCanvasResult? = withContext(Dispatchers.IO) {
        // TODO: Implement using Apple Music canvas sources
        null
    }

    override suspend fun getByAlbumArtist(
        album: String,
        artist: String
    ): AuraCanvasResult? = withContext(Dispatchers.IO) {
        // TODO: Implement using Apple Music canvas sources
        null
    }
}
