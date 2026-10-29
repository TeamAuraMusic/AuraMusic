/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.library

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.ShowCachedPlaylistKey
import com.auramusic.app.constants.ShowDownloadedPlaylistKey
import com.auramusic.app.constants.ShowLikedPlaylistKey
import com.auramusic.app.constants.ShowLikedVideosKey
import com.auramusic.app.constants.ShowSavedVideosKey
import com.auramusic.app.constants.ShowTopPlaylistKey
import com.auramusic.app.constants.YtmSyncKey
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.PlaylistEntity
import com.auramusic.app.ui.component.AlbumGridItem
import com.auramusic.app.ui.component.ArtistGridItem
import com.auramusic.app.ui.component.CollectionSectionHeader
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.PlaylistGridItem
import com.auramusic.app.ui.component.artistSubscriberSubtitle
import com.auramusic.app.ui.menu.AlbumMenu
import com.auramusic.app.ui.menu.ArtistMenu
import com.auramusic.app.ui.menu.PlaylistMenu
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.viewmodels.LibraryMixViewModel
import com.auramusic.app.video.VideoDownloadStore
import com.auramusic.app.video.VideoPlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Library landing page. Music and video are kept in two clearly marked zones: the
 * video zone is the watch history, subscriptions and saved videos; the music zone is
 * the shortcuts plus distinct shelves for artists, albums, playlists and Wrapped
 * playlists. Nothing from one side is flattened into the other.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    onOpenArtists: () -> Unit = {},
    onOpenAlbums: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

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
    var expandedShelf by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()

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

    val likedPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = "shortcut_liked",
            name = stringResource(R.string.liked)
        ),
        songCount = 0,
        songThumbnails = emptyList(),
    )
    val downloadPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = "shortcut_downloaded",
            name = stringResource(R.string.offline)
        ),
        songCount = 0,
        songThumbnails = emptyList(),
    )
    val topPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = "shortcut_top",
            name = stringResource(R.string.my_top) + " $topSize"
        ),
        songCount = 0,
        songThumbnails = emptyList(),
    )
    val cachePlaylist = Playlist(
        playlist = PlaylistEntity(
            id = "shortcut_cached",
            name = stringResource(R.string.cached_playlist)
        ),
        songCount = 0,
        songThumbnails = emptyList(),
    )
    val hiddenPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = "shortcut_hidden",
            name = stringResource(R.string.hidden_playlists)
        ),
        songCount = 0,
        songThumbnails = emptyList(),
    )
    val hiddenPlaylistIds by rememberPreference(com.auramusic.app.constants.HiddenPlaylistIdsKey, emptySet())

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showLikedVideos) = rememberPreference(ShowLikedVideosKey, true)
    val (showSavedVideos) = rememberPreference(ShowSavedVideosKey, true)

    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val libraryPlaylists by viewModel.playlists.collectAsState()

    val wrappedPlaylists = remember(libraryPlaylists) {
        libraryPlaylists.filter { isWrappedPlaylist(it.playlist.name) }
    }
    val wrappedIds = remember(wrappedPlaylists) { wrappedPlaylists.map { it.id }.toSet() }
    val likedPlaylists = remember(libraryPlaylists, wrappedIds) {
        libraryPlaylists.filterNot { it.id in wrappedIds }
    }

    val videoItemCount = videoHistory.size + videoChannels.size +
        (if (showLikedVideos) likedVideos.size else 0) +
        (if (showSavedVideos) savedVideos.size else 0) +
        videoDownloads.size

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
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
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(key = "filter") {
                filterContent()
            }

            // Video first: the library's time-sensitive half is what was watched and
            // what arrived from the channels the user follows.
            if (videoItemCount > 0) {
                item(key = "video_zone") {
                    LibraryZoneHeader(
                        title = stringResource(R.string.videos),
                        subtitle = stringResource(R.string.library_video_subtitle),
                        icon = R.drawable.slow_motion_video,
                        accent = MaterialTheme.colorScheme.tertiary,
                        countText = videoItemCount.toString(),
                    )
                }

                if (videoHistory.isNotEmpty()) {
                    item(key = "video_history") {
                        VideoShelf(
                            titleRes = R.string.video_recently_watched,
                            onSeeMore = { expandedShelf = R.string.video_recently_watched },
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(videoHistory, key = { it.videoId }) { entry ->
                                    VideoHistoryThumb(
                                        entry = entry,
                                        onClick = { playVideoEntry(entry) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (videoChannels.isNotEmpty()) {
                    item(key = "video_channels") {
                        VideoShelf(
                            titleRes = R.string.video_subscriptions,
                            onSeeMore = { expandedShelf = R.string.video_subscriptions },
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(videoChannels, key = { it.channelId }) { channel ->
                                    VideoChannelAvatar(
                                        channel = channel,
                                        onClick = { openVideoChannel(channel.channelId) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (likedVideos.isNotEmpty() && showLikedVideos) {
                    item(key = "video_liked") {
                        VideoShelf(
                            titleRes = R.string.liked_videos,
                            onSeeMore = { expandedShelf = R.string.liked_videos },
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(likedVideos, key = { it.videoId }) { entry ->
                                    VideoHistoryThumb(
                                        entry = entry,
                                        onClick = { playVideoEntry(entry) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (savedVideos.isNotEmpty() && showSavedVideos) {
                    item(key = "video_saved") {
                        VideoShelf(
                            titleRes = R.string.saved_videos,
                            onSeeMore = { expandedShelf = R.string.saved_videos },
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(savedVideos, key = { it.videoId }) { entry ->
                                    VideoHistoryThumb(
                                        entry = entry,
                                        onClick = { playVideoEntry(entry) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (videoDownloads.isNotEmpty()) {
                    item(key = "video_downloads") {
                        VideoShelf(
                            titleRes = R.string.video_downloads,
                            onSeeMore = { navController.navigate("auto_playlist/downloaded") },
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(videoDownloads, key = { it.videoId }) { download ->
                                    VideoDownloadThumb(
                                        download = download,
                                        onClick = {
                                            VideoPlaybackManager.playWithDetails(
                                                context = context,
                                                videoId = download.videoId,
                                                title = download.title,
                                                channelName = download.channelName,
                                                channelThumbnail = null,
                                                thumbnails = listOfNotNull(
                                                    download.thumbnailUrl?.let {
                                                        com.auramusic.innertube.models.Thumbnail(it, null, null)
                                                    },
                                                ),
                                            )
                                        },
                                        onRemove = { VideoDownloadStore.cancel(context, download.videoId) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "music_zone") {
                LibraryZoneHeader(
                    title = stringResource(R.string.filter_music),
                    subtitle = stringResource(R.string.library_music_subtitle),
                    icon = R.drawable.music_note,
                    accent = MaterialTheme.colorScheme.primary,
                    countText = (
                        artists.size + albums.size + likedPlaylists.size + wrappedPlaylists.size
                    ).toString(),
                )
            }

            item(key = "shortcuts") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CollectionSectionHeader(
                        title = stringResource(R.string.your_shortcuts),
                        accent = MaterialTheme.colorScheme.primary,
                    )
                    LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                        if (showLiked) {
                            item(key = "shortcut_liked") {
                                PlaylistGridItem(
                                    playlist = likedPlaylist,
                                    autoPlaylist = true,
                                    modifier = Modifier.clickable {
                                        navController.navigate("auto_playlist/liked")
                                    },
                                )
                            }
                        }
                        if (showDownloaded) {
                            item(key = "shortcut_downloaded") {
                                PlaylistGridItem(
                                    playlist = downloadPlaylist,
                                    autoPlaylist = true,
                                    modifier = Modifier.clickable {
                                        navController.navigate("auto_playlist/downloaded")
                                    },
                                )
                            }
                        }
                        if (showTop) {
                            item(key = "shortcut_top") {
                                PlaylistGridItem(
                                    playlist = topPlaylist,
                                    autoPlaylist = true,
                                    modifier = Modifier.clickable {
                                        navController.navigate("top_playlist/$topSize")
                                    },
                                )
                            }
                        }
                        if (showCached) {
                            item(key = "shortcut_cached") {
                                PlaylistGridItem(
                                    playlist = cachePlaylist,
                                    autoPlaylist = true,
                                    modifier = Modifier.clickable {
                                        navController.navigate("cache_playlist/cached")
                                    },
                                )
                            }
                        }
                        if (hiddenPlaylistIds.isNotEmpty()) {
                            item(key = "shortcut_hidden") {
                                PlaylistGridItem(
                                    playlist = hiddenPlaylist,
                                    autoPlaylist = true,
                                    modifier = Modifier.clickable {
                                        navController.navigate("hidden_playlists")
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (artists.isNotEmpty()) {
                item(key = "artists") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CollectionSectionHeader(
                            title = stringResource(R.string.subscribed_artists),
                            accent = MaterialTheme.colorScheme.primary,
                            trailing = {
                                SectionSeeAll(
                                    accent = MaterialTheme.colorScheme.primary,
                                    onClick = onOpenArtists,
                                )
                            },
                        )
                        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                            items(artists, key = { it.id }) { artistItem ->
                                ArtistGridItem(
                                    artist = artistItem,
                                    subtitle = artistSubscriberSubtitle(artistItem),
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            navController.navigate("artist/${artistItem.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                ArtistMenu(
                                                    originalArtist = artistItem,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item(key = "albums") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CollectionSectionHeader(
                            title = stringResource(R.string.liked_albums),
                            accent = MaterialTheme.colorScheme.primary,
                            trailing = {
                                SectionSeeAll(
                                    accent = MaterialTheme.colorScheme.primary,
                                    onClick = onOpenAlbums,
                                )
                            },
                        )
                        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                            items(albums, key = { it.id }) { albumItem ->
                                AlbumGridItem(
                                    album = albumItem,
                                    isActive = albumItem.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${albumItem.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = albumItem,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            if (likedPlaylists.isNotEmpty()) {
                item(key = "liked_playlists") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CollectionSectionHeader(
                            title = stringResource(R.string.liked_playlists),
                            accent = MaterialTheme.colorScheme.primary,
                            trailing = {
                                SectionSeeAll(
                                    accent = MaterialTheme.colorScheme.primary,
                                    onClick = onOpenPlaylists,
                                )
                            },
                        )
                        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                            items(likedPlaylists, key = { it.id }) { playlistItem ->
                                PlaylistGridItem(
                                    playlist = playlistItem,
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            navController.navigate("local_playlist/${playlistItem.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                PlaylistMenu(
                                                    playlist = playlistItem,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            if (wrappedPlaylists.isNotEmpty()) {
                item(key = "wrapped_playlists") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CollectionSectionHeader(
                            title = stringResource(R.string.from_your_wrapped),
                            accent = MaterialTheme.colorScheme.secondary,
                            trailing = {
                                SectionSeeAll(
                                    accent = MaterialTheme.colorScheme.secondary,
                                    onClick = onOpenPlaylists,
                                )
                            },
                        )
                        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                            items(wrappedPlaylists, key = { it.id }) { playlistItem ->
                                PlaylistGridItem(
                                    playlist = playlistItem,
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            navController.navigate("local_playlist/${playlistItem.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                PlaylistMenu(
                                                    playlist = playlistItem,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(16.dp))
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

/** A zone heading that says which half of the library this is. */
@Composable
private fun LibraryZoneHeader(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    accent: Color,
    countText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = accent.copy(alpha = 0.10f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.18f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.14f),
                contentColor = accent,
            ) {
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** The chevron used by every shelf heading to open the full collection. */
@Composable
private fun SectionSeeAll(accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = accent.copy(alpha = 0.14f),
        contentColor = accent,
        modifier = Modifier.size(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = stringResource(R.string.see_more),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** One horizontal video shelf under the video zone heading. */
@Composable
private fun VideoShelf(
    titleRes: Int,
    onSeeMore: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CollectionSectionHeader(
            title = stringResource(titleRes),
            accent = MaterialTheme.colorScheme.tertiary,
            trailing = if (onSeeMore != null) {
                {
                    SectionSeeAll(
                        accent = MaterialTheme.colorScheme.tertiary,
                        onClick = onSeeMore,
                    )
                }
            } else {
                null
            },
        )
        content()
        Spacer(Modifier.height(8.dp))
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
    BasicAlertDialog(onDismissRequest = onDismiss) {
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
                Spacer(Modifier.height(8.dp))
                if (channels.isNotEmpty()) {
                    // Channel shelves grid by avatar rather than thumbnail.
                    LazyVerticalGrid(
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
                    LazyVerticalGrid(
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
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Wrapped saves its monthly playlist as "AuraMusic <Month> <Year>". The month is
 *  localised, so only the branding and the year are matched rather than English names. */
private val wrappedPlaylistName = Regex(
    "^AuraMusic .+ \\d{4}$",
    RegexOption.IGNORE_CASE,
)

private fun isWrappedPlaylist(name: String): Boolean = wrappedPlaylistName.matches(name)
