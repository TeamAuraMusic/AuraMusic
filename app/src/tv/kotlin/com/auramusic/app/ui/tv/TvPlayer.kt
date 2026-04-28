/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import coil3.compose.AsyncImage
import com.auramusic.app.R
import com.auramusic.app.db.entities.Song
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.innertube.models.WatchEndpoint
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.auramusic.app.utils.makeTimeString

@Composable
private fun TvPlayerControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    tint: Color = Color.White,
) {
    var isFocused by remember { mutableStateOf(false) }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            ),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/**
 * TV-compatible full-screen player with large controls optimized for remote control navigation.
 * Features large touch targets, clear visual hierarchy, and TV-friendly layout.
 */
@Composable
fun TvPlayerScreen(
    playerConnection: PlayerConnection?,
    onBackClick: () -> Unit,
) {
    val currentSong by (playerConnection?.currentSong?.collectAsState(null) ?: remember { mutableStateOf(null) })
    val isPlaying by (playerConnection?.isPlaying?.collectAsState(false) ?: remember { mutableStateOf(false) })
    val currentPosition by (playerConnection?.currentPosition?.collectAsState(0L) ?: remember { mutableStateOf(0L) })

    var duration by remember { mutableStateOf(0L) }
    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerEndTime by remember { mutableStateOf<Long?>(null) }
    var showLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(playerConnection?.player) {
        while (true) {
            playerConnection?.player?.let { player ->
                duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            }
            delay(1000) // Update every second
        }
    }

    // Sleep timer countdown
    LaunchedEffect(sleepTimerEndTime) {
        sleepTimerEndTime?.let { endTime ->
            while (System.currentTimeMillis() < endTime) {
                val remaining = endTime - System.currentTimeMillis()
                sleepTimerMinutes = (remaining / (1000 * 60)).toInt()
                delay(60000) // Update every minute
            }
            // Timer expired - pause playback
            playerConnection?.togglePlayPause()
            sleepTimerMinutes = null
            sleepTimerEndTime = null
        }
    }

    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background with album art blur
            currentSong?.song?.thumbnailUrl?.let { thumbnailUrl ->
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
            )

            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(24.dp)
                    .size(64.dp)
                    .align(Alignment.TopStart),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Album art
                Box(
                    modifier = Modifier
                        .size(400.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                currentSong?.song?.let { song ->
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Song info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 48.dp),
                ) {
                    Text(
                        text = currentSong?.song?.title ?: "No song playing",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )

                    Text(
                        text = currentSong?.artists?.joinToString(", ") { it.name } ?: "",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }

                // Lyrics overlay
                if (showLyrics) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Lyrics not available for this song",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Progress bar and time
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = makeTimeString(currentPosition),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                        Text(
                            text = makeTimeString(duration),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }

                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Previous song
                    TvPlayerControlButton(
                        onClick = { playerConnection?.seekToPrevious() },
                        icon = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous song",
                    )

                    // Rewind 10 seconds
                    TvPlayerControlButton(
                        onClick = {
                            val currentPos = playerConnection?.currentPosition?.value ?: 0L
                            val newPos = maxOf(0L, currentPos - 10000L) // 10 seconds back
                            playerConnection?.player?.seekTo(newPos)
                        },
                        icon = Icons.Filled.FastRewind,
                        contentDescription = "Rewind 10 seconds",
                    )

                    // Play/Pause (larger)
                    TvPlayerControlButton(
                        onClick = { playerConnection?.togglePlayPause() },
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        size = 96.dp,
                    )

                    // Fast forward 10 seconds
                    TvPlayerControlButton(
                        onClick = {
                            val currentPos = playerConnection?.currentPosition?.value ?: 0L
                            val duration = playerConnection?.player?.duration?.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
                            val newPos = minOf(duration, currentPos + 10000L) // 10 seconds forward
                            playerConnection?.player?.seekTo(newPos)
                        },
                        icon = Icons.Filled.FastForward,
                        contentDescription = "Fast forward 10 seconds",
                    )

                    // Next song
                    TvPlayerControlButton(
                        onClick = { playerConnection?.seekToNext() },
                        icon = Icons.Filled.SkipNext,
                        contentDescription = "Next song",
                    )
                }

                // Additional controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TvPlayerControlButton(
                        onClick = { playerConnection?.toggleShuffle() },
                        icon = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerConnection?.shuffleModeEnabled?.value == true)
                            MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    )

                    TvPlayerControlButton(
                        onClick = {
                            playerConnection?.player?.let { player ->
                                val currentMode = player.repeatMode
                                val newMode = when (currentMode) {
                                    androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                    androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                }
                                player.repeatMode = newMode
                            }
                        },
                        icon = when (playerConnection?.repeatMode?.value) {
                            androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (playerConnection?.repeatMode?.value != androidx.media3.common.Player.REPEAT_MODE_OFF)
                            MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    )

                    TvPlayerControlButton(
                        onClick = {
                            playerConnection?.player?.let { player ->
                                val currentSpeed = player.playbackParameters.speed
                                val newSpeed = when (currentSpeed) {
                                    1.0f -> 1.25f  // Normal -> 1.25x
                                    1.25f -> 1.5f  // 1.25x -> 1.5x
                                    1.5f -> 1.75f  // 1.5x -> 1.75x
                                    1.75f -> 2.0f  // 1.75x -> 2x
                                    2.0f -> 0.5f   // 2x -> 0.5x (slow)
                                    0.5f -> 0.75f  // 0.5x -> 0.75x
                                    else -> 1.0f   // Any other -> Normal
                                }
                                val params = androidx.media3.common.PlaybackParameters(newSpeed)
                                player.setPlaybackParameters(params)
                            }
                        },
                        icon = Icons.Filled.Speed,
                        contentDescription = "Playback speed",
                        tint = Color.White.copy(alpha = 0.7f),
                    )

                    TvPlayerControlButton(
                        onClick = {
                            val currentMinutes = sleepTimerMinutes
                            val newMinutes = when (currentMinutes) {
                                null -> 15     // Start with 15 minutes
                                15 -> 30       // 15 -> 30 minutes
                                30 -> 60       // 30 -> 60 minutes
                                60 -> 120      // 60 -> 120 minutes
                                else -> null   // Any other -> Cancel timer
                            }

                            if (newMinutes != null) {
                                sleepTimerEndTime = System.currentTimeMillis() + (newMinutes * 60 * 1000L)
                                sleepTimerMinutes = newMinutes
                            } else {
                                sleepTimerEndTime = null
                                sleepTimerMinutes = null
                            }
                        },
                        icon = Icons.Filled.Bedtime,
                        contentDescription = if (sleepTimerMinutes != null) "Cancel sleep timer (${sleepTimerMinutes} min)" else "Set sleep timer",
                        tint = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    )

                    TvPlayerControlButton(
                        onClick = { showLyrics = !showLyrics },
                        icon = Icons.Filled.Lyrics,
                        contentDescription = if (showLyrics) "Hide lyrics" else "Show lyrics",
                        tint = if (showLyrics) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
fun TvQueueItem(
    mediaItem: androidx.media3.common.MediaItem,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "tvQueueItemScale",
    )
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else if (isCurrentSong) {
        MaterialTheme.colorScheme.secondary
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentSong) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                mediaItem.mediaMetadata.artworkUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = mediaItem.mediaMetadata.title?.toString(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Icon(
                    painterResource(R.drawable.music_note),
                    contentDescription = "Music note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            // Duration
            val durationMs = mediaItem.mediaMetadata.durationMs ?: 0L
            if (durationMs > 0) {
                Text(
                    text = makeTimeString(durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * TV queue screen showing current playlist with large, focusable items.
 */
@Composable
fun TvQueueScreen(
    playerConnection: PlayerConnection?,
    onBackClick: () -> Unit,
) {
    val queueWindows by (playerConnection?.queueWindows?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val currentWindowIndex = playerConnection?.player?.currentMediaItemIndex ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            // Back button, title, and clear queue button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // Clear queue button
                IconButton(
                    onClick = {
                        playerConnection?.player?.let { player ->
                            // Keep only the current song, remove all others
                            if (player.mediaItemCount > 1) {
                                val currentIndex = player.currentMediaItemIndex
                                // Remove all items after current
                                for (i in player.mediaItemCount - 1 downTo currentIndex + 1) {
                                    player.removeMediaItem(i)
                                }
                                // Remove all items before current
                                for (i in currentIndex - 1 downTo 0) {
                                    player.removeMediaItem(i)
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    enabled = queueWindows.size > 1,
                ) {
                    Icon(
                        painterResource(R.drawable.clear_all),
                        contentDescription = "Clear queue",
                        tint = if (queueWindows.size > 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        items(queueWindows.size) { index ->
            val window = queueWindows.getOrNull(index)
            val isCurrentSong = index == currentWindowIndex

            if (window != null) {
                TvQueueItem(
                    mediaItem = window.mediaItem,
                    isCurrentSong = isCurrentSong,
                    onClick = {
                        playerConnection?.player?.seekTo(index, 0)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (queueWindows.isEmpty()) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                ) {
                    Text(
                        text = "No songs in queue",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Add some music to get started",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}}
