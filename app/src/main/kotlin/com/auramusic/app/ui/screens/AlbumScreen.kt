/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.auramusic.app.ui.utils.toHighQualityThumbnail
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.auramusic.app.ui.component.AuraCanvasImage
import androidx.media3.common.util.UnstableApi
import com.auramusic.app.LocalDatabase
import com.auramusic.app.LocalDownloadUtil
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.constants.HideVideoSongsKey
import com.auramusic.app.db.entities.Album
import com.auramusic.app.playback.queues.LocalAlbumRadio
import com.auramusic.app.ui.component.CollectionActionRow
import com.auramusic.app.ui.component.CollectionArtwork
import com.auramusic.app.ui.component.CollectionHeroBackdrop
import com.auramusic.app.ui.component.CollectionSectionHeader
import com.auramusic.app.ui.component.CollectionTitleBlock
import com.auramusic.app.ui.component.HeroIconButton
import com.auramusic.app.ui.component.HeroPillButton
import com.auramusic.app.ui.component.IconButton
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.SongListItem
import com.auramusic.app.ui.component.YouTubeGridItem
import com.auramusic.app.ui.component.rememberArtworkAccent
import com.auramusic.app.ui.menu.AlbumMenu
import com.auramusic.app.ui.menu.SelectionSongMenu
import com.auramusic.app.ui.menu.SongMenu
import com.auramusic.app.ui.menu.YouTubeAlbumMenu
import com.auramusic.app.ui.utils.backToMain
import com.auramusic.app.utils.makeTimeString
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.viewmodels.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(key = HideVideoSongsKey, defaultValue = false)

    val filteredSongs = remember(albumWithSongs, hideExplicit, hideVideoSongs) {
        var songs = albumWithSongs?.songs ?: emptyList()
        if (hideExplicit) {
            songs = songs.filter { !it.song.explicit }
        }
        if (hideVideoSongs) {
            songs = songs.filter { !it.song.isVideo }
        }
        songs
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val accent = rememberArtworkAccent(albumWithSongs?.album?.thumbnailUrl)
    val lazyListState = rememberLazyListState()

    // The hero is drawn behind the transparent top bar, so the list itself gets no top
    // padding: the system bar + app bar height is applied inside the backdrop instead.
    val layoutDirection = LocalLayoutDirection.current
    val playerAwarePadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val heroTopPadding = playerAwarePadding.calculateTopPadding()
    val listContentPadding = PaddingValues(
        start = playerAwarePadding.calculateStartPadding(layoutDirection),
        end = playerAwarePadding.calculateEndPadding(layoutDirection),
        bottom = playerAwarePadding.calculateBottomPadding() + 16.dp,
    )

    // Once the cover and the hero title have scrolled under the top bar it turns solid and
    // shows the title (12dp top gap + 232dp cover + 28dp spacer + ~28dp of the title).
    val collapseThresholdPx = with(LocalDensity.current) { 300.dp.roundToPx() }
    val headerScrolledPast by remember(collapseThresholdPx) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > collapseThresholdPx
        }
    }
    val solidTopBar = headerScrolledPast || inSelectMode
    val surfaceColor = MaterialTheme.colorScheme.surface
    val topBarColor by animateColorAsState(
        targetValue = if (solidTopBar) surfaceColor else surfaceColor.copy(alpha = 0f),
        label = "albumTopBarColor",
    )
    val topBarTitleAlpha by animateFloatAsState(
        targetValue = if (solidTopBar) 1f else 0f,
        label = "albumTopBarTitleAlpha",
    )

    LazyColumn(
        state = lazyListState,
        contentPadding = listContentPadding,
    ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
            item(key = "album_header") {
                CollectionHeroBackdrop(
                    imageUrl = albumWithSongs.album.thumbnailUrl,
                    accent = accent,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = heroTopPadding + 12.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CollectionArtwork(
                            accent = accent,
                            size = 232.dp,
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            @UnstableApi
                            AuraCanvasImage(
                                title = albumWithSongs.album.title,
                                artist = albumWithSongs.songs.firstOrNull()?.artists?.firstOrNull()?.name,
                                staticImageUrl = albumWithSongs.album.thumbnailUrl?.toHighQualityThumbnail(),
                                contentScale = ContentScale.Crop,
                                candidateTracks = albumWithSongs.songs.take(8).mapNotNull { it.song.id }, // for on-demand remote lookup
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Year, song count and total duration as chips
                        val totalDuration = albumWithSongs.songs.sumOf { it.song.duration }
                        val songCount = pluralStringResource(
                            R.plurals.n_song,
                            albumWithSongs.songs.size,
                            albumWithSongs.songs.size
                        )
                        val chips = buildList {
                            albumWithSongs.album.year?.let { add(it.toString()) }
                            add(songCount)
                            if (totalDuration > 0) {
                                add(makeTimeString(totalDuration * 1000L))
                            }
                        }

                        CollectionTitleBlock(
                            title = albumWithSongs.album.title,
                            overline = "Album",
                            accent = accent,
                            chips = chips,
                            subtitle = {
                                // Clickable artist names
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                            ).toSpanStyle()
                                        ) {
                                            albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                                val link = LinkAnnotation.Clickable(artist.id) {
                                                    navController.navigate("artist/${artist.id}")
                                                }
                                                withLink(link) {
                                                    append(artist.name)
                                                }
                                                if (index != albumWithSongs.artists.lastIndex) {
                                                    append(", ")
                                                }
                                            }
                                        }
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action buttons: like, play, shuffle, menu.
                        // On narrow screens shuffle collapses to an icon so the row never overflows.
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val compactActions = maxWidth < 400.dp
                            val isLiked = albumWithSongs.album.bookmarkedAt != null
                            val onPlay: () -> Unit = {
                                playerConnection.service.getAutomix(playlistId)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs),
                                )
                            }
                            val onShuffle: () -> Unit = {
                                playerConnection.service.getAutomix(playlistId)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())),
                                )
                            }

                            CollectionActionRow {
                                HeroIconButton(
                                    icon = if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
                                    onClick = {
                                        database.query {
                                            update(albumWithSongs.album.toggleLike())
                                        }
                                    },
                                    tint = if (isLiked) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    },
                                )

                                HeroPillButton(
                                    text = stringResource(R.string.play),
                                    icon = R.drawable.play,
                                    onClick = onPlay,
                                    accent = accent,
                                    filled = true,
                                    modifier = Modifier.weight(1f, fill = false),
                                )

                                if (compactActions) {
                                    HeroIconButton(
                                        icon = R.drawable.shuffle,
                                        onClick = onShuffle,
                                        contentDescription = stringResource(R.string.shuffle),
                                    )
                                } else {
                                    HeroPillButton(
                                        text = stringResource(R.string.shuffle),
                                        icon = R.drawable.shuffle,
                                        onClick = onShuffle,
                                        accent = accent,
                                        filled = false,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                }

                                HeroIconButton(
                                    icon = R.drawable.more_vert,
                                    onClick = {
                                        menuState.show {
                                            AlbumMenu(
                                                originalAlbum = Album(
                                                    albumWithSongs.album,
                                                    albumWithSongs.artists
                                                ),
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (filteredSongs.isNotEmpty()) {
                item(key = "songs_title") {
                    CollectionSectionHeader(
                        title = stringResource(R.string.songs),
                        accent = accent,
                        modifier = Modifier.animateItem(),
                    )
                }

                itemsIndexed(
                    items = filteredSongs,
                    key = { _, song -> song.id },
                ) { index, song ->
                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    }

                    SongListItem(
                        song = song,
                        albumIndex = index + 1,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        trailingContent = {
                            if (inSelectMode) {
                                Checkbox(
                                    checked = song.id in selection,
                                    onCheckedChange = onCheckedChange
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
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
                            }
                        },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .combinedClickable(
                                onClick = {
                                    if (inSelectMode) {
                                        onCheckedChange(song.id !in selection)
                                    } else if (song.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.service.getAutomix(playlistId)
                                        playerConnection.playQueue(
                                            LocalAlbumRadio(albumWithSongs, startIndex = index),
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!inSelectMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        inSelectMode = true
                                        onCheckedChange(true)
                                    }
                                },
                            ),
                    )
                }
            }

            if (otherVersions.isNotEmpty()) {
                item(key = "other_versions_title") {
                    CollectionSectionHeader(
                        title = stringResource(R.string.other_versions),
                        accent = accent,
                        modifier = Modifier
                            .animateItem()
                            .padding(top = 12.dp),
                    )
                }
                item(key = "other_versions_list") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                    ) {
                        items(
                            items = otherVersions.distinctBy { it.id },
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }
            }
        } else {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = heroTopPadding + 32.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            }
        }
    }

    TopAppBar(
        title = {
            if (inSelectMode) {
                Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
            } else {
                Text(
                    text = albumWithSongs?.album?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = topBarTitleAlpha }
                )
            }
        },
        navigationIcon = {
            if (inSelectMode) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(
                    onClick = { navController.navigateUp() },
                    onLongClick = { navController.backToMain() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            }
        },
        actions = {
            if (inSelectMode) {
                Checkbox(
                    checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                    onCheckedChange = {
                        if (selection.size == filteredSongs.size) {
                            selection.clear()
                        } else {
                            selection.clear()
                            selection.addAll(filteredSongs.map { it.id })
                        }
                    }
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = selection.mapNotNull { songId ->
                                    filteredSongs.find { it.id == songId }
                                },
                                onDismiss = menuState::dismiss,
                                clearAction = onExitSelectionMode
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topBarColor,
            scrolledContainerColor = topBarColor,
        )
    )
}
