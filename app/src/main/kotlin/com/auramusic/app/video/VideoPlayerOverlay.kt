/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.auramusic.app.LocalPlayerAwareWindowInsets
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
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val density = LocalDensity.current

    // Drag distance tracked in px, rendered via density conversion.
    var dragPx by remember { mutableFloatStateOf(0f) }
    val animatedDragPx by animateFloatAsState(
        targetValue = dragPx,
        animationSpec = tween(durationMillis = 220),
        label = "miniDragPx",
    )
    val dismissProgress = (dragPx / 380f).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = with(density) { animatedDragPx.toDp() })
                .graphicsLayer {
                    scaleX = 1f - 0.05f * dismissProgress
                    scaleY = 1f - 0.05f * dismissProgress
                    alpha = 1f - 0.4f * dismissProgress
                }
                .widthIn(max = 360.dp)
                .fillMaxWidth(0.62f)
                .padding(start = 14.dp)
                .padding(bottom = insets.calculateBottomPadding() + 84.dp)
                .shadow(18.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(16f / 9f)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragPx > with(density) { 140.dp.toPx() }) {
                                    onClose()
                                } else {
                                    dragPx = 0f
                                }
                            },
                            onDragCancel = { dragPx = 0f },
                        ) { change, dragAmount ->
                            if (dragAmount > 0f) {
                                dragPx = (dragPx + dragAmount).coerceIn(0f, 480f)
                                change.consume()
                            }
                        }
                    }
            ) {
                AndroidVideoSurface(player, resizeModeOverride = state.resizeMode)

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                        .size(30.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                        .padding(horizontal = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiniControlButton(
                            iconRes = R.drawable.ic_skip_previous,
                            contentDescriptionRes = R.string.video_player_up_next,
                            onClick = { VideoPlaybackManager.playPrevious() },
                        )
                        Surface(
                            onClick = { VideoPlaybackManager.togglePlayPause() },
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (state.isBuffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(if (state.isPlaying) R.drawable.pause else R.drawable.play),
                                        contentDescription = stringResource(
                                            if (state.isPlaying) R.string.pause else R.string.play
                                        ),
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                        MiniControlButton(
                            iconRes = R.drawable.ic_skip_next,
                            contentDescriptionRes = R.string.video_player_up_next,
                            onClick = { VideoPlaybackManager.playNext() },
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                    // Determinate progress line.
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 6.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniControlButton(
    iconRes: Int,
    contentDescriptionRes: Int,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(contentDescriptionRes),
            modifier = Modifier.size(18.dp),
            tint = Color.White
        )
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
 * vertical drag collapses, horizontal drag scrubs) + top/bottom control bars.
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
                        // Double-tap sides seek ±10s; center toggles controls.
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
                // Base position captured once at drag start; each event adds its own delta.
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
                    // 1dp of drag ≈ 50ms of video, a smooth scrub speed.
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

        scrubPreviewMs?.let { preview ->
            SeekPreviewBadge(
                previewMs = preview,
                positionMs = uiState.positionMs,
                isForward = isForward,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        seekHint?.let { hint ->
            Text(
                text = hint,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                )
            )
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 12.dp)
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                painter = painterResource(R.drawable.expand_less),
                contentDescription = stringResource(R.string.collapse_video),
                tint = Color.White
            )
        }
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
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = { VideoPlaybackManager.toggleSettings() }) {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = stringResource(R.string.video_quality),
                tint = Color.White
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.close),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PlayerBottomControls(
    uiState: VideoPlaybackManager.UiState,
    modifier: Modifier = Modifier,
) {
    var scrubPosition by remember { mutableFloatStateOf(-1f) }
    val durationSeconds = uiState.durationMs.toFloat().coerceAtLeast(1f)
    val effectivePosition = if (scrubPosition >= 0f) scrubPosition else uiState.positionMs.toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                )
            )
            .padding(top = 14.dp, bottom = 12.dp)
    ) {
        Slider(
            value = effectivePosition.coerceIn(0f, durationSeconds),
            onValueChange = { scrubPosition = it },
            onValueChangeFinished = {
                VideoPlaybackManager.seekTo(scrubPosition.toLong())
                scrubPosition = -1f
            },
            valueRange = 0f..durationSeconds,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = formatTime(effectivePosition.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = " / ${formatTime(uiState.durationMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { VideoPlaybackManager.playPrevious() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_previous),
                    contentDescription = stringResource(R.string.video_player_up_next),
                    tint = Color.White
                )
            }
            Surface(
                onClick = { VideoPlaybackManager.togglePlayPause() },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(if (uiState.isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = stringResource(
                                if (uiState.isPlaying) R.string.pause else R.string.play
                            ),
                            modifier = Modifier.size(26.dp),
                            tint = Color.Black
                        )
                    }
                }
            }
            IconButton(onClick = { VideoPlaybackManager.playNext() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_next),
                    contentDescription = stringResource(R.string.video_player_up_next),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { VideoPlaybackManager.toggleSettings() }) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = stringResource(R.string.video_quality),
                    tint = Color.White
                )
            }
        }
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Infinite scroll for both tabs.
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
                .size(40.dp)
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
                        .padding(8.dp),
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
            shape = RoundedCornerShape(20.dp),
            color = if (isSubscribed) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(36.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe),
                    contentDescription = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe),
                    modifier = Modifier.size(18.dp),
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton(
            icon = R.drawable.ic_thumb_up,
            label = stringResource(R.string.video_player_like),
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
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.height(40.dp)
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
    Comments(R.string.video_player_comments),
    UpNext(R.string.video_player_up_next),
}

@Composable
private fun VideoTabs(uiState: VideoPlaybackManager.UiState) {
    var selectedTab by remember { mutableIntStateOf(DetailTab.UpNext.ordinal) }

    Column {
        ScrollableTabRow(
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
                            // Pass the full item so the player never shows a blank title.
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
                .width(140.dp)
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
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "▶",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
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
            Text(
                text = listOfNotNull(
                    item.channelName.takeIf { it.isNotBlank() },
                    item.viewCountText,
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.video_quality),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Playback speed: ${uiState.playbackSpeed}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.playbackSpeed == speed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        onClick = {
                            val nextMode = when (uiState.resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            VideoPlaybackManager.setResizeMode(nextMode)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (uiState.resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Resize: Fit"
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Resize: Stretch"
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Resize: Crop"
                                else -> "Resize: Fit"
                            },
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
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
            // The resize setting was previously never applied to the surface.
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
