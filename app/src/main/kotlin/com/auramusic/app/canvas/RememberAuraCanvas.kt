package com.auramusic.app.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.auramusic.app.constants.AuraCanvasRemoteEnabledKey
import com.auramusic.app.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Remembers a canvas for an album by trying the configured providers.
 */
@Composable
fun rememberAlbumCanvas(
    albumTitle: String?,
    artistName: String?,
    candidateTracks: List<String> = emptyList(),
    provider: AuraCanvasProvider = createDefaultAuraCanvasProvider()
): AuraCanvasResult? {
    var result by remember(albumTitle, artistName) { mutableStateOf<AuraCanvasResult?>(null) }

    val (remoteEnabled, _) = rememberPreference(AuraCanvasRemoteEnabledKey, defaultValue = false)

    LaunchedEffect(albumTitle, artistName, candidateTracks, remoteEnabled) {
        if (albumTitle.isNullOrBlank() || artistName.isNullOrBlank()) {
            result = null
            return@LaunchedEffect
        }

        val canvas = withContext(Dispatchers.IO) {
            // Try direct album first
            provider.getByAlbumArtist(albumTitle, artistName)
                ?: provider.getBySongArtist(albumTitle, artistName)
                ?: if (remoteEnabled && candidateTracks.isNotEmpty() && provider is RemoteCanvasProvider) {
                    provider.getAlbumCanvas(albumTitle, artistName, candidateTracks)
                } else null
        }

        result = canvas
    }

    return result
}

/**
 * Remembers a canvas for an artist by trying the configured providers.
 * Pass candidate tracks from the artist's top songs for on-demand lookup.
 */
@Composable
fun rememberArtistCanvas(
    artistName: String?,
    candidateTracks: List<String> = emptyList(),
    provider: AuraCanvasProvider = createDefaultAuraCanvasProvider()
): AuraCanvasResult? {
    var result by remember(artistName) { mutableStateOf<AuraCanvasResult?>(null) }

    val (remoteEnabled, _) = rememberPreference(AuraCanvasRemoteEnabledKey, defaultValue = false)

    LaunchedEffect(artistName, candidateTracks, remoteEnabled) {
        if (artistName.isNullOrBlank()) {
            result = null
            return@LaunchedEffect
        }

        val canvas = withContext(Dispatchers.IO) {
            // Try any song by the artist from manifest first
            candidateTracks.firstNotNullOfOrNull { track ->
                provider.getBySongArtist("", artistName) // will use artist matching
            } ?: if (remoteEnabled && candidateTracks.isNotEmpty() && provider is RemoteCanvasProvider) {
                provider.getArtistCanvas(artistName, candidateTracks)
            } else null
        }

        result = canvas
    }

    return result
}
