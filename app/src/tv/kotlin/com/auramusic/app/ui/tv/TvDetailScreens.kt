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
import androidx.compose.runtime.remember
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
import com.auramusic.app.db.entities.PlaylistSong
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.viewmodels.LibraryAlbumsViewModel
import com.auramusic.app.viewmodels.LibraryArtistsViewModel
import com.auramusic.app.viewmodels.LibraryPlaylistsViewModel
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.SongItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// Unified interface for both local and YouTube songs
sealed class DisplaySong {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
    abstract val artists: String
    abstract val duration: String?

    data class LocalSong(val song: Song) : DisplaySong() {
        override val id: String = song.id
        override val title: String = song.title
        override val thumbnailUrl: String? = song.thumbnailUrl
        override val artists: String = song.artists.joinToString(", ") { it.name }
        override val duration: String? = null // Local songs don't have duration in this format
    }

    data class YouTubeSong(val songItem: SongItem) : DisplaySong() {
        override val id: String = songItem.id
        override val title: String = songItem.title
        override val thumbnailUrl: String? = songItem.thumbnail
        override val artists: String = songItem.artists?.joinToString(", ") { it.name } ?: ""
        override val duration: String? = songItem.duration
    }
}

/* ------------------------------ Album ------------------------------ */

@Composable
fun TvAlbumDetailScreen(albumId: String, playerConnection: PlayerConnection?, onBackClick: () -> Unit) {
    val albumsViewModel: LibraryAlbumsViewModel = hiltViewModel()
    val database = LocalDatabase.current

    val albums by albumsViewModel.allAlbums.collectAsState()
    val localAlbum = albums.find { it.album.id == albumId }

    val localSongs by remember(albumId) { database.albumSongs(albumId) }.collectAsState(emptyList<Song>())

    // YouTube data
    val ytSongs = remember { mutableStateOf<List<SongItem>?>(null) }
    val ytAlbum = remember { mutableStateOf<com.auramusic.innertube.models.AlbumItem?>(null) }

    LaunchedEffect(albumId) {
        if (localAlbum == null && localSongs.isEmpty()) {
            // Try to fetch from YouTube
            YouTube.album(albumId).onSuccess { albumPage ->
                ytAlbum.value = albumPage.album
                ytSongs.value = albumPage.songs
            }
        }
    }

    val displaySongs = if (localSongs.isNotEmpty()) {
        localSongs.map { DisplaySong.LocalSong(it) }
    } else {
        ytSongs.value?.map { DisplaySong.YouTubeSong(it) } ?: emptyList()
    }

    val displayTitle = localAlbum?.album?.title ?: ytAlbum.value?.title ?: "Album"
    val displaySubtitle = localAlbum?.artists?.joinToString(", ") { it.name } ?: ytAlbum.value?.artists?.joinToString(", ") { it.name }.orEmpty()
    val displayThumbnail = localAlbum?.album?.thumbnailUrl ?: ytAlbum.value?.thumbnail
    val songCount = displaySongs.size

    TvDetailLayout(
        title = displayTitle,
        subtitle = displaySubtitle,
        meta = "$songCount songs",
        thumbnailUrl = displayThumbnail,
        displaySongs = displaySongs,
        playerConnection = playerConnection,
        playAllTitle = displayTitle,
        onBackClick = onBackClick,
    )
}

/* ------------------------------ Artist ------------------------------ */

@Composable
fun TvArtistDetailScreen(artistId: String, playerConnection: PlayerConnection?, onBackClick: () -> Unit) {
    val artistsViewModel: LibraryArtistsViewModel = hiltViewModel()
    val database = LocalDatabase.current

    val artists by artistsViewModel.allArtists.collectAsStateWithLifecycle()
    val localArtist = artists.find { it.artist.id == artistId }

    val localSongs by remember(artistId) {
        database.artistSongs(artistId, ArtistSongSortType.CREATE_DATE, true)
    }.collectAsState(emptyList<Song>())

    // YouTube data
    val ytSongs = remember { mutableStateOf<List<SongItem>?>(null) }
    val ytArtist = remember { mutableStateOf<com.auramusic.innertube.models.ArtistItem?>(null) }

    LaunchedEffect(artistId) {
        if (localArtist == null && localSongs.isEmpty()) {
            // Try to fetch from YouTube
            YouTube.artist(artistId).onSuccess { artistPage ->
                ytArtist.value = artistPage.artist
                ytSongs.value = artistPage.sections
                    .flatMap { it.items }
                    .filterIsInstance<SongItem>()
                    .distinctBy { it.id }
            }
        }
    }

    val displaySongs = if (localSongs.isNotEmpty()) {
        localSongs.map { DisplaySong.LocalSong(it) }
    } else {
        ytSongs.value?.map { DisplaySong.YouTubeSong(it) } ?: emptyList()
    }

    val displayTitle = localArtist?.artist?.name ?: ytArtist.value?.title ?: "Artist"
    val displayThumbnail = localArtist?.artist?.thumbnailUrl ?: ytArtist.value?.thumbnail
    val songCount = displaySongs.size

    TvDetailLayout(
        title = displayTitle,
        subtitle = "Artist",
        meta = "$songCount songs",
        thumbnailUrl = displayThumbnail,
        displaySongs = displaySongs,
        playerConnection = playerConnection,
        playAllTitle = displayTitle,
        onBackClick = onBackClick,
    )
}

