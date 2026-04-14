/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.PodcastItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.YTItem
import com.auramusic.innertube.models.EpisodeItem
import com.auramusic.innertube.models.Artist
import com.auramusic.innertube.models.WatchEndpoint

@Entity(tableName = "speed_dial_item")
data class SpeedDialItem(
    @PrimaryKey
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
                shuffleEndpoint = shuffleEndpoint?.let { WatchEndpoint(playlistId = it) },
                radioEndpoint = radioEndpoint?.let { WatchEndpoint(playlistId = it) }
            )
            "PLAYLIST" -> PlaylistItem(
                id = id,
                title = title,
                author = artistName?.let { Artist(name = it, id = null) },
                songCountText = null,
                thumbnail = thumbnailUrl ?: "",
                playEndpoint = playEndpoint?.let { WatchEndpoint(playlistId = id) },
                shuffleEndpoint = shuffleEndpoint?.let { WatchEndpoint(playlistId = it) },
                radioEndpoint = radioEndpoint?.let { WatchEndpoint(playlistId = it) }
            )
            "PODCAST" -> PodcastItem(
                id = id,
                title = title,
                author = artistName?.let { Artist(name = it, id = artistId) },
                episodeCountText = null,
                thumbnail = thumbnailUrl ?: "",
                playEndpoint = playEndpoint?.let { WatchEndpoint(playlistId = playlistId ?: "") },
                shuffleEndpoint = shuffleEndpoint?.let { WatchEndpoint(playlistId = it) }
            )
            "EPISODE" -> EpisodeItem(
                id = id,
                title = title,
                author = artistName?.let { Artist(name = it, id = artistId) },
                thumbnail = thumbnailUrl ?: "",
                duration = null,
                endpoint = playEndpoint?.let { WatchEndpoint(videoId = it) }
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
                    playEndpoint = item.endpoint?.videoId
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
                    id = item.id,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "PODCAST",
                    subtype = null,
                    artistName = item.author?.name,
                    artistId = item.author?.id,
                    playlistId = item.playEndpoint?.playlistId,
                    shuffleEndpoint = item.shuffleEndpoint?.playlistId,
                    radioEndpoint = null,
                    playEndpoint = item.playEndpoint?.playlistId
                )
                is EpisodeItem -> SpeedDialItem(
                    id = item.id,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "EPISODE",
                    subtype = null,
                    artistName = item.author?.name,
                    artistId = item.author?.id,
                    playlistId = null,
                    shuffleEndpoint = null,
                    radioEndpoint = null,
                    playEndpoint = item.endpoint?.videoId
                )
            }
        }
    }
}