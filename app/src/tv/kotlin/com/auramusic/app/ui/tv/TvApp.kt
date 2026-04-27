/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.auramusic.app.R
import com.auramusic.app.db.entities.Album
import com.auramusic.app.db.entities.Artist
import com.auramusic.app.db.entities.LocalItem
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.Song
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.viewmodels.HomeViewModel
import com.auramusic.app.viewmodels.LibraryAlbumsViewModel
import com.auramusic.app.viewmodels.LibraryArtistsViewModel
import com.auramusic.app.viewmodels.LibraryPlaylistsViewModel
import com.auramusic.app.viewmodels.LibrarySongsViewModel
import com.auramusic.app.viewmodels.LocalFilter
import com.auramusic.app.viewmodels.LocalSearchViewModel
import com.auramusic.app.viewmodels.CombinedSearchResult
import com.auramusic.app.viewmodels.TvSearchViewModel
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.YTItem
import kotlinx.coroutines.launch

/**
 * Top-level Compose entry point for the Android TV variant.
 *
 * Uses lightweight in-memory tab navigation so the TV variant can ship without
 * pulling in androidx.navigation:navigation-compose. Reuses the existing
 * mobile ViewModels so phone and TV share the same data pipeline.
 *
 * D-pad focus handling:
 * - Every focusable surface visually highlights on focus (border + scale).
 * - Focused items request to be brought into view so LazyRow / LazyColumn
 *   scrolls them on screen as the user navigates with the remote.
 * - Initial focus is requested on the navigation bar so the user can start
 *   navigating immediately without a touchscreen.
 */
@Composable
fun TvApp(playerConnection: PlayerConnection?) {
    var section by remember { mutableStateOf(TvSection.HOME) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TvNavigationBar(
                current = section,
                onSelect = { section = it },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (section) {
                    TvSection.HOME -> TvHomeScreen(playerConnection = playerConnection)
                    TvSection.LIBRARY -> TvLibraryScreen(playerConnection = playerConnection)
                    TvSection.SEARCH -> TvSearchScreen(playerConnection = playerConnection)
                }
            }
        }
    }
}

private enum class TvSection(val label: String) {
    HOME("Home"),
    LIBRARY("Library"),
    SEARCH("Search"),
}

@Composable
private fun TvNavigationBar(current: TvSection, onSelect: (TvSection) -> Unit) {
    val firstButtonFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { firstButtonFocus.requestFocus() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AuraMusic",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(24.dp))
        TvSection.entries.forEachIndexed { index, section ->
            val isSelected = section == current
            TvNavButton(
                label = section.label,
                isSelected = isSelected,
                focusRequester = if (index == 0) firstButtonFocus else null,
                onClick = { onSelect(section) },
            )
        }
    }
}

@Composable
private fun TvNavButton(
    label: String,
    isSelected: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    Button(
        onClick = onClick,
        colors = if (isSelected) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
        modifier = Modifier
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged { isFocused = it.isFocused }
            .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(20.dp)),
    ) {
        Text(text = label)
    }
}

/* -------------------------- Home -------------------------- */

