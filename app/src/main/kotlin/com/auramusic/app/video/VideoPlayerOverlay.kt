/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.auramusic.app.LocalVideoMiniPlayerBottomPadding
import com.auramusic.app.R
import com.auramusic.app.video.VideoPlaybackManager.CommentItem
import com.auramusic.app.video.VideoPlaybackManager.RecommendationItem
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerOverlay() {
    val state by VideoPlaybackManager.uiState.collectAsState()
    val session = state.session ?: return

    val context = LocalContext.current
    val player = VideoPlaybackManager.playerOrNull() ?: return
    val activity = context as? android.app.Activity

    DisposableEffect(activity, state.minimized) {
        if (activity != null && !state.minimized) {
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = !state.minimized) {
        VideoPlaybackManager.collapse()
    }

    AnimatedContent(
        targetState = state.minimized,
        transitionSpec = {
            if (targetState) {
                (fadeIn(tween(200)) + scaleIn(tween(260))).togetherWith(fadeOut(tween(140)) + scaleOut(tween(200)))
            } else {
                (fadeIn(tween(220)) + scaleIn(tween(300))).togetherWith(fadeOut(tween(160)))
            }
        },
        label = "videoPlayerState"
    ) { minimized ->
        if (minimized) {
            VideoMinimizedTile(
                player = player,
                onExpand = { VideoPlaybackManager.expand() },
                onClose = { VideoPlaybackManager.close() },
            )
        } else {
            VideoExpandedPlayer(
                player = player,
                session = session,
                uiState = state,
                onCollapse = { VideoPlaybackManager.collapse() },
                onClose = { VideoPlaybackManager.close() },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Mini player
// ---------------------------------------------------------------------------

@OptIn(UnstableApi::class)
@Composable
private fun VideoMinimizedTile(
    player: ExoPlayer,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    val state by VideoPlaybackManager.uiState.collectAsState()
    val density = LocalDensity.current

    var dragPx by remember { mutableFloatStateOf(0f) }
    val animatedDragPx by animateFloatAsState(
        targetValue = dragPx,
        animationSpec = tween(durationMillis = 220),
        label = "miniDragPx",
    )
    val dismissProgress = (dragPx / 360f).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = with(density) { animatedDragPx.toDp() })
                .graphicsLayer {
                    scaleX = 1f - 0.06f * dismissProgress
                    scaleY = 1f - 0.06f * dismissProgress
                    alpha = 1f - 0.45f * dismissProgress
                }
                .padding(horizontal = 10.dp)
                .padding(bottom = LocalVideoMiniPlayerBottomPadding.current)
                .fillMaxWidth()
                .height(128.dp)
                .shadow(24.dp, RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(184.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragPx > with(density) { 130.dp.toPx() }) {
                                        onClose()
                                    } else {
                                        dragPx = 0f
                                    }
                                },
                                onDragCancel = { dragPx = 0f },
                            ) { change, dragAmount ->
                                if (dragAmount > 0f) {
                                    dragPx = (dragPx + dragAmount).coerceIn(0f, 460f)
                                    change.consume()
                                }
                            }
                        }
                ) {
                    AndroidVideoSurface(player, resizeModeOverride = state.resizeMode)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp),
                ) {
                    Row {
                        Text(
                            text = state.session?.title.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (!state.session?.channelName.isNullOrBlank()) {
                        Text(
                            text = state.session?.channelName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Progress line + times.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5f))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    ) {
                        Text(
                            text = "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            onClick = { VideoPlaybackManager.togglePlayPause() },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                            tonalElevation = 3.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (state.isBuffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(if (state.isPlaying) R.drawable.pause else R.drawable.play),
                                        contentDescription = stringResource(
                                            if (state.isPlaying) R.string.pause else R.string.play
                                        ),
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Expanded player
// ---------------------------------------------------------------------------

@OptIn(UnstableApi::class)
@Composable
private fun VideoExpandedPlayer(
    player: ExoPlayer,
    session: VideoPlaybackManager.VideoSession,
    uiState: VideoPlaybackManager.UiState,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600
    val context = LocalContext.current

    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.isPlaying, showControls) {
        if (uiState.isPlaying && showControls) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLandscape && isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                VideoSurfaceWithControls(
                    player = player,
                    session = session,
                    uiState = uiState,
                    showControls = showControls,
                    onToggleControls = { showControls = !showControls },
                    onCollapse = onCollapse,
                    onClose = onClose,
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                    useFullHeight = true,
                )
                VideoDetailPane(
                    session = session,
                    uiState = uiState,
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                VideoSurfaceWithControls(
                    player = player,
                    session = session,
                    uiState = uiState,
                    showControls = showControls,
                    onToggleControls = { showControls = !showControls },
                    onCollapse = onCollapse,
                    onClose = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    useFullHeight = false,
                )
                VideoDetailPane(
                    session = session,
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }

        if (uiState.showSettings) {
            SettingsOverlay(
                uiState = uiState,
                onDismiss = { VideoPlaybackManager.toggleSettings() },
            )
        }
    }
}

/**
 * The 16:9 video area: surface + one coherent gesture layer (tap toggles controls,
 * double-tap sides seek ±10s, vertical drag collapses, horizontal drag scrubs).
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoSurfaceWithControls(
    player: ExoPlayer,
    session: VideoPlaybackManager.VideoSession,
    uiState: VideoPlaybackManager.UiState,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    useFullHeight: Boolean,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var collapseDragPx by remember { mutableFloatStateOf(0f) }
    var scrubPreviewMs by remember { mutableStateOf<Long?>(null) }
    var isForward by remember { mutableStateOf(true) }
    var seekHint by remember { mutableStateOf<String?>(null) }
    val seekForwardLabel = stringResource(R.string.seek_forward_dynamic)
    val seekBackwardLabel = stringResource(R.string.seek_backward_dynamic)

    LaunchedEffect(seekHint) {
        if (seekHint != null) {
            delay(700)
            seekHint = null
        }
    }

    val collapseFraction = if (useFullHeight) 0f else (collapseDragPx / 900f).coerceIn(0f, 0.75f)

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = collapseDragPx
                alpha = 1f - collapseFraction
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onToggleControls()
                        if (!showControls) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    },
                    onDoubleTap = { offset ->
                        val side = when {
                            offset.x < size.width / 3 -> -1
                            offset.x > size.width * 2 / 3 -> 1
                            else -> 0
                        }
                        if (side == 0) {
                            onToggleControls()
                        } else {
                            val target = (VideoPlaybackManager.uiState.value.positionMs + side * 10_000L)
                                .coerceIn(0L, VideoPlaybackManager.uiState.value.durationMs)
                            VideoPlaybackManager.seekTo(target)
                            isForward = side > 0
                            seekHint = if (side > 0) seekForwardLabel else seekBackwardLabel
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
            .pointerInput(useFullHeight) {
                if (useFullHeight) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (collapseDragPx > with(density) { 120.dp.toPx() }) onCollapse() else collapseDragPx = 0f
                    },
                    onDragCancel = { collapseDragPx = 0f },
                ) { change, dragAmount ->
                    if (dragAmount > 0f) {
                        collapseDragPx = (collapseDragPx + dragAmount).coerceIn(0f, 800f)
                        change.consume()
                    }
                }
            }
            .pointerInput(uiState.durationMs) {
                var dragBaseMs = 0L
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragBaseMs = VideoPlaybackManager.uiState.value.positionMs
                    },
                    onDragEnd = {
                        scrubPreviewMs?.let { VideoPlaybackManager.seekTo(it) }
                        scrubPreviewMs = null
                    },
                    onDragCancel = {
                        scrubPreviewMs = null
                    },
                ) { change, dragAmount ->
                    change.consume()
                    val deltaMs = dragAmount.toDp().value * 50f
                    isForward = dragAmount > 0
                    scrubPreviewMs = (dragBaseMs + deltaMs.toLong())
                        .coerceIn(0L, uiState.durationMs)
                }
            }
    ) {
        AndroidVideoSurface(player, resizeModeOverride = uiState.resizeMode)

        if (showControls) {
            PlayerTopBar(
                onCollapse = onCollapse,
                onClose = onClose,
                title = session.title,
                channelName = session.channelName,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            PlayerBottomControls(
                uiState = uiState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        seekHint?.let { hint ->
            SeekHintBadge(
                hint = hint,
                isForward = isForward,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        scrubPreviewMs?.let { preview ->
            SeekPreviewBadge(
                previewMs = preview,
                positionMs = uiState.positionMs,
                isForward = isForward,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    title: String,
    channelName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 14.dp)
    ) {
        Surface(
            onClick = onCollapse,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.expand_less),
                    contentDescription = stringResource(R.string.collapse_video),
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (channelName.isNotBlank()) {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Surface(
            onClick = { VideoPlaybackManager.toggleSettings() },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = stringResource(R.string.video_quality),
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            onClick = onClose,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun PlayerBottomControls(
    uiState: VideoPlaybackManager.UiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                )
            )
            .padding(top = 20.dp, bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = { VideoPlaybackManager.playPrevious() },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = stringResource(R.string.video_player_up_next),
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Surface(
                onClick = { VideoPlaybackManager.togglePlayPause() },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(62.dp),
                tonalElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(if (uiState.isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = stringResource(
                                if (uiState.isPlaying) R.string.pause else R.string.play
                            ),
                            modifier = Modifier.size(32.dp),
                            tint = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Surface(
                onClick = { VideoPlaybackManager.playNext() },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = stringResource(R.string.video_player_up_next),
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = formatTime(uiState.positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTime(uiState.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f)
            )
        }

        SlimSeekBar(
            positionMs = uiState.positionMs,
            durationMs = uiState.durationMs,
            onSeek = { VideoPlaybackManager.seekTo(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
    }
}

/**
 * YouTube-style slim progress bar: a thin track with a small thumb that can be
 * tapped or dragged to seek.
 */
@Composable
private fun SlimSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationSeconds = durationMs.toFloat().coerceAtLeast(1f)
    var scrub by remember { mutableFloatStateOf(-1f) }
    val effective = if (scrub >= 0f) scrub else positionMs.toFloat()
    val progress = (effective / durationSeconds).coerceIn(0f, 1f)

    val trackHeight = 3.dp
    val thumbSize = 13.dp

    BoxWithConstraints(
        modifier = modifier
            .height(20.dp)
            .pointerInput(durationSeconds) {
                detectDragGestures(
                    onDragStart = { offset ->
                        scrub = (offset.x / size.width * durationSeconds).coerceIn(0f, durationSeconds)
                    },
                    onDrag = { change, _ ->
                        scrub = (change.position.x / size.width * durationSeconds).coerceIn(0f, durationSeconds)
                        change.consume()
                    },
                    onDragEnd = {
                        scrub.takeIf { it >= 0f }?.let {
                            onSeek(it.toLong())
                            scrub = -1f
                        }
                    },
                    onDragCancel = { scrub = -1f },
                )
            }
            .pointerInput(durationSeconds) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width * durationSeconds).coerceIn(0f, durationSeconds).toLong())
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(Color.White.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .align(Alignment.CenterStart)
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth * progress) - (thumbSize / 2))
                .align(Alignment.Center)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .shadow(3.dp, CircleShape)
        )
    }
}

