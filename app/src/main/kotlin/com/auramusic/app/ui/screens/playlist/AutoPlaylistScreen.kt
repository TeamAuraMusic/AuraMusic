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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.auramusic.app.LocalDownloadUtil
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.constants.SongSortDescendingKey
import com.auramusic.app.constants.SongSortType
import com.auramusic.app.constants.SongSortTypeKey
import com.auramusic.app.constants.YtmSyncKey
import com.auramusic.app.db.entities.Song
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.playback.ExoDownloadService
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.ui.component.CollectionSectionHeader
import com.auramusic.app.ui.component.CollectionSongHero
import com.auramusic.app.ui.component.DefaultDialog
import com.auramusic.app.ui.component.DraggableScrollbar
import com.auramusic.app.ui.component.EmptyPlaceholder
import com.auramusic.app.ui.component.IconButton
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.SongListItem
import com.auramusic.app.ui.component.SortHeader
import com.auramusic.app.ui.component.rememberArtworkAccent
import com.auramusic.app.ui.menu.AutoPlaylistMenu
import com.auramusic.app.ui.menu.SelectionSongMenu
import com.auramusic.app.ui.menu.SongMenu
import com.auramusic.app.ui.utils.backToMain
import com.auramusic.app.utils.makeTimeString
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.viewmodels.AutoPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.auramusic.app.video.VideoDownloadStore
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.models.Thumbnail
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
/** Which side of the downloads screen is being looked at. */
enum class DownloadsKind { Music, Videos }

/**
 * The switch between downloaded songs and downloaded videos.
 *
 * Both are downloads, so they are reached from the same place; they are not the same
 * thing, so they are not listed together. The switch is here rather than on a separate
 * screen because the question behind it is "which of my downloads is this", and the
 * answer should not need a trip somewhere else.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsKindSelector(
    selected: DownloadsKind,
    onSelect: (DownloadsKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        DownloadsKind.entries.forEach { kind ->
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(kind)
                },
                shape = RoundedCornerShape(20.dp),
                color = if (kind == selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (kind == DownloadsKind.Music) R.drawable.music_note else R.drawable.slow_motion_video,
                        ),
                        contentDescription = null,
                        tint = if (kind == selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (kind == DownloadsKind.Music) R.string.downloads_music else R.string.downloads_videos,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (kind == selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/**
 * What the videos side of the downloads screen is holding.
 *
 * Count and size, kept separate from the rows: a shelf with no total gives the listener
 * no way to tell one video from forty, and nothing to weigh a "clear this" against.
 */
