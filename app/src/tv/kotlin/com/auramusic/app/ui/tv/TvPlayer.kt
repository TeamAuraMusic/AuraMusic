/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
 import androidx.compose.foundation.basicMarquee
 import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
 import androidx.compose.ui.unit.dp
 import androidx.compose.ui.unit.sp
  import androidx.media3.common.C
  import androidx.media3.common.MediaItem
  import androidx.media3.common.MediaMetadata
  import androidx.media3.common.Timeline
 import coil3.compose.AsyncImage
 import com.auramusic.app.R
 import com.auramusic.app.db.entities.Song
 import com.auramusic.app.playback.PlayerConnection
 import com.auramusic.app.playback.queues.YouTubeQueue
 import com.auramusic.app.playback.queues.ListQueue
 import com.auramusic.innertube.models.WatchEndpoint
 import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
 import androidx.lifecycle.viewModelScope
 import androidx.lifecycle.compose.collectAsStateWithLifecycle
 import com.auramusic.app.utils.makeTimeString
 import com.auramusic.app.LocalPlayerConnection
 import com.auramusic.app.ui.component.LocalMenuState
 import androidx.compose.ui.platform.LocalDensity
 import androidx.compose.ui.platform.LocalContext
 import androidx.compose.ui.platform.LocalWindowInfo
 import androidx.compose.ui.viewinterop.AndroidView
 import androidx.media3.ui.AspectRatioFrameLayout
 import androidx.media3.ui.PlayerView
 import com.auramusic.app.LocalListenTogetherManager
 import com.auramusic.app.constants.VideoModeEnabledKey
 import com.auramusic.app.models.toMediaMetadata
 import com.auramusic.app.ui.component.Lyrics

 @Composable
 private fun TvPlayerControlButton(
     onClick: () -> Unit,
     icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
     painter: androidx.compose.ui.graphics.painter.Painter? = null,
     contentDescription: String,
     size: androidx.compose.ui.unit.Dp = 72.dp,
     tint: Color = Color.White,
     focusRequester: FocusRequester? = null,
 ) {
     val isFocusedState = remember { mutableStateOf(false) }

     IconButton(
         onClick = onClick,
         modifier = Modifier
             .size(size)
             .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
             .onFocusChanged { isFocusedState.value = it.isFocused }
             .border(
                 width = if (isFocusedState.value) 3.dp else 0.dp,
                 color = if (isFocusedState.value) MaterialTheme.colorScheme.primary else Color.Transparent,
                 shape = CircleShape
             ),
     ) {
         if (icon != null) {
             Icon(
                 icon,
                 contentDescription = contentDescription,
                 tint = tint,
                 modifier = Modifier.size(size * 0.6f)
             )
         } else if (painter != null) {
             Icon(
                 painter = painter,
                 contentDescription = contentDescription,
                 tint = tint,
                 modifier = Modifier.size(size * 0.6f)
             )
         }
     }
 }

  @Composable
  private fun TvQueueItem(
      window: Timeline.Window,
      isCurrentSong: Boolean,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
  ) {
      val isFocusedState = remember { mutableStateOf(false) }
      val borderColor = if (isFocusedState.value || isCurrentSong) {
          MaterialTheme.colorScheme.primary
      } else {
          Color.Transparent
      }
      val backgroundColor = if (isCurrentSong) {
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      } else {
          MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
      }

      val mediaItem = window.mediaItem
          val metadata = mediaItem.mediaMetadata

      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          modifier = modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .border(2.dp, borderColor, RoundedCornerShape(8.dp))
              .onFocusChanged { isFocusedState.value = it.isFocused }
              .focusable()
              .clickable(onClick = onClick)
              .background(backgroundColor)
              .padding(12.dp),
      ) {
          Box(
              modifier = Modifier
                  .size(56.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant),
          ) {
              AsyncImage(
                  model = metadata?.artworkUri?.toString(),
                  contentDescription = metadata?.title?.toString() ?: "Song",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
              )
          }
           Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata?.title?.toString() ?: "Unknown title",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(
                        iterations = 1,
                        initialDelayMillis = 3000,
                        velocity = 30.dp
                    )
                )
                Text(
                    text = metadata?.artist?.toString() ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(
                        iterations = 1,
                        initialDelayMillis = 3000,
                        velocity = 30.dp
                    )
                )
           }
          // Duration
          val durationMs = window.durationMs
          if (durationMs > 0) {
              Text(
                  text = makeTimeString(durationMs),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
          }
      }
  }