@Composable
private fun SeekHintBadge(
    hint: String,
    isForward: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "seekHint")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
        label = "seekScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(8.dp)
    ) {
        Icon(
            painter = painterResource(if (isForward) R.drawable.fast_forward else R.drawable.replay),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = Color.White
        )
        Text(
            text = hint,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SeekPreviewBadge(
    previewMs: Long,
    positionMs: Long,
    isForward: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = formatTime(previewMs),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        val deltaSec = (previewMs - positionMs) / 1000L
        if (deltaSec != 0L) {
            Text(
                text = (if (deltaSec > 0) "+" else "") + "${deltaSec}s",
                color = if (isForward) Color(0xFF8BC34A) else Color(0xFFFF8A65),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Detail pane: title, channel row, actions, tabs (comments / up next)
// ---------------------------------------------------------------------------

@Composable
private fun VideoDetailPane(
    session: VideoPlaybackManager.VideoSession,
    uiState: VideoPlaybackManager.UiState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState, uiState.comments.size, uiState.recommendations.size) {
        snapshotFlowSafe(listState) { nearEnd ->
            if (!nearEnd) return@snapshotFlowSafe
            when {
                uiState.isLoadingMoreComments || uiState.isLoadingMoreRecommendations -> Unit
                else -> {
                    VideoPlaybackManager.loadMoreComments()
                    VideoPlaybackManager.loadMoreRecommendations()
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "info") {
            VideoInfoSection(
                session = session,
                uiState = uiState,
            )
        }
        item(key = "tabs") {
            VideoTabs(uiState = uiState)
        }
    }
}

/** Small indirection so the effect body stays readable. */
private suspend fun snapshotFlowSafe(
    listState: androidx.compose.foundation.lazy.LazyListState,
    onNearEnd: suspend (Boolean) -> Unit,
) {
    androidx.compose.runtime.snapshotFlow {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= listState.layoutInfo.totalItemsCount - 3
    }.collect { nearEnd -> onNearEnd(nearEnd) }
}

@Composable
private fun VideoInfoSection(
    session: VideoPlaybackManager.VideoSession,
    uiState: VideoPlaybackManager.UiState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = session.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        val metaLine = listOfNotNull(
            session.viewCountText,
            session.publishedTimeText,
        ).joinToString(" • ")
        if (metaLine.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ChannelRow(
            session = session,
            isSubscribed = uiState.isSubscribed,
            onSubscribeClick = { VideoPlaybackManager.toggleSubscribe() },
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionButtonsRow(
            isLiked = uiState.isLiked,
            isDisliked = uiState.isDisliked,
            isSaved = uiState.isSaved,
            likeCountText = session.likeCountText,
        )

        Spacer(modifier = Modifier.height(12.dp))

        session.description?.let { desc ->
            if (desc.isNotBlank()) {
                ExpandableDescription(
                    description = desc,
                    expanded = uiState.expandedDescription,
                    onToggle = { VideoPlaybackManager.toggleExpandedDescription() },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ChannelRow(
    session: VideoPlaybackManager.VideoSession,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            if (!session.channelThumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = session.channelThumbnail,
                    contentDescription = session.channelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = session.channelName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.channelName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            session.subscriberCountText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Surface(
            onClick = onSubscribeClick,
            shape = RoundedCornerShape(22.dp),
            color = if (isSubscribed) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(38.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSubscribed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSubscribed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    isLiked: Boolean,
    isDisliked: Boolean,
    isSaved: Boolean,
    likeCountText: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionButton(
            icon = R.drawable.ic_thumb_up,
            label = buildString {
                append(stringResource(R.string.video_player_like))
                if (!likeCountText.isNullOrBlank()) append(" • ").append(likeCountText)
            },
            selected = isLiked,
            onClick = { VideoPlaybackManager.toggleLike() },
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            icon = R.drawable.ic_thumb_down,
            label = stringResource(R.string.video_player_dislike),
            selected = isDisliked,
            onClick = { VideoPlaybackManager.toggleDislike() },
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            icon = if (isSaved) R.drawable.library_add_check else R.drawable.library_add,
            label = stringResource(if (isSaved) R.string.video_player_saved else R.string.video_player_save),
            selected = isSaved,
            onClick = { VideoPlaybackManager.toggleSave() },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(22.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ExpandableDescription(
    description: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (description.length > 100 || expanded) {
            TextButton(onClick = onToggle, contentPadding = PaddingValues(0.dp)) {
                Text(
                    text = stringResource(if (expanded) R.string.video_player_show_less else R.string.video_player_show_more),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    painter = painterResource(if (expanded) R.drawable.expand_less else R.drawable.expand_more),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private enum class DetailTab(val labelRes: Int) {
    UpNext(R.string.video_player_up_next),
    Comments(R.string.video_player_comments),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoTabs(uiState: VideoPlaybackManager.UiState) {
    var selectedTab by remember { mutableIntStateOf(DetailTab.UpNext.ordinal) }

    Column {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            DetailTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab.ordinal,
                    onClick = { selectedTab = tab.ordinal },
                    text = {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == tab.ordinal) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab.ordinal) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            DetailTab.Comments.ordinal -> CommentsSection(uiState = uiState)
            else -> UpNextSection(uiState = uiState)
        }
    }
}

@Composable
private fun CommentsSection(uiState: VideoPlaybackManager.UiState) {
    when {
        uiState.isLoadingComments -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
        uiState.commentsError != null && uiState.comments.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.commentsError.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                uiState.comments.forEach { comment ->
                    CommentRow(comment = comment)
                }
                if (uiState.isLoadingMoreComments) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!comment.authorThumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = comment.authorThumbnail,
                    contentDescription = comment.authorName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = comment.authorName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                comment.likeCount?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_thumb_up),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            Text(
                text = buildString {
                    if (comment.isPinned) append("📌 ")
                    append(comment.content)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                comment.publishedTime?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                comment.replyCount?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpNextSection(uiState: VideoPlaybackManager.UiState) {
    val context = LocalContext.current

    when {
        uiState.isLoadingRecommendations -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                }
            }
        }
        uiState.recommendations.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.video_player_no_recommendations),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                uiState.recommendations.forEach { item ->
                    UpNextRow(
                        item = item,
                        isPlaying = item.videoId == uiState.session?.videoId,
                        onClick = {
                            VideoPlaybackManager.playWithDetails(
                                context = context,
                                videoId = item.videoId,
                                title = item.title,
                                channelName = item.channelName,
                                channelId = item.channelId,
                                viewCountText = item.viewCountText,
                                publishedTimeText = item.publishedTimeText,
                            )
                        }
                    )
                }
                if (uiState.isLoadingMoreRecommendations) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpNextRow(
    item: RecommendationItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(148.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            val thumbnailUrl = item.thumbnail
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (item.durationText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.durationText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (isPlaying) {
                val pulse by rememberInfiniteTransition(label = "upNow").animateFloat(
                    initialValue = 0.45f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                    label = "upPulse"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f * pulse))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pause),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.channelName.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            listOfNotNull(
                item.viewCountText,
                item.publishedTimeText,
            ).takeIf { it.isNotEmpty() }?.joinToString(" • ")?.let { meta ->
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Settings overlay
// ---------------------------------------------------------------------------

@Composable
private fun SettingsOverlay(
    uiState: VideoPlaybackManager.UiState,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.video_quality),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Playback speed: ${uiState.playbackSpeed}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    listOf(
                        0.5f to "0.5x",
                        0.75f to "0.75x",
                        1.0f to "1x",
                        1.25f to "1.25x",
                        1.5f to "1.5x",
                        2.0f to "2x",
                    ).forEach { (speed, label) ->
                        Surface(
                            onClick = { VideoPlaybackManager.setPlaybackSpeed(speed) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.playbackSpeed == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.playbackSpeed == speed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aspect ratio",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    listOf(
                        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
                        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch",
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop",
                    ).forEach { (mode, label) ->
                        Surface(
                            onClick = { VideoPlaybackManager.setResizeMode(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.resizeMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.resizeMode == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Surface + helpers
// ---------------------------------------------------------------------------

@OptIn(UnstableApi::class)
@Composable
private fun AndroidVideoSurface(player: ExoPlayer, resizeModeOverride: Int) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            view.player = player
            if (view.resizeMode != resizeModeOverride) {
                view.resizeMode = resizeModeOverride
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}