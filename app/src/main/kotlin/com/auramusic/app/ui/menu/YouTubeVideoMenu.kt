/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.menu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.auramusic.app.R
import com.auramusic.app.ui.component.Material3MenuGroup
import com.auramusic.app.ui.component.Material3MenuItemData
import com.auramusic.app.ui.component.NewAction
import com.auramusic.app.ui.component.NewActionGrid
import com.auramusic.app.utils.AuraPlayerUtils
import com.auramusic.app.utils.joinByBullet
import com.auramusic.app.video.VideoDownloadDialog
import com.auramusic.app.video.VideoDownloadStore
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.models.Artist
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.coroutines.launch

/**
 * The video side's counterpart of [YouTubeSongMenu]: the same sheet layout, with the actions
 * that make sense for a video - queueing it, downloading the video itself, opening the
 * channel that posted it, and sharing it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YouTubeVideoMenu(
    video: YouTubeVideoItem,
    channels: List<Artist>,
    /** Null where the channel is already open, which leaves the channel entries out. */
    onChannelClick: ((String) -> Unit)?,
    onDismiss: () -> Unit,
    /**
     * False where the video is already in the queue. "Play next" and "Add to queue"
     * then have nothing left to say; the queue opens this menu to offer the opposite.
     */
    showQueueActions: Boolean = true,
    /** Takes the video back out of the queue, shown in place of the queue actions. */
    onRemoveFromQueue: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(video.videoId) { VideoDownloadStore.load(context) }
    val entries by VideoDownloadStore.entries(context).collectAsState()
    val progressById by VideoDownloadStore.progress(context).collectAsState()
    val entry = entries.firstOrNull { it.videoId == video.videoId }
    val progress = progressById[video.videoId]
    val downloading = progress?.done == false && progress.failed == false

    val downloadDescription: String? = when {
        entry != null -> stringResource(R.string.video_downloaded, formatBytes(entry.sizeBytes))
        downloading && progress != null && progress.totalBytes > 0 ->
            "${(progress.downloadedBytes * 100 / progress.totalBytes).coerceIn(0L, 100L)}%"
        else -> null
    }

    var showDownloadDialog by remember { mutableStateOf(false) }
    var preparingDownload by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    fun askForNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) runCatching { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    ListItem(
        headlineContent = {
            Text(
                text = video.title,
                modifier = Modifier.basicMarquee(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = joinByBullet(video.channelName, video.durationText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                AsyncImage(
                    model = video.thumbnails.maxByOrNull { it.width ?: 0 }?.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                video.durationText?.let { duration ->
                    Text(
                        text = duration,
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        },
    )

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
        contentPadding = PaddingValues(
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            NewActionGrid(
                actions = buildList {
                    if (showQueueActions) {
                        add(
                            NewAction(
                                icon = { MenuIcon(R.drawable.playlist_play) },
                                text = stringResource(R.string.play_next),
                                onClick = {
                                    VideoPlaybackManager.queueNext(video)
                                    onDismiss()
                                },
                            ),
                        )
                        add(
                            NewAction(
                                icon = { MenuIcon(R.drawable.ic_queue) },
                                text = stringResource(R.string.add_to_queue),
                                onClick = {
                                    VideoPlaybackManager.addToQueue(video)
                                    onDismiss()
                                },
                            ),
                        )
                    }
                    if (onRemoveFromQueue != null) {
                        add(
                            NewAction(
                                icon = { MenuIcon(R.drawable.close) },
                                text = stringResource(R.string.video_player_remove_from_queue),
                                onClick = {
                                    onRemoveFromQueue()
                                    onDismiss()
                                },
                            ),
                        )
                    }
                    add(
                        NewAction(
                            icon = { MenuIcon(R.drawable.share) },
                            text = stringResource(R.string.share),
                            onClick = {
                                VideoPlaybackManager.shareVideo(context, video.videoId, video.title)
                                onDismiss()
                            },
                        ),
                    )
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }

        item {
            Material3MenuGroup(
                items = buildList {
                    add(
                        Material3MenuItemData(
                            title = {
                                Text(
                                    text = when {
                                        entry != null -> stringResource(R.string.video_download_complete)
                                        downloading || preparingDownload -> stringResource(R.string.video_downloading)
                                        progress?.failed == true -> stringResource(R.string.video_download_retry)
                                        else -> stringResource(R.string.video_download)
                                    },
                                )
                            },
                            description = downloadDescription?.let { text -> @Composable { Text(text) } },
                            icon = { Icon(painterResource(if (entry != null) R.drawable.downloaded else R.drawable.download), null) },
                            onClick = {
                                when {
                                    preparingDownload || downloading -> Unit
                                    progress?.failed == true -> {
                                        VideoDownloadStore.retry(context, video.videoId)
                                        onDismiss()
                                    }
                                    else -> showDownloadDialog = true
                                }
                            },
                        ),
                    )
                    if (onChannelClick != null) channels.filter { it.id != null }.ifEmpty {
                        listOfNotNull(video.channelId?.let { Artist(name = video.channelName, id = it) })
                    }.forEach { channel ->
                        add(
                            Material3MenuItemData(
                                title = { Text(stringResource(R.string.open_channel)) },
                                description = { Text(channel.name) },
                                icon = { Icon(painterResource(R.drawable.person), null) },
                                onClick = {
                                    onDismiss()
                                    channel.id?.let(onChannelClick)
                                },
                            ),
                        )
                    }
                    add(
                        Material3MenuItemData(
                            title = { Text(stringResource(R.string.copy_link)) },
                            icon = { Icon(painterResource(R.drawable.link), null) },
                            onClick = {
                                VideoPlaybackManager.copyVideoLink(context, video.videoId)
                                onDismiss()
                            },
                        ),
                    )
                },
            )
        }
    }

    if (showDownloadDialog) {
        VideoDownloadDialog(
            existing = entry,
            onDismiss = { showDownloadDialog = false },
            onRemove = { download ->
                showDownloadDialog = false
                VideoDownloadStore.cancel(context, download.videoId)
                onDismiss()
            },
            onDownload = { quality, codec ->
                showDownloadDialog = false
                preparingDownload = true
                askForNotificationPermissionIfNeeded()
                scope.launch {
                    // Same flow as the player's download button: resolve at the chosen quality,
                    // then hand the request to the store, which starts the download service.
                    val source = AuraPlayerUtils.getVideoStreamSource(video.videoId, quality, codec).getOrNull()
                    preparingDownload = false
                    if (source == null) {
                        Toast.makeText(context, R.string.video_download_unavailable, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    VideoDownloadStore.download(
                        context,
                        VideoDownloadStore.Request(
                            videoId = video.videoId,
                            title = video.title,
                            channelName = video.channelName,
                            thumbnailUrl = video.thumbnails.maxByOrNull { it.width ?: 0 }?.url,
                            quality = quality,
                            codec = codec,
                            durationMs = parseDurationMs(video.durationText),
                        ),
                    )
                    onDismiss()
                }
            },
        )
    }
}

@Composable
private fun MenuIcon(icon: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** "1:02:03" or "3:45" to milliseconds; 0 when the feed did not say. */
private fun parseDurationMs(text: String?): Long =
    text?.split(":")?.mapNotNull { it.trim().toLongOrNull() }
        ?.takeIf { it.isNotEmpty() }
        ?.fold(0L) { total, part -> total * 60 + part }
        ?.times(1000) ?: 0L

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format("%.1f GB", mb / 1024.0) else String.format("%.0f MB", mb)
}