@Composable
private fun VideoDownloadsSummary(
    finished: Int,
    downloading: Int,
    sizeBytes: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = finished.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (finished == 1) {
                    stringResource(R.string.video_downloads_one)
                } else {
                    stringResource(R.string.video_downloads_count, finished)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = buildString {
                    append(formatDownloadedVideoSize(sizeBytes))
                    if (downloading > 0) {
                        append(" • ")
                        append(stringResource(R.string.video_downloads_pending_count, downloading))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A downloaded video.
 *
 * A video is not a song in a square: it is a picture with something happening in it, so
 * the row shows a 16:9 thumbnail, says plainly what was saved at, and marks the copy as
 * one that is on the device rather than one that happens to be playing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadedVideoRow(
    download: VideoDownloadStore.Entry,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onLongClick = onRemove, onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            download.thumbnailUrl?.let { thumbnail ->
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                        ),
                    ),
            )
            // Played from the device, so the affordance is the same one the player uses.
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
            ) {
                Text(
                    text = download.quality.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = download.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            download.channelName.takeIf { it.isNotBlank() }?.let { channel ->
                Text(
                    text = channel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // What it was saved at, not what it is playing at: this is the copy on the
            // device, and a later change of preference must not rewrite it.
            Text(
                text = listOfNotNull(
                    // The height that was actually saved, where there was one: a row
                    // reading "Auto" tells the viewer nothing about what they can play.
                    download.height.takeIf { it > 0 }?.let { "${it}p" }
                        ?: download.quality.label.takeIf { !download.quality.isAuto },
                    download.codec.label.takeIf { !download.codec.isAuto },
                    formatDownloadedVideoSize(download.sizeBytes),
                    download.durationMs.takeIf { it > 0 }?.let { formatDownloadedVideoDuration(it) },
                ).joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.delete),
                contentDescription = stringResource(R.string.remove_download),
            )
        }
    }
}

/** A video on its way: what it is, how far it has got, and what can be done about it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingVideoRow(
    pending: VideoDownloadStore.Pending,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = (pending.totalBytes - pending.downloadedBytes).coerceAtLeast(0L)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(112.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                pending.thumbnailUrl?.let { thumbnail ->
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (pending.failed) {
                        Icon(
                            painter = painterResource(R.drawable.error),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { pending.fraction ?: 0f },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = pending.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                pending.channelName.takeIf { it.isNotBlank() }?.let { channel ->
                    Text(
                        text = channel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = when {
                        pending.failed -> stringResource(R.string.video_download_failed)
                        // Megabytes in, megabytes to go: a percentage alone says how far
                        // along it is, and not how much of the device it is eating on the
                        // way there.
                        pending.totalBytes > 0 -> buildString {
                            append(pending.fraction?.let { "${(it * 100).toInt()}%" }
                                ?: stringResource(R.string.video_downloading))
                            append(" • ")
                            append("${formatDownloadedVideoSize(pending.downloadedBytes)} / ")
                            append(formatDownloadedVideoSize(pending.totalBytes))
                            if (remaining > 0) {
                                append(" • ")
                                append(
                                    stringResource(
                                        R.string.video_download_left,
                                        formatDownloadedVideoSize(remaining),
                                    ),
                                )
                            }
                        }

                        // No length yet: what has landed is still worth saying, because a
                        // row that only reads "Downloading" gives nothing to wait on.
                        pending.downloadedBytes > 0 ->
                            "${stringResource(R.string.video_downloading)} • " +
                                formatDownloadedVideoSize(pending.downloadedBytes)

                        else -> stringResource(R.string.video_downloading)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (pending.failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = if (pending.failed) onRetry else onRemove) {
                Icon(
                    painter = painterResource(if (pending.failed) R.drawable.refresh else R.drawable.close),
                    contentDescription = stringResource(
                        if (pending.failed) R.string.video_download_retry else R.string.remove_download,
                    ),
                )
            }
        }

        // The bar itself. Kept under the row rather than inside the thumbnail so the
        // length of the transfer is readable at a glance while it is happening.
        LinearProgressIndicator(
            progress = { pending.fraction ?: 0f },
            color = if (pending.failed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
    }
}

private fun formatDownloadedVideoSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format("%.1f GB", mb / 1024.0) else String.format("%.1f MB", mb)
}

private fun formatDownloadedVideoDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {

    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist = when (viewModel.playlist) {
        "liked" -> stringResource(R.string.liked)
        // Uploaded feature is temporarily disabled
        // "uploaded" -> stringResource(R.string.uploaded_playlist)
        else -> stringResource(R.string.offline)
    }

    val songs by viewModel.likedSongs.collectAsState(null)
    val mutableSongs =
        remember {
            mutableStateListOf<Song>()
        }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val likeLength =
        remember(songs) {
            songs?.fastSumBy { it.song.duration } ?: 0
        }
    val heroAccent = rememberArtworkAccent(songs?.firstOrNull()?.song?.thumbnailUrl)

    val playlistId = viewModel.playlist
    val playlistType = when (playlistId) {
        "liked" -> PlaylistType.LIKE
        "downloaded" -> PlaylistType.DOWNLOAD
        // Uploaded feature is temporarily disabled
        // "uploaded" -> PlaylistType.UPLOADED
        else -> PlaylistType.OTHER
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

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }
    
    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                if (playlistType == PlaylistType.LIKE) viewModel.syncLikedSongs()
                // Uploaded feature is temporarily disabled
                // if (playlistType == PlaylistType.UPLOADED) viewModel.syncUploadedSongs()
            }
        }
    }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            songs?.let { addAll(it) }
        }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) {
                    Download.STATE_COMPLETED
                } else if (songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs!!.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
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

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs ?: emptyList()
        else songs?.filter { song ->
            song.song.title.contains(query.text, true) ||
                song.artists.any { it.name.contains(query.text, true) }
        } ?: emptyList()
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val state = rememberLazyListState()

    // Downloads arrive as two different things - songs kept out of the network and
    // videos kept by the download service - and one list of both reads as though they
    // were the same thing twice. Which one is being looked at is the switch's answer,
    // remembered across visits so the screen does not open on the other one each time.
    var downloadsKind by rememberSaveable { mutableStateOf(DownloadsKind.Music) }
    val showingVideos = playlistType == PlaylistType.DOWNLOAD && downloadsKind == DownloadsKind.Videos

    LaunchedEffect(Unit) {
        VideoDownloadStore.load(context)
    }
    val videoDownloads by VideoDownloadStore.entries(context).collectAsState()
    // Watched rather than polled: the service reports every chunk, and a row that only
    // refreshed once a second showed a download as stalled between ticks.
    val pendingVideoDownloads by VideoDownloadStore.pendingFlow(context).collectAsState()

    val playDownloadedVideo: (VideoDownloadStore.Entry) -> Unit = { download ->
        VideoPlaybackManager.playWithDetails(
            context = context,
            videoId = download.videoId,
            title = download.title,
            channelName = download.channelName,
            channelThumbnail = null,
            thumbnails = listOfNotNull(
                download.thumbnailUrl?.let { Thumbnail(it, null, null) },
            ),
        )
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val canRefresh = playlistType == PlaylistType.LIKE || playlistType == PlaylistType.UPLOADED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (canRefresh) {
                    Modifier.pullToRefresh(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = viewModel::refresh
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        LazyColumn(
            state = state,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (playlistType == PlaylistType.DOWNLOAD) {
                item(key = "downloads_kind") {
                    DownloadsKindSelector(
                        selected = downloadsKind,
                        onSelect = { downloadsKind = it },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (showingVideos) {
                if (videoDownloads.isEmpty() && pendingVideoDownloads.isEmpty()) {
                    item(key = "videos_empty") {
                        EmptyPlaceholder(
                            icon = R.drawable.slow_motion_video,
                            text = stringResource(R.string.video_downloads_empty),
                        )
                    }
                } else {
                    item(key = "videos_hero") {
                        val firstDownload = videoDownloads.firstOrNull()
                        val chips = buildList {
                            add(stringResource(R.string.video_downloads_count, videoDownloads.size))
                            add(formatDownloadedVideoSize(videoDownloads.sumOf { it.sizeBytes }))
                            if (pendingVideoDownloads.isNotEmpty()) {
                                add(
                                    stringResource(
                                        R.string.video_downloads_pending_count,
                                        pendingVideoDownloads.size,
                                    )
                                )
                            }
                        }
                        CollectionSongHero(
                            title = stringResource(R.string.offline),
                            overline = stringResource(R.string.videos),
                            coverUrl = firstDownload?.thumbnailUrl,
                            chips = chips,
                            onPlay = {
                                firstDownload?.let(playDownloadedVideo)
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    items(pendingVideoDownloads, key = { "pending_${it.videoId}" }) { pending ->
                        PendingVideoRow(
                            pending = pending,
                            onRetry = { VideoDownloadStore.retry(context, pending.videoId) },
                            onRemove = { VideoDownloadStore.cancel(context, pending.videoId) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    items(videoDownloads, key = { it.videoId }) { download ->
                        DownloadedVideoRow(
                            download = download,
                            onPlay = { playDownloadedVideo(download) },
                            onRemove = { VideoDownloadStore.cancel(context, download.videoId) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                return@LazyColumn
            }

            if (songs != null) {
                if (songs!!.isEmpty()) {
                    item(key = "empty_placeholder") {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                        )
                    }
                } else {
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            AutoPlaylistHeader(
                                name = playlist,
                                songs = songs!!,
                                likeLength = likeLength,
                                downloadState = downloadState,
                                onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                menuState = menuState,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    item(key = "songs_header") {
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
                        )
                    }
                }

                if (filteredSongs.isNotEmpty()) {
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
                            isActive = song.song.id == mediaMetadata?.id,
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
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(song.id !in selection)
                                        } else if (song.song.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlist,
                                                    items = songs!!.map { it.toMediaItem() },
                                                    startIndex = songs!!.indexOfFirst { it.id == song.id }
                                                ),
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
                                )
                                .animateItem()
                        )
                    }
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
            scrollState = state,
            headerItems = 2
        )

        if (canRefresh) {
            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }

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
                            text = playlist,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
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
                                    clearAction = onExitSelectionMode,
                                )
                            }
                        },
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
    }
}

@Composable
private fun AutoPlaylistHeader(
    name: String,
    songs: List<Song>,
    likeLength: Int,
    downloadState: Int,
    onShowRemoveDownloadDialog: () -> Unit,
    menuState: com.auramusic.app.ui.component.MenuState,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    val countLabel = pluralStringResource(R.plurals.n_song, songs.size, songs.size)
    val lengthLabel = likeLength.takeIf { it > 0 }?.let { makeTimeString(it * 1000L) }

    CollectionSongHero(
        title = name,
        overline = stringResource(R.string.auto_playlist),
        coverUrl = songs.firstOrNull()?.song?.thumbnailUrl,
        chips = listOfNotNull(countLabel, lengthLabel),
        onPlay = {
            playerConnection.playQueue(
                ListQueue(
                    title = name,
                    items = songs.map { it.toMediaItem() },
                ),
            )
        },
        onShuffle = {
            playerConnection.playQueue(
                ListQueue(
                    title = name,
                    items = songs.shuffled().map { it.toMediaItem() },
                ),
            )
        },
        onMenu = {
            menuState.show {
                AutoPlaylistMenu(
                    downloadState = downloadState,
                    onQueue = {
                        playerConnection.addToQueue(
                            songs.map { it.toMediaItem() }
                        )
                    },
                    onDownload = {
                        when (downloadState) {
                            Download.STATE_COMPLETED -> onShowRemoveDownloadDialog()
                            Download.STATE_DOWNLOADING -> {
                                songs.forEach { song ->
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.song.id,
                                        false,
                                    )
                                }
                            }
                            else -> {
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
                            }
                        }
                    },
                    onDismiss = { menuState.dismiss() }
                )
            }
        },
        modifier = modifier,
    )
}


enum class PlaylistType {
    LIKE, DOWNLOAD, UPLOADED, OTHER
}
