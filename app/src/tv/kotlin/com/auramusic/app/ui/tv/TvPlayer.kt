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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
 import androidx.compose.foundation.shape.CircleShape
 import androidx.compose.foundation.shape.RoundedCornerShape
 import androidx.compose.foundation.basicMarquee
 import androidx.compose.ui.text.style.TextOverflow
 import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.mutableStateMapOf
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
import kotlinx.coroutines.withContext
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
 import com.auramusic.app.constants.VideoCodec
 import com.auramusic.app.constants.VideoCodecKey
 import com.auramusic.app.constants.VideoModeEnabledKey
 import com.auramusic.app.constants.VideoQuality
 import com.auramusic.app.constants.VideoQualityKey
 import com.auramusic.app.models.toMediaMetadata
 import com.auramusic.app.ui.component.Lyrics

 // Where each fullscreen transport button sits in the row the remote walks. The slots
 // are fixed rather than positional so a button that is not drawn - the lyrics toggle
 // while a video owns the picture - does not shift the ones around it.
 private const val FULLSCREEN_PREVIOUS_SLOT = 0
 private const val FULLSCREEN_REWIND_SLOT = 1
 private const val FULLSCREEN_PLAY_SLOT = 2
 private const val FULLSCREEN_FORWARD_SLOT = 3
 private const val FULLSCREEN_NEXT_SLOT = 4
 private const val FULLSCREEN_LYRICS_SLOT = 5
 private const val FULLSCREEN_QUEUE_SLOT = 6
 private const val FULLSCREEN_QUALITY_SLOT = 7
 private const val FULLSCREEN_SUBTITLES_SLOT = 8
 private const val FULLSCREEN_EXIT_SLOT = 9
 private const val FULLSCREEN_TRANSPORT_SLOTS = 10

 @Composable
 private fun TvPlayerControlButton(
     onClick: () -> Unit,
     icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
     painter: androidx.compose.ui.graphics.painter.Painter? = null,
     contentDescription: String,
     size: androidx.compose.ui.unit.Dp = 72.dp,
     tint: Color = Color.White,
     focusRequester: FocusRequester? = null,
     modifier: Modifier = Modifier,
     onFocused: () -> Unit = {},
 ) {
     val isFocusedState = remember { mutableStateOf(false) }

     IconButton(
         onClick = onClick,
         modifier = modifier
             .size(size)
             .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
             .onFocusChanged {
                 isFocusedState.value = it.isFocused
                 // A button nobody can see is a button nobody can aim at, so landing
                 // focus on one brings the whole overlay back.
                 if (it.isFocused) onFocused()
             }
             .background(
                 color = if (isFocusedState.value) {
                     MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                 } else {
                     Color.Transparent
                 },
                 shape = CircleShape,
             )
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

 /**
  * One video in the queue, drawn the way a song is drawn in it.
  *
  * A video that comes next is not a timeline window - it has no window until it is
  * chosen - so it is listed from the video's own list of what follows, which is the
  * same list the next button moves through.
  */
 @Composable
 private fun TvVideoQueueItem(
     item: com.auramusic.app.video.VideoPlaybackManager.RecommendationItem,
     isCurrent: Boolean,
     onClick: () -> Unit,
     modifier: Modifier = Modifier,
     focusRequester: FocusRequester? = null,
     onFocused: () -> Unit = {},
 ) {
     val isFocusedState = remember { mutableStateOf(false) }
     val bringIntoViewRequester = remember { BringIntoViewRequester() }
     val bringIntoViewScope = rememberCoroutineScope()
     val borderColor = if (isFocusedState.value || isCurrent) {
         MaterialTheme.colorScheme.primary
     } else {
         Color.Transparent
     }
     val backgroundColor = when {
         isFocusedState.value -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
         isCurrent -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
         else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
     }

     Row(
         verticalAlignment = Alignment.CenterVertically,
         horizontalArrangement = Arrangement.spacedBy(16.dp),
         modifier = modifier
             .fillMaxWidth()
             .clip(RoundedCornerShape(8.dp))
             .border(2.dp, borderColor, RoundedCornerShape(8.dp))
             .bringIntoViewRequester(bringIntoViewRequester)
             .onFocusChanged {
                 isFocusedState.value = it.isFocused
                 if (it.isFocused) {
                     bringIntoViewScope.launch {
                         runCatching { bringIntoViewRequester.bringIntoView() }
                     }
                     onFocused()
                 }
             }
             .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
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
                 model = item.thumbnail,
                 contentDescription = item.title,
                 contentScale = ContentScale.Crop,
                 modifier = Modifier.fillMaxSize(),
             )
         }
         Column(modifier = Modifier.weight(1f)) {
             Text(
                 text = item.title,
                 style = MaterialTheme.typography.bodyLarge,
                 fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                 color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                 maxLines = 2,
                 overflow = TextOverflow.Ellipsis,
             )
             if (item.channelName.isNotBlank()) {
                 Text(
                     text = item.channelName,
                     style = MaterialTheme.typography.bodySmall,
                     color = Color.White.copy(alpha = 0.7f),
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis,
                 )
             }
         }
         if (item.durationText != null) {
             Text(
                 text = item.durationText,
                 style = MaterialTheme.typography.bodySmall,
                 color = Color.White.copy(alpha = 0.7f),
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
      focusRequester: FocusRequester? = null,
      onFocused: () -> Unit = {},
  ) {
      val isFocusedState = remember { mutableStateOf(false) }
      // Keeps the row the remote is actually on inside the panel, so pressing down
      // past the bottom of the list scrolls instead of going nowhere.
      val bringIntoViewRequester = remember { BringIntoViewRequester() }
      val bringIntoViewScope = rememberCoroutineScope()
      val borderColor = if (isFocusedState.value || isCurrentSong) {
          MaterialTheme.colorScheme.primary
      } else {
          Color.Transparent
      }
      val backgroundColor = when {
          isFocusedState.value -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
          isCurrentSong -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
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
              .bringIntoViewRequester(bringIntoViewRequester)
              .onFocusChanged {
                  isFocusedState.value = it.isFocused
                  if (it.isFocused) {
                      bringIntoViewScope.launch {
                          runCatching { bringIntoViewRequester.bringIntoView() }
                      }
                      onFocused()
                  }
              }
              .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
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
 * The resolutions a television is asked to fetch, best first.
 *
 * Auto is offered because a household connection is not ours to measure, but the
 * fixed steps above 720p are the reason this control exists at all: YouTube only
 * ships separate video and audio streams past that point, which is why 1080p and
 * above look sharper than the default rather than merely larger.
 */
private val QUALITY_OPTIONS = listOf(
    VideoQuality.QUALITY_AUTO,
    VideoQuality.QUALITY_2160P,
    VideoQuality.QUALITY_1440P,
    VideoQuality.QUALITY_1080P,
    VideoQuality.QUALITY_720P,
    VideoQuality.QUALITY_480P,
    VideoQuality.QUALITY_360P,
)

/** One subtitle track, or turning them off: a row the remote can land on and press. */
@Composable
private fun TvSubtitleOption(
    label: String,
    detail: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
) {
    val isFocused = remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val bringIntoViewScope = rememberCoroutineScope()
    val borderColor = when {
        isFocused.value -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .background(
                if (isFocused.value) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                },
            )
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged {
                isFocused.value = it.isFocused
                if (it.isFocused) {
                    bringIntoViewScope.launch {
                        runCatching { bringIntoViewRequester.bringIntoView() }
                    }
                    onFocused()
                }
            }
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected || isFocused.value) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** One resolution in the quality panel: a row the remote can land on and press. */
@Composable
private fun TvQualityOption(
    quality: VideoQuality,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
) {
    val isFocused = remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val bringIntoViewScope = rememberCoroutineScope()
    val borderColor = when {
        isFocused.value -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .background(
                if (isFocused.value) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                },
            )
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged {
                isFocused.value = it.isFocused
                if (it.isFocused) {
                    bringIntoViewScope.launch {
                        runCatching { bringIntoViewRequester.bringIntoView() }
                    }
                    onFocused()
                }
            }
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    quality.isAuto -> "Auto"
                    else -> quality.label
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected || isFocused.value) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = when {
                    quality.isAuto -> "Let the app choose"
                    quality.height >= 1080 -> "Sharper, needs more bandwidth"
                    quality.height == 720 -> "Default"
                    else -> "Smoother, uses less data"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Media3 PlayerView hosting the active player plus the switch/buffering overlays.
 * Exactly one instance may be composed at a time (split layout vs. fullscreen
 * layer): the surface claims the video output, so the two must never coexist.
 */
@Composable
private fun TvVideoView(
    activePlayer: androidx.media3.common.Player?,
    isVideoSwitching: Boolean,
    isBuffering: Boolean,
    videoId: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // Which view currently holds the player's surface. A player has one surface
        // at a time, and it does not care which view asked for it: the two instances
        // here live in mutually exclusive branches, so moving between them disposes
        // one while creating the other in the same frame, and whichever loses the
        // race takes the surface away from the winner on its way out.
        val surfaceOwner = remember { arrayOfNulls<PlayerView>(1) }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = activePlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    surfaceOwner[0] = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                // Always sync player reference and resume surface. Keep player
                // attached across navigations to avoid black screen after 2+ re-entries.
                surfaceOwner[0] = playerView
                if (playerView.player !== activePlayer) {
                    playerView.player = activePlayer
                }
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                playerView.onResume()
                playerView.requestLayout()
            },
            onRelease = { playerView ->
                // Only let go if this is still the view holding the picture. A view
                // replaced by another one has already handed the surface over, and
                // releasing it here would blank the picture that replaced it until
                // something else happened to run the update block.
                if (surfaceOwner[0] === playerView) {
                    surfaceOwner[0] = null
                    playerView.onPause()
                }
            },
        )

        // Captions, the way the phone draws them: an overlay on top of the picture,
        // read from the app-wide preference, and never part of what is loaded.
        //
        // That last part is the whole of it. Carrying captions on the media item -
        // which is what this was doing before - puts a round trip to a caption server
        // in front of the picture, so a video waits on translations before it will
        // appear. Drawn over the top, the video starts when its stream does and the
        // words arrive whenever they arrive.
        //
        // Anchored to the bottom of the picture rather than left wherever it lands:
        // this view centres its contents, which put the words in the middle of the
        // frame over the thing being watched.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            (activePlayer as? androidx.media3.exoplayer.ExoPlayer)?.let { exo ->
                com.auramusic.app.video.VideoCaptionOverlay(
                    videoId = videoId,
                    player = exo,
                    // Clear of the transport controls in fullscreen, which is where
                    // the captions would otherwise sit on top of.
                    bottomPadding = 132.dp,
                    // Readable from a sofa. The size setting is a phone setting; this
                    // is the room asking for bigger, without the setting itself
                    // meaning something different on a television than it does in a hand.
                    fontSizeScale = 1.8f,
                )
            }
        }

        if (isVideoSwitching) {
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
        } else if (isBuffering) {
            // Lighter overlay for brief buffering mid-playback
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
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
) {
    // The queue opened over the media is the first thing Back closes: leaving it
    // open and dropping straight out of fullscreen throws away the way out.
    var fullscreenQueueOpen by remember { mutableStateOf(false) }
    var videoQualityPanelOpen by remember { mutableStateOf(false) }
    var subtitlePanelOpen by remember { mutableStateOf(false) }
    BackHandler {
        when {
            fullscreenQueueOpen -> fullscreenQueueOpen = false
            videoQualityPanelOpen -> videoQualityPanelOpen = false
            subtitlePanelOpen -> subtitlePanelOpen = false
            isFullScreen -> onFullScreenChange(false)
            else -> onBackClick()
        }
    }
    // Leaving fullscreen takes the panels with it, so neither can reappear over the
    // split layout with no way to close it.
    LaunchedEffect(isFullScreen) {
        if (!isFullScreen) {
            fullscreenQueueOpen = false
            videoQualityPanelOpen = false
            subtitlePanelOpen = false
        }
    }
    var duration by remember { mutableStateOf(0L) }
    var currentPosition by remember { mutableStateOf(0L) }
    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerEndTime by remember { mutableStateOf<Long?>(null) }
    var sleepTimerMessage by remember { mutableStateOf<String?>(null) }
    // Keep TV's expanded-lyrics mode independent from mobile and voice-command UI state.
    val (showLyrics, onShowLyricsChange) = com.auramusic.app.utils.rememberPreference(
        com.auramusic.app.constants.TvShowLyricsKey,
        false,
    )
    // On TV, enabling lyrics always means expanded mode. The preference survives
    // navigation, so returning to the player restores the same expanded view.
    val lyricsExpanded = showLyrics

    // Fullscreen media overlay: the bottom controls fade out after a few idle
    // seconds and any remote key press wakes them again — the wake counter
    // restarts the hide timer.
    var controlsVisible by remember { mutableStateOf(true) }
    var wakeTick by remember { mutableStateOf(0) }
    LaunchedEffect(isFullScreen, wakeTick) {
        if (!isFullScreen) return@LaunchedEffect
        controlsVisible = true
        delay(5000)
        controlsVisible = false
    }
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        label = "fullscreenControls",
    )
    // Fullscreen transport controls are driven from the remote rather than a pointer,
    // so left/right walks the row itself instead of hoping the focus search picks the
    // right neighbour: the row is the only thing on screen holding focus, and the
    // search would otherwise have nowhere to go once it reached either end.
    val transportFocus = remember {
        List(FULLSCREEN_TRANSPORT_SLOTS) { FocusRequester() }
    }
    // The queue panel's own close button, so the remote can leave the list without
    // having to walk all the way back to the row underneath it.
    val queueCloseFocus = remember { FocusRequester() }
    // One requester per queue row, kept across recompositions so the panel can put the
    // remote back on the row that was playing when it is opened.
    val queueItemFocusers = remember { mutableStateMapOf<String, FocusRequester>() }
    fun queueFocuserFor(mediaId: String): FocusRequester =
        queueItemFocusers.getOrPut(mediaId) { FocusRequester() }
    val qualityOptionFocusers = remember { mutableStateMapOf<String, FocusRequester>() }
    val qualityCloseFocus = remember { FocusRequester() }
    val subtitleOnFocus = remember { FocusRequester() }
    val subtitleOffFocus = remember { FocusRequester() }
    val subtitleCloseFocus = remember { FocusRequester() }
    // Which button the remote is on. Kept as state rather than read off focus so the
    // row can move focus without racing the focus system's own bookkeeping.
    var focusedTransport by remember { mutableStateOf(FULLSCREEN_PLAY_SLOT) }
    // Whether the remote is inside the queue panel or back on the transport row. The
    // panel is open over controls that still work, so left and right have to know
    // which of the two they are leaving.
    var focusInQueue by remember { mutableStateOf(false) }
    // Waking the overlay when a button takes focus: the remote has just proved the
    // listener is still watching, and controls that stay faded out are controls that
    // cannot be aimed at.
    fun wakeControls() {
        controlsVisible = true
        wakeTick++
    }

    /** Waking the overlay and noting that the remote is back on the transport row. */
    fun wakeTransport() {
        focusInQueue = false
        wakeControls()
    }

    // Resolve player connection: prefer passed-in parameter, fall back to composition local.
    // We avoid early return to show loading UI when service not ready.
    val effectivePlayerConnection = playerConnection ?: LocalPlayerConnection.current

    // This screen is where a video is actually shown. While it is on screen a video
    // switch waits for its first frame before resuming, so the picture and the sound
    // arrive together; while it is not, there is nothing to render a frame onto and
    // waiting for one would only be silence.
    //
    // Driven by whichever connection the screen actually resolved rather than the one
    // it was handed. Reading the handed one left the service believing nobody was
    // watching whenever the connection came from the composition instead, so every
    // switch paid the full wait for a frame nothing was going to render.
    LaunchedEffect(effectivePlayerConnection) {
        effectivePlayerConnection?.service?.setTvPlayerVisible(true)
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            effectivePlayerConnection?.service?.setTvPlayerVisible(false)
        }
    }

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
    // A video played from the video shelf is on this same player as a video from its
    // first frame, rather than as an audio item something later swaps a video in for.
    // Both are a picture on screen, so both are drawn the same way.
    val guestVideoActive by effectivePlayerConnection?.guestVideoActive?.collectAsState(false) ?: remember { mutableStateOf(false) }
    val showVideo = videoModeEnabled || guestVideoActive
    // The videos that follow the one playing.
    //
    // A television's playlist holds only the video itself - the music service owns
    // the queue and a video does not add a second one - so what comes next is kept
    // by the video pipeline rather than by the player. This is the list the remote
    // walks, which is what a queue is: without it a video ended and went nowhere,
    // and next had nothing to move to.
    val videoUiState by com.auramusic.app.video.VideoPlaybackManager.uiState.collectAsState()
    val currentVideoId = videoUiState.session?.videoId

    val videoQueueShuffled = videoUiState.queueOrder != null
    // Starting one video from this list replaces it with another, and between the
    // two the list is briefly rebuilt from the new video's own recommendations. The
    // panel fell out of that gap and showed nothing, then came back - so it is given
    // the video that is playing whenever the list cannot answer, and never empties
    // while a video is.
    val videoQueue = videoUiState.queue.ifEmpty {
        videoUiState.session?.let { session ->
            listOf(
                com.auramusic.app.video.VideoPlaybackManager.RecommendationItem(
                    videoId = session.videoId,
                    title = session.title,
                    channelName = session.channelName,
                    channelId = session.channelId,
                    thumbnail = session.videoThumbnail ?: session.channelThumbnail,
                    durationText = null,
                ),
            )
        }.orEmpty()
    }
    // The queue follows what is playing.
    //
    // A queue that does not move is a list to read rather than a place to be: when
    // the song changes, the row playing is somewhere off-screen with nothing on it
    // saying so. Each panel scrolls to wherever the current item now is, so the list
    // is always showing the one thing that matters.
    //
    // Only when the current item actually changes, and only while the panel is
    // closed - a list that moves under the remote while they are in it is a list
    // they cannot read. Their own scrolling is left alone entirely.
    val splitQueueListState = rememberLazyListState()
    val fullQueueListState = rememberLazyListState()
    LaunchedEffect(currentWindowIndex, guestVideoActive, currentVideoId) {
        if (fullscreenQueueOpen) return@LaunchedEffect
        val index = if (guestVideoActive) {
            videoQueue.indexOfFirst { it.videoId == currentVideoId }
        } else {
            currentWindowIndex
        }
        if (index < 0) return@LaunchedEffect
        // A short distance is a nudge; anything further is a jump, and animating
        // through a whole queue to reach the end is something to wait for rather
        // than watch.
        // Is it on screen already? Glide if so, jump if not - animating through
        // a whole queue to reach the end is something to wait for, not watch.
        if (splitQueueListState.layoutInfo.visibleItemsInfo.any { it.index == index }) {
            splitQueueListState.animateScrollToItem(index)
        } else {
            splitQueueListState.scrollToItem(index)
        }
    }

    // The open panel follows too, but only when the remote is not on it - someone
    // reading down a queue should not have it move out from under them. Once focus
    // is elsewhere in the panel again, it resumes following what is playing.
    LaunchedEffect(currentWindowIndex, guestVideoActive, currentVideoId, focusInQueue) {
        if (!fullscreenQueueOpen || focusInQueue) return@LaunchedEffect
        val index = if (guestVideoActive) {
            videoQueue.indexOfFirst { it.videoId == currentVideoId }
        } else {
            currentWindowIndex
        }
        if (index < 0) return@LaunchedEffect
        // Is it on screen already? Glide if so, jump if not - animating through
        // a whole queue to reach the end is something to wait for, not watch.
        if (fullQueueListState.layoutInfo.visibleItemsInfo.any { it.index == index }) {
            fullQueueListState.animateScrollToItem(index)
        } else {
            fullQueueListState.scrollToItem(index)
        }
    }

    // Subtitles are offered by the same service that offers them on the phone, so
    // what is listed here is what is listed there and choosing one does the same.
    // The same switch the caption overlay and the phone player read. Captions are
    // drawn over the picture rather than carried on the media item, so there is no
    // track list to be empty: the preference is the whole of what is on offer.
    val (captionsOn, setCaptionsOn) = com.auramusic.app.utils.rememberPreference(
        com.auramusic.app.constants.SubtitlesEnabledKey,
        defaultValue = true,
    )
    val isVideoAvailable by effectivePlayerConnection?.isVideoAvailable?.collectAsState(false) ?: remember { mutableStateOf(false) }
    val isVideoSwitching by effectivePlayerConnection?.isVideoSwitching?.collectAsState(false) ?: remember { mutableStateOf(false) }
    val playbackState by effectivePlayerConnection?.playbackState?.collectAsState(androidx.media3.common.Player.STATE_IDLE)
        ?: remember { mutableStateOf(androidx.media3.common.Player.STATE_IDLE) }
    val serviceSleepTimer = effectivePlayerConnection?.service?.sleepTimer
    val sleepTimerActive = serviceSleepTimer?.isActive == true

    // The resolution the video is fetched at. A television is watched from across a
    // room, where the phone-sized 720p default is visibly soft, so this is chosen from
    // the player rather than left in settings nobody finds.
    val (videoQualityName, onVideoQualityNameChange) = com.auramusic.app.utils.rememberPreference(
        VideoQualityKey,
        defaultValue = VideoQuality.QUALITY_1080P.name,
    )
    val (videoCodecName, _) = com.auramusic.app.utils.rememberPreference(
        VideoCodecKey,
        defaultValue = VideoCodec.CODEC_AUTO.name,
    )
    var videoQualityMessage by remember { mutableStateOf<String?>(null) }
    val qualityScope = rememberCoroutineScope()
    val videoQuality = VideoQuality.fromName(videoQualityName)

    val currentLyrics by (effectivePlayerConnection?.currentLyrics?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) })

    // Local alias for concise usage throughout the composable
    val pc = effectivePlayerConnection
    val activePlayer by (pc?.service?.playerFlow?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) })

    // Focus requesters for TV navigation
    val playButtonFocus = remember { FocusRequester() }
    val queueItemFocus = remember { FocusRequester() }

    // The slots the remote walks, in the order they sit on screen. The lyrics toggle
    // has nowhere to go while a video owns the picture, so the row closes up around
    // it instead of stepping onto a button that was never drawn.
    //
    // Keyed on what the row is actually built from. Keying it on video mode alone
    // meant a video that arrived as a video - which never turns video mode on at all -
    // left the row describing a layout that no longer existed, and the remote walked
    // onto a lyrics button that was no longer drawn and went nowhere.
    val transportSlots = remember(showVideo) {
        buildList {
            add(FULLSCREEN_PREVIOUS_SLOT)
            add(FULLSCREEN_REWIND_SLOT)
            add(FULLSCREEN_PLAY_SLOT)
            add(FULLSCREEN_FORWARD_SLOT)
            add(FULLSCREEN_NEXT_SLOT)
            if (!showVideo) add(FULLSCREEN_LYRICS_SLOT)
            // Quality sits next to the queue: both are "how this plays" rather than
            // "what plays next", and both open over the media.
            add(FULLSCREEN_QUEUE_SLOT)
            add(FULLSCREEN_QUALITY_SLOT)
            add(FULLSCREEN_SUBTITLES_SLOT)
            add(FULLSCREEN_EXIT_SLOT)
        }
    }

    /** Moves the remote one button left or right along the drawn row. */
    fun moveTransportFocus(delta: Int) {
        val position = transportSlots.indexOf(focusedTransport)
            .takeIf { it >= 0 }
            ?: transportSlots.indexOf(FULLSCREEN_PLAY_SLOT)
        val target = transportSlots[(position + delta).coerceIn(0, transportSlots.lastIndex)]
        focusedTransport = target
        wakeControls()
        runCatching { transportFocus[target].requestFocus() }
    }

    // The remote's next and previous.
    //
    // While a video is playing, the playlist holds that video and nothing else - a
    // television has one owner of its queue - so a raw seek, which moves through a
    // music queue, has nothing to move to and the button does nothing at all. The
    // video pipeline keeps the list of what comes next and can start it.
    fun skipNext() {
        if (guestVideoActive) com.auramusic.app.video.VideoPlaybackManager.playNext()
        else pc?.seekToNext()
    }

    fun skipPrevious() {
        if (guestVideoActive) com.auramusic.app.video.VideoPlaybackManager.playPrevious()
        else pc?.seekToPrevious()
    }

    fun focusTransportSlot(slot: Int) {
        focusedTransport = slot
        focusInQueue = false
        wakeControls()
        runCatching { transportFocus[slot].requestFocus() }
    }

    // Opening the subtitle panel lands on the track in use, or on "Off" when the
    // video is playing without one - the state a reader is most likely to be leaving.
    LaunchedEffect(subtitlePanelOpen, captionsOn) {
        if (!subtitlePanelOpen) return@LaunchedEffect
        wakeControls()
        delay(150)
        runCatching { (if (captionsOn) subtitleOnFocus else subtitleOffFocus).requestFocus() }
    }

    // Opening the panel puts the remote on the resolution in use, so left and right
    // move between neighbouring options rather than starting from the top of a list.
    LaunchedEffect(videoQualityPanelOpen) {
        if (!videoQualityPanelOpen) return@LaunchedEffect
        wakeControls()
        delay(150)
        runCatching { qualityOptionFocusers[videoQuality.name]?.requestFocus() }
    }

    /**
     * Fetches the video again at [quality].
     *
     * The stream already loaded is the one that was asked for when this song started,
     * so writing the preference changes what comes next and not what is on screen. The
     * switch is therefore forced - video off, then on - which re-resolves the source
     * against the new preference. A video that has nothing at the resolution asked for
     * says so and stays where it was: a soft picture is better than a stopped one, and
     * a resolution that is quietly downgraded is better told than assumed.
     */
    fun applyVideoQuality(quality: VideoQuality) {
        if (quality == videoQuality) {
            videoQualityPanelOpen = false
            return
        }
        val videoId = pc?.mediaMetadata?.value?.id
        videoQualityPanelOpen = false
        qualityScope.launch {
            // The preference is written whatever happens: it is what the next video is
            // fetched at, and a resolution this one lacks is not a wrong choice for
            // the one after it.
            onVideoQualityNameChange(quality.name)
            com.auramusic.app.utils.AuraPlayerUtils.setPreferredVideoQuality(quality)

            if (videoId == null) {
                // Nothing playing to re-fetch: the setting is in place and takes
                // effect on its own when there is a video to fetch it for.
                return@launch
            }
            val wasVideoMode = videoModeEnabled
            if (wasVideoMode) {
                // Back to the audio item first: the service refuses a second switch to
                // the mode it is already in, and the stream it holds is the old one.
                pc?.enableVideoMode(false)
                delay(150)
            }

            val resolved = withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.auramusic.app.utils.AuraPlayerUtils
                    .getVideoStreamSource(videoId, quality, VideoCodec.fromName(videoCodecName))
                    .isSuccess
            }
            if (resolved) {
                pc?.enableVideoMode(true)
            } else {
                // Asked for more than this video has. Say so rather than silently
                // showing something else - and leave the video off rather than
                // re-running the switch that already failed.
                videoQualityMessage = "${quality.label} unavailable for this video"
                delay(2200)
                videoQualityMessage = null
                if (wasVideoMode) {
                    com.auramusic.app.utils.AuraPlayerUtils.setPreferredVideoQuality(videoQuality)
                    onVideoQualityNameChange(videoQuality.name)
                    pc?.enableVideoMode(true)
                }
            }
        }
    }

    LaunchedEffect(isFullScreen) {
        if (isFullScreen) {
            delay(120)
            // Play/pause is where the remote starts: it is the one control every
            // viewer reaches for first.
            focusTransportSlot(FULLSCREEN_PLAY_SLOT)
        }
    }

    /** Puts the remote on the row that is playing, so up and down walk from there. */
    fun focusQueue() {
        controlsVisible = true
        wakeTick++
        val playing = queueWindows.getOrNull(currentWindowIndex)?.mediaItem?.mediaId
        val requester = playing?.let { queueItemFocusers[it] }
            ?: queueWindows.firstOrNull()?.mediaItem?.mediaId?.let { queueItemFocusers[it] }
            ?: queueCloseFocus
        if (requester != null) runCatching { requester.requestFocus() }
    }

    // Opening the queue hands the remote to it: the row that is playing takes focus
    // so up and down walk the list from a known place, rather than starting the
    // listener back at the top of a queue they cannot see.
    LaunchedEffect(fullscreenQueueOpen, currentWindowIndex) {
        if (!fullscreenQueueOpen) return@LaunchedEffect
        // The controls stay on screen for as long as the panel is: a queue that hid
        // the controls it was opened alongside made pausing it a two-press errand.
        controlsVisible = true
        delay(150)
        focusQueue()
    }

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

    LaunchedEffect(activePlayer) {
        val player = activePlayer ?: return@LaunchedEffect
        while (true) {
            delay(250)
            val playbackPosition = runCatching {
                (player.duration.takeIf { it != C.TIME_UNSET } ?: 0L) to player.currentPosition
            }.getOrNull() ?: break
            duration = playbackPosition.first
            currentPosition = playbackPosition.second
        }
    }

    // Fetch lyrics when song changes. Use player metadata as a fallback so TV
    // playback that starts before the song row is present in the local DB still
    // has player artwork, lyrics and video state.
    val mediaMetadata = currentSong?.toMediaMetadata() ?: currentMediaMetadata

    // Keyed on whether a video is already playing as one. Without it the guard below
    // was read once, when the effect started, so a video started while the effect was
    // still waiting on the network was switched to video mode a moment later - which
    // is the switch that pauses a playing video and never brings it back.
    LaunchedEffect(
        mediaMetadata?.id,
        mediaMetadata?.isVideoSong,
        videoModeToggleEnabled,
        guestVideoActive,
    ) {
        val videoId = mediaMetadata?.id ?: return@LaunchedEffect
        val playerConnection = pc ?: return@LaunchedEffect

        // If video mode is already active in the service, never re-trigger it.
        // The PlayerView update block handles surface re-attachment on re-entry.
        // Re-calling enableVideoMode on re-entry pauses playback, shows loading,
        // and causes the black-screen-then-loading bug on the 2nd+ re-entry.
        if (videoModeEnabled) return@LaunchedEffect

        // A video played as a video is already on the player, carrying its own
        // picture. Asking the service to switch this item to video mode is asking it
        // to replace the very thing that is playing with a video of the same name -
        // which pauses, re-resolves, and then leaves playback sitting still because
        // the item it was told about is no longer the one on the player. A video
        // that arrived as a video is left exactly as it is.
        if (guestVideoActive) return@LaunchedEffect

        // Fast path for TV: if this is a known video song, enable video mode immediately
        // without waiting for the network availability check. The check is still done
        // in parallel to disable if unavailable, but the video starts fetching right away.
        if (videoModeToggleEnabled && mediaMetadata.isVideoSong) {
            playerConnection.enableVideoMode(true)
            // Verify availability in background; if not actually available, fall back to audio.
            launch(kotlinx.coroutines.Dispatchers.IO) {
                val available = playerConnection.service.checkVideoAvailability(videoId)
                val currentId = playerConnection.mediaMetadata.value?.id
                if (currentId == videoId && !available) {
                    playerConnection.enableVideoMode(false)
                }
            }
            return@LaunchedEffect
        }

        val available = playerConnection.service.checkVideoAvailability(videoId)

        // Staleness check: song may have changed during network call
        val currentId = playerConnection.mediaMetadata.value?.id
        if (currentId != videoId) return@LaunchedEffect

        if (videoModeToggleEnabled && mediaMetadata.isVideoSong && available) {
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
                if (t is kotlinx.coroutines.CancellationException) throw t
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
            // The backdrop is the song's own canvas - the same one the phone puts
            // behind its player - and it changes with what is playing, because it is
            // looked up by title and artist rather than being a fixed picture. Black
            // under it, so a song with no artwork yet has somewhere to go.
            //
            // Only when fullscreen is not drawing its own: a canvas is a playing video,
            // and two of them would be two decoders for one picture.
            if (!isFullScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                )
                TvCanvasBackdrop(
                    backdropTitle = mediaMetadata?.title.orEmpty(),
                    backdropArtist = mediaMetadata?.artists?.joinToString(", ") { it.name },
                    imageUrl = mediaMetadata?.thumbnailUrl,
                    // Media is watched against black, not against the page colour.
                    scrimColor = Color.Black,
                )
            }

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

            if (!isFullScreen) {
            // Main content: Two-column layout. Some TVs expose less usable
            // height because of overscan/scaling, so compact the left player
            // column to keep playback controls visible on every TV.
            Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val compactHeight = maxHeight < 720.dp
                val veryCompact = maxHeight < 540.dp
                val contentPadding = if (veryCompact) 8.dp else if (compactHeight) 16.dp else 24.dp
                val contentSpacing = if (veryCompact) 8.dp else if (compactHeight) 20.dp else 32.dp
                val playerVerticalSpacing = if (veryCompact) 4.dp else if (compactHeight) 12.dp else 24.dp
                val topSpacerHeight = if (veryCompact) 4.dp else if (compactHeight) 16.dp else 48.dp
                val artworkSize = if (veryCompact) 120.dp else if (compactHeight) 200.dp else 280.dp
                val controlSpacing = if (veryCompact) 4.dp else if (compactHeight) 8.dp else 16.dp
                val secondaryTopPadding = if (veryCompact) 2.dp else if (compactHeight) 8.dp else 16.dp
                val sideControlSize = if (veryCompact) 44.dp else if (compactHeight) 56.dp else 72.dp
                // The secondary row now holds seven buttons (radio was added), so it is drawn a
                // little smaller than the transport row to keep the row inside the left column.
                val secondaryControlSize = sideControlSize * 0.8f
                val playControlSize = if (veryCompact) 52.dp else if (compactHeight) 68.dp else 80.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(contentSpacing),
                verticalAlignment = Alignment.Top,
            ) {
                // LEFT SIDE: Player controls (40% width, or 55% on very compact)
                Column(
                    modifier = Modifier
                        .weight(if (veryCompact) 0.55f else 0.4f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(playerVerticalSpacing),
                ) {
                    Spacer(modifier = Modifier.height(topSpacerHeight)) // Space for top bar

                    // Album art / Lyrics / Video container
                    if (lyricsExpanded && showLyrics) {
                        // Expanded lyrics mode — lyrics fill the entire left column
                            val positionProvider = { effectivePlayerConnection?.player?.currentPosition ?: currentPosition }
                        val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                lyrics == null -> {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                                }
                                lyrics == com.auramusic.app.db.entities.LyricsEntity.LYRICS_NOT_FOUND -> {
                                    Text("Lyrics not found", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                                }
                                else -> {
                                    androidx.compose.material3.ProvideTextStyle(
                                        value = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp, textAlign = TextAlign.Center, color = Color(0xFFFFFBFE).copy(alpha = 0.98f))
                                    ) {
                                        com.auramusic.app.ui.component.Lyrics(
                                            sliderPositionProvider = positionProvider,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                            showLyrics = true,
                                            disableInteractiveFeatures = true
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                    // Video fills the available left panel; artwork keeps its thumbnail size.
                    Box(
                        modifier = (if (showVideo) {
                            Modifier.fillMaxWidth().weight(1f)
                        } else {
                            Modifier.size(artworkSize)
                        })
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isVideoBuffering = showVideo &&
                            playbackState == androidx.media3.common.Player.STATE_BUFFERING

                        if (showVideo) {
                            TvVideoView(
                                activePlayer = activePlayer,
                                isVideoSwitching = isVideoSwitching,
                                isBuffering = isVideoBuffering,
                                videoId = videoUiState.session?.videoId,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (showLyrics) {
                            // Show lyrics behind thumbnail when enabled
                            val positionProvider = { effectivePlayerConnection?.player?.currentPosition ?: currentPosition }

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
                    } // end else (normal mode)

                    // Song info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
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
                        if (mediaMetadata?.explicit == true) {
                            Icon(
                                painter = painterResource(com.auramusic.app.R.drawable.explicit),
                                contentDescription = "Explicit",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }

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
                            onClick = { skipPrevious() },
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
                            onClick = { skipNext() },
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
                            onClick = {
                                if (guestVideoActive) {
                                    // The player is holding one video, so shuffling it
                                    // moves nothing at all. The queue that video belongs
                                    // to is the thing with a list to reorder, so that is
                                    // what the button is for - and it means the same
                                    // thing here as it does for music: shuffled, or back
                                    // the way it was rather than shuffled again.
                                    if (videoQueueShuffled) {
                                        com.auramusic.app.video.VideoPlaybackManager.unreshuffleQueue()
                                    } else {
                                        com.auramusic.app.video.VideoPlaybackManager.reshuffleQueue()
                                    }
                                } else {
                                    pc?.toggleShuffle()
                                }
                            },
                            icon = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (guestVideoActive) {
                                if (videoQueueShuffled) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.7f)
                            } else if (pc?.shuffleModeEnabled?.value == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.7f)
                            },
                            size = secondaryControlSize,
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
                            size = secondaryControlSize,
                        )

                        // Start radio: replaces the queue with an endless mix seeded from what is
                        // playing (same action as "Start radio" in the phone player menu). A video
                        // played from a shelf is not an item of the music queue, so for it the
                        // radio is made of videos and queued behind it: the video keeps playing
                        // instead of being reloaded as a song.
                        TvPlayerControlButton(
                            onClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.starting_radio),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                                val videoId = currentVideoId
                                if (guestVideoActive && videoId != null) {
                                    com.auramusic.app.video.VideoPlaybackManager.startRadio(videoId)
                                } else {
                                    pc?.startRadioSeamlessly()
                                }
                            },
                            painter = painterResource(R.drawable.radio),
                            contentDescription = context.getString(R.string.start_radio),
                            tint = Color.White.copy(alpha = 0.7f),
                            size = secondaryControlSize,
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
                            size = secondaryControlSize,
                        )

                        TvPlayerControlButton(
                            onClick = {
                                onShowLyricsChange(!showLyrics)
                            },
                            icon = Icons.Filled.Lyrics,
                            contentDescription = if (showLyrics) "Show album artwork" else "Show expanded lyrics",
                            tint = if (showLyrics) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                            size = secondaryControlSize,
                        )

                        // The same resolution choice as fullscreen, in the split view too: a soft
                        // video is a problem whether or not it is filling the screen,
                        // and the control belongs beside the picture it changes.
                        TvPlayerControlButton(
                            onClick = { videoQualityPanelOpen = true },
                            icon = Icons.Filled.HighQuality,
                            contentDescription = "Video quality",
                            tint = MaterialTheme.colorScheme.primary,
                            size = secondaryControlSize,
                        )

                        val isLiked = currentSong?.song?.liked == true
                        TvPlayerControlButton(
                            onClick = { pc?.toggleLike() },
                            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = if (isLiked) "Remove from liked songs" else "Add to liked songs",
                            tint = if (isLiked) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.7f),
                            size = secondaryControlSize,
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
                        state = splitQueueListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (guestVideoActive) {
                            // While a video plays, the playlist holds that video and
                            // nothing else, so the player's own windows would show a
                            // list of one. What follows it is real and lives with the
                            // videos, and that is what the remote should be walking.
                            items(
                                count = videoQueue.size,
                                key = { index -> "video_${videoQueue[index].videoId}" },
                            ) { index ->
                                val upcoming = videoQueue[index]
                                TvVideoQueueItem(
                                    item = upcoming,
                                    isCurrent = upcoming.videoId == currentVideoId,
                                    onClick = {
                                        com.auramusic.app.video.VideoPlaybackManager.playFromQueue(upcoming)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
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
                        }

                        if (queueWindows.isEmpty() && !guestVideoActive) {
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

            // The way out to fullscreen is a fixed button rather than one more row of
            // controls, because that row is exactly what a full-height video or a
            // column of lyrics pushes off the bottom of a short screen: it was
            // reachable with artwork, and unreachable exactly when it was wanted.
            // Pinned to the corner under the app bar, and on the player side of the
            // split rather than over the queue, so it works the same for video, for
            // lyrics and for artwork without covering what is queued.
            TvPlayerControlButton(
                onClick = { onFullScreenChange(true) },
                painter = painterResource(R.drawable.fullscreen),
                contentDescription = "Enter fullscreen",
                size = if (compactHeight) 56.dp else 68.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = if (compactHeight) 84.dp else 104.dp, start = 24.dp),
            )

            // Back button removed from here — now in the top bar
            }
            }
            }

            // A resolution is worth naming when it has just changed: the picture is easier to
            // judge against what was asked for than against what it was.
            videoQualityMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(6f)
                        .padding(top = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }

            if (isFullScreen) {
                // Full-bleed media with auto-hiding controls overlaid. The split
                // layout is not composed in this state, so this is the only
                // PlayerView instance and it owns the video surface. The parent
                // top bar is hidden too (see TvApp), making this truly edge-to-edge.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                        .background(if (isFullScreen) Color.Black else Color.Transparent)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            wakeTick++
                            // Left/right walk the transport row by hand. The row is the
                            // only thing on the screen that holds focus, so leaving it to
                            // the focus search gives the remote nowhere to go once it
                            // reaches either end.
                            when (event.key) {
                                Key.DirectionLeft, Key.DirectionRight -> {
                                    val goingLeft = event.key == Key.DirectionLeft
                                    when {
                                        // The quality list is the only genuinely modal
                                        // panel: it is a short list of choices and
                                        // nothing behind it is worth reaching for.
                                        videoQualityPanelOpen -> true
                                        subtitlePanelOpen -> true

                                        // The queue is not: it is open over the media,
                                        // and the transport controls are still there
                                        // underneath. Left walks back out of the list
                                        // onto the row, and right walks into the list
                                        // from it, so playback is never more than one
                                        // press of the remote away.
                                        fullscreenQueueOpen -> {
                                            if (focusInQueue == goingLeft) {
                                                if (goingLeft) {
                                                    focusTransportSlot(focusedTransport)
                                                } else {
                                                    focusQueue()
                                                }
                                            } else {
                                                moveTransportFocus(if (goingLeft) -1 else 1)
                                            }
                                            true
                                        }

                                        else -> {
                                            moveTransportFocus(if (goingLeft) -1 else 1)
                                            true
                                        }
                                    }
                                }
                                else -> false
                            }
                        },
                ) {
                    // The same canvas as the split view, carried into fullscreen so
                    // expanding the player does not throw the song's picture away.
                    // A video covers it completely, so it is not run at all then: a
                    // canvas is a playing video in its own right, and decoding one
                    // behind another is two decoders and a frame dropped for a
                    // picture nobody can see. The split view stands its own down for
                    // the same reason.
                    if (!showVideo) {
                        TvCanvasBackdrop(
                            backdropTitle = mediaMetadata?.title.orEmpty(),
                            backdropArtist = mediaMetadata?.artists?.joinToString(", ") { it.name },
                            imageUrl = mediaMetadata?.thumbnailUrl,
                            scrimColor = Color.Black,
                        )
                    }

                    when {
                        showVideo -> TvVideoView(
                            activePlayer = activePlayer,
                            videoId = videoUiState.session?.videoId,
                            isVideoSwitching = isVideoSwitching,
                            isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING,
                            modifier = Modifier.fillMaxSize(),
                        )

                        showLyrics -> {
                            val positionProvider = { effectivePlayerConnection?.player?.currentPosition ?: currentPosition }
                            val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Words are read against a picture that is moving and
                                // can be any colour at any moment, so the area they sit
                                // in is darkened on its own account rather than trusting
                                // the backdrop's scrim to be enough. Darkest in the middle
                                // where the text is, lighter at the edges so the canvas is
                                // still visible behind it.
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.82f),
                                                    Color.Black.copy(alpha = 0.5f),
                                                ),
                                                radius = 1400f,
                                            ),
                                        ),
                                )
                                when {
                                    lyrics == null -> {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }

                                    lyrics == com.auramusic.app.db.entities.LyricsEntity.LYRICS_NOT_FOUND -> {
                                        Text(
                                            text = "Lyrics not found",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center,
                                        )
                                    }

                                    else -> {
                                        androidx.compose.material3.ProvideTextStyle(
                                            value = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 26.sp,
                                                textAlign = TextAlign.Center,
                                                color = Color(0xFFFFFBFE).copy(alpha = 0.98f),
                                            ),
                                        ) {
                                            com.auramusic.app.ui.component.Lyrics(
                                                sliderPositionProvider = positionProvider,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 64.dp, vertical = 96.dp),
                                                showLyrics = true,
                                                disableInteractiveFeatures = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // No artwork drawn here on purpose. The backdrop already shows
                        // the cover and fades the canvas in over it, so drawing the
                        // thumbnail again at this size would simply cover the canvas
                        // back up - which is exactly what it did: the canvas appeared
                        // only once the lyrics replaced this branch.
                    }

                    // Scrim keeps the overlaid controls readable over bright video.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                ),
                            ),
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            // Above the queue panel: the panel is folded out over the
                            // media, not over the controls, so they stay visible and
                            // pressable while it is open.
                            .zIndex(6f)
                            .graphicsLayer { alpha = controlsAlpha }
                            .padding(horizontal = 48.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = mediaMetadata?.title ?: "No song playing",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = 1,
                                    initialDelayMillis = 3000,
                                    velocity = 30.dp,
                                ),
                        )
                        Text(
                            text = mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = 1,
                                    initialDelayMillis = 3000,
                                    velocity = 30.dp,
                                ),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TvPlayerControlButton(
                                onClick = { skipPrevious() },
                                icon = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous song",
                                focusRequester = transportFocus[FULLSCREEN_PREVIOUS_SLOT],
                                onFocused = {
                                    focusedTransport = FULLSCREEN_PREVIOUS_SLOT
                                    wakeTransport()
                                },
                            )
                            TvPlayerControlButton(
                                onClick = {
                                    val newPos = maxOf(0L, currentPosition - 10000L)
                                    pc?.player?.seekTo(newPos)
                                },
                                icon = Icons.Filled.FastRewind,
                                contentDescription = "Rewind 10 seconds",
                                focusRequester = transportFocus[FULLSCREEN_REWIND_SLOT],
                                onFocused = {
                                    focusedTransport = FULLSCREEN_REWIND_SLOT
                                    wakeTransport()
                                },
                            )
                            TvPlayerControlButton(
                                onClick = { pc?.togglePlayPause() },
                                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                size = 88.dp,
                                focusRequester = transportFocus[FULLSCREEN_PLAY_SLOT],
                                onFocused = {
                                    focusedTransport = FULLSCREEN_PLAY_SLOT
                                    wakeTransport()
                                },
                            )
                            TvPlayerControlButton(
                                onClick = {
                                    val durationVal = pc?.player?.duration?.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
                                    val newPos = minOf(durationVal, currentPosition + 10000L)
                                    pc?.player?.seekTo(newPos)
                                },
                                icon = Icons.Filled.FastForward,
                                contentDescription = "Fast forward 10 seconds",
                                focusRequester = transportFocus[FULLSCREEN_FORWARD_SLOT],
                                onFocused = {
                                    focusedTransport = FULLSCREEN_FORWARD_SLOT
                                    wakeTransport()
                                },
                            )
                            TvPlayerControlButton(
                                onClick = { skipNext() },
                                icon = Icons.Filled.SkipNext,
                                contentDescription = "Next song",
                                focusRequester = transportFocus[FULLSCREEN_NEXT_SLOT],
                                onFocused = {
                                    focusedTransport = FULLSCREEN_NEXT_SLOT
                                    wakeTransport()
                                },
                            )
                            if (!showVideo) {
                                TvPlayerControlButton(
                                    onClick = { onShowLyricsChange(!showLyrics) },
                                    icon = Icons.Filled.Lyrics,
                                    contentDescription = if (showLyrics) "Show album artwork" else "Show lyrics",
                                    tint = if (showLyrics) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                    focusRequester = transportFocus[FULLSCREEN_LYRICS_SLOT],
                                    onFocused = {
                                        focusedTransport = FULLSCREEN_LYRICS_SLOT
                                        wakeControls()
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            // Kebab: opens and closes the queue over the media. Everything
                            // the queue offers stays reachable behind it - closing it puts
                            // the remote back on this button rather than at the far end of
                            // the row.
                            TvPlayerControlButton(
                                onClick = {
                                    fullscreenQueueOpen = !fullscreenQueueOpen
                                    if (!fullscreenQueueOpen) {
                                        focusTransportSlot(FULLSCREEN_QUEUE_SLOT)
                                    }
                                },
                                icon = if (fullscreenQueueOpen) Icons.Filled.MenuOpen else Icons.Filled.MoreVert,
                                contentDescription = if (fullscreenQueueOpen) "Close queue" else "Open queue",
                                tint = if (fullscreenQueueOpen) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White
                                },
                                focusRequester = transportFocus[FULLSCREEN_QUEUE_SLOT],
                                onFocused = {
                                    focusedTransport = FULLSCREEN_QUEUE_SLOT
                                    wakeTransport()
                                },
                            )
                            // Resolution, for the same reason it exists on the phone: the
                            // picture is only as sharp as the stream it was fetched at,
                            // and from a sofa the default is not sharp enough.
                            TvPlayerControlButton(
                                onClick = {
                                    videoQualityPanelOpen = !videoQualityPanelOpen
                                    if (!videoQualityPanelOpen) {
                                        focusTransportSlot(FULLSCREEN_QUALITY_SLOT)
                                    }
                                },
                                icon = Icons.Filled.HighQuality,
                                contentDescription = "Video quality",
                                tint = if (videoQualityPanelOpen) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White
                                },
                                focusRequester = transportFocus[FULLSCREEN_QUALITY_SLOT],
                                onFocused = {
                                    focusedTransport = FULLSCREEN_QUALITY_SLOT
                                    wakeTransport()
                                },
                            )
                            // Subtitles, meaning the same thing they mean on the
                            // phone: captions drawn over the picture, switched by one
                            // app-wide setting.
                            run {
                                TvPlayerControlButton(
                                    onClick = {
                                        subtitlePanelOpen = !subtitlePanelOpen
                                        if (!subtitlePanelOpen) {
                                            focusTransportSlot(FULLSCREEN_SUBTITLES_SLOT)
                                        }
                                    },
                                    painter = painterResource(R.drawable.ic_subtitles),
                                    contentDescription = "Subtitles",
                                    tint = if (captionsOn) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.7f)
                                    },
                                    focusRequester = transportFocus[FULLSCREEN_SUBTITLES_SLOT],
                                    onFocused = {
                                        focusedTransport = FULLSCREEN_SUBTITLES_SLOT
                                        wakeTransport()
                                    },
                                )
                            }
                            TvPlayerControlButton(
                                onClick = { onFullScreenChange(false) },
                                painter = painterResource(R.drawable.fullscreen_exit),
                                contentDescription = "Exit fullscreen",
                                focusRequester = transportFocus[FULLSCREEN_EXIT_SLOT],
                                onFocused = {
                                    focusedTransport = FULLSCREEN_EXIT_SLOT
                                    wakeTransport()
                                },
                            )
                        }
                    }


                    if (fullscreenQueueOpen) {
                        // The queue, folded out over the media. It takes the whole right
                        // edge so a title is never clipped by the panel, and every row is
                        // focusable and selectable from the remote.
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(460.dp)
                                .zIndex(4f),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                            tonalElevation = 6.dp,
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                                ) {
                                    Text(
                                        text = "Queue",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = makeTimeString(duration),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    TvPlayerControlButton(
                                        onClick = {
                                            fullscreenQueueOpen = false
                                            focusTransportSlot(FULLSCREEN_QUEUE_SLOT)
                                        },
                                        icon = Icons.Filled.Close,
                                        contentDescription = "Close queue",
                                        size = 48.dp,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        focusRequester = queueCloseFocus,
                                        onFocused = {
                                            focusInQueue = true
                                            wakeControls()
                                        },
                                    )
                                }

                                LazyColumn(
                                    state = fullQueueListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (guestVideoActive) {
                                        items(
                                            count = videoQueue.size,
                                            key = { index -> "video_${videoQueue[index].videoId}" },
                                        ) { index ->
                                            val upcoming = videoQueue[index]
                                            TvVideoQueueItem(
                                                item = upcoming,
                                                isCurrent = upcoming.videoId == currentVideoId,
                                                onClick = {
                                                    fullscreenQueueOpen = false
                                                    com.auramusic.app.video.VideoPlaybackManager.playFromQueue(upcoming)
                                                    focusTransportSlot(FULLSCREEN_PLAY_SLOT)
                                                },
                                                focusRequester = queueFocuserFor(upcoming.videoId),
                                                onFocused = {
                                                    focusInQueue = true
                                                    wakeControls()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp),
                                            )
                                        }
                                    } else {
                                        items(
                                            count = queueWindows.size,
                                            key = { index ->
                                                "${queueWindows.getOrNull(index)?.mediaItem?.mediaId ?: "unknown"}_$index"
                                            },
                                        ) { index ->
                                            val window = queueWindows.getOrNull(index)
                                            val isCurrentSong = index == currentWindowIndex
                                            if (window != null) {
                                                val mediaId = window.mediaItem.mediaId
                                                TvQueueItem(
                                                    window = window,
                                                    isCurrentSong = isCurrentSong,
                                                    onClick = {
                                                        fullscreenQueueOpen = false
                                                        pc?.player?.seekTo(index, C.TIME_UNSET)
                                                        focusTransportSlot(FULLSCREEN_PLAY_SLOT)
                                                    },
                                                    focusRequester = queueFocuserFor(mediaId),
                                                    onFocused = {
                                                        focusInQueue = true
                                                        wakeControls()
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp),
                                                )
                                            }
                                        }
                                    }

                                    if (queueWindows.isEmpty() && !guestVideoActive) {
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
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (subtitlePanelOpen) {
                // The same list the phone offers, from the same service, so the
                // languages are the same and choosing one behaves identically.
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(420.dp)
                        .zIndex(7f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 6.dp,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                        ) {
                            Text(
                                text = "Subtitles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            TvPlayerControlButton(
                                onClick = {
                                    subtitlePanelOpen = false
                                    focusTransportSlot(FULLSCREEN_SUBTITLES_SLOT)
                                },
                                icon = Icons.Filled.Close,
                                contentDescription = "Close subtitles",
                                size = 48.dp,
                                tint = MaterialTheme.colorScheme.onSurface,
                                focusRequester = subtitleCloseFocus,
                                onFocused = { wakeControls() },
                            )
                        }

                        // Two answers, because there are two: captions on, or off.
                        // Not a list of tracks to pick from - the caption overlay
                        // draws the best track it has for the language already set, so
                        // there is nothing here to choose between.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TvSubtitleOption(
                                label = "Captions on",
                                detail = null,
                                isSelected = captionsOn,
                                onClick = {
                                    subtitlePanelOpen = false
                                    setCaptionsOn(true)
                                },
                                focusRequester = subtitleOnFocus,
                                onFocused = { wakeControls() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TvSubtitleOption(
                                label = "Captions off",
                                detail = null,
                                isSelected = !captionsOn,
                                onClick = {
                                    subtitlePanelOpen = false
                                    setCaptionsOn(false)
                                },
                                focusRequester = subtitleOffFocus,
                                onFocused = { wakeControls() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            if (videoQualityPanelOpen) {
                // Resolutions, over the media rather than in settings: the point
                // of choosing is to see whether it helped, and that cannot be
                // judged from a screen two rooms away in the settings menu.
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(420.dp)
                        .zIndex(5f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 6.dp,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Video quality",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Restart the video to change",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TvPlayerControlButton(
                                onClick = {
                                    videoQualityPanelOpen = false
                                    focusTransportSlot(FULLSCREEN_QUALITY_SLOT)
                                },
                                icon = Icons.Filled.Close,
                                contentDescription = "Close quality",
                                size = 48.dp,
                                tint = MaterialTheme.colorScheme.onSurface,
                                focusRequester = qualityCloseFocus,
                                onFocused = { wakeControls() },
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                count = QUALITY_OPTIONS.size,
                                key = { QUALITY_OPTIONS[it].name },
                            ) { index ->
                                val option = QUALITY_OPTIONS[index]
                                TvQualityOption(
                                    quality = option,
                                    isSelected = option == videoQuality,
                                    onClick = { applyVideoQuality(option) },
                                    focusRequester = qualityOptionFocusers.getOrPut(option.name) {
                                        FocusRequester()
                                    },
                                    onFocused = { wakeControls() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
