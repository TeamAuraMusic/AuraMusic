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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.auramusic.app.R
import com.auramusic.app.ui.tv.TvNavigation.LocalTvNavigator
import com.auramusic.app.ui.tv.TvNavigation.rememberTvNavigator
import com.auramusic.app.ui.tv.TvNavigation.TvDestination
import com.auramusic.app.utils.makeTimeString
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.app.db.entities.Artist
import com.auramusic.app.db.entities.Album
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.Song
import com.auramusic.app.db.entities.SpeedDialItem
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.viewmodels.HomeViewModel
import com.auramusic.app.viewmodels.LibraryAlbumsViewModel
import com.auramusic.app.viewmodels.LibraryArtistsViewModel
import com.auramusic.app.viewmodels.LibraryPlaylistsViewModel
import com.auramusic.app.viewmodels.LibrarySongsViewModel
import com.auramusic.app.viewmodels.CombinedSearchResult
import com.auramusic.app.viewmodels.TvSearchViewModel
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.YTItem
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.EpisodeItem
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.PodcastItem
import com.auramusic.innertube.models.SongItem
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

enum class TvSection(val label: String) {
    HOME("Home"),
    LIBRARY("Library"),
    SEARCH("Search"),
    SETTINGS("Settings"),
}

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
    var section by remember { mutableStateOf<TvSection>(TvSection.HOME) }
    val navigator = rememberTvNavigator()

    // Handle keyboard shortcuts for TV remote
    val onPreviewKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { event ->
        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.VolumeUp -> {
                    playerConnection?.player?.let { player ->
                        val currentVolume = player.volume
                        player.volume = (currentVolume + 0.1f).coerceAtMost(1f)
                    }
                    true
                }
                Key.VolumeDown -> {
                    playerConnection?.player?.let { player ->
                        val currentVolume = player.volume
                        player.volume = (currentVolume - 0.1f).coerceAtLeast(0f)
                    }
                    true
                }
                Key.MediaPlayPause -> {
                    playerConnection?.togglePlayPause()
                    true
                }
                Key.MediaNext -> {
                    playerConnection?.seekToNext()
                    true
                }
                Key.MediaPrevious -> {
                    playerConnection?.seekToPrevious()
                    true
                }
                else -> false
            }
        } else {
            false
        }
    }

    CompositionLocalProvider(LocalTvNavigator provides navigator) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent(onPreviewKeyEvent),
            color = MaterialTheme.colorScheme.background,
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TvNavigationBar(
                current = section,
                onSelect = { selectedSection: TvSection -> section = selectedSection },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (section) {
                    TvSection.HOME -> TvHomeScreen(playerConnection = playerConnection)
                    TvSection.LIBRARY -> TvLibraryScreen(playerConnection = playerConnection)
                    TvSection.SEARCH -> TvSearchScreen(playerConnection = playerConnection)
                    TvSection.SETTINGS -> TvSettingsScreen(onBackClick = { section = TvSection.HOME })
                }

                // Overlay player/queue if needed
                val currentDestination = rememberTvNavigator().current
                when (currentDestination) {
                    is TvDestination.Player -> TvPlayerScreen(
                        playerConnection = playerConnection,
                        onBackClick = { rememberTvNavigator().popBack() }
                    )
                    is TvDestination.Queue -> TvQueueScreen(
                        playerConnection = playerConnection,
                        onBackClick = { rememberTvNavigator().popBack() }
                    )
                    is TvDestination.Album -> TvAlbumDetailScreen(
                        albumId = currentDestination.id,
                        playerConnection = playerConnection,
                        onBackClick = { rememberTvNavigator().popBack() }
                    )
                    is TvDestination.Artist -> TvArtistDetailScreen(
                        artistId = currentDestination.id,
                        playerConnection = playerConnection,
                        onBackClick = { rememberTvNavigator().popBack() }
                    )
                    is TvDestination.Playlist -> TvPlaylistDetailScreen(
                        playlistId = currentDestination.id,
                        playerConnection = playerConnection,
                        onBackClick = { rememberTvNavigator().popBack() }
                    )
                    else -> Unit
                }
            }
        }
    }
}



