/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO
import com.auramusic.innertube.models.SongItem
import com.auramusic.app.db.entities.Song
import com.auramusic.app.models.MediaMetadata
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.app.ui.utils.resize
import com.auramusic.app.ui.utils.toHighQualityThumbnail
import com.auramusic.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

private const val METADATA_KEY_ALBUM_ART_URI = "android.media.metadata.ALBUM_ART_URI"
private const val METADATA_KEY_ART_URI = "android.media.metadata.ART_URI"
private const val METADATA_KEY_DISPLAY_ICON_URI = "android.media.metadata.DISPLAY_ICON_URI"

private fun String?.toLockScreenArtworkUrl(): String? =
    this?.toHighQualityThumbnail() ?: this?.resize(1200, 1200)

private fun Bundle.putArtworkExtras(artworkUrl: String?) {
    artworkUrl ?: return
    putString("artwork_uri", artworkUrl)
    putString(METADATA_KEY_ALBUM_ART_URI, artworkUrl)
    putString(METADATA_KEY_ART_URI, artworkUrl)
    putString(METADATA_KEY_DISPLAY_ICON_URI, artworkUrl)
}

fun Song.toMediaItem() = MediaItem.Builder()
    .setMediaId(song.id)
    .setUri(song.id)
    .setCustomCacheKey(song.id)
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(song.title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(song.thumbnailUrl.toLockScreenArtworkUrl()?.toUri())
            .setAlbumTitle(song.albumName)
            .setAlbumArtist(artists.firstOrNull()?.name)
            .setDisplayTitle(song.title)
            .setMediaType(if (song.isVideo) MEDIA_TYPE_VIDEO else MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                putArtworkExtras(song.thumbnailUrl.toLockScreenArtworkUrl())
            })
            .build()
    )
    .build()

fun SongItem.toMediaItem() = MediaItem.Builder()
    .setMediaId(id)
    .setUri(id)
    .setCustomCacheKey(id)
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(thumbnail.toLockScreenArtworkUrl()?.toUri())
            .setAlbumTitle(album?.name)
            .setAlbumArtist(artists.firstOrNull()?.name)
            .setDisplayTitle(title)
            .setMediaType(if (isVideoSong) MEDIA_TYPE_VIDEO else MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                putArtworkExtras(thumbnail.toLockScreenArtworkUrl())
            })
            .build()
    )
    .build()

fun MediaMetadata.toMediaItem() = MediaItem.Builder()
    .setMediaId(id)
    .setUri(id)
    .setCustomCacheKey(id)
    .setTag(this)
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(thumbnailUrl.toLockScreenArtworkUrl()?.toUri())
            .setAlbumTitle(album?.title)
            .setAlbumArtist(artists.firstOrNull()?.name)
            .setDisplayTitle(title)
            .setMediaType(if (isVideoSong) MEDIA_TYPE_VIDEO else MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                putArtworkExtras(thumbnailUrl.toLockScreenArtworkUrl())
            })
            .build()
    )
    .build()
