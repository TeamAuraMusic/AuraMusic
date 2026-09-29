/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.menu

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.auramusic.innertube.YouTube
import com.auramusic.app.LocalDatabase
import com.auramusic.app.LocalDownloadUtil
import com.auramusic.app.LocalListenTogetherManager
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.PlaylistSong
import com.auramusic.app.db.entities.Song
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.playback.ExoDownloadService
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.ui.component.DefaultDialog
import com.auramusic.app.ui.component.Material3MenuGroup
import com.auramusic.app.ui.component.Material3MenuItemData
import com.auramusic.app.ui.component.NewAction
import com.auramusic.app.ui.component.NewActionGrid
import com.auramusic.app.ui.component.PlaylistListItem
import com.auramusic.app.ui.component.TextFieldDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun PlaylistMenu(
    playlist: Playlist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    autoPlaylist: Boolean? = false,
    downloadPlaylist: Boolean? = false,
    songList: List<Song>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val playlistFlow = remember(playlist.id) { database.playlist(playlist.id) }
    val dbPlaylist by playlistFlow.collectAsState(initial = playlist)
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    LaunchedEffect(Unit) {
        if (autoPlaylist == false) {
            database.playlistSongs(playlist.id).collect {
                songs = it.map(PlaylistSong::song)
            }
        } else {
            if (songList != null) {
                songs = songList
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist.playlist.isEditable == true

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_playlist)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue =
            TextFieldValue(
                playlist.playlist.name,
                TextRange(playlist.playlist.name.length),
            ),
            onDone = { name ->
                onDismiss()
                database.query {
                    update(
                        playlist.playlist.copy(
                            name = name,
                            lastUpdateTime = LocalDateTime.now()
                        )
                    )
                }
                coroutineScope.launch(Dispatchers.IO) {
                    playlist.playlist.browseId?.let { YouTube.renamePlaylist(it, name) }
                }
            },
        )
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.playlist.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showDeletePlaylistDialog by remember {
        mutableStateOf(false)
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist.playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        onDismiss()
                        database.transaction {
                            // First toggle the like using the same logic as the like button
                            if (playlist.playlist.bookmarkedAt != null) {
                                // Using the same toggleLike() method that's used in the like button
                                update(playlist.playlist.toggleLike())
                            }
                            // Then delete the playlist
                            delete(playlist.playlist)
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            playlist.playlist.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    PlaylistListItem(
        playlist = playlist,
        trailingContent = {
            if (playlist.playlist.isEditable != true) {
                IconButton(
                    onClick = {
                        database.query {
                            dbPlaylist?.playlist?.toggleLike()?.let { update(it) }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                        tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        contentDescription = null
                    )
                }
            }
        },
    )

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            NewActionGrid(
                actions = listOfNotNull(
                    if (!isGuest) {
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.play),
                            onClick = {
                                onDismiss()
                                if (songs.isNotEmpty()) {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = playlist.playlist.name,
                                            items = songs.map(Song::toMediaItem)
                                        )
                                    )
                                }
                            }
                        )
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.shuffle),
                            onClick = {
                                onDismiss()
                                if (songs.isNotEmpty()) {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = playlist.playlist.name,
                                            items = songs.shuffled().map(Song::toMediaItem)
                                        )
                                    )
                                }
                            }
                        )
                    } else null,
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.share),
                        onClick = {
                            onDismiss()
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${dbPlaylist?.playlist?.browseId}")
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    )
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                columns = if (isGuest) 1 else 3
            )
        }

        item {
            Material3MenuGroup(
                items = buildList {
                    if (!isGuest) {
                        playlist.playlist.browseId?.let { browseId ->
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(R.string.start_radio)) },
                                    description = { Text(text = stringResource(R.string.start_radio_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.radio),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            YouTube.playlist(browseId).getOrNull()?.playlist?.let { playlistItem ->
                                                playlistItem.radioEndpoint?.let { radioEndpoint ->
                                                    withContext(Dispatchers.Main) {
                                                        playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                                    }
                                                }
                                            }
                                        }
                                        onDismiss()
                                    }
                                )
                            )
                        }
                    }
                    if (!isGuest) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.play_next)) },
                                description = { Text(text = stringResource(R.string.play_next_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.playlist_play),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        playerConnection.playNext(songs.map { it.toMediaItem() })
                                    }
                                    onDismiss()
                                }
                            )
                        )
                    }
                    if (!isGuest) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.add_to_queue)) },
                                description = { Text(text = stringResource(R.string.add_to_queue_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.queue_music),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    playerConnection.addToQueue(songs.map { it.toMediaItem() })
                                }
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items = buildList {
                    if (editable && autoPlaylist != true && !isGuest) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.edit)) },
                                description = { Text(text = stringResource(R.string.edit_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.edit),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    showEditDialog = true
                                }
                            )
                        )
                    }
                    if (downloadPlaylist != true) {
                        add(
                            when (downloadState) {
                                Download.STATE_COMPLETED -> {
                                    Material3MenuItemData(
                                        title = {
                                            Text(
                                                text = stringResource(R.string.remove_download)
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showRemoveDownloadDialog = true
                                        }
                                    )
                                }
                                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                    Material3MenuItemData(
                                        title = { Text(text = stringResource(R.string.downloading)) },
                                        icon = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        },
                                        onClick = {
                                            showRemoveDownloadDialog = true
                                        }
                                    )
                                }
                                else -> {
                                    Material3MenuItemData(
                                        title = { Text(text = stringResource(R.string.action_download)) },
                                        description = { Text(text = stringResource(R.string.download_desc)) },
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            songs.forEach { song ->
                                                val downloadRequest =
                                                    DownloadRequest
                                                        .Builder(song.id, song.id.toUri())
                                                        .setCustomCacheKey(song.id)
                                                        .setData(song.song.title.toByteArray())
                                                        .build()
                                                DownloadService.sendAddDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    downloadRequest,
                                                    false,
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                    if (autoPlaylist != true && !isGuest) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.delete)) },
                                description = { Text(text = stringResource(R.string.delete_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    showDeletePlaylistDialog = true
                                }
                            )
                        )
                    }
                    playlist.playlist.shareLink?.let { shareLink ->
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.share)) },
                                description = { Text(text = stringResource(R.string.share_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.share),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareLink)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                    onDismiss()
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}