/**
 * TV-compatible full-screen player with large controls optimized for remote control navigation.
 * Features large touch targets, clear visual hierarchy, and TV-friendly layout.
 * Split layout: left side = player controls, right side = queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvPlayerScreen(
    playerConnection: PlayerConnection?,
    onBackClick: () -> Unit,
) {
    BackHandler { onBackClick() }
    var duration by remember { mutableStateOf(0L) }
    var currentPosition by remember { mutableStateOf(0L) }
    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerEndTime by remember { mutableStateOf<Long?>(null) }
    var sleepTimerMessage by remember { mutableStateOf<String?>(null) }
    // Wire to ShowLyricsKey so MusicService fetches lyrics for the current song
    val (showLyrics, onShowLyricsChange) = com.auramusic.app.utils.rememberPreference(
        com.auramusic.app.constants.ShowLyricsKey,
        false,
    )

    // Resolve player connection: prefer passed-in parameter, fall back to composition local.
    // We avoid early return to show loading UI when service not ready.
    val effectivePlayerConnection = playerConnection ?: LocalPlayerConnection.current

    // Collect state from the resolved connection; if null, show loading/empty state
    val currentSong by effectivePlayerConnection?.currentSong?.collectAsState(null) ?: remember { mutableStateOf(null) }
    val currentMediaMetadata by effectivePlayerConnection?.mediaMetadata?.collectAsState(null) ?: remember { mutableStateOf(null) }
    val isPlaying by effectivePlayerConnection?.isPlaying?.collectAsState(false) ?: remember { mutableStateOf(false) }
    val queueWindows by effectivePlayerConnection?.queueWindows?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val currentWindowIndex by effectivePlayerConnection?.currentMediaItemIndex?.collectAsState() ?: remember { mutableStateOf(0) }
    val videoModeToggleEnabled by com.auramusic.app.utils.rememberPreference(
        VideoModeEnabledKey,
        defaultValue = true,
    )
    val videoModeEnabled by effectivePlayerConnection?.videoModeEnabled?.collectAsState(false) ?: remember { mutableStateOf(false) }
    val isVideoAvailable by effectivePlayerConnection?.isVideoAvailable?.collectAsState(false) ?: remember { mutableStateOf(false) }
    val isVideoSwitching by effectivePlayerConnection?.isVideoSwitching?.collectAsState(false) ?: remember { mutableStateOf(false) }
    val playbackState by effectivePlayerConnection?.playbackState?.collectAsState(androidx.media3.common.Player.STATE_IDLE)
        ?: remember { mutableStateOf(androidx.media3.common.Player.STATE_IDLE) }
    val serviceSleepTimer = effectivePlayerConnection?.service?.sleepTimer
    val sleepTimerActive = serviceSleepTimer?.isActive == true

    val currentLyrics by (effectivePlayerConnection?.currentLyrics?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) })

    // Local alias for concise usage throughout the composable
    val pc = effectivePlayerConnection

    // Focus requesters for TV navigation
    val playButtonFocus = remember { FocusRequester() }
    val queueItemFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Request focus on play button initially
        runCatching { playButtonFocus.requestFocus() }
    }

    LaunchedEffect(sleepTimerActive) {
        if (!sleepTimerActive) {
            sleepTimerMinutes = null
            sleepTimerEndTime = null
        }
    }

    LaunchedEffect(sleepTimerMessage) {
        if (sleepTimerMessage != null) {
            delay(1800)
            sleepTimerMessage = null
        }
    }

    LaunchedEffect(pc?.player) {
        val player = pc?.player ?: return@LaunchedEffect
        while (true) {
            delay(250)
            duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            currentPosition = player.currentPosition
        }
    }

    // Fetch lyrics when song changes. Use player metadata as a fallback so TV
    // playback that starts before the song row is present in the local DB still
    // has player artwork, lyrics and video state.
    val mediaMetadata = currentSong?.toMediaMetadata() ?: currentMediaMetadata

    LaunchedEffect(mediaMetadata?.id, mediaMetadata?.isVideoSong, videoModeToggleEnabled) {
        val videoId = mediaMetadata?.id ?: return@LaunchedEffect
        val playerConnection = pc ?: return@LaunchedEffect
        val available = playerConnection.service.checkVideoAvailability(videoId)

        if (videoModeToggleEnabled && mediaMetadata.isVideoSong && available && !videoModeEnabled) {
            playerConnection.enableVideoMode(true)
        } else if (videoModeEnabled && !mediaMetadata.isVideoSong) {
            playerConnection.enableVideoMode(false)
        }
    }

    // TV-specific: fetch lyrics fresh per song without database storage.
    // The mobile path uses MusicService + database caching, but on TV we
    // bypass the database (so storage doesn't accumulate) and push the
    // result directly into PlayerConnection.tvLyricsFlow.
    val context = LocalContext.current
    LaunchedEffect(mediaMetadata?.id, showLyrics) {
        if (!showLyrics || mediaMetadata == null || pc == null) return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.auramusic.app.di.LyricsHelperEntryPoint::class.java
                )
                val lyricsHelper = entryPoint.lyricsHelper()
                val fetched = lyricsHelper.getLyrics(mediaMetadata)
                pc.updateTvLyrics(mediaMetadata.id, fetched.lyrics, fetched.provider)
            } catch (t: Throwable) {
                pc.updateTvLyrics(
                    mediaMetadata.id,
                    com.auramusic.app.db.entities.LyricsEntity.LYRICS_NOT_FOUND,
                    "",
                )
            }
        }
    }

    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background with album art blur (full screen)
            mediaMetadata?.thumbnailUrl?.let { thumbnailUrl ->
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

            sleepTimerMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(1f)
                        .padding(top = 32.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                    )
                }
            }

            // Main content: Two-column layout. Some TVs expose less usable
            // height because of overscan/scaling, so compact the left player
            // column to keep playback controls visible on every TV.
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val compactHeight = maxHeight < 720.dp
                val contentPadding = if (compactHeight) 16.dp else 24.dp
                val contentSpacing = if (compactHeight) 20.dp else 32.dp
                val playerVerticalSpacing = if (compactHeight) 12.dp else 24.dp
                val topSpacerHeight = if (compactHeight) 16.dp else 48.dp
                val artworkSize = if (compactHeight) 220.dp else 280.dp
                val controlSpacing = if (compactHeight) 8.dp else 16.dp
                val secondaryTopPadding = if (compactHeight) 8.dp else 16.dp
                val sideControlSize = if (compactHeight) 56.dp else 72.dp
                val playControlSize = if (compactHeight) 68.dp else 80.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(contentSpacing),
                verticalAlignment = Alignment.Top,
            ) {
                // LEFT SIDE: Player controls (40% width)
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(playerVerticalSpacing),
                ) {
                    Spacer(modifier = Modifier.height(topSpacerHeight)) // Space for back button

                    // Album art / Lyrics / Video container
                    Box(
                        modifier = Modifier
                            .size(artworkSize)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isVideoBuffering = videoModeEnabled && playbackState == androidx.media3.common.Player.STATE_BUFFERING

                        if (videoModeEnabled) {
                            if (isVideoSwitching) {
                                // Show loading while video source is being fetched
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(48.dp),
                                        )
                                        Text(
                                            text = "Loading video...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            } else {
                                // Video is ready - show PlayerView
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = pc?.player
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            setBackgroundColor(android.graphics.Color.BLACK)
                                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                                            keepScreenOn = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    update = { playerView ->
                                        playerView.player = pc?.player
                                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        playerView.requestLayout()
                                    },
                                )

                                // Show buffering indicator on top of video when buffering
                                if (isVideoBuffering) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }
                                }
                            }
                        } else if (showLyrics) {
                            // Show lyrics behind thumbnail when enabled
                            val positionProvider = { currentPosition }

                            val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }

                            when {
                                lyrics == null -> {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                lyrics == com.auramusic.app.db.entities.LyricsEntity.LYRICS_NOT_FOUND -> {
                                    Text(
                                        text = "Lyrics not found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                else -> {
                                    androidx.compose.material3.ProvideTextStyle(
                                        value = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 18.sp,
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFFFFFBFE).copy(alpha = 0.98f) // Brighter white with high alpha
                                        )
                                    ) {
                                        com.auramusic.app.ui.component.Lyrics(
                                            sliderPositionProvider = positionProvider,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            showLyrics = true,
                                            disableInteractiveFeatures = true
                                        )
                                    }
                                }
                            }
                        } else {
                            // Show album art when lyrics are disabled
                            mediaMetadata?.let { metadata ->
                                AsyncImage(
                                    model = metadata.thumbnailUrl,
                                    contentDescription = metadata.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            // Subtle visualizer overlay at the bottom of album art
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .align(Alignment.BottomCenter)
                            ) {
                                TvAudioVisualizer(
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .padding(horizontal = 24.dp),
                                    barCount = 20,
                                    barColor = Color.White.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }

                    // Song info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                    Text(
                        text = mediaMetadata?.title ?: "No song playing",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            iterations = 1,
                            initialDelayMillis = 3000,
                            velocity = 30.dp
                        )
                    )

                        Text(
                            text = mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(
                                iterations = 1,
                                initialDelayMillis = 3000,
                                velocity = 30.dp
                            )
                        )
                    }

                    // Progress bar and time
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = makeTimeString(currentPosition),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Text(
                                text = makeTimeString(duration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }

                    // Playback controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(controlSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TvPlayerControlButton(
                            onClick = { pc?.seekToPrevious() },
                            icon = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous song",
                            size = sideControlSize,
                        )

                        TvPlayerControlButton(
                            onClick = {
                                val newPos = maxOf(0L, currentPosition - 10000L)
                                pc?.player?.seekTo(newPos)
                            },
                            icon = Icons.Filled.FastRewind,
                            contentDescription = "Rewind 10 seconds",
                            size = sideControlSize,
                        )

                        TvPlayerControlButton(
                            onClick = { pc?.togglePlayPause() },
                            icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            size = playControlSize,
                            focusRequester = playButtonFocus,
                        )

                        TvPlayerControlButton(
                            onClick = {
                                val durationVal = pc?.player?.duration?.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
                                val newPos = minOf(durationVal, currentPosition + 10000L)
                                pc?.player?.seekTo(newPos)
                            },
                            icon = Icons.Filled.FastForward,
                            contentDescription = "Fast forward 10 seconds",
                            size = sideControlSize,
                        )

                        TvPlayerControlButton(
                            onClick = { pc?.seekToNext() },
                            icon = Icons.Filled.SkipNext,
                            contentDescription = "Next song",
                            size = sideControlSize,
                        )
                    }

                    // Secondary controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(controlSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = secondaryTopPadding)
                    ) {
                        TvPlayerControlButton(
                            onClick = { pc?.toggleShuffle() },
                            icon = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (pc?.shuffleModeEnabled?.value == true)
                                MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            size = sideControlSize,
                        )

                        TvPlayerControlButton(
                            onClick = {
                                pc?.player?.let { player ->
                                    val currentMode = player.repeatMode
                                    val newMode = when (currentMode) {
                                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                    }
                                    player.repeatMode = newMode
                                }
                            },
                            icon = when (pc?.repeatMode?.value) {
                                androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                                else -> Icons.Filled.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (pc?.repeatMode?.value != androidx.media3.common.Player.REPEAT_MODE_OFF)
                                MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            size = sideControlSize,
                        )

                        TvPlayerControlButton(
                            onClick = {
                                val currentMinutes = sleepTimerMinutes
                                val newMinutes = when (currentMinutes) {
                                    null -> 15
                                    15 -> 30
                                    30 -> 60
                                    60 -> 120
                                    else -> null
                                }

                                if (newMinutes != null) {
                                    sleepTimerEndTime = System.currentTimeMillis() + (newMinutes * 60 * 1000L)
                                    sleepTimerMinutes = newMinutes
                                    pc?.service?.sleepTimer?.start(newMinutes)
                                    sleepTimerMessage = "Sleep timer: $newMinutes minutes"
                                } else {
                                    sleepTimerEndTime = null
                                    sleepTimerMinutes = null
                                    pc?.service?.sleepTimer?.clear()
                                    sleepTimerMessage = "Sleep timer off"
                                }
                            },
                            painter = painterResource(R.drawable.bedtime),
                            contentDescription = if (sleepTimerActive) "Cancel sleep timer" else "Set sleep timer",
                            tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            size = sideControlSize,
                        )

                        TvPlayerControlButton(
                            onClick = { onShowLyricsChange(!showLyrics) },
                            icon = Icons.Filled.Lyrics,
                            contentDescription = if (showLyrics) "Hide lyrics" else "Show lyrics",
                            tint = if (showLyrics) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            size = sideControlSize,
                        )
                    }
                }

                // RIGHT SIDE: Queue (60% width) - always visible
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                ) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            count = queueWindows.size,
                            key = { index -> "${queueWindows.getOrNull(index)?.mediaItem?.mediaId ?: "unknown"}_$index" }
                        ) { index ->
                            val window = queueWindows.getOrNull(index)
                            val isCurrentSong = index == currentWindowIndex

                            if (window != null) {
                                TvQueueItem(
                                    window = window,
                                    isCurrentSong = isCurrentSong,
                                    onClick = {
                                        pc?.player?.seekTo(index, C.TIME_UNSET)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        if (queueWindows.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Queue is empty",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            // Back button (top-left)
            var backButtonFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(24.dp)
                    .size(64.dp)
                    .align(Alignment.TopStart)
                    .onFocusChanged { backButtonFocused = it.isFocused }
                    .border(
                        width = if (backButtonFocused) 3.dp else 0.dp,
                        color = if (backButtonFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