/* ------------------------------ Playlist ------------------------------ */

@Composable
fun TvPlaylistDetailScreen(playlistId: String, playerConnection: PlayerConnection?, onBackClick: () -> Unit) {
    val playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val database = LocalDatabase.current

    val playlists by playlistsViewModel.allPlaylists.collectAsStateWithLifecycle()
    val localPlaylist = playlists.find { it.playlist.id == playlistId }

    val localPlaylistSongs by database.playlistSongs(playlistId).collectAsState(emptyList())
    val localSongs = localPlaylistSongs.map { it.song }

    // YouTube data
    val ytSongs = remember { mutableStateOf<List<SongItem>?>(null) }
    val ytPlaylist = remember { mutableStateOf<com.auramusic.innertube.models.PlaylistItem?>(null) }

    LaunchedEffect(playlistId) {
        if (localPlaylist == null && localSongs.isEmpty()) {
            // Try to fetch from YouTube
            YouTube.playlist(playlistId).onSuccess { playlistPage ->
                ytPlaylist.value = playlistPage.playlist
                ytSongs.value = playlistPage.songs
            }
        }
    }

    val displaySongs = if (localSongs.isNotEmpty()) {
        localSongs.map { DisplaySong.LocalSong(it) }
    } else {
        ytSongs.value?.map { DisplaySong.YouTubeSong(it) } ?: emptyList()
    }

    val displayTitle = localPlaylist?.playlist?.name ?: ytPlaylist.value?.title ?: "Playlist"
    val displayThumbnail = localPlaylist?.playlist?.thumbnailUrl ?: ytPlaylist.value?.thumbnail
    val songCount = displaySongs.size

    TvDetailLayout(
        title = displayTitle,
        subtitle = "",
        meta = "$songCount songs",
        thumbnailUrl = displayThumbnail,
        displaySongs = displaySongs,
        playerConnection = playerConnection,
        playAllTitle = displayTitle,
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
    displaySongs: List<DisplaySong>,
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
                            playerConnection.playAll(displaySongs, playAllTitle)
                        }
                        TvSecondaryButton(label = "Shuffle") {
                            playerConnection.playAll(displaySongs.shuffled(), playAllTitle)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }

        items(displaySongs) { displaySong ->
            SongRowItem(displaySong = displaySong) { playerConnection?.playDisplaySong(displaySong) }
        }

        if (displaySongs.isEmpty()) {
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
private fun SongRowItem(displaySong: DisplaySong, onClick: () -> Unit) {
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
                model = displaySong.thumbnailUrl,
                contentDescription = displaySong.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displaySong.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = displaySong.artists,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        // Duration if available
        displaySong.duration?.let { duration ->
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun PlayerConnection?.playDisplaySong(displaySong: DisplaySong) {
    if (this == null) return
    when (displaySong) {
        is DisplaySong.LocalSong -> {
            playQueue(
                ListQueue(
                    title = displaySong.song.song.title,
                    items = listOf(displaySong.song.toMediaItem()),
                    startIndex = 0,
                ),
            )
        }
        is DisplaySong.YouTubeSong -> {
            // For YouTube songs, use YouTubeQueue
            playQueue(com.auramusic.app.playback.queues.YouTubeQueue(
                com.auramusic.innertube.models.WatchEndpoint(videoId = displaySong.songItem.id)
            ))
        }
    }
}

private fun PlayerConnection?.playAll(displaySongs: List<DisplaySong>, title: String?) {
    if (this == null || displaySongs.isEmpty()) return

    val localSongs = displaySongs.filterIsInstance<DisplaySong.LocalSong>().map { it.song }
    val ytSongs = displaySongs.filterIsInstance<DisplaySong.YouTubeSong>().map { it.songItem }

    if (localSongs.isNotEmpty()) {
        // If we have local songs, play them
        playQueue(
            ListQueue(
                title = title,
                items = localSongs.map { it.toMediaItem() },
                startIndex = 0,
            ),
        )
    } else if (ytSongs.isNotEmpty()) {
        // If we only have YouTube songs, play the first one
        playQueue(com.auramusic.app.playback.queues.YouTubeQueue(
            com.auramusic.innertube.models.WatchEndpoint(videoId = ytSongs.first().id)
        ))
    }
}