@Composable
private fun TvHomeScreen(playerConnection: PlayerConnection?) {
    val viewModel: HomeViewModel = hiltViewModel()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // Refresh indicator
        if (isRefreshing) {
            item {
                Text(
                    text = "Syncing…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (!quickPicks.isNullOrEmpty()) {
            item {
                SongRow(
                    title = "Quick picks",
                    songs = quickPicks!!,
                    onSongClick = { song -> playerConnection?.playSong(song) },
                )
            }
        }

        if (!forgottenFavorites.isNullOrEmpty()) {
            item {
                SongRow(
                    title = "Forgotten favorites",
                    songs = forgottenFavorites!!,
                    onSongClick = { song -> playerConnection?.playSong(song) },
                )
            }
        }

        // Display home page sections from YouTube
        homePage?.sections?.forEachIndexed { index, section ->
            if (section.items.isNotEmpty()) {
                item {
                    YouTubeSectionRow(
                        title = section.title,
                        items = section.items,
                        playerConnection = playerConnection,
                        onYTItemClick = { item ->
                            when (item) {
                                is com.auramusic.innertube.models.SongItem -> {
                                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                }
                                // Add other item types as needed
                                else -> {
                                    // Handle navigation to artist/album/playlist pages
                                }
                            }
                        }
                    )
                }
            }
        }

        if (quickPicks.isNullOrEmpty() && forgottenFavorites.isNullOrEmpty() && homePage?.sections.isNullOrEmpty() != false) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                ) {
                    Text(
                        text = when {
                            isLoading -> "Loading your music…"
                            playerConnection == null -> "Connecting to player…"
                            else -> "No music available"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!isLoading && playerConnection != null) {
                        Text(
                            text = "Pull down to refresh or use the refresh button",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeSectionRow(
    title: String,
    items: List<YTItem>,
    playerConnection: PlayerConnection?,
    onYTItemClick: (YTItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items) { item ->
                YouTubeMediaCard(
                    item = item,
                    onClick = { onYTItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun YouTubeAlbumRow(
    title: String,
    albums: List<AlbumItem>,
    onAlbumClick: (AlbumItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(albums) { album ->
                YouTubeAlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) },
                )
            }
        }
    }
}

@Composable
private fun YouTubeMediaCard(
    item: YTItem,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "tvYouTubeCardScale",
    )
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 220.dp, height = 280.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = when (item) {
                        is SongItem -> item.thumbnail
                        is AlbumItem -> item.thumbnail
                        is ArtistItem -> item.thumbnail
                        is PlaylistItem -> item.thumbnail
                        else -> ""
                    },
                    contentDescription = when (item) {
                        is SongItem -> item.title
                        is AlbumItem -> item.title
                        is ArtistItem -> item.title
                        is PlaylistItem -> item.title
                        else -> ""
                    },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (item) {
                    is SongItem -> item.title
                    is AlbumItem -> item.title
                    is ArtistItem -> item.title
                    is PlaylistItem -> item.title
                    else -> ""
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = when (item) {
                    is SongItem -> item.artists?.joinToString(", ") { it.name } ?: ""
                    is AlbumItem -> item.artists?.joinToString(", ") { it.name } ?: ""
                    is ArtistItem -> "Artist"
                    is PlaylistItem -> item.author?.name ?: "Playlist"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun YouTubeAlbumCard(
    album: AlbumItem,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "tvAlbumCardScale",
    )
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 220.dp, height = 280.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = album.thumbnail,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = album.artists?.joinToString(", ") { it.name } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/* -------------------------- Library -------------------------- */

@Composable
private fun TvLibraryScreen(playerConnection: PlayerConnection?) {
    val songsViewModel: LibrarySongsViewModel = hiltViewModel()
    val artistsViewModel: LibraryArtistsViewModel = hiltViewModel()
    val albumsViewModel: LibraryAlbumsViewModel = hiltViewModel()
    val playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel()

    val songs by songsViewModel.allSongs.collectAsState()
    val artists by artistsViewModel.allArtists.collectAsState()
    val albums by albumsViewModel.allAlbums.collectAsState()
    val playlists by playlistsViewModel.allPlaylists.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        if (songs.isNotEmpty()) {
            item {
                SongRow(
                    title = "Songs",
                    songs = songs,
                    onSongClick = { song -> playerConnection?.playSong(song) },
                )
            }
        }
        if (playlists.isNotEmpty()) {
            item { LocalItemRow(title = "Playlists", items = playlists) }
        }
        if (artists.isNotEmpty()) {
            item { LocalItemRow(title = "Artists", items = artists) }
        }
        if (albums.isNotEmpty()) {
            item { LocalItemRow(title = "Albums", items = albums) }
        }

        if (songs.isEmpty() && playlists.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                ) {
                    Text(
                        text = "Your library is empty.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Sync your music from YouTube to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/* -------------------------- Search -------------------------- */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Composable
private fun TvSearchScreen(playerConnection: PlayerConnection?) {
    val tvSearchViewModel: TvSearchViewModel = hiltViewModel()
    val query by tvSearchViewModel.query.collectAsState()
    val searchResults by tvSearchViewModel.searchResults.collectAsState()
    val isLoading by tvSearchViewModel.isLoading.collectAsState()
    val recentSearches by tvSearchViewModel.recentSearches.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { tvSearchViewModel.updateQuery(it) },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (query.isEmpty()) {
            Text("Enter search query")
        } else if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text("Search results: ${searchResults.localItems.size} local, ${searchResults.ytItems.size} YouTube")
        }
    }
}

/* -------------------------- Shared rows -------------------------- */

@Composable
private fun SongRow(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(songs) { song ->
                MediaCard(
                    title = song.song.title,
                    subtitle = song.artists.joinToString(", ") { it.name },
                    thumbnailUrl = song.song.thumbnailUrl,
                    onClick = { onSongClick(song) },
                )
            }
        }
    }
}

@Composable
private fun LocalItemRow(title: String, items: List<LocalItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items) { item ->
                when (item) {
                    is Artist -> MediaCard(
                        title = item.artist.name,
                        subtitle = "${item.songCount} songs",
                        thumbnailUrl = item.artist.thumbnailUrl,
                        onClick = {},
                    )
                    is Album -> MediaCard(
                        title = item.album.title,
                        subtitle = item.artists.joinToString(", ") { it.name },
                        thumbnailUrl = item.album.thumbnailUrl,
                        onClick = {},
                    )
                    is Playlist -> MediaCard(
                        title = item.playlist.name,
                        subtitle = "${item.songCount} songs",
                        thumbnailUrl = item.playlist.thumbnailUrl,
                        onClick = {},
                    )
                    is Song -> MediaCard(
                        title = item.song.title,
                        subtitle = item.artists.joinToString(", ") { it.name },
                        thumbnailUrl = item.song.thumbnailUrl,
                        onClick = {},
                    )
                }
            }
        }
    }
}

/**
 * TV-friendly card. Visually responds to D-pad focus with a colored border,
 * a subtle scale-up, and asks the parent lazy row/column to scroll it into
 * view via [BringIntoViewRequester].
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MediaCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "tvCardScale",
    )
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 220.dp, height = 280.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun PlayerConnection?.playSong(song: Song) {
    this?.playQueue(
        YouTubeQueue(endpoint = WatchEndpoint(videoId = song.song.id)),
    )
}
