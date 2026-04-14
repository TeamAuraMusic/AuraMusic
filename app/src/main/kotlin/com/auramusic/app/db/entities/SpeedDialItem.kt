/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.db.entities

import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.PodcastItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.YTItem
import com.auramusic.innertube.models.EpisodeItem

data class SpeedDialItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val type: String,
    val subtype: String?,
    val artistName: String?,
    val artistId: String?,
    val playlistId: String?,
    val shuffleEndpoint: String?,
    val radioEndpoint: String?,
    val playEndpoint: String?,
) {
    fun toYTItem(): YTItem {
        return when (type) {
            "SONG" -> SongItem(
                id = id,
                title = title,
                artists = listOfNotNull(artistName?.let { Artist(name = it, id = artistId) }),
                album = null,
                duration = null,
                thumbnail = thumbnailUrl ?: "",
                explicit = false
            )
            "ALBUM" -> AlbumItem(
                browseId = id,
                playlistId = playlistId ?: "",
                title = title,
                artists = listOfNotNull(artistName?.let { Artist(name = it, id = artistId) }),
                year = null,
                thumbnail = thumbnailUrl ?: "",
                explicit = false
            )
            "ARTIST" -> ArtistItem(
                id = id,
                title = title,
                thumbnail = thumbnailUrl ?: "",
                shuffleEndpoint = shuffleEndpoint,
                radioEndpoint = radioEndpoint
            )
            "PLAYLIST" -> PlaylistItem(
                id = id,
                title = title,
                author = artistName?.let { Artist(name = it, id = null) },
                songCountText = null,
                thumbnail = thumbnailUrl ?: "",
                playEndpoint = playEndpoint?.let { WatchPlaylistEndpoint(playlistId = id) },
                shuffleEndpoint = shuffleEndpoint,
                radioEndpoint = radioEndpoint
            )
            "PODCAST" -> PodcastItem(
                browseId = id,
                title = title,
                authors = listOfNotNull(artistName?.let { Artist(name = it, id = artistId) }),
                thumbnail = thumbnailUrl ?: "",
                playEndpoint = playEndpoint?.let { WatchPlaylistEndpoint(playlistId = playlistId ?: "") },
                shuffleEndpoint = shuffleEndpoint,
                radioEndpoint = radioEndpoint
            )
            "EPISODE" -> EpisodeItem(
                id = id,
                title = title,
                artists = listOfNotNull(artistName?.let { Artist(name = it, id = artistId) }),
                thumbnail = thumbnailUrl ?: "",
                duration = null,
                playEndpoint = playEndpoint
            )
            else -> SongItem(
                id = id,
                title = title,
                artists = listOfNotNull(artistName?.let { Artist(name = it, id = artistId) }),
                album = null,
                duration = null,
                thumbnail = thumbnailUrl ?: "",
                explicit = false
            )
        }
    }

    companion object {
        fun fromYTItem(item: YTItem): SpeedDialItem {
            return when (item) {
                is SongItem -> SpeedDialItem(
                    id = item.id,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "SONG",
                    subtype = null,
                    artistName = item.artists.firstOrNull()?.name,
                    artistId = item.artists.firstOrNull()?.id,
                    playlistId = null,
                    shuffleEndpoint = null,
                    radioEndpoint = null,
                    playEndpoint = item.endpoint?.let { "${it.videoId}" }
                )
                is AlbumItem -> SpeedDialItem(
                    id = item.browseId,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "ALBUM",
                    subtype = null,
                    artistName = item.artists.firstOrNull()?.name,
                    artistId = item.artists.firstOrNull()?.id,
                    playlistId = item.playlistId,
                    shuffleEndpoint = null,
                    radioEndpoint = null,
                    playEndpoint = null
                )
                is ArtistItem -> SpeedDialItem(
                    id = item.id,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "ARTIST",
                    subtype = null,
                    artistName = null,
                    artistId = null,
                    playlistId = null,
                    shuffleEndpoint = item.shuffleEndpoint?.playlistId,
                    radioEndpoint = item.radioEndpoint?.playlistId,
                    playEndpoint = null
                )
                is PlaylistItem -> SpeedDialItem(
                    id = item.id,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "PLAYLIST",
                    subtype = null,
                    artistName = item.author?.name,
                    artistId = null,
                    playlistId = null,
                    shuffleEndpoint = item.shuffleEndpoint?.playlistId,
                    radioEndpoint = item.radioEndpoint?.playlistId,
                    playEndpoint = item.playEndpoint?.playlistId
                )
                is PodcastItem -> SpeedDialItem(
                    id = item.browseId,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "PODCAST",
                    subtype = null,
                    artistName = item.authors.firstOrNull()?.name,
                    artistId = item.authors.firstOrNull()?.id,
                    playlistId = item.playEndpoint?.playlistId,
                    shuffleEndpoint = item.shuffleEndpoint?.playlistId,
                    radioEndpoint = item.radioEndpoint?.playlistId,
                    playEndpoint = item.playEndpoint?.playlistId
                )
                is EpisodeItem -> SpeedDialItem(
                    id = item.id,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "EPISODE",
                    subtype = null,
                    artistName = item.artists.firstOrNull()?.name,
                    artistId = item.artists.firstOrNull()?.id,
                    playlistId = null,
                    shuffleEndpoint = null,
                    radioEndpoint = null,
                    playEndpoint = item.playEndpoint
                )
            }
        }
    }
}

data class WatchPlaylistEndpoint(
    val playlistId: String,
)