@Composable
fun TvNavigationBar(current: TvSection, onSelect: (TvSection) -> Unit) {
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
fun TvNavButton(
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
fun TvHomeScreen(playerConnection: PlayerConnection?) {
    val viewModel: HomeViewModel = hiltViewModel()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // Mini player (show when music is playing)
        val isPlaying by (playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
        if (isPlaying) {
            item {
                TvMiniPlayer(
                    playerConnection = playerConnection,
                    onPlayerClick = { rememberTvNavigator().navigate(TvDestination.Player) }
                )
            }
        }

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

        // Hero Carousel - Trending content
        homePage?.sections?.flatMap { it.items }
            ?.distinctBy { it.id }
            ?.take(6)
            ?.takeIf { it.isNotEmpty() }
            ?.let { heroItems ->
                item {
                    YouTubeSectionRow(
                        title = "Trending Now",
                        items = heroItems,
                        playerConnection = playerConnection,
                         onYTItemClick = { item: YTItem ->
                              val navigator = rememberTvNavigator()
                              when (item) {
                                 is SongItem -> {
                                     playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                 }
                                 is AlbumItem -> {
                                     item.browseId?.let { browseId ->
                                         navigator.navigate(TvDestination.Album(browseId))
                                     } ?: run {
                                         playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(playlistId = item.playlistId)))
                                     }
                                 }
                                 is ArtistItem -> {
                                     item.id?.let { artistId ->
                                         navigator.navigate(TvDestination.Artist(artistId))
                                     }
                                 }
                                 is PlaylistItem -> {
                                     navigator.navigate(TvDestination.Playlist(item.id))
                                 }
                                 is EpisodeItem -> {
                                     playerConnection?.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                 }
                                 is PodcastItem -> {
                                     item.id?.let { podcastId ->
                                         // Navigate to podcast detail (could reuse playlist destination)
                                         navigator.navigate(TvDestination.Playlist(podcastId))
                                     }
                                 }
                             }
    }

                    )
                }
            }

        // Speed Dial Section
        pinnedSpeedDialItems.takeIf { it.isNotEmpty() }?.let { speedDialItems ->
            item {
                YouTubeSectionRow(
                    title = "Speed Dial",
                    items = speedDialItems.take(6).map { it.toYTItem() },
                    playerConnection = playerConnection,
                    onYTItemClick = { item: YTItem ->
                        // Handle speed dial item clicks
                        val navigator = rememberTvNavigator()
                        when (item) {
                            is SongItem -> {
                                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                            }
                            is AlbumItem -> {
                                item.browseId?.let { browseId ->
                                    navigator.navigate(TvDestination.Album(browseId))
                                }
                            }
                            is ArtistItem -> {
                                item.id?.let { artistId ->
                                    navigator.navigate(TvDestination.Artist(artistId))
                                }
                            }
                            is PlaylistItem -> {
                                navigator.navigate(TvDestination.Playlist(item.id))
                            }
                            else -> {}
                        }
                    }
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

        if (!keepListening.isNullOrEmpty()) {
            item {
                LocalItemRow(
                    title = "Keep listening",
                    items = keepListening!!,
                    playerConnection = playerConnection,
                )
            }
        }

        // Similar recommendations
        similarRecommendations?.takeIf { it.isNotEmpty() }?.let { recommendations ->
            recommendations.forEach { recommendation ->
                item {
                    SongRow(
                        title = "Similar to ${recommendation.title}",
                        songs = recommendation.songs,
                        onSongClick = { song: Song -> playerConnection?.playSong(song) },
                    )
                }
            }
        }

        // Account playlists
        accountPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
            item {
                YouTubeSectionRow(
                    title = "Your YouTube Playlists",
                    items = playlists.take(10),
                     playerConnection = playerConnection,
                       onYTItemClick = { item: YTItem ->
                           when (item) {
                               is PlaylistItem -> {
                                  rememberTvNavigator().navigate(TvDestination.Playlist(item.id))
                              }
                              else -> {}
                          }
                      }
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
                          onYTItemClick = { item: YTItem ->
                              val navigator = rememberTvNavigator()
                              when (item) {
                                 is SongItem -> {
                                     playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                 }
                                 is AlbumItem -> {
                                     item.browseId?.let { browseId ->
                                         navigator.navigate(TvDestination.Album(browseId))
                                     } ?: run {
                                         playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(playlistId = item.playlistId)))
                                     }
                                 }
                                 is ArtistItem -> {
                                     item.id?.let { artistId ->
                                         navigator.navigate(TvDestination.Artist(artistId))
                                     }
                                 }
                                 is PlaylistItem -> {
                                     navigator.navigate(TvDestination.Playlist(item.id))
                                 }
                                 else -> {}
                             }
                         }
                    )
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
fun YouTubeSectionRow(
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
fun YouTubeAlbumRow(
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
fun YouTubeMediaCard(
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
fun YouTubeAlbumCard(
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
fun TvLibraryScreen(playerConnection: PlayerConnection?) {
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
                    onSongClick = { song: Song -> playerConnection?.playSong(song) },
                )
            }
        }
        if (playlists.isNotEmpty()) {
            item { LocalItemRow(title = "Playlists", items = playlists, playerConnection = playerConnection) }
        }
        if (artists.isNotEmpty()) {
            item { LocalItemRow(title = "Artists", items = artists, playerConnection = playerConnection) }
        }
        if (albums.isNotEmpty()) {
            item { LocalItemRow(title = "Albums", items = albums, playerConnection = playerConnection) }
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
fun TvSearchScreen(playerConnection: PlayerConnection?) {
    val tvSearchViewModel: TvSearchViewModel = hiltViewModel()
    val query by tvSearchViewModel.query.collectAsState()
    val searchResults by tvSearchViewModel.searchResults.collectAsState()
    val isLoading by tvSearchViewModel.isLoading.collectAsState()
    val recentSearches by tvSearchViewModel.recentSearches.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { tvSearchViewModel.updateQuery(it) },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (query.isEmpty()) {
            // Show recent searches
            if (recentSearches.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent searches",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(recentSearches) { recentQuery ->
                    TvRecentSearchItem(
                        query = recentQuery,
                        onClick = { tvSearchViewModel.updateQuery(recentQuery) },
                    )
                }
            } else {
                item {
                    Text(
                        text = "Enter search query",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Searching…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            // Show search results
            if (searchResults.localItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Local Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(searchResults.localItems.take(10)) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { handleSearchItemClick(item, playerConnection) },
                    )
                }
            }

            if (searchResults.ytItems.isNotEmpty()) {
                item {
                    Text(
                        text = "YouTube Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(searchResults.ytItems.take(10)) { item ->
                    TvYTSearchResultItem(
                        item = item,
                        onClick = { handleYTSearchItemClick(item, playerConnection) },
                    )
                }
            }

            if (searchResults.localItems.isEmpty() && searchResults.ytItems.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                    ) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Try a different search term",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvRecentSearchItem(query: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painterResource(R.drawable.search),
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun TvSearchResultItem(item: LocalItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = when (item) {
                        is Artist -> "${item.songCount} songs"
                        is Album -> item.artists.joinToString(", ") { it.name }
                        is Playlist -> "${item.songCount} songs"
                        is Song -> item.artists.joinToString(", ") { it.name }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Text(
                text = when (item) {
                    is Artist -> "Artist"
                    is Album -> "Album"
                    is Playlist -> "Playlist"
                    is Song -> "Song"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun TvYTSearchResultItem(item: YTItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = when (item) {
                        is com.auramusic.innertube.models.SongItem -> item.thumbnail
                        is com.auramusic.innertube.models.AlbumItem -> item.thumbnail
                        is com.auramusic.innertube.models.ArtistItem -> item.thumbnail
                        is com.auramusic.innertube.models.PlaylistItem -> item.thumbnail
                        else -> ""
                    },
                    contentDescription = when (item) {
                        is com.auramusic.innertube.models.SongItem -> item.title
                        is com.auramusic.innertube.models.AlbumItem -> item.title
                        is com.auramusic.innertube.models.ArtistItem -> item.title
                        is com.auramusic.innertube.models.PlaylistItem -> item.title
                        else -> ""
                    },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (item) {
                        is com.auramusic.innertube.models.SongItem -> item.title
                        is com.auramusic.innertube.models.AlbumItem -> item.title
                        is com.auramusic.innertube.models.ArtistItem -> item.title
                        is com.auramusic.innertube.models.PlaylistItem -> item.title
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = when (item) {
                        is com.auramusic.innertube.models.SongItem -> item.artists?.joinToString(", ") { it.name } ?: ""
                        is com.auramusic.innertube.models.AlbumItem -> item.artists?.joinToString(", ") { it.name } ?: ""
                        is com.auramusic.innertube.models.ArtistItem -> "Artist"
                        is com.auramusic.innertube.models.PlaylistItem -> item.author?.name ?: ""
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "YT",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF0000),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when (item) {
                        is com.auramusic.innertube.models.SongItem -> "Song"
                        is com.auramusic.innertube.models.AlbumItem -> "Album"
                        is com.auramusic.innertube.models.ArtistItem -> "Artist"
                        is com.auramusic.innertube.models.PlaylistItem -> "Playlist"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF0000),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun handleSearchItemClick(item: LocalItem, playerConnection: PlayerConnection?) {
    val navigator = LocalTvNavigator.current
    when (item) {
        is Song -> playerConnection?.playSong(item)
        is Album -> navigator.navigate(TvDestination.Album(item.id))
        is Artist -> navigator.navigate(TvDestination.Artist(item.id))
        is Playlist -> navigator.navigate(TvDestination.Playlist(item.id))
    }
}

@Composable
fun handleYTSearchItemClick(item: YTItem, playerConnection: PlayerConnection?) {
    val navigator = LocalTvNavigator.current
    when (item) {
        is SongItem -> {
            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
        }
        is AlbumItem -> {
            item.browseId?.let { browseId ->
                navigator.navigate(TvDestination.Album(browseId))
            } ?: run {
                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(playlistId = item.playlistId)))
            }
        }
        is ArtistItem -> {
            item.id?.let { artistId ->
                navigator.navigate(TvDestination.Artist(artistId))
            }
        }
                                  is PlaylistItem -> {
                                      navigator.navigate(TvDestination.Playlist(item.id))
                                  }
        is EpisodeItem -> {
            playerConnection?.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
        }
        is PodcastItem -> {
            item.id?.let { podcastId ->
                navigator.navigate(TvDestination.Playlist(podcastId))
            }
        }
    }
}

/* -------------------------- Shared rows -------------------------- */

@Composable
fun SongRow(
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

/**
 * TV-friendly card. Visually responds to D-pad focus with a colored border,
 * a subtle scale-up, and asks the parent lazy row/column to scroll it into
 * view via [BringIntoViewRequester].
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
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

@Composable
fun LocalItemRow(title: String, items: List<LocalItem>, playerConnection: PlayerConnection?) {
    val navigator = LocalTvNavigator.current

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
                        onClick = { navigator.navigate(TvDestination.Artist(item.id)) },
                    )
                    is Album -> MediaCard(
                        title = item.album.title,
                        subtitle = item.artists.joinToString(", ") { it.name },
                        thumbnailUrl = item.album.thumbnailUrl,
                        onClick = { navigator.navigate(TvDestination.Album(item.id)) },
                    )
                    is Playlist -> MediaCard(
                        title = item.playlist.name,
                        subtitle = "${item.songCount} songs",
                        thumbnailUrl = item.playlist.thumbnailUrl,
                        onClick = { navigator.navigate(TvDestination.Playlist(item.id)) },
                    )
                    is Song -> MediaCard(
                        title = item.song.title,
                        subtitle = item.artists.joinToString(", ") { it.name },
                        thumbnailUrl = item.song.thumbnailUrl,
                        onClick = { playerConnection?.playSong(item.song) },
                    )
                }
            }
        }
    }
}


@Composable
fun TvMiniPlayer(
    playerConnection: PlayerConnection?,
    onPlayerClick: () -> Unit,
) {
    val currentSong by (playerConnection?.currentSong?.collectAsState(null) ?: remember { mutableStateOf(null) })
    val isPlaying by (playerConnection?.isPlaying?.collectAsState(false) ?: remember { mutableStateOf(false) })

    Surface(
        onClick = onPlayerClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = currentSong?.thumbnailUrl,
                    contentDescription = currentSong?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "No song playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = currentSong?.artists?.joinToString(", ") { it.name } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { playerConnection?.seekToPrevious() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { playerConnection?.togglePlayPause() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { playerConnection?.seekToNext() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/* -------------------------- Settings -------------------------- */

@Composable
fun TvSettingsCategoryItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "tvSettingsItemScale",
    )
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate to $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TvSettingsScreen(onBackClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            // Back button and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.size(64.dp)) // Balance the back button
            }
        }

        item {
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Settings categories
        item {
            TvSettingsCategoryItem(
                title = "Appearance",
                subtitle = "Theme, colors, and display settings",
                onClick = { /* TODO: Navigate to appearance settings */ }
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Playback",
                subtitle = "Audio quality, playback behavior",
                onClick = { /* TODO: Navigate to playback settings */ }
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Content",
                subtitle = "Sync settings, content filters",
                onClick = { /* TODO: Navigate to content settings */ }
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Account",
                subtitle = "YouTube account, sync preferences",
                onClick = { /* TODO: Navigate to account settings */ }
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "About",
                subtitle = "App version, licenses, and information",
                onClick = { /* TODO: Navigate to about screen */ }
            )
        }
    }
}

fun PlayerConnection?.playSong(song: Song) {
    if (this == null) return
    playQueue(
        ListQueue(
            title = song.title,
            items = listOf(song.toMediaItem()),
            startIndex = 0,
        ),
    )
}
