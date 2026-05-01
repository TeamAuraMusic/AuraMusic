/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.db.entities.Song
import com.auramusic.app.utils.makeTimeString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.rememberCoroutineScope
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
 import com.auramusic.app.db.entities.Artist
 import com.auramusic.app.db.entities.Album
 import com.auramusic.app.db.entities.Playlist
 import com.auramusic.app.db.entities.SpeedDialItem
 import com.auramusic.app.db.entities.LocalItem
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
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.auramusic.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.auramusic.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.auramusic.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.auramusic.innertube.YouTube.SearchFilter.Companion.FILTER_PODCAST
import com.auramusic.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.auramusic.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.auramusic.innertube.pages.HomePage
import com.auramusic.app.constants.DarkModeKey
import com.auramusic.app.ui.screens.settings.DarkMode
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import android.os.Build
import com.auramusic.innertube.pages.ExplorePage
import androidx.compose.foundation.layout.width
import com.auramusic.app.ui.component.ChipsRow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource

data class CarouselDimens(
    val height: Dp,
    val horizontalPadding: Dp,
    val cornerRadius: Dp,
    val playButtonSize: Dp,
    val playIconSize: Dp,
    val pageSpacing: Dp,
    val indicatorWidth: Dp,
    val indicatorDot: Dp,
    val indicatorSpacing: Dp,
)

