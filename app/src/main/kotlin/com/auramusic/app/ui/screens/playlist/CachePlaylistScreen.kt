/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.constants.SongSortDescendingKey
import com.auramusic.app.constants.SongSortType
import com.auramusic.app.constants.SongSortTypeKey
import com.auramusic.app.db.entities.Song
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.playback.ExoDownloadService
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.ui.component.CollectionSectionHeader
import com.auramusic.app.ui.component.CollectionSongHero
import com.auramusic.app.ui.component.DraggableScrollbar
import com.auramusic.app.ui.component.EmptyPlaceholder
import com.auramusic.app.ui.component.IconButton
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.SongListItem
import com.auramusic.app.ui.component.SortHeader
import com.auramusic.app.ui.component.rememberArtworkAccent
import com.auramusic.app.ui.menu.CachePlaylistMenu
import com.auramusic.app.ui.menu.SelectionSongMenu
import com.auramusic.app.ui.menu.SongMenu
import com.auramusic.app.ui.utils.backToMain
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.viewmodels.CachePlaylistViewModel
import java.time.LocalDateTime

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: CachePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val cachedSongs by viewModel.cachedSongs.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val sortedSongs = remember(cachedSongs, sortType, sortDescending) {
        val sorted = when (sortType) {
            SongSortType.CREATE_DATE -> cachedSongs.sortedBy { it.song.dateDownload ?: LocalDateTime.MIN }
            SongSortType.NAME -> cachedSongs.sortedBy { it.song.title }
            SongSortType.ARTIST -> cachedSongs.sortedBy { song ->
                song.artists.joinToString(separator = "") { it.name }
            }
            SongSortType.PLAY_TIME -> cachedSongs.sortedBy { it.song.totalPlayTime }
        }
        if (sortDescending) sorted.reversed() else sorted
    }
    val heroAccent = rememberArtworkAccent(sortedSongs.firstOrNull()?.song?.thumbnailUrl)

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

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
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

    val filteredSongs = remember(sortedSongs, query) {
        if (query.text.isEmpty()) sortedSongs
        else sortedSongs.filter { song ->
            song.title.contains(query.text, true) ||
                song.artists.any { it.name.contains(query.text, true) }
        }
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (filteredSongs.isEmpty() && !isSearching) {
                item(key = "empty_placeholder") {
                    EmptyPlaceholder(
                        icon = R.drawable.music_note,
                        text = stringResource(R.string.playlist_is_empty),
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (filteredSongs.isEmpty() && isSearching) {
                item(key = "no_results") {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                        modifier = Modifier.animateItem()
                    )
                }
            } else {
                if (filteredSongs.isNotEmpty() && !isSearching) {
                    item(key = "playlist_header") {
                        CachePlaylistHeader(
                            songs = filteredSongs,
                            context = context,
                            menuState = menuState,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                if (filteredSongs.isNotEmpty()) {
                    item(key = "sort_header") {
                        CollectionSectionHeader(
                            title = stringResource(R.string.songs),
                            accent = heroAccent,
                            trailing = {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { sortType ->
                                        when (sortType) {
                                            SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            SongSortType.NAME -> R.string.sort_by_name
                                            SongSortType.ARTIST -> R.string.sort_by_artist
                                            SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    },
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    }

                    SongListItem(
                        song = song,
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
                                IconButton(onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                            isFromCache = true,
                                        )
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .combinedClickable(
                                onClick = {
                                    if (inSelectMode) {
                                        onCheckedChange(song.id !in selection)
                                    } else if (song.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Cache Songs",
                                                items = cachedSongs.map { it.toMediaItem() },
                                                startIndex = cachedSongs.indexOfFirst { it.id == song.id }
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
                            .animateItem()
                    )
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 2
        )

        TopAppBar(
            title = {
                when {
                    inSelectMode -> {
                        Text(
                            text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    isSearching -> {
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
                    }
                    else -> {
                        Text(
                            stringResource(R.string.cached_playlist),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    when {
                        isSearching -> {
                            isSearching = false
                            query = TextFieldValue()
                            focusManager.clearFocus()
                        }
                        inSelectMode -> {
                            onExitSelectionMode()
                        }
                        else -> {
                            navController.navigateUp()
                        }
                    }
                }, onLongClick = {
                    if (!isSearching && !inSelectMode) {
                        navController.backToMain()
                    }
                }) {
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
                                selection.addAll(filteredSongs.map { it.id })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = filteredSongs.filter { it.id in selection },
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
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun CachePlaylistHeader(
    songs: List<Song>,
    context: android.content.Context,
    menuState: com.auramusic.app.ui.component.MenuState,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    CollectionSongHero(
        title = stringResource(R.string.cached_playlist),
        overline = stringResource(R.string.auto_playlist),
        coverUrl = songs.firstOrNull()?.song?.thumbnailUrl,
        chips = listOf(pluralStringResource(R.plurals.n_song, songs.size, songs.size)),
        onPlay = {
            playerConnection.playQueue(
                ListQueue(
                    title = "Cache Songs",
                    items = songs.map { it.toMediaItem() },
                )
            )
        },
        onShuffle = {
            playerConnection.playQueue(
                ListQueue(
                    title = "Cache Songs",
                    items = songs.shuffled().map { it.toMediaItem() },
                )
            )
        },
        onMenu = {
            menuState.show {
                CachePlaylistMenu(
                    downloadState = Download.STATE_STOPPED,
                    onQueue = {
                        playerConnection.addToQueue(
                            songs.map { it.toMediaItem() }
                        )
                    },
                    onDownload = {
                        // Download all cached songs
                        songs.forEach { song ->
                            val downloadRequest = DownloadRequest
                                .Builder(song.song.id, song.song.id.toUri())
                                .setCustomCacheKey(song.song.id)
                                .setData(song.song.title.toByteArray())
                                .build()
                            DownloadService.sendAddDownload(
                                context,
                                ExoDownloadService::class.java,
                                downloadRequest,
                                false,
                            )
                        }
                    },
                    onDismiss = { menuState.dismiss() }
                )
            }
        },
        modifier = modifier,
    )
}
