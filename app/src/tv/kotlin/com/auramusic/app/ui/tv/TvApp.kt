/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Storage
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.db.entities.Song
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.app.utils.makeTimeString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.toArgb
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
import com.auramusic.app.ui.screens.settings.AppFont
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import android.os.Build
import com.auramusic.innertube.pages.ExplorePage
import androidx.compose.foundation.layout.width
import com.auramusic.app.ui.component.ChipsRow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
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
     val currentMediaMetadata by playerConnection?.mediaMetadata?.collectAsState(null) ?: remember { mutableStateOf(null) }
     val showMiniPlayer = currentSong != null || currentMediaMetadata != null
     var showExitDialog by remember { mutableStateOf(false) }

      // Handle remote back button: go back in navigator, or show exit dialog
      // Only handle back when no overlay/dialog is showing to avoid conflicts
      androidx.activity.compose.BackHandler(enabled = !showExitDialog) {
          val nav = navigator
          if (nav.current is TvDestination.Player) {
              nav.popBack()
          } else if (nav.current != TvDestination.Home) {
              nav.popBack()
          } else if (sectionState.value != TvSection.HOME) {
              sectionState.value = TvSection.HOME
          } else {
              showExitDialog = true
          }
      }

     val view = LocalView.current

      // Exit confirmation dialog
      if (showExitDialog) {
          var exitButtonFocused by remember { mutableStateOf(false) }
          var stayButtonFocused by remember { mutableStateOf(false) }
          androidx.compose.material3.AlertDialog(
              onDismissRequest = { showExitDialog = false },
              title = { Text("Exit AuraMusic?", fontWeight = FontWeight.Bold) },
              text = { Text("Music will stop playing if you exit.") },
               confirmButton = {
                   Button(
                       onClick = {
                           showExitDialog = false
                           (view.context as? android.app.Activity)?.finish()
                       },
                       modifier = Modifier
                           .onFocusChanged { exitButtonFocused = it.isFocused }
                           .border(
                               width = if (exitButtonFocused) 3.dp else 0.dp,
                               color = if (exitButtonFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                               shape = RoundedCornerShape(8.dp),
                           ),
                   ) {
                       Text("Exit")
                   }
               },
              dismissButton = {
                  Button(
                      onClick = { showExitDialog = false },
                      colors = ButtonDefaults.outlinedButtonColors(
                          containerColor = MaterialTheme.colorScheme.surface,
                          contentColor = MaterialTheme.colorScheme.onSurface,
                      ),
                      modifier = Modifier
                          .onFocusChanged { stayButtonFocused = it.isFocused }
                          .border(
                              width = if (stayButtonFocused) 3.dp else 0.dp,
                              color = if (stayButtonFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                              shape = RoundedCornerShape(8.dp),
                          ),
                  ) {
                      Text("Stay")
                  }
              },
          )
      }

     // Keep screen on when music is playing
     val (keepScreenOnPref, _) = rememberPreference(com.auramusic.app.constants.KeepScreenOn, true)
     DisposableEffect(isPlayingState.value, keepScreenOnPref) {
         val activity = view.context as? android.app.Activity
         val window = activity?.window
         if (isPlayingState.value && keepScreenOnPref) {
             window?.addFlags(
                 android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                 android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                 android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
             )
             // Disable TV screensaver while music is playing
             try {
                 val uiModeManager = activity?.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager
                 uiModeManager?.disableCarMode(0)
             } catch (_: Exception) {}
         } else {
             window?.clearFlags(
                 android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                 android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                 android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
             )
         }
         onDispose {
             window?.clearFlags(
                 android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                 android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                 android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
             )
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
             val currentDestination = navigator.current

             // Full-screen player takes over the entire UI (no top bar)
             if (currentDestination is TvDestination.Player) {
                 TvPlayerScreen(
                     playerConnection = playerConnection,
                     onBackClick = { navigator.popBack() }
                 )
             } else {
                 Box(modifier = Modifier.fillMaxSize()) {
                     // Focus requesters for navigation between top bar and content
                     val topBarFocusRequester = remember { FocusRequester() }
                     val homeFocusRequester = remember { FocusRequester() }
                     val detailFocusRequester = remember { FocusRequester() }
                     val overlayFocusRequester = remember { FocusRequester() }

                      // Set up focus for content screens - focus content when section changes
                       LaunchedEffect(sectionState.value, currentDestination) {
                           kotlinx.coroutines.delay(50)
                           when {
                               currentDestination != TvDestination.Home && currentDestination != TvDestination.Settings -> runCatching { overlayFocusRequester.requestFocus() }
                               sectionState.value == TvSection.HOME -> runCatching { homeFocusRequester.requestFocus() }
                               else -> runCatching { detailFocusRequester.requestFocus() }
                           }
                       }

                     // Content fills full screen - focused panel art starts at y=0 behind nav bar
                     Box(modifier = Modifier.fillMaxSize()) {
                         when (sectionState.value) {
                             TvSection.HOME -> TvHomeScreen(
                                 playerConnection = playerConnection,
                                 focusRequester = homeFocusRequester,
                                 onNavigateUp = { runCatching { topBarFocusRequester.requestFocus() } }
                             )
                             TvSection.LIBRARY -> TvLibraryScreen(
                                 playerConnection = playerConnection,
                                 focusRequester = detailFocusRequester,
                                 onNavigateUp = { runCatching { topBarFocusRequester.requestFocus() } }
                             )
                             TvSection.SEARCH -> TvSearchScreen(
                                 playerConnection = playerConnection,
                                 focusRequester = detailFocusRequester,
                                 onNavigateUp = { runCatching { topBarFocusRequester.requestFocus() } }
                             )
                              TvSection.SETTINGS -> TvSettingsScreen(
                                   onBackClick = { sectionState.value = TvSection.HOME },
                                   onAppearanceClick = { navigator.navigate(TvDestination.AppearanceSettings) },
                                   onAccountClick = { navigator.navigate(TvDestination.AccountSettings) },
                                   onPlaybackClick = { navigator.navigate(TvDestination.PlaybackSettings) },
                                   onContentClick = { navigator.navigate(TvDestination.ContentSettings) },
                                   onStorageClick = { navigator.navigate(TvDestination.StorageSettings) },
                                   onSystemClick = { navigator.navigate(TvDestination.SystemSettings) },
                                   onUpdaterClick = { navigator.navigate(TvDestination.UpdaterScreen) },
                                   onAboutClick = { navigator.navigate(TvDestination.AboutScreen) },
                                   focusRequester = detailFocusRequester,
                                   onNavigateUp = { runCatching { topBarFocusRequester.requestFocus() } }
                               )
                         }

                         // Overlay detail/setting screens if needed
                         if (currentDestination != TvDestination.Home && currentDestination != TvDestination.Settings) {
                             val onUp: () -> Unit = { runCatching { detailFocusRequester.requestFocus() } }
                             // Full screen overlay for all destinations
                             Surface(
                                 modifier = Modifier.fillMaxSize(),
                                 color = MaterialTheme.colorScheme.background,
                             ) {
                                 when (currentDestination) {
                                     is TvDestination.Album -> TvAlbumDetailScreen(
                                         albumId = currentDestination.id,
                                         playerConnection = playerConnection,
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     is TvDestination.Artist -> TvArtistDetailScreen(
                                         artistId = currentDestination.id,
                                         playerConnection = playerConnection,
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     is TvDestination.Playlist -> TvPlaylistDetailScreen(
                                         playlistId = currentDestination.id,
                                         playerConnection = playerConnection,
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     TvDestination.AppearanceSettings -> TvAppearanceSettingsScreen(
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     TvDestination.AccountSettings -> TvAccountSettingsScreen(
                                         onBackClick = { navigator.popBack() },
                                         onLoginClick = { navigator.navigate(TvDestination.Login) },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     TvDestination.PlaybackSettings -> TvPlaybackSettingsScreen(
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                      TvDestination.ContentSettings -> TvContentSettingsScreen(
                                          onBackClick = { navigator.popBack() },
                                          focusRequester = overlayFocusRequester,
                                          onNavigateUp = onUp,
                                      )
                                      TvDestination.StorageSettings -> TvStorageSettingsScreen(
                                          onBackClick = { navigator.popBack() },
                                          focusRequester = overlayFocusRequester,
                                          onNavigateUp = onUp,
                                      )
                                      TvDestination.AboutScreen -> TvAboutScreen(
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     TvDestination.UpdaterScreen -> TvUpdaterScreen(
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     TvDestination.Login -> TvLoginScreen(
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     TvDestination.SystemSettings -> TvSystemSettingsScreen(
                                         onBackClick = { navigator.popBack() },
                                         focusRequester = overlayFocusRequester,
                                         onNavigateUp = onUp,
                                     )
                                     else -> Unit
                                 }
                             }
                         }
                     }

                     // Top bar overlaid on top with transparent background
                     // so focused panel art shows through behind it
                     TvTopBar(
                         sectionState = sectionState,
                         isPlaying = isPlayingState.value,
                         currentSong = currentSong,
                         currentMediaMetadata = currentMediaMetadata,
                         showMiniPlayer = showMiniPlayer,
                         playerConnection = playerConnection,
                         onMiniPlayerClick = { navigator.navigate(TvDestination.Player) },
                         onNavigateDown = {
                             when {
                                 currentDestination != TvDestination.Home -> runCatching { overlayFocusRequester.requestFocus() }
                                 sectionState.value == TvSection.HOME -> runCatching { homeFocusRequester.requestFocus() }
                                 else -> runCatching { detailFocusRequester.requestFocus() }
                             }
                         },
                         onNavigateUp = {
                             runCatching { topBarFocusRequester.requestFocus() }
                         },
                         topBarFocusRequester = topBarFocusRequester,
                     )
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
                text = "AuraMusic Tv",
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
    currentMediaMetadata: com.auramusic.app.models.MediaMetadata? = null,
    showMiniPlayer: Boolean = false,
    playerConnection: PlayerConnection?,
    onMiniPlayerClick: () -> Unit,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null,
    topBarFocusRequester: FocusRequester? = null,
) {
    val focusRequesters = remember {
        if (TvSection.entries.size > 1) {
            List(TvSection.entries.size - 1) { FocusRequester() }
        } else {
            emptyList<FocusRequester>()
        }
    }

    LaunchedEffect(sectionState.value) {
        val index = TvSection.entries.indexOf(sectionState.value)
        if (index >= 0) {
            val requester = when (index) {
                0 -> topBarFocusRequester
                else -> focusRequesters[index - 1]
            }
            requester?.requestFocus()
        }
    }

     Surface(
         modifier = Modifier
             .fillMaxWidth()
             .onPreviewKeyEvent { event ->
                 if (event.type == KeyEventType.KeyDown) {
                     when (event.key) {
                         Key.DirectionDown -> {
                             onNavigateDown?.invoke()
                             true
                         }
                         Key.DirectionUp -> {
                             onNavigateUp?.invoke()
                             true
                         }
                         else -> false
                     }
                 } else {
                     false
                 }
             },
         color = Color.Transparent,
         tonalElevation = 0.dp,
     ) {
         Row(
             modifier = Modifier
                 .fillMaxWidth()
                 .background(
                     Brush.verticalGradient(
                         colors = listOf(
                             Color.Black.copy(alpha = 0.6f),
                             Color.Black.copy(alpha = 0.3f),
                             Color.Transparent,
                         )
                     )
                 )
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
                  text = "AuraMusic Tv",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(start = 12.dp)
              )

             Spacer(modifier = Modifier.weight(1f))

                // Mini player (expanded to show full song and artist info)
                if (showMiniPlayer && (currentSong != null || currentMediaMetadata != null)) {
                    val miniTitle = currentSong?.title ?: currentMediaMetadata?.title.orEmpty()
                    val miniArtists = currentSong?.artists?.joinToString(", ") { it.name }
                        ?: currentMediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty()
                    val miniThumbnail = currentSong?.thumbnailUrl ?: currentMediaMetadata?.thumbnailUrl
                    var miniInfoFocused by remember { mutableStateOf(false) }
                    var miniPlayFocused by remember { mutableStateOf(false) }
                    val miniInfoScale by animateFloatAsState(
                        targetValue = if (miniInfoFocused) 1.03f else 1f,
                        label = "miniInfoScale",
                    )
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .weight(2f, fill = false)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Clickable song info area (opens full player)
                        Surface(
                            onClick = onMiniPlayerClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = miniInfoScale
                                    scaleY = miniInfoScale
                                }
                                .border(
                                    width = if (miniInfoFocused) 3.dp else 0.dp,
                                    color = if (miniInfoFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .onFocusChanged { miniInfoFocused = it.isFocused },
                            shape = RoundedCornerShape(8.dp),
                            color = if (miniInfoFocused)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                                    model = miniThumbnail,
                                    contentDescription = miniTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            // Song info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = miniTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = miniArtists,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                        // Separate play/pause button
                        val playButtonScale by animateFloatAsState(
                            targetValue = if (miniPlayFocused) 1.1f else 1f,
                            label = "miniPlayScale",
                        )
                        IconButton(
                            onClick = { playerConnection?.togglePlayPause() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .graphicsLayer {
                                    scaleX = playButtonScale
                                    scaleY = playButtonScale
                                }
                                .border(
                                    width = if (miniPlayFocused) 3.dp else 0.dp,
                                    color = if (miniPlayFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .background(
                                    if (miniPlayFocused)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else
                                        Color.Transparent
                                )
                                .onFocusChanged { miniPlayFocused = it.isFocused },
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

             Spacer(modifier = Modifier.weight(1f))

            // Navigation buttons
            TvSection.entries.forEachIndexed { index, section ->
                val isSelected = section == sectionState.value
                val buttonFocusRequester = when (index) {
                    0 -> topBarFocusRequester
                    else -> focusRequesters[index - 1]
                }
                TvNavButton(
                    label = section.label,
                    isSelected = isSelected,
                    focusRequester = buttonFocusRequester,
                    onClick = { sectionState.value = section },
                )
            }
         }
     }
 }

/* -------------------------- Home -------------------------- */

data class FocusedItemInfo(
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val thumbnailUrl: String? = null,
    val type: String = "", // "Song", "Artist", "Album", "Playlist"
    val artistId: String? = null,
    val subscriberCountText: String? = null,
    val monthlyListenerCount: String? = null,
)

@Composable
fun TvFocusedDetailPanel(
    focusedItem: FocusedItemInfo?,
    modifier: Modifier = Modifier,
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (focusedItem != null) 1f else 0.3f,
        animationSpec = tween(300),
        label = "detailAlpha",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        if (focusedItem != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = focusedItem.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.45f
                            scaleX = 1.05f
                            scaleY = 1.05f
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.20f),
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 28.dp, top = 95.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { alpha = animatedAlpha },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ) {
                            Text(
                                text = focusedItem.type.ifBlank { "Music" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }

                        Text(
                            text = focusedItem.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (focusedItem.subtitle.isNotBlank()) {
                            Text(
                                text = focusedItem.subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        val stats = listOfNotNull(
                            focusedItem.monthlyListenerCount?.takeIf { it.isNotBlank() },
                            focusedItem.subscriberCountText?.takeIf { it.isNotBlank() },
                        )
                        if (stats.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                stats.forEach { stat ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.12f),
                                    ) {
                                        Text(
                                            text = stat,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }

                        if (focusedItem.description.isNotBlank()) {
                            Text(
                                text = focusedItem.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = if (focusedItem.type == "Artist") 3 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(if (focusedItem.type == "Artist") CircleShape else RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.3f),
                                shape = if (focusedItem.type == "Artist") CircleShape else RoundedCornerShape(20.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = focusedItem.thumbnailUrl,
                            contentDescription = focusedItem.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else {
            // Default state when nothing is focused
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "Browse your music",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
fun TvHomeScreen(
    playerConnection: PlayerConnection?,
    focusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
) {
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
    val communityPlaylists = viewModel.communityPlaylists.collectAsState().value
    val isRefreshing = viewModel.isRefreshing.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val isPlaying = (playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }).value
    val homeListState = rememberLazyListState()

    // Track which content section (row) is currently focused
    var focusedItemIndex by remember { mutableStateOf(-1) }

    // Spotify-style focused detail panel state
    var focusedItem by remember { mutableStateOf<FocusedItemInfo?>(null) }

    // See More overlay state
    var seeMoreItems by remember { mutableStateOf<List<YTItem>?>(null) }
    var seeMoreTitle by remember { mutableStateOf("") }

    val firstHomeFocusIndex = when {
        pinnedSpeedDialItems.isNotEmpty() -> 1
        !quickPicks.isNullOrEmpty() -> 2
        !forgottenFavorites.isNullOrEmpty() -> 3
        !keepListening.isNullOrEmpty() -> 4
        !similarRecommendations.isNullOrEmpty() -> 5
        !accountPlaylists.isNullOrEmpty() -> 5 + (similarRecommendations?.size ?: 0)
        homePage?.sections?.any { it.items.isNotEmpty() } == true -> 6 + (similarRecommendations?.size ?: 0)
        !communityPlaylists.isNullOrEmpty() -> 7 + (similarRecommendations?.size ?: 0) + homePage?.sections.orEmpty().count { it.items.isNotEmpty() }
        else -> -1
    }

    LaunchedEffect(focusedItem?.artistId) {
        val artist = focusedItem?.takeIf { it.type == "Artist" && !it.artistId.isNullOrBlank() }
        val artistId = artist?.artistId ?: return@LaunchedEffect
        YouTube.artist(artistId).onSuccess { page ->
            if (focusedItem?.artistId == artistId) {
                focusedItem = focusedItem?.copy(
                    subtitle = page.monthlyListenerCount ?: focusedItem?.subtitle.orEmpty(),
                    description = page.description
                        ?: page.descriptionRuns?.joinToString("") { it.text }
                        ?: focusedItem?.description.orEmpty(),
                    subscriberCountText = page.subscriberCountText ?: focusedItem?.subscriberCountText,
                    monthlyListenerCount = page.monthlyListenerCount ?: focusedItem?.monthlyListenerCount,
                    thumbnailUrl = page.artist.thumbnail ?: focusedItem?.thumbnailUrl,
                )
            }
        }
    }

    LaunchedEffect(homeListState, homePage?.continuation) {
        snapshotFlow {
            val lastVisibleIndex = homeListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = homeListState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 3
        }
            .distinctUntilChanged()
            .collect { nearBottom ->
                val continuation = homePage?.continuation
                if (nearBottom && continuation != null) {
                    viewModel.loadMoreYouTubeItems(continuation)
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TvFocusedDetailPanel(
            focusedItem = focusedItem,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
            state = homeListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester ?: remember { FocusRequester() })
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                        if (focusedItemIndex == firstHomeFocusIndex) {
                            onNavigateUp?.invoke()
                            true
                        } else {
                            false // Let LazyColumn handle normal focus movement
                        }
                    } else {
                        false
                    }
                },
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
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

        // Speed Dial Section
        pinnedSpeedDialItems.takeIf { it.isNotEmpty() }?.let { speedDialItems ->
           item(key = "speed_dial") {
                YouTubeSectionRow(
                    title = "Speed Dial",
                    items = speedDialItems.take(6).map { it.toYTItem() },
                    playerConnection = playerConnection,
                    onItemFocused = { focusedItem = it },
                    onSeeMore = {
                        seeMoreItems = speedDialItems.take(6).map { it.toYTItem() }
                        seeMoreTitle = "Speed Dial"
                    },
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
                   },
                   modifier = Modifier.onFocusChanged { state ->
                       if (state.hasFocus) focusedItemIndex = 1
                   }
               )
           }
        }

           if (!quickPicks.isNullOrEmpty()) {
               item(key = "quick_picks") {
                    SongRow(
                        title = "Quick picks",
                        songs = quickPicks!!,
                        onSongClick = { song: Song -> playerConnection?.playQueue(YouTubeQueue.radio(song.toMediaMetadata())) },
                        onItemFocused = { focusedItem = it },
                        modifier = Modifier.onFocusChanged { state ->
                            if (state.hasFocus) focusedItemIndex = 2
                        }
                    )
               }
           }

           if (!forgottenFavorites.isNullOrEmpty()) {
               item(key = "forgotten_favorites") {
                    SongRow(
                        title = "Forgotten favorites",
                        songs = forgottenFavorites!!,
                        onSongClick = { song: Song -> playerConnection?.playQueue(YouTubeQueue.radio(song.toMediaMetadata())) },
                        onItemFocused = { focusedItem = it },
                        modifier = Modifier.onFocusChanged { state ->
                            if (state.hasFocus) focusedItemIndex = 3
                        }
                    )
               }
            }

           if (!keepListening.isNullOrEmpty()) {
               item(key = "keep_listening") {
                   LocalItemRow(
                        title = "Keep listening",
                        localItems = keepListening!!,
                        playerConnection = playerConnection,
                        onItemFocused = { focusedItem = it },
                        modifier = Modifier.onFocusChanged { state ->
                           if (state.hasFocus) focusedItemIndex = 4
                       }
                   )
               }
           }


             // Similar recommendations
            similarRecommendations?.takeIf { it.isNotEmpty() }?.let { recommendations ->
                recommendations.forEachIndexed { recIndex, recommendation ->
                    val titleName = when (recommendation.title) {
                        is com.auramusic.app.db.entities.Artist -> recommendation.title.artist.name
                        is com.auramusic.app.db.entities.Album -> recommendation.title.album.title
                        is com.auramusic.app.db.entities.Playlist -> recommendation.title.playlist.name
                        is com.auramusic.app.db.entities.Song -> recommendation.title.song.title
                        else -> recommendation.title.toString()
                    }
                    item(key = "similar_$recIndex") {
                        YouTubeSectionRow(
                            title = "Similar to $titleName",
                            items = recommendation.items,
                            playerConnection = playerConnection,
                            onItemFocused = { focusedItem = it },
                            onSeeMore = {
                                seeMoreItems = recommendation.items
                                seeMoreTitle = "Similar to $titleName"
                            },
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
                            },
                            modifier = Modifier.onFocusChanged { state ->
                                // Calculate dynamic index based on preceding items
                                val baseIndex = 5 // hero(0) + speed_dial(1) + quick_picks(2) + forgotten(3) + keep_listening(4)
                                if (state.hasFocus) focusedItemIndex = baseIndex + recIndex
                            }
                        )
                    }
                }
            }

        // Account playlists
        accountPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
            item(key = "account_playlists") {
                YouTubeSectionRow(
                    title = "Your YouTube Playlists",
                    items = playlists.take(10),
                    playerConnection = playerConnection,
                    onItemFocused = { focusedItem = it },
                    onSeeMore = {
                        seeMoreItems = playlists.take(10)
                        seeMoreTitle = "Your YouTube Playlists"
                    },
                    onYTItemClick = { item: YTItem ->
                        when (item) {
                            is PlaylistItem -> {
                                navigator.navigate(TvDestination.Playlist(item.id))
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier.onFocusChanged { state ->
                        val baseIndex = 5 + (similarRecommendations?.size ?: 0)
                        if (state.hasFocus) focusedItemIndex = baseIndex
                    }
                )
            }
        }

        // Display home page sections from YouTube
        val sections = homePage?.sections.orEmpty()
        sections.forEachIndexed { sectionIndex, section ->
            if (section.items.isNotEmpty()) {
                item(key = "yt_section_$sectionIndex") {
                    val sectionTitle = section.title
                    YouTubeSectionRow(
                        title = sectionTitle,
                        items = section.items,
                        playerConnection = playerConnection,
                        onItemFocused = { focusedItem = it },
                        onSeeMore = {
                            seeMoreItems = section.items
                            seeMoreTitle = sectionTitle
                        },
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
                        },
                        modifier = Modifier.onFocusChanged { state ->
                            val baseIndex = 5 + (similarRecommendations?.size ?: 0) + 1 + sectionIndex
                            if (state.hasFocus) focusedItemIndex = baseIndex
                        }
                    )
                }
            }
        }

        // Community Playlists
        communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
            item(key = "community_playlists") {
                YouTubeSectionRow(
                    title = "Community Playlists",
                    items = communityPlaylists.map { it.playlist },
                    playerConnection = playerConnection,
                    onItemFocused = { focusedItem = it },
                    onSeeMore = {
                        seeMoreItems = communityPlaylists.map { it.playlist }
                        seeMoreTitle = "Community Playlists"
                    },
                    onYTItemClick = { item: YTItem ->
                        when (item) {
                            is PlaylistItem -> navigator.navigate(TvDestination.Playlist(item.id))
                            is SongItem -> playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                            else -> {}
                        }
                    },
                )
            }
        }

        if (quickPicks.isNullOrEmpty() && forgottenFavorites.isNullOrEmpty() && homePage?.sections.isNullOrEmpty() != false) {
            item {
                when {
                    isLoading -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        ) {
                            TvLoadingRow(itemCount = 5, itemWidth = 160.dp, itemHeight = 200.dp)
                            TvLoadingRow(itemCount = 6, itemWidth = 140.dp, itemHeight = 180.dp)
                            TvLoadingRow(itemCount = 5, itemWidth = 160.dp, itemHeight = 200.dp)
                        }
                    }
                    playerConnection == null -> {
                        TvLoadingScreen(
                            message = "Connecting to player…",
                            modifier = Modifier.padding(top = 64.dp),
                        )
                    }
                    else -> {
                        TvEmptyState(
                            message = "No music available",
                            subtitle = "Sign in to YouTube Music or play local songs to get started",
                            modifier = Modifier.padding(top = 64.dp),
                        )
                    }
                }
            }
        }
    }
    }

    // See More full-screen overlay
    if (seeMoreItems != null) {
        BackHandler { seeMoreItems = null }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Header with back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var backFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { seeMoreItems = null },
                        modifier = Modifier
                            .size(56.dp)
                            .onFocusChanged { backFocused = it.isFocused }
                            .border(
                                width = if (backFocused) 3.dp else 0.dp,
                                color = if (backFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape,
                            ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    Text(
                        text = seeMoreTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                // Grid of items
                val distinctItems = seeMoreItems?.distinctBy { it.id }.orEmpty()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(start = 48.dp, top = 24.dp, end = 48.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(distinctItems, key = { it.id }) { item ->
                        YouTubeMediaCard(
                            item = item,
                            onClick = {
                                seeMoreItems = null
                                when (item) {
                                    is SongItem -> playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                    is AlbumItem -> {
                                        val browseId = item.browseId
                                        if (browseId != null) navigator.navigate(TvDestination.Album(browseId))
                                        else playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(playlistId = item.playlistId)))
                                    }
                                    is ArtistItem -> item.id?.let { navigator.navigate(TvDestination.Artist(it)) }
                                    is PlaylistItem -> navigator.navigate(TvDestination.Playlist(item.id))
                                    is EpisodeItem -> playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id)))
                                    is PodcastItem -> item.id?.let { navigator.navigate(TvDestination.Playlist(it)) }
                                    else -> {}
                                }
                            },
                            onFocusChanged = { focusedItem = it },
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
    modifier: Modifier = Modifier,
    onItemFocused: ((FocusedItemInfo?) -> Unit)? = null,
    onSeeMore: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section header with underline
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (items.size > 5) {
                    var seeMoreFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { onSeeMore?.invoke() },
                        modifier = Modifier
                            .onFocusChanged { seeMoreFocused = it.isFocused }
                            .border(
                                width = if (seeMoreFocused) 2.dp else 0.dp,
                                color = if (seeMoreFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = if (seeMoreFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    ) {
                        Text(
                            text = "See more \u2192",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp)
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
            items(items.distinctBy { it.id }, key = { it.id }) { item ->
                YouTubeMediaCard(
                    item = item,
                    onClick = { onYTItemClick(item) },
                    onFocusChanged = onItemFocused,
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
    onFocusChanged: ((FocusedItemInfo?) -> Unit)? = null,
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
    val title = item.tvTitle
    val subtitle = item.tvSubtitle
    val metadata = item.tvMetadata
    val isArtist = item is ArtistItem

    // Build focused item info based on type
    LaunchedEffect(isFocusedState.value) {
        if (isFocusedState.value && onFocusChanged != null) {
            val type = when (item) {
                is SongItem -> "Song"
                is ArtistItem -> "Artist"
                is AlbumItem -> "Album"
                is PlaylistItem -> "Playlist"
                is EpisodeItem -> "Episode"
                is PodcastItem -> "Podcast"
                else -> ""
            }
            onFocusChanged(FocusedItemInfo(
                title = title,
                subtitle = subtitle,
                description = metadata ?: "",
                thumbnailUrl = item.thumbnail,
                type = type,
                artistId = (item as? ArtistItem)?.id,
            ))
        } else if (!isFocusedState.value && onFocusChanged != null) {
            // Don't clear if another item might have focus
        }
    }

    if (isArtist) {
        // Artist: circular thumbnail with name below, no card surface
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(200.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    isFocusedState.value = focusState.isFocused
                    if (focusState.isFocused) {
                        scope.launch { runCatching { bringIntoViewRequester.bringIntoView() } }
                    }
                }
                .clickable(onClick = onClick)
                .padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .border(width = 3.dp, color = borderColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        // Non-artist: regular card with square thumbnail
        val shape = RoundedCornerShape(12.dp)
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(width = 200.dp, height = 220.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    isFocusedState.value = focusState.isFocused
                    if (focusState.isFocused) {
                        scope.launch { runCatching { bringIntoViewRequester.bringIntoView() } }
                    }
                }
                .border(width = 3.dp, color = borderColor, shape = shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val YTItem.tvTitle: String
    get() = title

private val YTItem.tvSubtitle: String
    get() = when (this) {
        is SongItem -> artists.joinToString(", ") { it.name }
        is AlbumItem -> artists?.joinToString(", ") { it.name }.orEmpty()
        is ArtistItem -> "Artist"
        is PlaylistItem -> author?.name ?: "Playlist"
        is PodcastItem -> author?.name ?: "Podcast"
        is EpisodeItem -> author?.name ?: podcast?.name ?: "Episode"
    }

private val YTItem.tvMetadata: String?
    get() = when (this) {
        is SongItem -> duration?.let { makeTimeString(it * 1000L) }
        is AlbumItem -> year?.toString()
        is PlaylistItem -> songCountText
        is PodcastItem -> episodeCountText
        is EpisodeItem -> duration?.let { makeTimeString(it * 1000L) } ?: publishDateText
        is ArtistItem -> null
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
            .size(width = 200.dp, height = 220.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                isFocusedState.value = focusState.isFocused
                if (focusState.isFocused) {
                    scope.launch { runCatching { bringIntoViewRequester.bringIntoView() } }
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
                    .height(150.dp)
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
                style = MaterialTheme.typography.bodyMedium,
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
fun TvLibraryScreen(
    playerConnection: PlayerConnection?,
    focusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
) {
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

    // Track which content section (row) is currently focused
    var focusedItemIndex by remember { mutableStateOf(-1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester ?: remember { FocusRequester() })
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    // Only navigate to top bar if focus is on the first item (header, index 0)
                    if (focusedItemIndex == 0) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false // Let LazyColumn handle normal focus movement
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(start = 48.dp, top = 95.dp, end = 48.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "header") {
            Text(
                text = if (isLoggedIn) "Your YouTube Music library" else "Your library",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 0 }
            )
        }

        if (songs.isNotEmpty()) {
            item(key = "songs") {
                SongRow(
                    title = "Liked songs",
                    songs = songs,
                    onSongClick = { song: Song -> playerConnection?.playSong(song) },
                    modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 1 }
                )
            }
        }
        if (playlists.isNotEmpty()) {
            item(key = "playlists") { 
                LocalItemRow(
                    title = "Playlists", 
                    localItems = playlists, 
                    playerConnection = playerConnection,
                    modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                ) 
            }
        }
        if (artists.isNotEmpty()) {
            item(key = "artists") {
                LocalItemRow(
                    title = "Subscribed artists",
                    localItems = artists,
                    playerConnection = playerConnection,
                    modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 3 }
                )
            }
        }
        if (albums.isNotEmpty()) {
            item(key = "albums") { 
                LocalItemRow(
                    title = "Albums", 
                    localItems = albums, 
                    playerConnection = playerConnection,
                    modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 4 }
                ) 
            }
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
fun TvSearchScreen(
    playerConnection: PlayerConnection?,
    focusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
) {
    val navigator = LocalTvNavigator.current
    val tvSearchViewModel: TvSearchViewModel = hiltViewModel()
    val query = tvSearchViewModel.query.collectAsState().value
    val filter = tvSearchViewModel.filter.collectAsState().value
    val searchResults = tvSearchViewModel.searchResults.collectAsState().value
    val isLoading = tvSearchViewModel.isLoading.collectAsState().value
    val recentSearches = tvSearchViewModel.recentSearches.collectAsState().value

    // Track which item is currently focused
    var focusedItemIndex by remember { mutableStateOf(-1) }

    // Save current query to recent searches when user clicks a result
    val saveQueryToHistory: () -> Unit = {
        if (query.isNotBlank()) {
            tvSearchViewModel.onSearchSubmitted(query)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester ?: remember { FocusRequester() })
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    // Only navigate to top bar if focus is on the search bar (first item, index 0)
                    if (focusedItemIndex == 0) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false // Let LazyColumn handle normal focus movement
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(start = 48.dp, top = 95.dp, end = 48.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "search_bar") {
            OutlinedTextField(
                value = query,
                onValueChange = { tvSearchViewModel.updateQuery(it) },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 0 }
            )
        }

        if (query.isEmpty()) {
            // Show recent searches
            if (recentSearches.isNotEmpty()) {
                item(key = "recent_header") {
                    Text(
                        text = "Recent searches",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(recentSearches, key = { it }) { recentQuery ->
                    TvRecentSearchItem(
                        query = recentQuery,
                        onClick = {
                            tvSearchViewModel.onSearchSubmitted(recentQuery)
                            tvSearchViewModel.updateQuery(recentQuery)
                        },
                        modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 1 }
                    )
                }
            } else {
                item(key = "empty_recent") {
                    Text(
                        text = "Enter search query",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (isLoading) {
            item(key = "loading") {
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
            item(key = "filter_chips") {
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
                    modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 1 }
                )
            }

            // Show search results
            val localSongs = searchResults.localItems.filterIsInstance<Song>().take(5)
            val localArtists = searchResults.localItems.filterIsInstance<Artist>().take(5)
            val localAlbums = searchResults.localItems.filterIsInstance<Album>().take(5)
            val localPlaylists = searchResults.localItems.filterIsInstance<Playlist>().take(5)

            var sectionIndex = 2 // Start after search bar(0) and filter chips(1)

            // Local results sections
            if (localSongs.isNotEmpty()) {
                item(key = "local_songs_header_$sectionIndex") {
                    Text(
                        text = "Local Songs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(localSongs, key = { "local_song_${it.id}" }) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { saveQueryToHistory(); handleSearchItemClick(item, playerConnection, navigator) },
                        modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                    )
                }
                sectionIndex += 2
            }

            if (localArtists.isNotEmpty()) {
                item(key = "local_artists_header_$sectionIndex") {
                    Text(
                        text = "Local Artists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(localArtists, key = { "artist_${it.id}" }) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { saveQueryToHistory(); handleSearchItemClick(item, playerConnection, navigator) },
                        modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = sectionIndex + 1 }
                    )
                }
                sectionIndex += 2
            }

            if (localAlbums.isNotEmpty()) {
                item(key = "local_albums_header_$sectionIndex") {
                    Text(
                        text = "Local Albums",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(localAlbums, key = { "album_${it.id}" }) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { saveQueryToHistory(); handleSearchItemClick(item, playerConnection, navigator) },
                        modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                    )
                }
                sectionIndex += 2
            }

            if (localPlaylists.isNotEmpty()) {
                item(key = "local_playlists_header_$sectionIndex") {
                    Text(
                        text = "Local Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(localPlaylists, key = { "playlist_${it.id}" }) { item ->
                    TvSearchResultItem(
                        item = item,
                        onClick = { saveQueryToHistory(); handleSearchItemClick(item, playerConnection, navigator) },
                        modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                    )
                }
                sectionIndex += 2
            }

            // YouTube results: use summary sections when available (filter == null), otherwise re-categorize
            val searchSummaryPage = searchResults.searchSummaryPage
            if (searchSummaryPage != null && filter == null) {
                // Render YouTube sections with their original titles from the API
                searchSummaryPage.summaries.forEach { summary ->
                    item(key = "yt_section_header_${summary.title}_$sectionIndex") {
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    items(
                        items = summary.items.take(10),
                        key = { "${summary.title}_${it.id}" }
                    ) { item ->
                        TvYTSearchResultItem(
                            item = item,
                            onClick = { saveQueryToHistory(); handleYTSearchItemClick(item, playerConnection, navigator) },
                            modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = sectionIndex + 1 }
                        )
                    }
                    sectionIndex += 2
                }
            } else {
                // Fallback: re-categorize filtered results by type
                val ytSongs = searchResults.ytItems.filterIsInstance<com.auramusic.innertube.models.SongItem>().take(5)
                val ytArtists = searchResults.ytItems.filterIsInstance<com.auramusic.innertube.models.ArtistItem>().take(5)
                val ytAlbums = searchResults.ytItems.filterIsInstance<com.auramusic.innertube.models.AlbumItem>().take(5)
                val ytPlaylists = searchResults.ytItems.filterIsInstance<com.auramusic.innertube.models.PlaylistItem>().take(5)

                if (ytSongs.isNotEmpty()) {
                    item(key = "yt_songs_header_$sectionIndex") {
                        Text(
                            text = "YouTube Songs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    items(ytSongs, key = { "song_${it.id}" }) { item ->
                        TvYTSearchResultItem(
                            item = item,
                            onClick = { saveQueryToHistory(); handleYTSearchItemClick(item, playerConnection, navigator) },
                            modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                        )
                    }
                    sectionIndex += 2
                }

                if (ytArtists.isNotEmpty()) {
                    item(key = "yt_artists_header_$sectionIndex") {
                        Text(
                            text = "YouTube Artists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    items(ytArtists, key = { it.id ?: it.title }) { item ->
                        TvYTSearchResultItem(
                            item = item,
                            onClick = { saveQueryToHistory(); handleYTSearchItemClick(item, playerConnection, navigator) },
                            modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                        )
                    }
                    sectionIndex += 2
                }

                if (ytAlbums.isNotEmpty()) {
                    item(key = "yt_albums_header_$sectionIndex") {
                        Text(
                            text = "YouTube Albums",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    items(ytAlbums, key = { it.browseId ?: it.id }) { item ->
                        TvYTSearchResultItem(
                            item = item,
                            onClick = { saveQueryToHistory(); handleYTSearchItemClick(item, playerConnection, navigator) },
                            modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                        )
                    }
                    sectionIndex += 2
                }

                if (ytPlaylists.isNotEmpty()) {
                    item(key = "yt_playlists_header_$sectionIndex") {
                        Text(
                            text = "YouTube Playlists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    items(ytPlaylists, key = { "ytplaylist_${it.id}" }) { item ->
                        TvYTSearchResultItem(
                            item = item,
                            onClick = { saveQueryToHistory(); handleYTSearchItemClick(item, playerConnection, navigator) },
                            modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                        )
                    }
                    sectionIndex += 2
                }
            }

            if (searchResults.localItems.isEmpty() && searchResults.ytItems.isEmpty() && searchSummaryPage == null) {
                item(key = "no_results") {
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
fun TvRecentSearchItem(query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isFocusedState = remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
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
fun TvSearchResultItem(item: LocalItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isFocusedState = remember { mutableStateOf(false) }
    val isArtist = item is Artist

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .onFocusChanged { isFocusedState.value = it.isFocused }
            .border(
                width = if (isFocusedState.value) 3.dp else 0.dp,
                color = if (isFocusedState.value) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
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
                    .then(if (isArtist) Modifier.clip(CircleShape) else Modifier.clip(RoundedCornerShape(6.dp)))
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
fun TvYTSearchResultItem(item: YTItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isFocusedState = remember { mutableStateOf(false) }
    val isArtist = item is com.auramusic.innertube.models.ArtistItem

    Surface(
        onClick = onClick,
        modifier = modifier
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
                    .then(if (isArtist) Modifier.clip(CircleShape) else Modifier.clip(RoundedCornerShape(6.dp)))
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
    modifier: Modifier = Modifier,
    onItemFocused: ((FocusedItemInfo?) -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header with underline
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp)
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
                    onFocusChanged = onItemFocused,
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
    isRound: Boolean = false,
    onFocusChanged: ((FocusedItemInfo?) -> Unit)? = null,
    artistId: String? = null,
    description: String = "",
    subscriberCountText: String? = null,
    monthlyListenerCount: String? = null,
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

    // Report focus state to parent
    LaunchedEffect(isFocusedState.value) {
        if (onFocusChanged != null) {
            if (isFocusedState.value) {
                onFocusChanged(FocusedItemInfo(
                    title = title,
                    subtitle = subtitle,
                    description = description,
                    thumbnailUrl = thumbnailUrl,
                    type = if (isRound) "Artist" else "Song",
                    artistId = artistId,
                    subscriberCountText = subscriberCountText,
                    monthlyListenerCount = monthlyListenerCount,
                ))
            }
        }
    }

    if (isRound) {
        // Artist card: circular thumbnail with name below, no card surface
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(200.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    isFocusedState.value = focusState.isFocused
                    if (focusState.isFocused) {
                        scope.launch { runCatching { bringIntoViewRequester.bringIntoView() } }
                    }
                }
                .clickable(onClick = onClick)
                .padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .border(width = 3.dp, color = borderColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        // Regular card: square thumbnail with rounded corners
        val shape = RoundedCornerShape(12.dp)
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(width = 200.dp, height = 220.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    isFocusedState.value = focusState.isFocused
                    if (focusState.isFocused) {
                        scope.launch { runCatching { bringIntoViewRequester.bringIntoView() } }
                    }
                }
                .border(width = 3.dp, color = borderColor, shape = shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
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
                    style = MaterialTheme.typography.bodyMedium,
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
}

@Composable
fun LocalItemRow(title: String, localItems: List<LocalItem>, playerConnection: PlayerConnection?, modifier: Modifier = Modifier, onItemFocused: ((FocusedItemInfo?) -> Unit)? = null) {
    val navigator = LocalTvNavigator.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header with underline
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(
                modifier = Modifier
                    .width(80.dp)
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
                        isRound = true,
                        onFocusChanged = onItemFocused,
                        artistId = item.id.takeIf { item.artist.isYouTubeArtist },
                        subscriberCountText = item.artist.subscriberCountText,
                    )
                    is Album -> MediaCard(
                        title = item.album.title,
                        subtitle = item.artists.joinToString(", ") { it.name },
                        thumbnailUrl = item.album.thumbnailUrl,
                        onClick = { navigator.navigate(TvDestination.Album(item.id)) },
                        onFocusChanged = onItemFocused,
                    )
                    is Playlist -> MediaCard(
                        title = item.playlist.name,
                        subtitle = "${item.songCount} songs",
                        thumbnailUrl = item.playlist.thumbnailUrl,
                        onClick = { navigator.navigate(TvDestination.Playlist(item.id)) },
                        onFocusChanged = onItemFocused,
                    )
                    is Song -> MediaCard(
                        title = item.song.title,
                        subtitle = item.artists.joinToString(", ") { it.name },
                        thumbnailUrl = item.song.thumbnailUrl,
                        onClick = { playerConnection?.playQueue(YouTubeQueue.radio(item.toMediaMetadata())) },
                        onFocusChanged = onItemFocused,
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
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section header with underline
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
    val title = item.tvTitle
    val subtitle = item.tvSubtitle
    val metadata = item.tvMetadata

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
                     scope.launch { runCatching { bringIntoViewRequester.bringIntoView() } }
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
                model = item.thumbnail,
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
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = listOfNotNull(subtitle.takeIf { it.isNotBlank() }, metadata).joinToString(" • "),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accent: Color? = null,
) {
    val isFocusedState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocusedState.value) 1.02f else 1f,
        label = "tvSettingsItemScale",
    )
    val borderColor = if (isFocusedState.value) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val resolvedAccent = accent ?: MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                isFocusedState.value = focusState.isFocused
                if (focusState.isFocused) {
                    scope.launch { runCatching { bringIntoViewRequester.bringIntoView() } }
                }
            }
            .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = if (isFocusedState.value)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        tonalElevation = if (isFocusedState.value) 6.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(resolvedAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedAccent,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate to $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun TvSettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun TvSettingsScreen(
    onBackClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onContentClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onSystemClick: () -> Unit = {},
    onUpdaterClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
) {
    val (innerTubeCookie, _) = rememberPreference(
        com.auramusic.app.constants.InnerTubeCookieKey,
        "",
    )
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in com.auramusic.innertube.utils.parseCookieString(innerTubeCookie)
    }

    // Track which content row is currently focused (index 0 == back button / first focusable)
    var focusedItemIndex by remember { mutableStateOf(0) }
    val firstItemFocus = focusRequester ?: remember { FocusRequester() }
    // FocusRequesters for all settings items
    val allFocusRequesters = remember {
        listOf(firstItemFocus) + List(8) { FocusRequester() }
    }

    // Re-claim focus on the previously focused item whenever a sub-settings overlay
    // closes. Settings is a top-level section while the lightweight navigator's
    // base destination remains Home, so returning from a settings child can land
    // on Home rather than a Settings destination.
    val navigator = LocalTvNavigator.current
    val currentDestination = navigator.current
    LaunchedEffect(currentDestination) {
        if (currentDestination == TvDestination.Home || currentDestination == TvDestination.Settings) {
            kotlinx.coroutines.delay(50)
            val index = focusedItemIndex.coerceIn(0, allFocusRequesters.size - 1)
            runCatching { allFocusRequesters[index].requestFocus() }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { firstItemFocus.requestFocus() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (focusedItemIndex == 0) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(start = 64.dp, top = 95.dp, end = 64.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "settings_header") {
            TvSettingsHeader(
                title = "Settings",
                onBackClick = onBackClick,
                focusRequester = firstItemFocus,
                onFocused = { focusedItemIndex = 0 },
            )
        }

        item(key = "account_label") { TvSettingsSectionLabel("Account") }

        item(key = "account_settings") {
            TvSettingsCategoryItem(
                title = "Account",
                subtitle = if (isLoggedIn)
                    "Signed in to YouTube Music — manage account"
                else
                    "Sign in to sync liked songs, playlists and subscriptions",
                onClick = onAccountClick,
                icon = Icons.Filled.Person,
                modifier = Modifier
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 1 }
                    .focusRequester(allFocusRequesters[1]),
            )
        }

        item(key = "app_label") { TvSettingsSectionLabel("App") }

        item(key = "appearance_settings") {
            TvSettingsCategoryItem(
                title = "Appearance",
                subtitle = "Theme, colors, and display settings",
                onClick = onAppearanceClick,
                icon = Icons.Filled.Palette,
                modifier = Modifier
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 2 }
                    .focusRequester(allFocusRequesters[2]),
            )
        }

        item(key = "playback_settings") {
            TvSettingsCategoryItem(
                title = "Playback",
                subtitle = "Audio quality and playback behavior",
                onClick = onPlaybackClick,
                icon = Icons.Filled.MusicNote,
                modifier = Modifier
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 3 }
                    .focusRequester(allFocusRequesters[3]),
            )
        }

        item(key = "content_settings") {
            TvSettingsCategoryItem(
                title = "Content",
                subtitle = "Sync settings and content filters",
                onClick = onContentClick,
                icon = Icons.Filled.Tune,
                modifier = Modifier
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 4 }
                    .focusRequester(allFocusRequesters[4]),
            )
        }

        item(key = "storage_settings") {
            TvSettingsCategoryItem(
                title = "Storage",
                subtitle = "Manage cache and clear accumulated data",
                onClick = onStorageClick,
                icon = Icons.Filled.Storage,
                modifier = Modifier
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 5 }
                    .focusRequester(allFocusRequesters[5]),
            )
        }

        item(key = "system_settings") {
            TvSettingsCategoryItem(
                title = "System",
                subtitle = "HDMI-CEC, auto-play, screen saver, audio output",
                onClick = onSystemClick,
                icon = Icons.Filled.Tune,
                modifier = Modifier
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 6 }
                    .focusRequester(allFocusRequesters[6]),
            )
        }

        item(key = "info_label") { TvSettingsSectionLabel("Information") }

        item(key = "updater_settings") {
            TvSettingsCategoryItem(
                title = "Check for Updates",
                subtitle = "Check for new AuraMusic TV versions",
                onClick = onUpdaterClick,
                icon = Icons.Filled.Refresh,
                modifier = Modifier
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 7 }
                    .focusRequester(allFocusRequesters[7]),
            )
        }

        item(key = "about_settings") {
            TvSettingsCategoryItem(
                title = "About",
                subtitle = "App version, licenses, and information",
                onClick = onAboutClick,
                icon = Icons.Filled.Info,
                modifier = Modifier
                    .onFocusChanged { state -> if (state.hasFocus) focusedItemIndex = 8 }
                    .focusRequester(allFocusRequesters[8]),
            )
        }
    }
}

@Composable
 fun TvAppearanceSettingsScreen(
     onBackClick: () -> Unit,
     focusRequester: FocusRequester? = null,
     onNavigateUp: (() -> Unit)? = null,
 ) {
     BackHandler { onBackClick() }
     val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
     val (selectedFont, onSelectedFontChange) = rememberEnumPreference(
         com.auramusic.app.constants.SelectedFontKey,
         com.auramusic.app.ui.screens.settings.AppFont.DEFAULT,
     )

     val dynamicThemeSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
     val dynamicThemeState: MutableState<Boolean> = if (dynamicThemeSupported) {
         rememberPreference(com.auramusic.app.constants.DynamicThemeKey, defaultValue = true)
     } else {
         remember { mutableStateOf(false) }
     }
     val (selectedThemeColorInt, onSelectedThemeColorChange) = rememberPreference(
         com.auramusic.app.constants.SelectedThemeColorKey,
         defaultValue = com.auramusic.app.ui.theme.DefaultThemeColor.toArgb(),
     )
     val backButtonFocus = focusRequester ?: remember { FocusRequester() }
     var backButtonFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { backButtonFocus.requestFocus() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (backButtonFocused) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(start = 64.dp, top = 95.dp, end = 64.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(64.dp)
                        .focusRequester(backButtonFocus)
                        .onFocusChanged {
                            backButtonFocused = it.isFocused
                        }
                        .border(
                            width = if (backButtonFocused) 3.dp else 0.dp,
                            color = if (backButtonFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
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
                    text = "Appearance",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.size(64.dp)) // Balance the back button
            }
        }

        item {
            Text(
                text = "THEME",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                },
                icon = Icons.Filled.DarkMode,
            )
        }

        // Dynamic theme if supported
        if (dynamicThemeSupported) {
            item {
                TvSettingsCategoryItem(
                    title = "Dynamic Colors",
                    subtitle = if (dynamicThemeState.value) {
                        "Using system wallpaper colors"
                    } else {
                        "Using selected theme color below"
                    },
                    onClick = { dynamicThemeState.value = !dynamicThemeState.value },
                    icon = Icons.Filled.Palette,
                )
            }
        }

        // When dynamic colors are off (or unsupported) show a color picker
        // so the toggle has a visible effect even on TV devices that don't
        // expose system wallpaper colors.
        if (!dynamicThemeSupported || !dynamicThemeState.value) {
            item {
                TvThemeColorPickerRow(
                    selectedColorInt = selectedThemeColorInt,
                    onColorSelected = { onSelectedThemeColorChange(it) },
                )
            }
        }

        item {
            Text(
                text = "FONTS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "App Font",
                subtitle = when (selectedFont) {
                    com.auramusic.app.ui.screens.settings.AppFont.DEFAULT -> "Default"
                    com.auramusic.app.ui.screens.settings.AppFont.OUTFIT -> "Outfit"
                    com.auramusic.app.ui.screens.settings.AppFont.MANROPE -> "Manrope"
                    com.auramusic.app.ui.screens.settings.AppFont.SPACE_GROTESK -> "Space Grotesk"
                },
                onClick = {
                    val fonts = com.auramusic.app.ui.screens.settings.AppFont.values()
                    val nextIndex = (fonts.indexOf(selectedFont) + 1) % fonts.size
                    onSelectedFontChange(fonts[nextIndex])
                },
                icon = Icons.Filled.Tune,
            )
        }
    }
}

private val TvThemeColorPresets: List<androidx.compose.ui.graphics.Color> = listOf(
    com.auramusic.app.ui.theme.DefaultThemeColor, // brand red
    androidx.compose.ui.graphics.Color(0xFF1DB954), // green
    androidx.compose.ui.graphics.Color(0xFF1E88E5), // blue
    androidx.compose.ui.graphics.Color(0xFF8E24AA), // purple
    androidx.compose.ui.graphics.Color(0xFFFB8C00), // orange
    androidx.compose.ui.graphics.Color(0xFF00ACC1), // teal
    androidx.compose.ui.graphics.Color(0xFFEC407A), // pink
)

@Composable
private fun TvThemeColorPickerRow(
    selectedColorInt: Int,
    onColorSelected: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Theme color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Pick the seed color used to generate your TV theme.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvThemeColorPresets.forEach { color ->
                    val argb = color.toArgb()
                    val isSelected = argb == selectedColorInt
                    var focused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .onFocusChanged { focused = it.isFocused }
                            .border(
                                width = when {
                                    focused -> 4.dp
                                    isSelected -> 3.dp
                                    else -> 0.dp
                                },
                                color = when {
                                    focused -> MaterialTheme.colorScheme.primary
                                    isSelected -> MaterialTheme.colorScheme.onSurface
                                    else -> Color.Transparent
                                },
                                shape = CircleShape,
                            )
                            .clickable { onColorSelected(argb) },
                    )
                }
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