@Composable
private fun rememberCarouselDimens(screenWidth: Dp): CarouselDimens {
    val isSmallScreen = screenWidth < 360.dp
    val isTablet = screenWidth >= 600.dp
    return CarouselDimens(
        height = when {
            isTablet -> 400.dp  // Smaller than mobile tablet (500dp)
            isSmallScreen -> 240.dp  // Smaller than mobile small screen (280dp)
            else -> 240.dp  // Smaller than mobile default (280dp)
        },
        horizontalPadding = when {
            isTablet -> 24.dp
            isSmallScreen -> 12.dp
            else -> 16.dp
        },
        cornerRadius = when {
            isTablet -> 16.dp
            isSmallScreen -> 10.dp
            else -> 14.dp
        },
        playButtonSize = when {
            isTablet -> 48.dp
            isSmallScreen -> 32.dp
            else -> 40.dp
        },
        playIconSize = when {
            isTablet -> 24.dp
            isSmallScreen -> 14.dp
            else -> 20.dp
        },
        pageSpacing = if (isTablet) 14.dp else 10.dp,
        indicatorWidth = if (isTablet) 20.dp else 16.dp,
        indicatorDot = if (isTablet) 6.dp else 5.dp,
        indicatorSpacing = if (isTablet) 12.dp else 8.dp,
    )
}

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
     val sectionState = remember { mutableStateOf<TvSection>(TvSection.HOME) }
     val navigator = rememberTvNavigator()
     val isPlayingState = playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
     val currentSong by playerConnection?.currentSong?.collectAsState(null) ?: remember { mutableStateOf(null) }

     // Keep screen on when music is playing
     val view = LocalView.current
     DisposableEffect(isPlayingState.value) {
         val window = (view.context as? android.app.Activity)?.window
         if (isPlayingState.value) {
             window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
         } else {
             window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
         }
         onDispose {
             window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
         }
     }

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
                  // Show top bar with mini player only when NOT in full player
                  val currentDestination = navigator.current
                  if (currentDestination !is TvDestination.Player) {
                      // Create focus requesters for detail screens
                      val detailFocusRequester = remember { FocusRequester() }

                      // Top bar with navigation and mini player
                      TvTopBar(
                          sectionState = sectionState,
                          isPlaying = isPlayingState.value,
                          currentSong = currentSong,
                          playerConnection = playerConnection,
                          onMiniPlayerClick = { navigator.navigate(TvDestination.Player) },
                          onNavigateDown = {
                              // Move focus to detail content when down is pressed from top nav
                              if (currentDestination != TvDestination.Home) {
                                  runCatching { detailFocusRequester.requestFocus() }
                              }
                          }
                      )

                      // Set up focus for detail screens
                      if (currentDestination != TvDestination.Home) {
                          LaunchedEffect(currentDestination) {
                              // Delay to ensure the detail screen is rendered
                              kotlinx.coroutines.delay(100)
                              runCatching { detailFocusRequester.requestFocus() }
                          }
                      }

                  Box(modifier = Modifier.fillMaxSize()) {
                      when (sectionState.value) {
                          TvSection.HOME -> TvHomeScreen(playerConnection = playerConnection)
                          TvSection.LIBRARY -> TvLibraryScreen(playerConnection = playerConnection)
                          TvSection.SEARCH -> TvSearchScreen(playerConnection = playerConnection)
                          TvSection.SETTINGS -> TvSettingsScreen(
                              onBackClick = { sectionState.value = TvSection.HOME },
                              onAppearanceClick = { navigator.navigate(TvDestination.AppearanceSettings) },
                              onAccountClick = { navigator.navigate(TvDestination.AccountSettings) },
                              onPlaybackClick = { navigator.navigate(TvDestination.PlaybackSettings) },
                              onContentClick = { navigator.navigate(TvDestination.ContentSettings) },
                              onUpdaterClick = { navigator.navigate(TvDestination.UpdaterScreen) },
                              onAboutClick = { navigator.navigate(TvDestination.AboutScreen) },
                          )
                      }

                      // Overlay player/queue/detail screens if needed
                      if (currentDestination != TvDestination.Home) {
                          // Full screen overlay for all destinations
                          Surface(
                              modifier = Modifier.fillMaxSize(),
                              color = MaterialTheme.colorScheme.background,
                          ) {
                               when (currentDestination) {
                                   is TvDestination.Player -> TvPlayerScreen(
                                       playerConnection = playerConnection,
                                       onBackClick = { navigator.popBack() }
                                   )
                                   is TvDestination.Album -> TvAlbumDetailScreen(
                                       albumId = currentDestination.id,
                                       playerConnection = playerConnection,
                                       onBackClick = { navigator.popBack() },
                                       focusRequester = detailFocusRequester
                                   )
                                   is TvDestination.Artist -> TvArtistDetailScreen(
                                       artistId = currentDestination.id,
                                       playerConnection = playerConnection,
                                       onBackClick = { navigator.popBack() },
                                       focusRequester = detailFocusRequester
                                   )
                                   is TvDestination.Playlist -> TvPlaylistDetailScreen(
                                       playlistId = currentDestination.id,
                                       playerConnection = playerConnection,
                                       onBackClick = { navigator.popBack() },
                                       focusRequester = detailFocusRequester
                                   )
                                   TvDestination.Settings -> TvSettingsScreen(
                                       onBackClick = { navigator.popBack() },
                                       onAppearanceClick = { navigator.navigate(TvDestination.AppearanceSettings) },
                                       onAccountClick = { navigator.navigate(TvDestination.AccountSettings) },
                                       onPlaybackClick = { navigator.navigate(TvDestination.PlaybackSettings) },
                                       onContentClick = { navigator.navigate(TvDestination.ContentSettings) },
                                       onUpdaterClick = { navigator.navigate(TvDestination.UpdaterScreen) },
                                       onAboutClick = { navigator.navigate(TvDestination.AboutScreen) },
                                   )
                                   TvDestination.AppearanceSettings -> TvAppearanceSettingsScreen(
                                       onBackClick = { navigator.popBack() }
                                   )
                                   TvDestination.AccountSettings -> TvAccountSettingsScreen(
                                       onBackClick = { navigator.popBack() },
                                       onLoginClick = { navigator.navigate(TvDestination.Login) },
                                   )
                                   TvDestination.PlaybackSettings -> TvPlaybackSettingsScreen(
                                       onBackClick = { navigator.popBack() }
                                   )
                                   TvDestination.ContentSettings -> TvContentSettingsScreen(
                                       onBackClick = { navigator.popBack() }
                                   )
                                   TvDestination.AboutScreen -> TvAboutScreen(
                                       onBackClick = { navigator.popBack() }
                                   )
                                   TvDestination.UpdaterScreen -> TvUpdaterScreen(
                                       onBackClick = { navigator.popBack() }
                                   )
                                   TvDestination.Login -> TvLoginScreen(
                                       onBackClick = { navigator.popBack() }
                                   )
                                   else -> Unit
                                   }
                                   }
                                   }
                                   }
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App logo/icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "AuraMusic",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "AuraMusic",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

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
}

 @Composable
 fun TvNavButton(
     label: String,
     isSelected: Boolean,
     focusRequester: FocusRequester?,
     onClick: () -> Unit,
 ) {
     val isFocusedState = remember { mutableStateOf(false) }
     val borderColor = if (isFocusedState.value) {
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
             .onFocusChanged { isFocusedState.value = it.isFocused }
             .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(20.dp)),
     ) {
         Text(text = label)
     }
 }

 @Composable
 fun TvTopBar(
     sectionState: androidx.compose.runtime.MutableState<TvSection>,
     isPlaying: Boolean,
     currentSong: com.auramusic.app.db.entities.Song?,
     playerConnection: PlayerConnection?,
     onMiniPlayerClick: () -> Unit,
     onNavigateDown: (() -> Unit)? = null,
 ) {
     val firstButtonFocus = remember { FocusRequester() }
     var currentlyFocusedIndex by remember { mutableStateOf(0) }

     LaunchedEffect(Unit) {
         runCatching { firstButtonFocus.requestFocus() }
     }

     Surface(
         modifier = Modifier
             .fillMaxWidth()
             .onPreviewKeyEvent { event ->
                 if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                     // Move focus back to content area when down is pressed
                     onNavigateDown?.invoke()
                     true // Consume the event
                 } else {
                     false
                 }
             },
         color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
         tonalElevation = 8.dp,
     ) {
         Row(
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(horizontal = 48.dp, vertical = 16.dp),
             horizontalArrangement = Arrangement.spacedBy(16.dp),
             verticalAlignment = Alignment.CenterVertically,
         ) {
             // App logo/icon
             Box(
                 modifier = Modifier
                     .size(48.dp)
                     .clip(RoundedCornerShape(12.dp))
                     .background(Color.Black),
                 contentAlignment = Alignment.Center,
             ) {
                 Icon(
                     painterResource(R.drawable.ic_launcher_foreground),
                     contentDescription = "AuraMusic",
                     tint = Color.Unspecified,
                     modifier = Modifier.size(32.dp)
                 )
             }

             Text(
                 text = "AuraMusic",
                 style = MaterialTheme.typography.titleLarge,
                 fontWeight = FontWeight.Bold,
                 color = MaterialTheme.colorScheme.onSurface,
                 modifier = Modifier.padding(start = 12.dp)
             )

             Spacer(modifier = Modifier.weight(1f))

              // Mini player (expanded to show full song and artist info)
              if (isPlaying && currentSong != null) {
                  Surface(
                      onClick = onMiniPlayerClick,
                       modifier = Modifier
                           .height(56.dp)
                           .weight(2f, fill = false) // Increased weight to make it wider
                           .padding(horizontal = 16.dp)
                          .clip(RoundedCornerShape(8.dp))
                          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                      tonalElevation = 2.dp,
                  ) {
                     Row(
                         modifier = Modifier
                             .fillMaxSize()
                             .padding(horizontal = 12.dp),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(12.dp),
                     ) {
                         // Album art
                         Box(
                             modifier = Modifier
                                 .size(40.dp)
                                 .clip(RoundedCornerShape(6.dp))
                                 .background(MaterialTheme.colorScheme.surface),
                             contentAlignment = Alignment.Center,
                         ) {
                             AsyncImage(
                                 model = currentSong.thumbnailUrl,
                                 contentDescription = currentSong.title,
                                 contentScale = ContentScale.Crop,
                                 modifier = Modifier.fillMaxSize(),
                             )
                         }

                          // Song info (expanded layout for full visibility)
                          Column(modifier = Modifier.weight(1f)) {
                              Text(
                                  text = currentSong.title,
                                  style = MaterialTheme.typography.bodyMedium,
                                  fontWeight = FontWeight.SemiBold,
                                  color = MaterialTheme.colorScheme.onSurface,
                                  maxLines = 2, // Allow title to wrap if needed
                                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                              )
                              Text(
                                  text = currentSong.artists?.joinToString(", ") { it.name } ?: "",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  maxLines = 1,
                                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                              )
                          }

                         // Play/Pause button
                         val scope = rememberCoroutineScope()
                         IconButton(
                             onClick = { playerConnection?.togglePlayPause() },
                             modifier = Modifier.size(40.dp),
                         ) {
                             Icon(
                                 if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                 contentDescription = if (isPlaying) "Pause" else "Play",
                                 tint = MaterialTheme.colorScheme.onSurface,
                                 modifier = Modifier.size(20.dp)
                             )
                         }
                     }
                 }
             }

             Spacer(modifier = Modifier.weight(1f))

             // Navigation buttons
             TvSection.entries.forEachIndexed { index, section ->
                 val isSelected = section == sectionState.value
                 TvNavButton(
                     label = section.label,
                     isSelected = isSelected,
                     focusRequester = if (index == 0) firstButtonFocus else null,
                     onClick = { sectionState.value = section },
                 )
             }
         }
     }
 }

