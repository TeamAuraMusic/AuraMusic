package com.auramusic.app.canvas

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Tries multiple AuraCanvasProviders in order until one returns a result.
 */
class CompositeAuraCanvasProvider(
    private val providers: List<AuraCanvasProvider>
) : AuraCanvasProvider {

    override suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String?
    ): AuraCanvasResult? {
        for (provider in providers) {
            val result = provider.getBySongArtist(song, artist, album)
            if (result != null) return result
        }
        return null
    }

    override suspend fun getByAlbumArtist(
        album: String,
        artist: String
    ): AuraCanvasResult? {
        for (provider in providers) {
            val result = provider.getByAlbumArtist(album, artist)
            if (result != null) return result
        }
        return null
    }
}

/**
 * Default provider chain used by the app.
 * Order matters — more reliable / local sources first.
 */
fun createDefaultAuraCanvasProvider(
    manifestProvider: AuraCanvasProvider = ManifestAuraCanvasProvider(),
    spotifyDirectProvider: AuraCanvasProvider? = null,
    appleProvider: AuraCanvasProvider? = null
): CompositeAuraCanvasProvider {
    val providers = buildList {
        add(manifestProvider)
        spotifyDirectProvider?.let { add(it) }
        appleProvider?.let { add(it) }
    }
    return CompositeAuraCanvasProvider(providers)
}
