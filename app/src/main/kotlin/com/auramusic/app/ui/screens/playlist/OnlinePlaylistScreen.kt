/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.app.LocalDatabase
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.LocalSyncUtils
import com.auramusic.app.R
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.PlaylistEntity
import com.auramusic.app.db.entities.PlaylistSongMap
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.playback.queues.YouTubePlaylistQueue
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.ui.component.CollectionActionRow
import com.auramusic.app.ui.component.CollectionArtwork
import com.auramusic.app.ui.component.CollectionHeroBackdrop
import com.auramusic.app.ui.component.CollectionSectionHeader
import com.auramusic.app.ui.component.CollectionTitleBlock
import com.auramusic.app.ui.component.HeroIconButton
import com.auramusic.app.ui.component.HeroPillButton
import com.auramusic.app.ui.component.IconButton
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.YouTubeListItem
import com.auramusic.app.ui.component.rememberArtworkAccent
import com.auramusic.app.ui.menu.YouTubePlaylistMenu
import com.auramusic.app.ui.menu.YouTubeSelectionSongMenu
import com.auramusic.app.ui.menu.YouTubeSongMenu
import com.auramusic.app.ui.utils.backToMain
import com.auramusic.app.utils.makeTimeString
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val HeroKey = "playlist_header"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs.mapIndexed { i, s -> i to s }
        else songs.mapIndexed { i, s -> i to s }.filter {
            it.second.title.contains(query.text, true) ||
                it.second.artists.fastAny { a -> a.name.contains(query.text, true) }
        }
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

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.second.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val accent = rememberArtworkAccent(playlist?.thumbnail)
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val showHero = playlist != null && songs.isNotEmpty() && !isSearching
    val topBarHeightPx = with(LocalDensity.current) {
        windowInsets.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding().toPx()
    }
    // The app bar stays transparent over the hero and turns solid once the hero has
    // scrolled up behind it.
    val scrolledPastHero by remember(topBarHeightPx) {
        derivedStateOf {
            val firstItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
            when {
                lazyListState.firstVisibleItemIndex > 0 -> true
                firstItem != null && firstItem.key == HeroKey ->
                    lazyListState.firstVisibleItemScrollOffset > firstItem.size - topBarHeightPx
                else -> false
            }
        }
    }
    val solidTopBar = isSearching || inSelectMode || scrolledPastHero
    val topBarColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface.copy(alpha = if (solidTopBar) 1f else 0f),
        animationSpec = tween(300),
        label = "topBarColor"
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = (
                if (showHero) windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                else windowInsets
            ).union(WindowInsets.ime).asPaddingValues(),
        ) {
            if (playlist == null || songs.isEmpty()) {
                if (isLoading) {
                    item(key = "loading_placeholder") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }
                } else if (error != null) {
                    // Without this branch a failed load (login rejected, network down,
                    // malformed header) rendered nothing at all: a blank page that looked
                    // like the playlist refused to open.
                    item(key = "load_error") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = error ?: stringResource(R.string.playlist_is_empty),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.retry() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                } else {
                    item(key = "empty_playlist") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.playlist_is_empty))
                        }
                    }
                }
            } else {
                playlist?.let { playlist ->
                    if (!isSearching) {
                        item(key = HeroKey) {
                            OnlinePlaylistHeader(
                                playlist = playlist,
                                songs = songs,
                                dbPlaylist = dbPlaylist,
                                navController = navController,
                                coroutineScope = coroutineScope,
                                continuation = viewModel.continuation,
                                accent = accent,
                                modifier = Modifier.animateItem()
                            )
                        }

                        item(key = "songs_section_header") {
                            CollectionSectionHeader(
                                title = stringResource(R.string.songs),
                                accent = accent,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    itemsIndexed(filteredSongs) { index, (_, songItem) ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(songItem.id)
                            } else {
                                selection.remove(songItem.id)
                            }
                        }

                        YouTubeListItem(
                            item = songItem,
                            isActive = mediaMetadata?.id == songItem.id,
                            isPlaying = isPlaying,
                            isSelected = inSelectMode && songItem.id in selection,
                            modifier = Modifier
                                .combinedClickable(
                                    enabled = !hideExplicit || !songItem.explicit,
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(songItem.id !in selection)
                                        } else if (songItem.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubePlaylistQueue(
                                                    playlistId = playlist.id,
                                                    playlistTitle = playlist.title,
                                                    initialSongs = filteredSongs.map { it.second },
                                                    initialContinuation = viewModel.continuation,
                                                    startIndex = index
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                        }
                                    }
                                )
                                .animateItem(),
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = songItem.id in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(songItem, navController, menuState::dismiss)
                                        }
                                    }) {
                                        Icon(painterResource(R.drawable.more_vert), null)
                                    }
                                }
                            }
                        )
                    }

                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }

        TopAppBar(
            title = {
                if (inSelectMode) {
                    Text(
                        text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else {
                    AnimatedVisibility(
                        visible = scrolledPastHero,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(200))
                    ) {
                        Text(
                            text = playlist?.title ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = topBarColor,
                scrolledContainerColor = topBarColor
            ),
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (inSelectMode) {
                            onExitSelectionMode()
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !inSelectMode) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (inSelectMode) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
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
                                selection.addAll(filteredSongs.map { it.second.id })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                YouTubeSelectionSongMenu(
                                    songSelection = filteredSongs.filter { it.second.id in selection }
                                        .map { it.second },
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
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun OnlinePlaylistHeader(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    dbPlaylist: Playlist?,
    navController: NavController,
    coroutineScope: CoroutineScope,
    continuation: String?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val syncUtils = LocalSyncUtils.current

    // The list no longer pads its top, so the backdrop runs under the status bar and the
    // transparent app bar; the artwork starts below them.
    val heroTopPadding = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Top)
        .asPaddingValues()
        .calculateTopPadding()

    val totalDuration = songs.sumOf { it.duration ?: 0 }
    val loadedSongCount = pluralStringResource(R.plurals.n_song, songs.size, songs.size)
    val chips = buildList {
        playlist.songCountText
            ?.takeIf { it.isNotBlank() && !it.equals(loadedSongCount, ignoreCase = true) }
            ?.let(::add)
        add(loadedSongCount)
        if (totalDuration > 0) add(makeTimeString(totalDuration * 1000L))
    }
    val author = playlist.author
    val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null

    CollectionHeroBackdrop(
        imageUrl = playlist.thumbnail,
        accent = accent,
        modifier = modifier,
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
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(playlist.thumbnail).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            CollectionTitleBlock(
                title = playlist.title,
                overline = "Playlist",
                accent = accent,
                chips = chips,
                subtitle = if (author != null) {
                    {
                        Text(
                            text = author.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = author.id != null) {
                                    navController.navigate("artist/${author.id}")
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    null
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val playLabel = stringResource(R.string.play)
                val shuffleLabel = stringResource(R.string.shuffle)
                val labelStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                // Narrow screens, large fonts or long translations collapse Shuffle to an icon
                // instead of clipping the labels.
                val shuffleLabelFits = remember(maxWidth, playLabel, shuffleLabel, labelStyle, density) {
                    val labelsWidth = textMeasurer.measure(playLabel, labelStyle).size.width +
                        textMeasurer.measure(shuffleLabel, labelStyle).size.width
                    // Row padding 40, two icon buttons 88, three gaps 36, two pill paddings and icons 148.
                    with(density) { labelsWidth.toDp() } + 312.dp <= maxWidth
                }

                val onShuffle: () -> Unit = {
                    val shuffleEndpoint = playlist.shuffleEndpoint
                    if (shuffleEndpoint != null) {
                        playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                    } else if (songs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = playlist.title,
                                items = songs.shuffled().map { it.toMediaItem() }
                            )
                        )
                    }
                }

                CollectionActionRow {
                    HeroIconButton(
                        icon = if (isBookmarked) R.drawable.favorite else R.drawable.favorite_border,
                        tint = if (isBookmarked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                        onClick = {
                            if (dbPlaylist != null) {
                                database.transaction {
                                    val currentPlaylist = dbPlaylist.playlist
                                    update(currentPlaylist, playlist)
                                    update(currentPlaylist.toggleLike())
                                }
                            } else {
                                database.transaction {
                                    val playlistEntity = PlaylistEntity(
                                        name = playlist.title,
                                        browseId = playlist.id,
                                        thumbnailUrl = playlist.thumbnail,
                                        isEditable = playlist.isEditable,
                                        remoteSongCount = playlist.songCountText?.let {
                                            Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                        },
                                        playEndpointParams = playlist.playEndpoint?.params,
                                        shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                        radioEndpointParams = playlist.radioEndpoint?.params
                                    ).toggleLike()
                                    insert(playlistEntity)
                                    coroutineScope.launch(Dispatchers.IO) {
                                        songs.map { it.toMediaMetadata() }
                                            .onEach(::insert)
                                            .mapIndexed { index, song ->
                                                PlaylistSongMap(
                                                    songId = song.id,
                                                    playlistId = playlistEntity.id,
                                                    position = index,
                                                    setVideoId = song.setVideoId
                                                )
                                            }
                                            .forEach(::insert)
                                    }
                                }
                            }
                        }
                    )

                    HeroPillButton(
                        text = playLabel,
                        icon = R.drawable.play,
                        accent = accent,
                        filled = true,
                        onClick = {
                            if (songs.isNotEmpty()) {
                                playerConnection.playQueue(
                                    YouTubePlaylistQueue(
                                        playlistId = playlist.id,
                                        playlistTitle = playlist.title,
                                        initialSongs = songs,
                                        initialContinuation = continuation
                                    )
                                )
                            }
                        }
                    )

                    if (shuffleLabelFits) {
                        HeroPillButton(
                            text = shuffleLabel,
                            icon = R.drawable.shuffle,
                            accent = accent,
                            filled = false,
                            onClick = onShuffle
                        )
                    } else {
                        HeroIconButton(
                            icon = R.drawable.shuffle,
                            contentDescription = shuffleLabel,
                            onClick = onShuffle
                        )
                    }

                    HeroIconButton(
                        icon = R.drawable.more_vert,
                        onClick = {
                            menuState.show {
                                YouTubePlaylistMenu(
                                    playlist = playlist,
                                    songs = songs,
                                    coroutineScope = coroutineScope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
