/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.auramusic.app.LocalDatabase
import com.auramusic.app.constants.ArtistSongSortType
import com.auramusic.app.db.entities.Song
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.viewmodels.LibraryAlbumsViewModel
import com.auramusic.app.viewmodels.LibraryArtistsViewModel
import com.auramusic.app.viewmodels.LibraryPlaylistsViewModel

/* ------------------------------ Album ------------------------------ */

@Composable
fun TvAlbumDetailScreen(albumId: String, playerConnection: PlayerConnection?, onBackClick: () -> Unit) {
    val albumsViewModel: LibraryAlbumsViewModel = hiltViewModel()
    val database = LocalDatabase.current

    val albums by albumsViewModel.allAlbums.collectAsState()
    val album = albums.find { it.album.id == albumId }

    val songs by remember(albumId) { database.albumSongs(albumId) }.collectAsState(emptyList<List<Song>>())

    TvDetailLayout(
        title = album?.album?.title.orEmpty().ifEmpty { "Album" },
        subtitle = album?.artists?.joinToString(", ") { it.name }.orEmpty(),
        meta = "${songs.size} songs",
        thumbnailUrl = album?.album?.thumbnailUrl,
        songs = songs,
        playerConnection = playerConnection,
        playAllTitle = album?.album?.title,
        onBackClick = onBackClick,
    )
}

/* ------------------------------ Artist ------------------------------ */

@Composable
fun TvArtistDetailScreen(artistId: String, playerConnection: PlayerConnection?, onBackClick: () -> Unit) {
    val artistsViewModel: LibraryArtistsViewModel = hiltViewModel()
    val database = LocalDatabase.current

    val artists by artistsViewModel.allArtists.collectAsStateWithLifecycle()
    val artist = artists.find { it.artist.id == artistId }

    val songs by remember(artistId) {
        database.artistSongs(artistId, ArtistSongSortType.CREATE_DATE, true)
    }.collectAsState(emptyList<List<Song>>())

    TvDetailLayout(
        title = artist?.artist?.name.orEmpty().ifEmpty { "Artist" },
        subtitle = "Artist",
        meta = "${songs.size} songs",
        thumbnailUrl = artist?.artist?.thumbnailUrl,
        songs = songs,
        playerConnection = playerConnection,
        playAllTitle = artist?.artist?.name,
        onBackClick = onBackClick,
    )
}

/* ------------------------------ Playlist ------------------------------ */

@Composable
fun TvPlaylistDetailScreen(playlistId: String, playerConnection: PlayerConnection?, onBackClick: () -> Unit) {
    val playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val database = LocalDatabase.current

    val playlists by playlistsViewModel.allPlaylists.collectAsStateWithLifecycle()
    val playlist = playlists.find { it.playlist.id == playlistId }

    val songs by remember(playlistId) {
        database.playlistSongs(playlistId).map { playlistSongs ->
            playlistSongs.map { it.song }
        }
    }.collectAsState(emptyList<List<Song>>())

    TvDetailLayout(
        title = playlist?.playlist?.name.orEmpty().ifEmpty { "Playlist" },
        subtitle = "",
        meta = "${songs.size} songs",
        thumbnailUrl = playlist?.playlist?.thumbnailUrl,
        songs = songs,
        playerConnection = playerConnection,
        playAllTitle = playlist?.playlist?.name,
        onBackClick = onBackClick,
    )
}

/* ------------------------------ Layout ------------------------------ */

@Composable
private fun TvDetailLayout(
    title: String,
    subtitle: String,
    meta: String,
    thumbnailUrl: String?,
    songs: List<Song>,
    playerConnection: PlayerConnection?,
    playAllTitle: String?,
    onBackClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(64.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvPrimaryButton(label = "Play all") {
                            playerConnection.playAll(songs, playAllTitle)
                        }
                        TvSecondaryButton(label = "Shuffle") {
                            playerConnection.playAll(songs.shuffled(), playAllTitle)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }

        items(songs) { song ->
            SongRowItem(song = song) { playerConnection.playOne(song) }
        }

        if (songs.isEmpty()) {
            item {
                Text(
                    text = "No songs in this collection.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SongRowItem(song: Song, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = song.song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun PlayerConnection?.playOne(song: Song) {
    if (this == null) return
    playQueue(
        ListQueue(
            title = song.song.title,
            items = listOf(song.toMediaItem()),
            startIndex = 0,
        ),
    )
}

private fun PlayerConnection?.playAll(songs: List<Song>, title: String?) {
    if (this == null || songs.isEmpty()) return
    playQueue(
        ListQueue(
            title = title,
            items = songs.map { it.toMediaItem() },
            startIndex = 0,
        ),
    )
}


