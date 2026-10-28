/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.AlbumViewTypeKey
import com.auramusic.app.constants.CONTENT_TYPE_HEADER
import com.auramusic.app.constants.CONTENT_TYPE_PLAYLIST
import com.auramusic.app.constants.GridItemSize
import com.auramusic.app.constants.GridItemsSizeKey
import com.auramusic.app.constants.GridThumbnailHeight
import com.auramusic.app.constants.LibraryViewType
import com.auramusic.app.constants.MixSortDescendingKey
import com.auramusic.app.constants.MixSortType
import com.auramusic.app.constants.MixSortTypeKey
import com.auramusic.app.constants.ShowCachedPlaylistKey
import com.auramusic.app.constants.ShowDownloadedPlaylistKey
import com.auramusic.app.constants.ShowLikedPlaylistKey
import com.auramusic.app.constants.ShowLikedVideosKey
import com.auramusic.app.constants.ShowSavedVideosKey
import com.auramusic.app.constants.ShowTopPlaylistKey
import com.auramusic.app.constants.YtmSyncKey
import com.auramusic.app.db.entities.Album
import com.auramusic.app.db.entities.Artist
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.PlaylistEntity
import com.auramusic.app.extensions.reversed
import com.auramusic.app.ui.component.AlbumGridItem
import com.auramusic.app.ui.component.AlbumListItem
import com.auramusic.app.ui.component.ArtistGridItem
import com.auramusic.app.ui.component.ArtistListItem
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.PlaylistGridItem
import com.auramusic.app.ui.component.PlaylistListItem
import com.auramusic.app.ui.component.SortHeader
import com.auramusic.app.ui.component.artistSubscriberSubtitle
import com.auramusic.app.ui.menu.AlbumMenu
import com.auramusic.app.ui.menu.ArtistMenu
import com.auramusic.app.ui.menu.PlaylistMenu
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.viewmodels.LibraryMixViewModel
import com.auramusic.app.video.VideoDownloadStore
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.models.Thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    // Video "Recently watched" history + subscribed video channels, shown at the
    // top of the Library landing page just like YouTube. Observed as flows so a
    // like/save from the floating player updates the shelf while this screen is
    // already composed - a one-shot read only refreshed when the screen was rebuilt.
    val context = LocalContext.current
    val videoHistory by remember {
        VideoPlaybackManager.videoHistoryFlow(context)
    }.collectAsState(initial = emptyList())
    val videoChannels by remember {
        VideoPlaybackManager.subscribedChannelsFlow(context)
    }.collectAsState(initial = emptyList())
    val likedVideos by remember {
        VideoPlaybackManager.likedVideosFlow(context)
    }.collectAsState(initial = emptyList())
    val savedVideos by remember {
        VideoPlaybackManager.savedVideosFlow(context)
    }.collectAsState(initial = emptyList())
    // Which shelf the see-more grid is showing, if any.
    var expandedShelf by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Downloaded videos, read from the store so the shelf is the same list the player
    // will play from.
    LaunchedEffect(Unit) { VideoDownloadStore.load(context) }
    val videoDownloads by VideoDownloadStore.entries(context).collectAsState()
    val playVideoEntry: (VideoPlaybackManager.VideoHistoryEntry) -> Unit = { entry ->
        VideoPlaybackManager.play(
            context = context,
            videoId = entry.videoId,
            title = entry.title,
            channelName = entry.channelName,
            // A finished video starts over; a partially watched one picks up where it left.
            startPositionMs = if (entry.shouldResume) entry.positionMs else 0L,
        )
    }
     val openVideoChannel: (String) -> Unit = { channelId ->
         navController.navigate("youtube_channel/$channelId?ts=${System.currentTimeMillis()}")
     }

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val hiddenPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = "hidden_playlists",
                name = stringResource(R.string.hidden_playlists)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )
    val hiddenPlaylistIds by rememberPreference(com.auramusic.app.constants.HiddenPlaylistIdsKey, emptySet())

    val uploadedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.uploaded_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

     val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
     val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
     val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
     val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
     val (showLikedVideos) = rememberPreference(ShowLikedVideosKey, true)
     val (showSavedVideos) = rememberPreference(ShowSavedVideosKey, true)
     // Uploaded songs feature is temporarily disabled
     val showUploaded = false // rememberPreference(ShowUploadedPlaylistKey, true)

    val albums = viewModel.albums.collectAsState()
    val artist = viewModel.artists.collectAsState()
    val playlist = viewModel.playlists.collectAsState()

    var allItems = albums.value + artist.value + playlist.value
    val collator = Collator.getInstance(Locale.getDefault())
    collator.strength = Collator.PRIMARY
    allItems =
        when (sortType) {
            MixSortType.CREATE_DATE ->
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
                    }
                }

            MixSortType.NAME ->
                allItems.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            else -> ""
                        }
                    },
                )

            MixSortType.LAST_UPDATED ->
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        else -> LocalDateTime.now()
                    }
                }
        }.reversed(sortDescending)


    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
         if (ytmSync) {
             withContext(Dispatchers.IO) {
                 viewModel.syncAllLibrary()
             }
         }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                        MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        MixSortType.NAME -> R.string.sort_by_name
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp),
            ) {
                Icon(
                    painter =
                    painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        },
                    ),
                    contentDescription = null,
                )
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
    ) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                         contentType = CONTENT_TYPE_HEADER,
                     ) {
                         headerContent()
                     }

                     if (videoHistory.isNotEmpty() || videoChannels.isNotEmpty()) {
                         item(
                             key = "videoSections",
                             contentType = CONTENT_TYPE_HEADER,
                         ) {
                             LibraryVideoSections(
                                 history = videoHistory,
                                 channels = videoChannels,
                                 onPlayVideo = playVideoEntry,
                                 onOpenChannel = openVideoChannel,
                                 onSeeMore = { expandedShelf = it },
                             )
                         }
                     }
                     if (likedVideos.isNotEmpty() && showLikedVideos) {
                         item(
                             key = "likedVideosSection",
                             contentType = CONTENT_TYPE_HEADER,
                         ) {
                             Column(modifier = Modifier.fillMaxWidth()) {
                                 VideoLibrarySectionHeader(R.string.liked_videos) {
                                        expandedShelf = R.string.liked_videos
                                    }
                                 LazyRow(
                                     contentPadding = PaddingValues(horizontal = 12.dp),
                                     horizontalArrangement = Arrangement.spacedBy(10.dp),
                                 ) {
                                     items(likedVideos) { entry ->
                                         VideoHistoryThumb(
                                             entry = entry,
                                             onClick = { playVideoEntry(entry) },
                                         )
                                     }
                                 }
                                 Spacer(modifier = Modifier.height(8.dp))
                             }
                         }
                     }
                     if (savedVideos.isNotEmpty() && showSavedVideos) {
                         item(
                             key = "savedVideosSection",
                             contentType = CONTENT_TYPE_HEADER,
                         ) {
                             Column(modifier = Modifier.fillMaxWidth()) {
                                 VideoLibrarySectionHeader(R.string.saved_videos) {
                                        expandedShelf = R.string.saved_videos
                                    }
                                 LazyRow(
                                     contentPadding = PaddingValues(horizontal = 12.dp),
                                     horizontalArrangement = Arrangement.spacedBy(10.dp),
                                 ) {
                                     items(savedVideos) { entry ->
                                         VideoHistoryThumb(
                                             entry = entry,
                                             onClick = { playVideoEntry(entry) },
                                         )
                                     }
                                 }
                                 Spacer(modifier = Modifier.height(8.dp))
                             }
                         }
                     }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = likedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/liked")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = downloadPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/downloaded")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = topPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("top_playlist/$topSize")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = cachePlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("cache_playlist/cached")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (hiddenPlaylistIds.isNotEmpty()) {
                        item(
                            key = "hiddenPlaylists",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = hiddenPlaylist,
                                autoPlaylist = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("hidden_playlists") }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showUploaded) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = uploadedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("auto_playlist/uploaded")
                                        }
                                        .animateItem(),
                            )
                        }
                    }

                    items(
                        items = allItems.distinctBy { it.id },
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistListItem(
                                    playlist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("local_playlist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    subtitle = artistSubscriberSubtitle(item),
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("artist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("album/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (videoHistory.isNotEmpty() || videoChannels.isNotEmpty()) {
                        item(
                            key = "videoSections",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            LibraryVideoSections(
                                history = videoHistory,
                                channels = videoChannels,
                                onPlayVideo = playVideoEntry,
                                onOpenChannel = openVideoChannel,
                                onSeeMore = { expandedShelf = it },
                            )
                        }
                    }

                    if (likedVideos.isNotEmpty() && showLikedVideos) {
                        item(
                            key = "likedVideosSection",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                VideoLibrarySectionHeader(R.string.liked_videos) {
                                        expandedShelf = R.string.liked_videos
                                    }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(likedVideos) { entry ->
                                        VideoHistoryThumb(
                                            entry = entry,
                                            onClick = { playVideoEntry(entry) },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                    if (savedVideos.isNotEmpty() && showSavedVideos) {
                        item(
                            key = "savedVideosSection",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                VideoLibrarySectionHeader(R.string.saved_videos) {
                                        expandedShelf = R.string.saved_videos
                                    }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(savedVideos) { entry ->
                                        VideoHistoryThumb(
                                            entry = entry,
                                            onClick = { playVideoEntry(entry) },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = likedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/liked")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = downloadPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/downloaded")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = topPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("top_playlist/$topSize")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = cachePlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("cache_playlist/cached")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (hiddenPlaylistIds.isNotEmpty()) {
                        item(
                            key = "hiddenPlaylists",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = hiddenPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("hidden_playlists") }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showUploaded) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = uploadedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("auto_playlist/uploaded")
                                        }
                                        .animateItem(),
                            )
                        }
                    }

                    items(
                        items = allItems.distinctBy { it.id },
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistGridItem(
                                    playlist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("local_playlist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Artist -> {
                                ArtistGridItem(
                                    artist = item,
                                    subtitle = artistSubscriberSubtitle(item),
                                    fillMaxWidth = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("artist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumGridItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    fillMaxWidth = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("album/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }
                }
        }

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )

        // One grid at a time: which shelf "see more" opened.
        expandedShelf?.let { shelf ->
            when (shelf) {
                R.string.video_recently_watched -> VideoShelfGridDialog(
                    titleRes = shelf,
                    entries = videoHistory,
                    onPlayVideo = playVideoEntry,
                    onDismiss = { expandedShelf = null },
                )
                R.string.liked_videos -> VideoShelfGridDialog(
                    titleRes = shelf,
                    entries = likedVideos,
                    onPlayVideo = playVideoEntry,
                    onDismiss = { expandedShelf = null },
                )
                R.string.saved_videos -> VideoShelfGridDialog(
                    titleRes = shelf,
                    entries = savedVideos,
                    onPlayVideo = playVideoEntry,
                    onDismiss = { expandedShelf = null },
                )
                R.string.video_subscriptions -> VideoShelfGridDialog(
                    titleRes = shelf,
                    entries = emptyList(),
                    channels = videoChannels,
                    onOpenChannel = openVideoChannel,
                    onDismiss = { expandedShelf = null },
                )
            }
        }
    }
}

/**
 * "Recently watched" + "Subscribed channels" rows at the top of the Library
 * landing page, mirroring YouTube's library layout for videos.
 */
@Composable
private fun LibraryVideoSections(
    history: List<VideoPlaybackManager.VideoHistoryEntry>,
    channels: List<VideoPlaybackManager.VideoSubscribedChannel>,
    onPlayVideo: (VideoPlaybackManager.VideoHistoryEntry) -> Unit,
    onOpenChannel: (String) -> Unit,
    onSeeMore: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (history.isNotEmpty()) {
            VideoLibrarySectionHeader(R.string.video_recently_watched) {
                onSeeMore(R.string.video_recently_watched)
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(history) { entry ->
                    VideoHistoryThumb(
                        entry = entry,
                        onClick = { onPlayVideo(entry) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (channels.isNotEmpty()) {
            VideoLibrarySectionHeader(R.string.video_subscriptions) {
                onSeeMore(R.string.video_subscriptions)
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(channels) { channel ->
                    VideoChannelAvatar(
                        channel = channel,
                        onClick = { onOpenChannel(channel.channelId) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** One downloaded video on the shelf, with the size it occupies on the device. */
@Composable
private fun VideoDownloadThumb(
    download: VideoDownloadStore.Entry,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (!download.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = download.thumbnailUrl,
                    contentDescription = download.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Icon(
                painter = painterResource(R.drawable.download),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(16.dp),
            )
            // Removing is a deliberate act, so it gets its own control rather than
            // making a long press on the tile do something unexpected.
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.remove_download),
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = download.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            // What it was saved at, not what it is playing at: this is the copy on the
            // device, and a later change of preference must not rewrite history.
            text = stringResource(
                R.string.video_download_format,
                formatDownloadSize(download.sizeBytes),
                download.quality.label,
            ) + (if (download.codec.isAuto) "" else " \u00b7 " + download.codec.label),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

private fun formatDownloadSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format("%.1f GB", mb / 1024.0) else String.format("%.0f MB", mb)
}

@Composable
private fun VideoLibrarySectionHeader(
    titleRes: Int,
    onSeeMore: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        // The shelves show a handful of rows and cut the rest off with no way to reach
        // them; "see more" opens the whole shelf as a grid.
        if (onSeeMore != null) {
            IconButton(onClick = onSeeMore, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = stringResource(R.string.see_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Full-screen grid of one Library shelf. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoShelfGridDialog(
    titleRes: Int,
    entries: List<VideoPlaybackManager.VideoHistoryEntry>,
    channels: List<VideoPlaybackManager.VideoSubscribedChannel> = emptyList(),
    onPlayVideo: (VideoPlaybackManager.VideoHistoryEntry) -> Unit = {},
    onOpenChannel: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.dismiss),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (channels.isNotEmpty()) {
                    // Channel shelves grid by avatar rather than thumbnail.
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 84.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(channels, key = { it.channelId }) { channel ->
                            VideoChannelAvatar(
                                channel = channel,
                                onClick = { onOpenChannel(channel.channelId) },
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 168.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(entries, key = { it.videoId }) { entry ->
                            VideoHistoryThumb(
                                entry = entry,
                                onClick = { onPlayVideo(entry) },
                                compact = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoHistoryThumb(
    entry: VideoPlaybackManager.VideoHistoryEntry,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val watchProgress = entry.watchProgress

    Column(
        modifier = Modifier
            // In the see-more grid the tile is sized by its column, not by a fixed
            // width; on the shelf it keeps the original strip size.
            .then(if (compact) Modifier.fillMaxWidth() else Modifier.width(170.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!entry.videoThumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.videoThumbnailUrl,
                    contentDescription = entry.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.slow_motion_video),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Watch progress bar at bottom of thumbnail (like YouTube)
            if (watchProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(watchProgress)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        entry.channelName.takeIf { it.isNotEmpty() }?.let { channel ->
            Text(
                text = channel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun VideoChannelAvatar(
    channel: VideoPlaybackManager.VideoSubscribedChannel,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (!channel.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.avatarUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = channel.name,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