/* -------------------------- Home -------------------------- */

@Composable
fun TvHomeScreen(playerConnection: PlayerConnection?) {
    val navigator = LocalTvNavigator.current
    val viewModel: HomeViewModel = hiltViewModel()
    val quickPicks = viewModel.quickPicks.collectAsState().value
    val forgottenFavorites = viewModel.forgottenFavorites.collectAsState().value
    val keepListening = viewModel.keepListening.collectAsState().value
    val similarRecommendations = viewModel.similarRecommendations.collectAsState().value
    val accountPlaylists = viewModel.accountPlaylists.collectAsState().value
    val homePage = viewModel.homePage.collectAsState().value
    val explorePage = viewModel.explorePage.collectAsState().value
    val pinnedSpeedDialItems = viewModel.pinnedSpeedDialItems.collectAsState().value
    val isRefreshing = viewModel.isRefreshing.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val isPlaying = (playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }).value

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

        // Hero Carousel - Trending content
        homePage?.sections?.flatMap { it.items }
            ?.distinctBy { it.id }
            ?.take(6)
            ?.takeIf { it.isNotEmpty() }
            ?.let { heroItems ->
                item {
                    TvHeroCarousel(
                        title = "Trending Now",
                        items = heroItems,
                        playerConnection = playerConnection,
                        onYTItemClick = { item: YTItem ->
                            when (item) {
                                is SongItem -> {
                                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                }
                                is AlbumItem -> {
                                    val browseId = item.browseId
                                    if (browseId != null) {
                                        navigator.navigate(TvDestination.Album(browseId))
                                    } else {
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
                                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                }
                                is PodcastItem -> {
                                    item.id?.let { podcastId ->
                                        // Navigate to podcast detail (could reuse playlist destination)
                                        navigator.navigate(TvDestination.Playlist(podcastId))
                                    }
                                }
                                else -> {}
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
                        when (item) {
                            is SongItem -> playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                            is AlbumItem -> {
                                val browseId = item.browseId
                                if (browseId != null) {
                                    navigator.navigate(TvDestination.Album(browseId))
                                } else {
                                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(playlistId = item.playlistId)))
                                }
                            }
                            is ArtistItem -> item.id?.let { navigator.navigate(TvDestination.Artist(it)) }
                            is PlaylistItem -> navigator.navigate(TvDestination.Playlist(item.id))
                            is EpisodeItem -> playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                            is PodcastItem -> item.id?.let { navigator.navigate(TvDestination.Playlist(it)) }
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
                        onSongClick = { song: Song -> playerConnection?.playSong(song) },
                    )
                }
            }

            if (!forgottenFavorites.isNullOrEmpty()) {
                item {
                    SongRow(
                        title = "Forgotten favorites",
                        songs = forgottenFavorites!!,
                        onSongClick = { song: Song -> playerConnection?.playSong(song) },
                    )
                }
            }

            if (!keepListening.isNullOrEmpty()) {
                item {
                    LocalItemRow(
                        title = "Keep listening",
                        localItems = keepListening!!,
                        playerConnection = playerConnection,
                    )
                }
            }

            // Similar recommendations
            similarRecommendations?.takeIf { it.isNotEmpty() }?.let { recommendations ->
                recommendations.forEach { recommendation ->
                    val titleName = when (recommendation.title) {
                        is com.auramusic.app.db.entities.Artist -> recommendation.title.artist.name
                        is com.auramusic.app.db.entities.Album -> recommendation.title.album.title
                        is com.auramusic.app.db.entities.Playlist -> recommendation.title.playlist.name
                        is com.auramusic.app.db.entities.Song -> recommendation.title.song.title
                        else -> recommendation.title.toString()
                    }
                    item {
                        YouTubeSectionRow(
                            title = "Similar to $titleName",
                            items = recommendation.items,
                            playerConnection = playerConnection,
                            onYTItemClick = { item: YTItem ->
    when (item) {
        is SongItem -> {
            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
        }
        is AlbumItem -> {
            val browseId = item.browseId
            if (browseId != null) {
                navigator.navigate(TvDestination.Album(browseId))
            } else {
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
            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
        }
        is PodcastItem -> {
            item.id?.let { podcastId ->
                navigator.navigate(TvDestination.Playlist(podcastId))
            }
        }
        else -> {}
    }
                            }
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
                                  navigator.navigate(TvDestination.Playlist(item.id))
                              }
                              else -> {}
                          }
                      }
                )
            }
        }

        // Display home page sections from YouTube
        val sections = homePage?.sections.orEmpty()
        for (section in sections) {
            if (section.items.isNotEmpty()) {
                item {
                    YouTubeSectionRow(
                        title = section.title,
                        items = section.items,
                         playerConnection = playerConnection,
                        onYTItemClick = { item: YTItem ->
                            when (item) {
                                is SongItem -> playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                is AlbumItem -> {
                                    val browseId = item.browseId
                                    if (browseId != null) {
                                        navigator.navigate(TvDestination.Album(browseId))
                                    } else {
                                        playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(playlistId = item.playlistId)))
                                    }
                                }
                                is ArtistItem -> item.id?.let { navigator.navigate(TvDestination.Artist(it)) }
                                is PlaylistItem -> navigator.navigate(TvDestination.Playlist(item.id))
                                is EpisodeItem -> playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                is PodcastItem -> item.id?.let { navigator.navigate(TvDestination.Playlist(it)) }
                                else -> {}
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
fun YouTubeSectionRow(
    title: String,
    items: List<YTItem>,
    playerConnection: PlayerConnection?,
    onYTItemClick: (YTItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Section header with underline
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
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
    val isFocusedState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocusedState.value) 1.06f else 1f,
        label = "tvYouTubeCardScale",
    )
    val borderColor = if (isFocusedState.value) {
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
                isFocusedState.value = focusState.isFocused
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    modifier = Modifier.weight(1f),
                )

                // Additional metadata
                when (item) {
                    is SongItem -> {
                        val duration = item.duration
                        if (duration != null) {
                            Text(
                                text = makeTimeString(duration * 1000L),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                    is AlbumItem -> {
                        val year = item.year
                        if (year != null) {
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                    is PlaylistItem -> {
                        val countText = item.songCountText
                        if (countText != null) {
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                    else -> {} // No additional metadata for ArtistItem, EpisodeItem, PodcastItem
                }
            }
        }
    }
}

@Composable
fun YouTubeAlbumCard(
    album: AlbumItem,
    onClick: () -> Unit,
) {
    val isFocusedState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocusedState.value) 1.06f else 1f,
        label = "tvAlbumCardScale",
    )
    val borderColor = if (isFocusedState.value) {
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
                isFocusedState.value = focusState.isFocused
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

    val (innerTubeCookie, _) = rememberPreference(
        com.auramusic.app.constants.InnerTubeCookieKey,
        "",
    )
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in com.auramusic.innertube.utils.parseCookieString(innerTubeCookie)
    }

    // When the user is signed in, kick off a one-shot sync of their YouTube
    // Music library so liked songs / albums / playlists / subscribed artists
    // appear here without needing to open the mobile app first.
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            runCatching {
                songsViewModel.syncLikedSongs()
                albumsViewModel.sync()
                artistsViewModel.sync()
                playlistsViewModel.sync()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        item {
            Text(
                text = if (isLoggedIn) "Your YouTube Music library" else "Your library",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (songs.isNotEmpty()) {
            item {
                SongRow(
                    title = "Liked songs",
                    songs = songs,
                    onSongClick = { song: Song -> playerConnection?.playSong(song) },
                )
            }
        }
        if (playlists.isNotEmpty()) {
            item { LocalItemRow(title = "Playlists", localItems = playlists, playerConnection = playerConnection) }
        }
        if (artists.isNotEmpty()) {
            item {
                LocalItemRow(
                    title = "Subscribed artists",
                    localItems = artists,
                    playerConnection = playerConnection,
                )
            }
        }
        if (albums.isNotEmpty()) {
            item { LocalItemRow(title = "Albums", localItems = albums, playerConnection = playerConnection) }
        }

        if (songs.isEmpty() && playlists.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                ) {
                    Text(
                        text = if (isLoggedIn) "Syncing your library…" else "Your library is empty.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (isLoggedIn)
                            "Liked songs, playlists, albums and subscribed artists will appear here."
                        else
                            "Sign in from Settings → Account to sync your YouTube Music library.",
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
    val navigator = LocalTvNavigator.current
    val tvSearchViewModel: TvSearchViewModel = hiltViewModel()
    val query = tvSearchViewModel.query.collectAsState().value
    val filter = tvSearchViewModel.filter.collectAsState().value
    val searchResults = tvSearchViewModel.searchResults.collectAsState().value
    val isLoading = tvSearchViewModel.isLoading.collectAsState().value
    val recentSearches = tvSearchViewModel.recentSearches.collectAsState().value

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
            // Filter chips like mobile search
            item {
                ChipsRow(
                    chips = listOf(
                        Pair(null as YouTube.SearchFilter?, stringResource(R.string.filter_all)),
                        Pair(FILTER_SONG, stringResource(R.string.filter_songs)),
                        Pair(FILTER_VIDEO, stringResource(R.string.filter_videos)),
                        Pair(FILTER_ALBUM, stringResource(R.string.filter_albums)),
                        Pair(FILTER_ARTIST, stringResource(R.string.filter_artists)),
                        Pair(FILTER_COMMUNITY_PLAYLIST, stringResource(R.string.filter_community_playlists)),
                        Pair(FILTER_FEATURED_PLAYLIST, stringResource(R.string.filter_featured_playlists)),
                        Pair(FILTER_PODCAST, stringResource(R.string.podcasts)),
                    ),
                    currentValue = filter,
                    onValueUpdate = { newFilter: YouTube.SearchFilter? ->
                        if (tvSearchViewModel.filter.value != newFilter) {
                            tvSearchViewModel.filter.value = newFilter
                            // Re-perform search with new filter
                            if (query.isNotEmpty()) {
                                tvSearchViewModel.updateQuery(query)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Show search results
            val localSongs = searchResults.localItems.filterIsInstance<Song>().take(5)
            val localArtists = searchResults.localItems.filterIsInstance<Artist>().take(5)
            val localAlbums = searchResults.localItems.filterIsInstance<Album>().take(5)
            val localPlaylists = searchResults.localItems.filterIsInstance<Playlist>().take(5)

            val ytSongs = searchResults.ytItems.filterIsInstance<com.auramusic.innertube.models.SongItem>().take(5)
            val ytArtists = searchResults.ytItems.filterIsInstance<com.auramusic.innertube.models.ArtistItem>().take(5)
            val ytAlbums = searchResults.ytItems.filterIsInstance<com.auramusic.innertube.models.AlbumItem>().take(5)
            val ytPlaylists = searchResults.ytItems.filterIsInstance<com.auramusic.innertube.models.PlaylistItem>().take(5)

            // Local Songs
            if (localSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "Local Songs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(localSongs) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { handleSearchItemClick(item, playerConnection, navigator) },
                    )
                }
            }

            // Local Artists
            if (localArtists.isNotEmpty()) {
                item {
                    Text(
                        text = "Local Artists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(localArtists) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { handleSearchItemClick(item, playerConnection, navigator) },
                    )
                }
            }

            // Local Albums
            if (localAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "Local Albums",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(localAlbums) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { handleSearchItemClick(item, playerConnection, navigator) },
                    )
                }
            }

            // Local Playlists
            if (localPlaylists.isNotEmpty()) {
                item {
                    Text(
                        text = "Local Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(localPlaylists) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { handleSearchItemClick(item, playerConnection, navigator) },
                    )
                }
            }

            // YouTube Songs
            if (ytSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "YouTube Songs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(ytSongs) { item ->
                    TvYTSearchResultItem(
                        item = item,
                        onClick = { handleYTSearchItemClick(item, playerConnection, navigator) },
                    )
                }
            }

            // YouTube Artists
            if (ytArtists.isNotEmpty()) {
                item {
                    Text(
                        text = "YouTube Artists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(ytArtists) { item ->
                    TvYTSearchResultItem(
                        item = item,
                        onClick = { handleYTSearchItemClick(item, playerConnection, navigator) },
                    )
                }
            }

            // YouTube Albums
            if (ytAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "YouTube Albums",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(ytAlbums) { item ->
                    TvYTSearchResultItem(
                        item = item,
                        onClick = { handleYTSearchItemClick(item, playerConnection, navigator) },
                    )
                }
            }

            // YouTube Playlists
            if (ytPlaylists.isNotEmpty()) {
                item {
                    Text(
                        text = "YouTube Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(ytPlaylists) { item ->
                    TvYTSearchResultItem(
                        item = item,
                        onClick = { handleYTSearchItemClick(item, playerConnection, navigator) },
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
    val isFocusedState = remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocusedState.value = it.isFocused }
            .border(
                width = if (isFocusedState.value) 2.dp else 0.dp,
                color = if (isFocusedState.value) MaterialTheme.colorScheme.primary else Color.Transparent,
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
    val isFocusedState = remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocusedState.value = it.isFocused }
            .border(
                width = if (isFocusedState.value) 2.dp else 0.dp,
                color = if (isFocusedState.value) MaterialTheme.colorScheme.primary else Color.Transparent,
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
                        else -> ""
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
                    else -> ""
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
    val isFocusedState = remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocusedState.value = it.isFocused }
            .border(
                width = if (isFocusedState.value) 2.dp else 0.dp,
                color = if (isFocusedState.value) MaterialTheme.colorScheme.primary else Color.Transparent,
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

fun handleSearchItemClick(item: LocalItem, playerConnection: PlayerConnection?, navigator: TvNavigator) {
    when (item) {
        is Song -> playerConnection?.playSong(item)
        is Album -> navigator.navigate(TvDestination.Album(item.id))
        is Artist -> navigator.navigate(TvDestination.Artist(item.id))
        is Playlist -> navigator.navigate(TvDestination.Playlist(item.id))
    }
}

fun handleYTSearchItemClick(item: YTItem, playerConnection: PlayerConnection?, navigator: TvNavigator) {
    when (item) {
        is SongItem -> {
            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
        }
                                is AlbumItem -> {
                                    val browseId = item.browseId
                                    if (browseId != null) {
                                        navigator.navigate(TvDestination.Album(browseId))
                                    } else {
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
            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
        }
        is PodcastItem -> {
            item.id?.let { podcastId ->
                navigator.navigate(TvDestination.Playlist(podcastId))
            }
        }
        else -> {}
    }
}

/* -------------------------- Shared rows -------------------------- */

@Composable
fun SongRow(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Section header with underline
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
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
    val isFocusedState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocusedState.value) 1.06f else 1f,
        label = "tvCardScale",
    )
    val borderColor = if (isFocusedState.value) {
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
                isFocusedState.value = focusState.isFocused
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
fun LocalItemRow(title: String, localItems: List<LocalItem>, playerConnection: PlayerConnection?) {
    val navigator = LocalTvNavigator.current

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Section header with underline
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(localItems) { item ->
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
                        onClick = { playerConnection?.playSong(item) },
                    )
                }
            }
        }
    }
}



@Composable
fun TvHeroCarousel(
    title: String,
    items: List<YTItem>,
    playerConnection: PlayerConnection?,
    onYTItemClick: (YTItem) -> Unit,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    // Auto-scroll functionality like mobile hero carousel
    LaunchedEffect(pagerState, items.size) {
        if (items.size > 1) {
            while (true) {
                delay(4000L) // Auto-scroll every 4 seconds like mobile
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Section header
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        // Hero carousel - smaller like mobile version
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val dimens = rememberCarouselDimens(maxWidth)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.height),
                pageSpacing = dimens.pageSpacing,
                contentPadding = PaddingValues(horizontal = dimens.horizontalPadding)
            ) { page ->
                val item = items[page]
                TvHeroCard(
                    item = item,
                    dimens = dimens,
                    onClick = { onYTItemClick(item) }
                )
            }

            // Page indicators like mobile version
            if (items.size > 1) {
                Spacer(modifier = Modifier.height(dimens.indicatorSpacing))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, _ ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) dimens.indicatorWidth else dimens.indicatorDot,
                            animationSpec = tween(300),
                            label = "indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(dimens.indicatorDot)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            }
        }


    }
}

@Composable
fun TvHeroCard(
    item: YTItem,
    dimens: CarouselDimens,
    onClick: () -> Unit,
) {
    val isFocusedState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocusedState.value) 1.06f else 1f,
        label = "tvHeroCardScale",
    )
    val borderColor = if (isFocusedState.value) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
             }
             .bringIntoViewRequester(bringIntoViewRequester)
             .onFocusChanged { focusState ->
                 isFocusedState.value = focusState.isFocused
                 if (focusState.isFocused) {
                     scope.launch { bringIntoViewRequester.bringIntoView() }
                 }
             }
             .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image - use FillBounds like mobile to cover without cropping
            AsyncImage(
                model = when (item) {
                    is SongItem -> item.thumbnail
                    is AlbumItem -> item.thumbnail
                    is ArtistItem -> item.thumbnail
                    is PlaylistItem -> item.thumbnail
                    else -> ""
                },
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = when (item) {
                        is SongItem -> item.title
                        is AlbumItem -> item.title
                        is ArtistItem -> item.title
                        is PlaylistItem -> item.title
                        else -> ""
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                )

                Text(
                    text = when (item) {
                        is SongItem -> item.artists?.joinToString(", ") { it.name } ?: ""
                        is AlbumItem -> item.artists?.joinToString(", ") { it.name } ?: ""
                        is ArtistItem -> "Artist"
                        is PlaylistItem -> item.author?.name ?: "Playlist"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                )

                // Play button
                Button(
                    onClick = onClick,
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Play")
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
    val isFocusedState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocusedState.value) 1.06f else 1f,
        label = "tvSettingsItemScale",
    )
    val borderColor = if (isFocusedState.value) {
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
                isFocusedState.value = focusState.isFocused
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
fun TvSettingsScreen(
    onBackClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onContentClick: () -> Unit = {},
    onUpdaterClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
) {
    val (innerTubeCookie, _) = rememberPreference(
        com.auramusic.app.constants.InnerTubeCookieKey,
        "",
    )
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in com.auramusic.innertube.utils.parseCookieString(innerTubeCookie)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { TvSettingsHeader(title = "Settings", onBackClick = onBackClick) }

        item {
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Account",
                subtitle = if (isLoggedIn)
                    "Signed in to YouTube Music — manage account"
                else
                    "Sign in to sync liked songs, playlists and subscriptions",
                onClick = onAccountClick,
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Appearance",
                subtitle = "Theme, colors, and display settings",
                onClick = onAppearanceClick,
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Playback",
                subtitle = "Audio quality, playback behavior",
                onClick = onPlaybackClick,
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Content",
                subtitle = "Sync settings, content filters",
                onClick = onContentClick,
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Check for Updates",
                subtitle = "Check for new AuraMusic TV versions",
                onClick = onUpdaterClick,
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "About",
                subtitle = "App version, licenses, and information",
                onClick = onAboutClick,
            )
        }
    }
}

@Composable
fun TvAppearanceSettingsScreen(onBackClick: () -> Unit) {
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            // Back button and title
            val backButtonFocus = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                runCatching { backButtonFocus.requestFocus() }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var backFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(64.dp)
                        .focusRequester(backButtonFocus)
                        .onFocusChanged { backFocused = it.isFocused }
                        .border(
                            width = if (backFocused) 3.dp else 0.dp,
                            color = if (backFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Appearance Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.size(64.dp)) // Balance the back button
            }
        }

        item {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Dark mode options
        item {
            TvSettingsCategoryItem(
                title = "Dark Theme",
                subtitle = when (darkMode) {
                    DarkMode.ON -> "Always dark"
                    DarkMode.OFF -> "Always light"
                    DarkMode.AUTO -> "Follow system"
                },
                onClick = {
                    val newMode = when (darkMode) {
                        DarkMode.ON -> DarkMode.OFF
                        DarkMode.OFF -> DarkMode.AUTO
                        DarkMode.AUTO -> DarkMode.ON
                    }
                    onDarkModeChange(newMode)
                }
            )
        }

        // Dynamic theme if supported
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            item {
                TvSettingsCategoryItem(
                    title = "Dynamic Colors",
                    subtitle = "Use wallpaper colors for theme",
                    onClick = {
                        // TODO: Implement dynamic theme toggle
                    }
                )
            }
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
