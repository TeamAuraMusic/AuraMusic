/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.EpisodeItem
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.PodcastItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.YTItem
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.ListItemHeight
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.NavigationTitle
import com.auramusic.app.ui.component.YouTubeGridItem
import com.auramusic.app.ui.component.shimmer.ShimmerHost
import com.auramusic.app.ui.component.shimmer.TextPlaceholder
import com.auramusic.app.ui.menu.YouTubeAlbumMenu
import com.auramusic.app.ui.menu.YouTubeArtistMenu
import com.auramusic.app.ui.menu.YouTubePlaylistMenu
import com.auramusic.app.ui.menu.YouTubeSongMenu
import com.auramusic.app.viewmodels.ChartsViewModel
import com.auramusic.app.viewmodels.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val explorePage by exploreViewModel.explorePage.collectAsState()
    val podcastsPage by exploreViewModel.podcastsPage.collectAsState()
    val mixesPage by exploreViewModel.mixesPage.collectAsState()
    val chartsPage by chartsViewModel.chartsPage.collectAsState()
    val isChartsLoading by chartsViewModel.isLoading.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = coroutineScope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(videoId = item.id),
                                    item.toMediaMetadata()
                                )
                            )
                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                            is EpisodeItem -> playerConnection.playQueue(
                                YouTubeQueue.radio(item.toMediaMetadata())
                            )
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )
                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = coroutineScope,
                                    onDismiss = menuState::dismiss
                                )
                                is PodcastItem -> YouTubePlaylistMenu(
                                    playlist = item.asPlaylistItem(),
                                    coroutineScope = coroutineScope,
                                    onDismiss = menuState::dismiss
                                )
                                is EpisodeItem -> YouTubeSongMenu(
                                    song = item.asSongItem(),
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(paddingValues),
        ) {
            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding(),
                ),
            )

            if (isChartsLoading || chartsPage == null || explorePage == null) {
                ShimmerHost {
                    TextPlaceholder(
                        height = 36.dp,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(0.5f),
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(4),
                            contentPadding = PaddingValues(start = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4),
                        ) {
                            items(4) {
                                Row(
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(ListItemHeight - 16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.onSurface),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier.fillMaxHeight(),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(16.dp)
                                                .width(120.dp)
                                                .background(MaterialTheme.colorScheme.onSurface),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .height(12.dp)
                                                .width(80.dp)
                                                .background(MaterialTheme.colorScheme.onSurface),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Podcasts section
                    NavigationTitle(
                        title = stringResource(R.string.podcasts),
                        onClick = {
                            navController.navigate("podcasts")
                        },
                    )

                    // Top 100 Charts section
                    NavigationTitle(
                        title = "Top 100 Charts",
                        onClick = {
                            navController.navigate("top_charts")
                        },
                    )
                }
            } else {
                // New Release Albums
                explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { albums ->
                    NavigationTitle(
                        title = stringResource(R.string.new_release_albums),
                    )
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                    ) {
                        items(albums) { album ->
                            ytGridItem(album)
                        }
                    }
                }

                // Podcasts section
                val podcasts = explorePage?.podcasts?.takeIf { it.isNotEmpty() }
                    ?: podcastsPage?.featured?.takeIf { it.isNotEmpty() }
                if (podcasts != null) {
                    NavigationTitle(
                        title = stringResource(R.string.podcasts),
                        onClick = {
                            navController.navigate("podcasts")
                        },
                    )
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                    ) {
                        items(podcasts.flatMap { it.items }.take(10)) { item ->
                            ytGridItem(item)
                        }
                    }
                }

                // Mixes section
                val mixes = explorePage?.mixes?.takeIf { it.isNotEmpty() }
                    ?: mixesPage?.mixes?.takeIf { it.isNotEmpty() }
                if (mixes != null) {
                    NavigationTitle(
                        title = stringResource(R.string.mixes),
                        onClick = {
                            navController.navigate("mixes")
                        },
                    )
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                    ) {
                        items(mixes.flatMap { it.items }.take(10)) { item ->
                            ytGridItem(item)
                        }
                    }
                }

                // Top 100 Charts section
                NavigationTitle(
                    title = "Top 100 Charts",
                    onClick = {
                        navController.navigate("top_charts")
                    },
                )
            }
        }
    }
}
