/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.constants.SubtitleLanguageKey
import com.auramusic.app.constants.SubtitlesEnabledKey
import com.auramusic.app.constants.VideoCodec
import com.auramusic.app.constants.VideoQuality
import com.auramusic.app.subtitles.SubtitleLanguageOptions
import com.auramusic.app.ui.component.BottomSheetMenu
import com.auramusic.app.ui.component.ChannelAvatarStack
import com.auramusic.app.ui.component.ChannelPickerDialog
import com.auramusic.app.ui.component.MenuState
import com.auramusic.app.ui.menu.YouTubeVideoMenu
import com.auramusic.app.utils.AuraPlayerUtils
import com.auramusic.app.video.VideoDownloadStore
import com.auramusic.app.utils.linkifiedText
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.video.VideoPlaybackManager.CommentItem
import com.auramusic.app.video.VideoPlaybackManager.RecommendationItem
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.Thumbnail
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun VideoPlayerOverlay(
    onChannelClick: ((String) -> Unit)? = null,
) {
    // Where the floating tile was put lives here rather than inside it: the tile is
    // disposed every time the player expands, and remembering its position inside
    // that subtree would drop it back into the corner on the way out.
    var floatX by rememberSaveable { mutableStateOf(0f) }
    var floatY by rememberSaveable { mutableStateOf(0f) }
    val state by VideoPlaybackManager.uiState.collectAsStateWithLifecycle()
    // The vertical Shorts pager renders the video surface itself, so the global
    // overlay (expanded player + mini tile) must stay out of the way while it is open.
    if (state.suppressOverlay) return
    // While the Activity is in picture-in-picture the system is already drawing the video into
    // the PiP window. Keeping the in-app overlay alive underneath would leave a second
    // PlayerView attached to the same player, so it is torn down entirely for the duration.
    if (state.inPictureInPicture) return
    val session = state.session ?: return
    // When music takes over the video is paused and its tile has to go. Returning here used to
    // be the whole handoff: the surface, the player view and the expanded player were all
    // disposed in the same frame that the audio switch, two notifications and the queue save
    // were already running in. The subtree now fades out first, which both looks intentional
    // and moves surface teardown off the frame where music starts.

    val context = LocalContext.current
    val player = VideoPlaybackManager.playerOrNull() ?: return
    val activity = context as? android.app.Activity

    // Minimised: the video shrinks into a small player drawn in this Activity's own window
    // and the rest of the app stays usable underneath it. Nothing system-level is involved -
    // no overlay grant, no second window - and the platform only takes the video over once
    // the user actually leaves the app (MainActivity.onUserLeaveHint).
    if (state.minimized) {
        AnimatedVisibility(
            visible = !state.hiddenByMusic,
            enter = fadeIn(tween(200)) + scaleIn(tween(240)),
            exit = fadeOut(tween(160)) + scaleOut(tween(180)),
            label = "videoFloatingPlayer",
        ) {
            VideoFloatingPlayer(
                player = player,
                session = session,
                offsetX = floatX,
                offsetY = floatY,
                onOffsetChange = { x, y ->
                    floatX = x
                    floatY = y
                },
                onExpand = { VideoPlaybackManager.expand() },
                onClose = { VideoPlaybackManager.close() },
            )
        }
        return
    }

    // The overlay covers the whole screen while expanded, so it must not keep the display
    // awake or swallow the back gesture once music has taken over.
    DisposableEffect(activity, state.hiddenByMusic) {
        val keepScreenOn = activity != null && !state.hiddenByMusic
        if (keepScreenOn) {
            activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Minimising drops the video into the floating player above. The system's picture-in-picture
    // window is deliberately not used here: it shrinks the whole Activity, so the app the user
    // was looking at disappears with it. The activity only enters it when the user leaves.
    val onCollapseRequest: () -> Unit = { VideoPlaybackManager.collapse() }

    // The top bar button does exactly what minimise does - there is no separate system window
    // to ask for any more - so it is the same request, just with a button for people who look
    // for a picture-in-picture control rather than a down arrow.
    val onPictureInPictureRequest: () -> Unit = { VideoPlaybackManager.collapse() }

    BackHandler(enabled = !state.hiddenByMusic) {
        val ui = VideoPlaybackManager.uiState.value
        // A video filling the window - full screen, or the pushed-down zoom - steps back
        // to the player on the first press. Only from the ordinary player does back mean
        // leaving the video.
        if (ui.isFullScreen || ui.zoomMode) VideoPlaybackManager.exitFullScreen()
        else onCollapseRequest()
    }

    AnimatedVisibility(
        visible = !state.hiddenByMusic,
        enter = fadeIn(tween(220)) + scaleIn(tween(300)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        label = "videoOverlayVisible",
    ) {
        // Navigating to a channel while expanded would leave the full-screen overlay
        // covering it, so float/hide the player first so the channel screen is visible.
        val expandedChannelClick: ((String) -> Unit)? = onChannelClick?.let { click ->
            { channelId ->
                VideoPlaybackManager.collapse()
                click(channelId)
            }
        }
        VideoExpandedPlayer(
            player = player,
            session = session,
            uiState = state,
            onCollapse = onCollapseRequest,
            onClose = { VideoPlaybackManager.close() },
            onChannelClick = expandedChannelClick,
            onPictureInPicture = onPictureInPictureRequest,
        )
    }
}

// ---------------------------------------------------------------------------
// Floating player
// ---------------------------------------------------------------------------

/**
 * The small player that takes over once the overlay is minimised.
 *
 * It lives in this Activity's own window, so it needs no "display over other apps" grant and
 * the app underneath stays fully interactive - which is the whole point: the video keeps
 * playing while the user browses. The system only takes over when the user actually leaves
 * the app, and that path is handled by MainActivity.onUserLeaveHint.
 *
 * The video itself is a SurfaceView, and rounding an ancestor of one forces SurfaceFlinger to
 * composite it through an offscreen target every frame. The shape is therefore drawn as an
 * outline on top instead of clipping the view - the corners stay square but scrolling the app
 * underneath costs nothing extra.
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoFloatingPlayer(
    player: ExoPlayer,
    session: VideoPlaybackManager.VideoSession,
    offsetX: Float,
    offsetY: Float,
    onOffsetChange: (Float, Float) -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    val state by VideoPlaybackManager.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    // Whatever the bottom of the app currently reserves (music bar, gesture bar) is where the
    // player has to hover, otherwise it lands on top of the music controls.
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    // A little under half the screen. The old 40% read as a preview of the video
    // rather than something being watched; the clamps keep small phones from losing
    // most of their screen and tablets from getting a second television.
    val width = (screenWidthDp * 0.48f).coerceIn(170.dp, 300.dp)
    val aspect = state.videoAspectRatio.takeIf { it != null && it > 0.5f && it < 3f } ?: (16f / 9f)
    val height = width / aspect

    var dragX by remember { mutableFloatStateOf(offsetX) }
    var dragY by remember { mutableFloatStateOf(offsetY) }
    val dismissThreshold = with(density) { 120.dp.toPx() }

    // Bounds: keep the tile fully on screen while it is being dragged around.
    val maxShiftPx = with(density) { (screenWidthDp - width - 32.dp).toPx() }
    val maxUpPx = with(density) {
        (LocalConfiguration.current.screenHeightDp.dp - height - bottomInset - 32.dp).toPx()
    }

    // Rotation or a different inset can leave the remembered spot outside the new
    // bounds; pull it back rather than letting the tile hang half off the screen.
    LaunchedEffect(maxShiftPx, maxUpPx) {
        val clampedX = dragX.coerceIn(-maxShiftPx, 0f)
        val clampedY = dragY.coerceIn(-maxUpPx, 0f)
        if (clampedX != dragX || clampedY != dragY) {
            dragX = clampedX
            dragY = clampedY
            onOffsetChange(clampedX, clampedY)
        }
    }

    val closeRequest by rememberUpdatedState(onClose)
    val commitOffset by rememberUpdatedState(onOffsetChange)

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomInset + 16.dp)
                .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                .size(width, height)
                .graphicsLayer {
                    val p = (dragY / dismissThreshold).coerceIn(0f, 1f)
                    scaleX = 1f - 0.05f * p
                    scaleY = 1f - 0.05f * p
                    alpha = 1f - 0.5f * p
                }
                .pointerInput(maxShiftPx, maxUpPx, dismissThreshold) {
                    detectDragGestures(
                        onDragEnd = {
                            if (dragY > dismissThreshold) {
                                closeRequest()
                            } else {
                                // Stay where the finger left it. Snapping back to the
                                // corner is why the tile could not be moved around.
                                dragX = dragX.coerceIn(-maxShiftPx, 0f)
                                dragY = dragY.coerceIn(-maxUpPx, 0f)
                                commitOffset(dragX, dragY)
                            }
                        },
                        onDragCancel = {
                            dragX = dragX.coerceIn(-maxShiftPx, 0f)
                            dragY = dragY.coerceIn(-maxUpPx, 0f)
                            commitOffset(dragX, dragY)
                        },
                        onDrag = { change, amount ->
                            dragX = (dragX + amount.x).coerceIn(-maxShiftPx, 0f)
                            dragY = (dragY + amount.y).coerceIn(-maxUpPx, dismissThreshold * 1.6f)
                            change.consume()
                        },
                    )
                },
            shape = RoundedCornerShape(18.dp),
            color = Color.Black,
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                AndroidVideoSurface(player, resizeModeOverride = state.resizeMode)
                // A soft top scrim so the close button reads against any picture.
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )
                // Playback progress as a hairline along the bottom edge. The position is
                // only read in the draw phase, so its ticks redraw this strip and nothing
                // else - the tile does not recompose twice a second.
                val positionPair = VideoPlaybackManager.positionState.collectAsStateWithLifecycle()
                val progressColor = MaterialTheme.colorScheme.primary
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                        .drawBehind {
                            val (pos, dur) = positionPair.value
                            val fraction = if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
                            drawRect(
                                color = progressColor,
                                size = Size(size.width * fraction, size.height),
                            )
                        }
                )
                // Rounded outline drawn on top - clipping the SurfaceView ancestor would
                // force offscreen compositing every frame.
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                )
                // Close button
                OverlayIconButton(
                    icon = R.drawable.close,
                    contentDescription = stringResource(R.string.close),
                    onClick = onClose,
                    size = 32.dp,
                    iconSize = 16.dp,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
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
    onChannelClick: ((String) -> Unit)?,
    onPictureInPicture: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600
    val context = LocalContext.current

    var showControls by remember { mutableStateOf(true) }

    // The surface on its own, filling the screen with the video centred inside it: either
    // full screen, or the zoom reached by pushing down. In both, the details below are
    // not shown - a video filling the window has no room beside it, and scrolling a
    // description underneath a black surface is worse than nothing.
    val surfaceOnly = uiState.isFullScreen || uiState.zoomMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (surfaceOnly) {
            VideoSurfaceWithControls(
                player = player,
                session = session,
                uiState = uiState,
                showControls = showControls,
                onControlsChange = { showControls = it },
                onCollapse = onCollapse,
                onClose = onClose,
                onPictureInPicture = onPictureInPicture,
                // Fills the window; the player centres the video inside it at its own
                // aspect, so a portrait video is neither cropped nor stretched.
                modifier = Modifier.fillMaxSize(),
                useFullHeight = true,
                allowZoomDrag = true,
                onChannelClick = onChannelClick,
            )
            UpNextTile(
                uiState = uiState,
                onPlayNext = { VideoPlaybackManager.playNext() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // The seek bar owns the bottom strip in these modes, so the tile is
                    // lifted clear of it - and of the navigation bar underneath - rather
                    // than sitting under the one control that is out for most of the time.
                    .padding(
                        end = 16.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + if (showControls) 96.dp else 16.dp,
                    ),
            )
        } else if (isWideScreen) {
            // Tablets and other large screens: the video spans the full width at a
            // true 16:9, and everything else - title, channel, action buttons,
            // description, recommendations - sits underneath it.
            //
            // This used to be a side-by-side Row (video 62% | details 38%). On a
            // tablet that pushed the like/share/download buttons into a narrow
            // right-hand column, far from the video they act on, and squeezed the
            // text into a column too narrow to read. One column is both simpler
            // and the shape a wide screen actually wants.
            Column(modifier = Modifier.fillMaxSize()) {
                VideoSurfaceWithControls(
                    player = player,
                    session = session,
                    uiState = uiState,
                    showControls = showControls,
                    onControlsChange = { showControls = it },
                    onCollapse = onCollapse,
                    onClose = onClose,
                    onPictureInPicture = onPictureInPicture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    useFullHeight = false,
                    onChannelClick = onChannelClick,
                )
                VideoDetailPane(
                    session = session,
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface),
                    onChannelClick = onChannelClick ?: {},
                )
            }
        } else if (isLandscape) {
            // Phone landscape: the screen height is small, so a 16:9 video at full
            // width would overflow it. Play the surface full-screen (the player's
            // RESIZE_MODE_FIT letterboxes it) and overlay a compact title + actions
            // bar that appears together with the controls.
            Box(modifier = Modifier.fillMaxSize()) {
                VideoSurfaceWithControls(
                    player = player,
                    session = session,
                    uiState = uiState,
                    showControls = showControls,
                    onControlsChange = { showControls = it },
                    onCollapse = onCollapse,
                    onClose = onClose,
                    onPictureInPicture = onPictureInPicture,
                    modifier = Modifier.fillMaxSize(),
                    useFullHeight = true,
                    onChannelClick = onChannelClick,
                    // Stacked above the seek bar inside the controls, so it fades with them
                    // and never sits on top of the track and times.
                    bottomActions = {
                        CompactLandscapeBar(
                            uiState = uiState,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                VideoSurfaceWithControls(
                    player = player,
                    session = session,
                    uiState = uiState,
                    showControls = showControls,
                    onControlsChange = { showControls = it },
                    onCollapse = onCollapse,
                    onClose = onClose,
                    onPictureInPicture = onPictureInPicture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 12f),
                    useFullHeight = false,
                    onChannelClick = onChannelClick,
                )
                VideoDetailPane(
                    session = session,
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface),
                    onChannelClick = onChannelClick ?: {},
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
/**
 * The next video, offered as a small tile while the current one finishes.
 *
 * Replaces the list of recommendations in a surface that has no room for one: inside a
 * video that fills the window, a full Up Next pane either has nowhere to go or pushes the
 * video off centre. It appears only in the last stretch of the video, so it cannot sit
 * over the picture for the whole runtime.
 */
@Composable
private fun UpNextTile(
    uiState: VideoPlaybackManager.UiState,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val posMs by remember { derivedStateOf { VideoPlaybackManager.positionState.value.first } }
    val durationMs = uiState.durationMs

    val next = remember(uiState.queue, uiState.recommendations, uiState.session?.videoId) {
        uiState.queue.firstOrNull { it.videoId != uiState.session?.videoId }
            ?: uiState.recommendations.firstOrNull { it.videoId != uiState.session?.videoId }
    }

    // Only in the closing stretch, and only when there is room left to act on it.
    val remaining = (durationMs - posMs).coerceAtLeast(0L)
    val show = next != null && durationMs > 0 && remaining in 1 until UP_NEXT_TILE_WINDOW_MS

    AnimatedVisibility(
        visible = show,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        val item = next ?: return@AnimatedVisibility
        Surface(
            onClick = onPlayNext,
            shape = RoundedCornerShape(18.dp),
            color = Color.Black.copy(alpha = 0.74f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shadowElevation = 6.dp,
            modifier = Modifier.width(236.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 100.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.video_player_up_next),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoSurfaceWithControls(
    player: ExoPlayer,
    session: VideoPlaybackManager.VideoSession,
    uiState: VideoPlaybackManager.UiState,
    showControls: Boolean,
    onControlsChange: (Boolean) -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    useFullHeight: Boolean,
    // Whether a downward drag in the middle of the picture should still be read as a
    // gesture. It is off in a plain landscape player, where the height is already the
    // whole screen and a drag there means nothing, and on once the surface has been
    // grown deliberately - there the next step is to shrink it again.
    allowZoomDrag: Boolean = false,
    onChannelClick: ((String) -> Unit)? = null,
    onPictureInPicture: (() -> Unit)? = null,
    bottomActions: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val audioManager = context.getSystemService(AudioManager::class.java)
    val activity = context as? android.app.Activity

    // Position ticks every 500ms while a video plays. Reading positionState directly here
    // would invalidate this whole scope - video surface included - on every tick, so only
    // the duration is observed, and only when it actually changes.
    val durMs by remember { derivedStateOf { VideoPlaybackManager.positionState.value.second } }
    // Settings reads go through a content provider; with the surface recomposing twice a
    // second that query used to run on the main thread that often too.
    val defaultBrightness = remember(context) {
        runCatching {
            val value = android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
            )
            value / 255f
        }.getOrDefault(0.5f)
    }

    var collapseDragPx by remember { mutableFloatStateOf(0f) }
    var scrubPreviewMs by remember { mutableStateOf<Long?>(null) }
    var isForward by remember { mutableStateOf(true) }
    var seekHint by remember { mutableStateOf<String?>(null) }
    var adjustPreview by remember {
        mutableStateOf<AdjustPreview?>(null)
    }
    val seekForwardLabel = stringResource(R.string.seek_forward_dynamic)
    val seekBackwardLabel = stringResource(R.string.seek_backward_dynamic)

    // When the controls were last touched. Keying the auto-hide timer on this means every
    // interaction restarts the window declaratively; a plain "delay then hide" effect would
    // keep counting from whenever the controls were revealed and could fire mid-gesture.
    var lastInteractionMs by remember {
        mutableLongStateOf(android.os.SystemClock.elapsedRealtime())
    }
    // Held through rememberUpdatedState because pointerInput's keys do not include them: the
    // gesture lambdas run once and would otherwise keep calling the first composition's
    // closures, so a second tap would consult a stale visibility.
    val noteInteraction by rememberUpdatedState(
        { lastInteractionMs = android.os.SystemClock.elapsedRealtime() },
    )
    val currentShowControls by rememberUpdatedState(showControls)
    val currentOnControlsChange by rememberUpdatedState(onControlsChange)

    // A scrub or a brightness/volume drag is in progress. The controls must not vanish
    // underneath the user's finger, so the auto-hide timer stands down for its duration.
    val isInteracting = scrubPreviewMs != null || adjustPreview != null

    LaunchedEffect(showControls, uiState.isPlaying, isInteracting, lastInteractionMs) {
        if (!showControls || !uiState.isPlaying || isInteracting) return@LaunchedEffect
        val remaining = AUTO_HIDE_CONTROLS_MS -
            (android.os.SystemClock.elapsedRealtime() - lastInteractionMs)
        if (remaining > 0) delay(remaining)
        currentOnControlsChange(false)
    }

    // Restore system brightness when leaving the expanded player so the window
    // brightness override set by the vertical gesture doesn't persist.
    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { window ->
                val attrs = window.attributes
                if (attrs.screenBrightness >= 0f) {
                    attrs.screenBrightness = -1f
                    window.attributes = attrs
                }
            }
        }
    }

    LaunchedEffect(seekHint) {
        if (seekHint != null) {
            delay(700)
            seekHint = null
        }
    }

    LaunchedEffect(adjustPreview) {
        if (adjustPreview != null) {
            delay(800)
            adjustPreview = null
        }
    }

    val collapseFraction = if (useFullHeight) 0f else (collapseDragPx / 900f).coerceIn(0f, 0.75f)

    // Only a surface that fills the window can end up under the navigation bar; the
    // inline player is laid out around it already.
    val navigationBarPadding = if (useFullHeight) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    Box(
        // A graphicsLayer directly above the video surface forces the whole subtree,
        // SurfaceView included, through an offscreen render target for every frame. It is
        // only needed while a collapse drag is in flight, so it is applied conditionally:
        // at rest the layer is absent and SurfaceFlinger composites the surface directly.
        modifier = modifier
            .then(
                if (collapseDragPx != 0f || collapseFraction != 0f) {
                    Modifier.graphicsLayer {
                        translationY = collapseDragPx
                        alpha = 1f - collapseFraction
                    }
                } else {
                    Modifier
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        noteInteraction()
                        onControlsChange(!currentShowControls)
                        if (!currentShowControls) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    },
                    onDoubleTap = { offset ->
                        noteInteraction()
                        val side = when {
                            offset.x < size.width / 3 -> -1
                            offset.x > size.width * 2 / 3 -> 1
                            else -> 0
                        }
                        if (side == 0) {
                            onControlsChange(!currentShowControls)
                        } else {
                            val (currentPos, currentDur) = VideoPlaybackManager.positionState.value
                            val target = (currentPos + side * 10_000L)
                                .coerceIn(0L, currentDur)
                            VideoPlaybackManager.seekTo(target)
                            isForward = side > 0
                            seekHint = if (side > 0) seekForwardLabel else seekBackwardLabel
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    }
                )
            }
            .pointerInput(useFullHeight) {
                var dragMode = DragMode.NONE
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        noteInteraction()
                        dragMode = when {
                            offset.x < size.width / 3f -> DragMode.BRIGHTNESS
                            offset.x > size.width * 2f / 3f -> DragMode.VOLUME
                            !useFullHeight || allowZoomDrag -> DragMode.COLLAPSE
                            else -> DragMode.NONE
                        }
                    },
                    onDragEnd = {
                        noteInteraction()
                        when (dragMode) {
                            DragMode.COLLAPSE -> {
                                if (collapseDragPx > with(density) { 120.dp.toPx() }) {
                                    // Pushing down steps the surface up one notch at a
                                    // time: ordinary, then the video filling the window
                                    // with its own shape kept, and only then out of the
                                    // way to the small player.
                                    if (uiState.zoomMode || uiState.isFullScreen) onCollapse()
                                    else VideoPlaybackManager.toggleZoomMode()
                                } else {
                                    collapseDragPx = 0f
                                }
                            }
                            else -> collapseDragPx = 0f
                        }
                        dragMode = DragMode.NONE
                    },
                    onDragCancel = {
                        noteInteraction()
                        collapseDragPx = 0f
                        dragMode = DragMode.NONE
                    },
                ) { change, dragAmount ->
                    when (dragMode) {
                        DragMode.COLLAPSE -> {
                            if (dragAmount > 0f) {
                                collapseDragPx = (collapseDragPx + dragAmount).coerceIn(0f, 800f)
                                change.consume()
                            }
                        }
                        DragMode.BRIGHTNESS -> {
                            change.consume()
                            val delta = -dragAmount / size.height
                            val base = activity?.window?.attributes?.screenBrightness
                                ?.takeIf { it >= 0f }
                                ?: defaultBrightness
                            val newBrightness = (base + delta * 1.2f).coerceIn(0.05f, 1f)
                            activity?.window?.let { window ->
                                val attrs = window.attributes
                                attrs.screenBrightness = newBrightness
                                window.attributes = attrs
                            }
                            adjustPreview = AdjustPreview(DragMode.BRIGHTNESS, newBrightness)
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                        DragMode.VOLUME -> {
                            change.consume()
                            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: return@detectVerticalDragGestures
                            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val target = (current + (-dragAmount / size.height) * max * 1.2f)
                                .toInt()
                                .coerceIn(0, max)
                            if (target != current) {
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    target,
                                    0,
                                )
                            }
                            adjustPreview = AdjustPreview(DragMode.VOLUME, target.toFloat() / max.coerceAtLeast(1))
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                        DragMode.NONE -> Unit
                    }
                }
            }
            .pointerInput(durMs) {
                var dragBaseMs = 0L
                detectHorizontalDragGestures(
                    onDragStart = {
                        noteInteraction()
                        dragBaseMs = VideoPlaybackManager.positionState.value.first
                    },
                    onDragEnd = {
                        noteInteraction()
                        scrubPreviewMs?.let { VideoPlaybackManager.seekTo(it) }
                        scrubPreviewMs = null
                    },
                    onDragCancel = {
                        noteInteraction()
                        scrubPreviewMs = null
                    },
                ) { change, dragAmount ->
                    change.consume()
                    val deltaMs = dragAmount.toDp().value * 50f
                    isForward = dragAmount > 0
                    scrubPreviewMs = (dragBaseMs + deltaMs.toLong())
                        .coerceIn(0L, durMs)
                }
            }
            // The surface is placed before the controls so it is drawn underneath. Controls
            // overlay it with translucent gradients; the video surface itself is never wrapped
            // in a clipping or transforming container.
    ) {
        AndroidVideoSurface(player, resizeModeOverride = uiState.resizeMode)

        // A light, even dim over the whole picture while the controls are up, so white
        // controls read on bright footage. Drawn below the captions so they stay crisp.
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.matchParentSize(),
            label = "controlsDim",
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }

        // Drawn after the surface so it lands on top of it, and kept out of the controls'
        // visibility state: captions are content, not chrome, so they stay up while the
        // controls fade - they just move down when the bar reappears.
        VideoCaptionOverlay(
            videoId = session.videoId,
            player = player,
            // Above the seek bar while the controls are up, above the navigation bar
            // always: a caption hidden behind either is a caption that is not there.
            bottomPadding = navigationBarPadding + if (showControls) 108.dp else 20.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.matchParentSize(),
            label = "playerControls",
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerTopBar(
                    onCollapse = onCollapse,
                    onClose = onClose,
                    onPictureInPicture = onPictureInPicture,
                    title = session.title,
                    channelName = session.channelName,
                    channelId = session.channelId,
                    onChannelClick = onChannelClick,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                PlayerBottomControls(
                    uiState = uiState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    clearNavigationBar = useFullHeight,
                    actions = bottomActions,
                )
                // Last, so the bars' scrims - which fade out past their own bounds - are
                // drawn underneath the transport controls rather than over them.
                PlayerCenterControls(
                    uiState = uiState,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
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
                positionMs = VideoPlaybackManager.positionState.value.first,
                isForward = isForward,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        adjustPreview?.let { preview ->
            AdjustPreviewBadge(
                mode = preview.mode,
                fraction = preview.fraction,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** Soft drop shadow for text drawn straight onto the picture. */
private val OverlayTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.55f),
    offset = Offset(0f, 1f),
    blurRadius = 6f,
)

/**
 * A round, translucent button for the chrome drawn over the video. The hairline border
 * keeps its edge visible on dark footage, the tinted fill keeps the icon visible on
 * bright footage.
 */
@Composable
private fun OverlayIconButton(
    icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color = Color.Black.copy(alpha = 0.32f),
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    tint: Color = Color.White,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = tint,
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onChannelClick: ((String) -> Unit)?,
    title: String,
    channelName: String,
    channelId: String? = null,
    modifier: Modifier = Modifier,
    onPictureInPicture: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            // A deep, soft scrim that keeps fading past the bar's own bottom edge, so the
            // picture darkens gradually instead of being cut off by a visible band.
            .drawBehind {
                val fadeTail = 56.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.72f),
                        0.5f to Color.Black.copy(alpha = 0.34f),
                        1f to Color.Transparent,
                        startY = 0f,
                        endY = size.height + fadeTail,
                    ),
                    size = Size(size.width, size.height + fadeTail),
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 14.dp)
    ) {
        OverlayIconButton(
            icon = R.drawable.expand_less,
            contentDescription = stringResource(R.string.collapse_video),
            onClick = onCollapse,
            iconSize = 24.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(shadow = OverlayTextShadow),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (channelName.isNotBlank()) {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.bodySmall.copy(shadow = OverlayTextShadow),
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            channelId?.let { onChannelClick?.invoke(it) }
                        }
                )
            }
        }
        OverlayIconButton(
            icon = R.drawable.settings,
            contentDescription = stringResource(R.string.video_quality),
            onClick = { VideoPlaybackManager.toggleSettings() },
            iconSize = 20.dp,
        )
        // The same switch Player settings writes. Captions are an app-wide preference rather
        // than a per-player mode, so flipping it here also governs the music video player.
        val (captionsEnabled, onCaptionsToggle) = rememberPreference(SubtitlesEnabledKey, true)
        val captionsContainer by animateColorAsState(
            targetValue = if (captionsEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Black.copy(alpha = 0.32f)
            },
            animationSpec = tween(180),
            label = "captionsContainer",
        )
        OverlayIconButton(
            icon = R.drawable.ic_subtitles,
            contentDescription = stringResource(R.string.closed_captions),
            onClick = { onCaptionsToggle(!captionsEnabled) },
            iconSize = 20.dp,
            containerColor = captionsContainer,
            tint = if (captionsEnabled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                Color.White.copy(alpha = 0.8f)
            },
        )
        if (onPictureInPicture != null) {
            OverlayIconButton(
                icon = R.drawable.picture_in_picture,
                contentDescription = stringResource(R.string.video_player_float),
                onClick = onPictureInPicture,
                iconSize = 20.dp,
            )
        }
        OverlayIconButton(
            icon = R.drawable.close,
            contentDescription = stringResource(R.string.close),
            onClick = onClose,
            iconSize = 20.dp,
        )
    }
}

/**
 * Transport controls, in the middle of the picture.
 *
 * Centre is where the eye already is, and it is the only place they can go once the
 * seek bar has been pushed up clear of the navigation bar: at the bottom the two
 * compete for the same strip, and a video filling the window has nowhere else to put
 * them.
 */
@Composable
private fun PlayerCenterControls(
    uiState: VideoPlaybackManager.UiState,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // The caller's alignment is what places this in the middle of the picture.
        // Without it the row falls to the top corner of the box, which is where these
        // controls used to live before they moved.
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
    ) {
        OverlayIconButton(
            icon = R.drawable.ic_skip_previous,
            contentDescription = stringResource(R.string.video_player_up_next),
            onClick = { VideoPlaybackManager.playPrevious() },
            size = 52.dp,
            iconSize = 28.dp,
        )
        // A frosted disc rather than a solid one: it stays the focal point without
        // punching a white hole in the middle of the picture. It dips slightly when
        // pressed so the tap is felt as well as seen.
        val playInteraction = remember { MutableInteractionSource() }
        val playPressed by playInteraction.collectIsPressedAsState()
        val playScale by animateFloatAsState(
            targetValue = if (playPressed) 0.92f else 1f,
            animationSpec = tween(120),
            label = "playPressScale",
        )
        Surface(
            onClick = { VideoPlaybackManager.togglePlayPause() },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.42f),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.26f)),
            interactionSource = playInteraction,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (uiState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Crossfade(
                        targetState = uiState.isPlaying,
                        animationSpec = tween(160),
                        label = "playPauseIcon",
                    ) { playing ->
                        Icon(
                            painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                            contentDescription = stringResource(
                                if (playing) R.string.pause else R.string.play
                            ),
                            modifier = Modifier.size(38.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
        OverlayIconButton(
            icon = R.drawable.ic_skip_next,
            contentDescription = stringResource(R.string.video_player_up_next),
            onClick = { VideoPlaybackManager.playNext() },
            size = 52.dp,
            iconSize = 28.dp,
        )
    }
}

@Composable
private fun PlayerBottomControls(
    uiState: VideoPlaybackManager.UiState,
    modifier: Modifier = Modifier,
    clearNavigationBar: Boolean = false,
    /** Extra controls stacked above the time row, so they can never cover the seek bar. */
    actions: (@Composable () -> Unit)? = null,
) {
    // Position and duration arrive as getters rather than values. The manager publishes a
    // fresh pair twice a second, and reading that state in composition would invalidate this
    // whole controls subtree — including the video surface above it — on every tick. Reading
    // through a getter at draw time confines the update to the two numbers that actually
    // change: the elapsed label and the seek bar.
    val posMs: () -> Long = { VideoPlaybackManager.positionState.value.first }
    val durMs: () -> Long = { VideoPlaybackManager.positionState.value.second }

    // Tabular figures, so the elapsed time does not jitter sideways as its digits change.
    val timeStyle = MaterialTheme.typography.labelLarge.copy(
        fontFeatureSettings = "tnum",
        shadow = OverlayTextShadow,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            // A deep, soft scrim that starts well above the bar and also runs under the
            // navigation bar, so the labels and the track always sit on dark ground.
            .drawBehind {
                val fadeHead = 64.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.42f),
                        1f to Color.Black.copy(alpha = 0.82f),
                        startY = -fadeHead,
                        endY = size.height,
                    ),
                    topLeft = Offset(0f, -fadeHead),
                    size = Size(size.width, size.height + fadeHead),
                )
            }
            // Only when the video fills the window. There it draws over the navigation
            // bar and the seek bar has to be lifted by however much of it the system is
            // showing. In the ordinary player the layout around the video already leaves
            // room for it, and lifting the bar here too left it floating above where it
            // belongs, with a gap under it.
            .then(
                if (clearNavigationBar) {
                    Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                } else {
                    Modifier
                },
            )
            .padding(top = 12.dp, bottom = 6.dp)
    ) {
        actions?.invoke()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
        ) {
            Text(
                text = formatTime(posMs()),
                style = timeStyle,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = " / ${formatTime(durMs())}",
                style = timeStyle,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.weight(1f))
            // Fullscreen toggle
            OverlayIconButton(
                icon = if (uiState.isFullScreen) R.drawable.fullscreen_exit else R.drawable.fullscreen,
                contentDescription = stringResource(R.string.close),
                onClick = { VideoPlaybackManager.toggleFullScreen() },
                iconSize = 24.dp,
                containerColor = Color.Transparent,
                borderColor = Color.Transparent,
            )
        }

        SlimSeekBar(
            positionMs = posMs,
            durationMs = durMs,
            chapters = uiState.chapters,
            onSeek = { VideoPlaybackManager.seekTo(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}

/**
 * YouTube-style slim progress bar: a thin track with a small thumb that can be
 * tapped or dragged to seek. While it is being dragged the track thickens and the
 * thumb grows a halo, so the control under the finger is plainly the one moving.
 */
@Composable
private fun SlimSeekBar(
    positionMs: () -> Long,
    durationMs: () -> Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    chapters: List<com.auramusic.innertube.VideoChapters.Chapter> = emptyList(),
) {
    var scrub by remember { mutableFloatStateOf(-1f) }
    val trackWidthPx = remember { mutableIntStateOf(0) }
    // Sized off the composition: the track width only changes on rotation or resize, and the
    // thumb reads it in the draw phase rather than recomposing on every progress tick.
    // Rounded to whole seconds so a second-long position tick does not invalidate this bar on
    // every publish. At a visible thumb size the sub-second movement is not perceptible, and
    // the thumb still tracks smoothly because scrubbing overrides with a continuous value.
    val positionSeconds = remember { derivedStateOf { positionMs() / 1000L } }.value
    val durationSeconds = remember { derivedStateOf { (durationMs() / 1000L).toFloat().coerceAtLeast(1f) } }.value
    // Scrub, seek and preview positions are all milliseconds.
    val durationMsF = durationSeconds * 1000f
    val effective = if (scrub >= 0f) scrub else positionSeconds * 1000f
    val progress = (effective / durationMsF).coerceIn(0f, 1f)

    val scrubbing = scrub >= 0f
    val trackHeight by animateDpAsState(
        targetValue = if (scrubbing) 6.dp else 4.dp,
        animationSpec = tween(150),
        label = "seekTrackHeight",
    )
    val thumbSize by animateDpAsState(
        targetValue = if (scrubbing) 18.dp else 12.dp,
        animationSpec = tween(150),
        label = "seekThumbSize",
    )
    val haloSize by animateDpAsState(
        targetValue = if (scrubbing) 28.dp else 12.dp,
        animationSpec = tween(150),
        label = "seekHaloSize",
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (scrubbing) 0.3f else 0f,
        animationSpec = tween(150),
        label = "seekHaloAlpha",
    )
    val activeColor = MaterialTheme.colorScheme.primary

    // While scrubbing, what is under the finger: the chapter being moved into, and a
    // thumbnail of that moment when this video has a storyboard.
    var previewAt by remember { mutableStateOf(-1L) }

    Box(
        modifier = modifier
            .height(28.dp)
            .onSizeChanged { trackWidthPx.intValue = it.width }
            .pointerInput(durationSeconds) {
                detectDragGestures(
                    onDragStart = { offset ->
                        scrub = (offset.x / size.width * durationMsF).coerceIn(0f, durationMsF)
                    },
                    onDrag = { change, _ ->
                        scrub = (change.position.x / size.width * durationMsF).coerceIn(0f, durationMsF)
                        previewAt = scrub.toLong()
                        change.consume()
                    },
                    onDragEnd = {
                        scrub.takeIf { it >= 0f }?.let {
                            onSeek(it.toLong())
                            scrub = -1f
                        }
                        previewAt = -1L
                    },
                    onDragCancel = { scrub = -1f; previewAt = -1L },
                )
            }
            .pointerInput(durationSeconds) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width * durationMsF).coerceIn(0f, durationMsF).toLong())
                }
            }
    ) {
        // Unplayed track, a touch thinner than the played part so the progress reads as
        // the stronger line.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .height(trackHeight - 1.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.28f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .align(Alignment.CenterStart)
                .height(trackHeight)
                .clip(CircleShape)
                .background(activeColor)
        )
        // Chapter boundaries, so the shape of the video is visible before scrubbing it.
        // Drawn as small gaps cut across the track.
        chapters.forEach { chapter ->
            if (chapter.startMs <= 0L || durationSeconds <= 0f) return@forEach
            val fraction = (chapter.startMs / (durationSeconds * 1000f)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { translationX = (trackWidthPx.intValue * fraction) - 1.dp.toPx() }
                    .size(width = 2.dp, height = trackHeight + 2.dp)
                    .background(Color.Black.copy(alpha = 0.7f)),
            )
        }
        // Halo around the thumb, only while scrubbing.
        if (haloAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = ((trackWidthPx.intValue - thumbSize.toPx()) * progress) -
                            (haloSize - thumbSize).toPx() / 2f
                    }
                    .size(haloSize)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = haloAlpha))
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                // Thumb-center tracks the fill edge; subtracting the thumb size
                // keeps it fully on the track from 0% to 100% instead of being
                // parked mid-bar (Center + progress offset was off by half the
                // bar width) or sliding off either end.
                //
                // Translated in the layer rather than laid out with .offset(), and
                // without the blur shadow: this moves on every progress tick, and
                // re-measuring plus re-blurring a shadow at 2Hz for a 13dp dot is
                // not a good trade.
                .graphicsLayer { translationX = ((trackWidthPx.intValue - thumbSize.toPx()) * progress) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )

        // Preview bubble, riding above the bar and following the scrubbed position.
        androidx.compose.animation.AnimatedVisibility(
            visible = previewAt >= 0L,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            // The bar is only 28dp tall; measure the bubble without that limit and let it
            // rise above the track instead of being squashed inside it.
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true),
        ) {
            val at = previewAt
            val chapterTitle = com.auramusic.innertube.VideoChapters.chapterAt(chapters, at)?.title
            val previewUrl = remember(at, durationSeconds) {
                if (at < 0L || durationSeconds <= 0f) null
                else com.auramusic.innertube.VideoChapters.storyboardUrl(
                    spec = VideoPlaybackManager.uiState.value.storyboardSpec.orEmpty(),
                    timeMs = at,
                    durationMs = (durationSeconds * 1000f).toLong(),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 30.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(4.dp),
                    ) {
                        if (previewUrl != null) {
                            AsyncImage(
                                model = previewUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                        Text(
                            text = formatTime(at),
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        chapterTitle?.let { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                        }
                    }
                }
            }
        }
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
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
        label = "seekScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        ) {
            Icon(
                painter = painterResource(if (isForward) R.drawable.fast_forward else R.drawable.replay),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
        Text(
            text = hint,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
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
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(
            text = formatTime(previewMs),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
        )
        val deltaSec = (previewMs - positionMs) / 1000L
        if (deltaSec != 0L) {
            Text(
                text = (if (deltaSec > 0) "+" else "") + "${deltaSec}s",
                color = if (isForward) Color(0xFF8BC34A) else Color(0xFFFF8A65),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** How long the controls stay up after the last touch on the video. */
private const val AUTO_HIDE_CONTROLS_MS = 3_000L

private enum class DragMode { NONE, BRIGHTNESS, VOLUME, COLLAPSE }

private data class AdjustPreview(
    val mode: DragMode,
    val fraction: Float,
)

@Composable
private fun AdjustPreviewBadge(
    mode: DragMode,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val label = when (mode) {
        DragMode.BRIGHTNESS -> stringResource(R.string.video_player_brightness)
        else -> stringResource(R.string.video_player_volume)
    }
    val icon = when (mode) {
        DragMode.BRIGHTNESS -> R.drawable.contrast
        else -> R.drawable.volume_up
    }
    val level = fraction.coerceIn(0f, 1f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .width(136.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = Color.White,
        )
        Text(
            text = "${(level * 100).toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(level)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
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
    onChannelClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(DetailTab.UpNext.ordinal) }
    // The app's menu host lives underneath the video overlay, so a sheet opened from
    // the queue would be hidden by the player. The pane hosts its own instead.
    val menuState = remember { MenuState() }
    // Reply expansion lives with the pane: comment rows are recycled by the lazy list,
    // so per-row remember state would not survive a scroll.
    val expandedReplyIds = remember { mutableStateOf(setOf<String>()) }

    // Load more triggers on queue size too: when the queue drains down to the last few
    // items the load-more request fires exactly as it would from comments, so the list
    // never runs dry without warning.
    LaunchedEffect(listState, uiState.comments.size, uiState.queue.size) {
        snapshotFlowSafe(listState) { nearEnd ->
            if (!nearEnd) return@snapshotFlowSafe
            val fresh = VideoPlaybackManager.uiState.value
            when {
                fresh.isLoadingMoreComments || fresh.isLoadingMoreRecommendations -> Unit
                else -> {
                    VideoPlaybackManager.loadMoreComments()
                    VideoPlaybackManager.loadMoreRecommendations()
                }
            }
        }
    }

    LaunchedEffect(uiState.session?.videoId) {
        expandedReplyIds.value = emptySet()
        // Auto-scroll: when the playing video advances, bring its row into view so the
        // queue always shows where the listener is, the same way the TV queue does.
        val currentId = uiState.session?.videoId ?: return@LaunchedEffect
        val index = uiState.queue.indexOfFirst { it.videoId == currentId }
        if (index < 0) return@LaunchedEffect
        // Count the fixed items above the queue rows (info + tabs + autoplay toggle = 3)
        // so the scroll target lands on the right row rather than three rows too high.
        val offset = 3
        if (listState.layoutInfo.visibleItemsInfo.any { it.index == index + offset }) {
            listState.animateScrollToItem(index + offset)
        } else {
            listState.scrollToItem(index + offset)
        }
    }

    // Pinned first, everything else in the order YouTube served it, so the
    // author's shout-out is not buried under 200 replies.
    val orderedComments = remember(uiState.comments) {
        uiState.comments.sortedByDescending { it.isPinned }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "info") {
            VideoInfoSection(
                session = session,
                uiState = uiState,
                onChannelClick = onChannelClick,
            )
        }
        item(key = "tabs") {
            VideoTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }
        // The tab bodies are lazy siblings instead of a Column nested inside one
        // item: a few hundred comments composed eagerly in a single slot made the
        // detail pane judder on scroll, and appending a page re-laid out the whole
        // block at once.
        if (selectedTab == DetailTab.Comments.ordinal) {
            when {
                uiState.isLoadingComments -> item(key = "comments-loading") {
                    ListSpinnerBlock(modifier = Modifier.padding(vertical = 24.dp))
                }
                uiState.commentsError != null && uiState.comments.isEmpty() -> {
                    item(key = "comments-error") {
                        CommentsErrorBlock(message = uiState.commentsError.orEmpty())
                    }
                }
                else -> {
                    // The id doubles as the key, but ids fall back to a content hash
                    // when the extractor leaves one blank - the index keeps a hash
                    // collision from taking the whole list down with it.
                    itemsIndexed(
                        items = orderedComments,
                        key = { index, comment -> "$index:${comment.commentId}" },
                    ) { _, comment ->
                        CommentThread(
                            comment = comment,
                            expanded = comment.commentId in expandedReplyIds.value,
                            onExpandedChange = { expand ->
                                expandedReplyIds.value = if (expand) {
                                    expandedReplyIds.value + comment.commentId
                                } else {
                                    expandedReplyIds.value - comment.commentId
                                }
                            },
                        )
                    }
                    if (uiState.isLoadingMoreComments) {
                        item(key = "comments-more") {
                            ListSpinnerBlock(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        } else {
            item(key = "autoplay") {
                AutoplayToggleRow(uiState = uiState)
            }
            when {
                // A skeleton only when there is nothing to show. Loading the next
                // video's recommendations used to flip this flag while the queue was
                // still full, and because this branch is tested first the whole list
                // was replaced by placeholders for as long as the fetch took - the
                // queue appearing to reload every time playback moved on.
                uiState.isLoadingRecommendations && uiState.queue.isEmpty() ->
                    item(key = "recommendations-loading") {
                        RecommendationSkeleton()
                    }
                uiState.queue.isEmpty() -> item(key = "recommendations-empty") {
                    NoRecommendationsBlock()
                }
                else -> {
                    itemsIndexed(
                        items = uiState.queue,
                        // Keyed by video, not by position. An index in the key meant
                        // every row was a different row the moment one was removed or
                        // inserted, so the whole list was thrown away and rebuilt
                        // instead of the one row that changed.
                        key = { _, item -> item.videoId },
                    ) { _, item ->
                        UpNextRow(
                            item = item,
                            isPlaying = item.videoId == uiState.session?.videoId,
                            // Already watched: still offered - clicking it is a
                            // deliberate choice - but no longer dressed up as something
                            // still to come.
                            hasPlayed = VideoPlaybackManager.hasPlayed(item.videoId),
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
                            },
                            onMenuClick = {
                                menuState.show {
                                    YouTubeVideoMenu(
                                        video = YouTubeVideoItem(
                                            videoId = item.videoId,
                                            title = item.title,
                                            channelName = item.channelName,
                                            channelId = item.channelId,
                                            viewCountText = item.viewCountText,
                                            publishedTimeText = item.publishedTimeText,
                                            durationText = item.durationText,
                                            thumbnails = listOfNotNull(
                                                item.thumbnail?.let {
                                                    Thumbnail(url = it, width = null, height = null)
                                                },
                                            ),
                                        ),
                                        channels = emptyList(),
                                        onChannelClick = { channelId ->
                                            VideoPlaybackManager.collapse()
                                            onChannelClick(channelId)
                                        },
                                        onDismiss = { menuState.dismiss() },
                                        // It is already in the queue: queueing it again
                                        // has nothing to say, and taking it out does.
                                        showQueueActions = false,
                                        onRemoveFromQueue = {
                                            VideoPlaybackManager.removeFromQueue(item.videoId)
                                        },
                                    )
                                }
                            },
                        )
                    }
                    if (uiState.isLoadingMoreRecommendations) {
                        item(key = "recommendations-more") {
                            ListSpinnerBlock(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
    // A host of its own: the app's menu host is drawn under this overlay, so a queue
    // sheet opened there would never be seen. A modal sheet is its own window, so it
    // can sit beside the list without taking part in its layout.
    BottomSheetMenu(state = menuState)
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
    onChannelClick: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val canGrow = !uiState.isFullScreen && !uiState.zoomMode

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Pushing down from the title and the channel grows the surface. It lives
            // here rather than on the picture because that is the part of the player
            // that reads as a header, and on the picture it fired whenever a thumb
            // rested there - including while someone was trying to scrub.
            .pointerInput(canGrow) {
                if (!canGrow) return@pointerInput
                val threshold = with(density) { 80.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var travelled = 0f
                    // Not touched until the drag is committed, so a swipe that belongs to
                    // the list below keeps working: downward, this list is already at the
                    // top and has nowhere to go.
                    var committed = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        val delta = change.positionChange().y
                        if (!committed) {
                            travelled += delta
                            if (travelled > threshold) {
                                committed = true
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            } else if (travelled < -threshold / 3f) {
                                // Upward, or sideways: not this gesture.
                                break
                            } else {
                                change.consume()
                                continue
                            }
                        }
                        change.consume()
                    }
                    if (committed) VideoPlaybackManager.toggleZoomMode()
                }
            }
            // Horizontal padding is applied per block rather than here, so the action row
            // can scroll edge to edge instead of being clipped at the margins.
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
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
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val subscribedChannelIds by VideoPlaybackManager.subscribedChannelIds.collectAsState()
            ChannelRow(
                session = session,
                isSubscribed = uiState.isSubscribed ||
                    VideoPlaybackManager.isChannelSubscribed(session.channelId),
                subscribedChannelIds = subscribedChannelIds,
                onSubscribeClick = { VideoPlaybackManager.toggleSubscribe() },
                onToggleChannelSubscribe = { channelId -> VideoPlaybackManager.toggleSubscribe(channelId) },
                onChannelClick = onChannelClick,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActionButtonsRow(
            isLiked = uiState.isLiked,
            isDisliked = uiState.isDisliked,
            isSaved = uiState.isSaved,
            likeCountText = session.likeCountText,
        )

        Spacer(modifier = Modifier.height(16.dp))

        session.description?.let { desc ->
            if (desc.isNotBlank()) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ExpandableDescription(
                        description = desc,
                        expanded = uiState.expandedDescription,
                        onToggle = { VideoPlaybackManager.toggleExpandedDescription() },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Compact title + actions bar overlaid at the bottom of the phone-landscape
 * player, where there's no room for the full detail pane next to the video.
 */
@Composable
private fun CompactLandscapeBar(
    uiState: VideoPlaybackManager.UiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                CompactActionChip(
                    icon = R.drawable.ic_thumb_up,
                    selected = uiState.isLiked,
                    contentDescription = stringResource(R.string.video_player_like),
                    onClick = { VideoPlaybackManager.toggleLike() },
                )
                CompactActionChip(
                    icon = R.drawable.ic_thumb_down,
                    selected = uiState.isDisliked,
                    contentDescription = stringResource(R.string.video_player_dislike),
                    onClick = { VideoPlaybackManager.toggleDislike() },
                )
                CompactActionChip(
                    icon = if (uiState.isSaved) R.drawable.library_add_check else R.drawable.library_add,
                    selected = uiState.isSaved,
                    contentDescription = stringResource(if (uiState.isSaved) R.string.video_player_saved else R.string.video_player_save),
                    onClick = { VideoPlaybackManager.toggleSave() },
                )
                Spacer(modifier = Modifier.weight(1f))
                val subscribeContainer by animateColorAsState(
                    targetValue = if (uiState.isSubscribed) {
                        Color.White.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    animationSpec = tween(200),
                    label = "compactSubscribeContainer",
                )
                Surface(
                    onClick = { VideoPlaybackManager.toggleSubscribe() },
                    shape = RoundedCornerShape(20.dp),
                    color = subscribeContainer,
                    border = if (uiState.isSubscribed) {
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    } else {
                        null
                    },
                    modifier = Modifier.height(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    ) {
                        Text(
                            text = stringResource(if (uiState.isSubscribed) R.string.subscribed else R.string.subscribe),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.isSubscribed) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
    }
}

@Composable
private fun CompactActionChip(
    icon: Int,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f),
        animationSpec = tween(180),
        label = "compactChipContainer",
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 0f else 0.12f)),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ChannelRow(
    session: VideoPlaybackManager.VideoSession,
    isSubscribed: Boolean,
    subscribedChannelIds: Set<String>,
    onSubscribeClick: () -> Unit,
    onToggleChannelSubscribe: (String) -> Unit,
    onChannelClick: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        var showChannelPicker by remember { mutableStateOf(false) }
        var showSubscribePicker by remember { mutableStateOf(false) }
        // Videos that credit two channels open a chooser instead of silently navigating
        // to the first one.
        val openChannel: () -> Unit = {
            if (session.channels.size > 1) showChannelPicker = true
            else session.channelId?.let(onChannelClick)
        }
        ChannelAvatarStack(
            channels = session.channels,
            primaryThumbnailUrl = session.channelAvatarUrl,
            size = 44.dp,
            onClick = openChannel,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = openChannel)
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = session.channelName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            session.subscriberCountText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showChannelPicker) {
            ChannelPickerDialog(
                channels = session.channels,
                onDismiss = { showChannelPicker = false },
                onSelect = { channel ->
                    showChannelPicker = false
                    channel.id?.let(onChannelClick)
                },
                primaryThumbnailUrl = session.channelAvatarUrl,
            )
        }

        if (showSubscribePicker) {
            ChannelPickerDialog(
                channels = session.channels,
                onDismiss = { showSubscribePicker = false },
                onSelect = { showSubscribePicker = false },
                primaryThumbnailUrl = session.channelAvatarUrl,
                subscribedChannelIds = subscribedChannelIds,
                onToggleSubscribe = { channel ->
                    channel.id?.let(onToggleChannelSubscribe)
                },
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val subscribeContainer by animateColorAsState(
            targetValue = if (isSubscribed) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.primary
            },
            animationSpec = tween(200),
            label = "subscribeContainer",
        )
        val subscribeContent by animateColorAsState(
            targetValue = if (isSubscribed) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
            animationSpec = tween(200),
            label = "subscribeContent",
        )
        Surface(
            // Two credited channels means two separate subscriptions, so the button asks
            // which one rather than silently following whichever happens to be first.
            onClick = {
                if (session.channels.size > 1) showSubscribePicker = true else onSubscribeClick()
            },
            shape = RoundedCornerShape(20.dp),
            color = subscribeContainer,
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 14.dp, end = 18.dp)
            ) {
                Icon(
                    painter = painterResource(if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = subscribeContent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = subscribeContent
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
    val context = LocalContext.current
    var showPlaylistDialog by remember { mutableStateOf(false) }

    // One scrollable line. These were weighted buttons sharing the available width,
    // which squeezed the labels until they were unreadable and left no room for
    // anything else; now each keeps its natural size and the row scrolls sideways.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            // Inside the scroll, so the first and last chips line up with the text
            // margins at rest but the row still scrolls all the way to the screen edge.
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LikeDislikePill(
            isLiked = isLiked,
            isDisliked = isDisliked,
            likeCountText = likeCountText,
        )
        ActionButton(
            icon = if (isSaved) R.drawable.library_add_check else R.drawable.library_add,
            label = stringResource(if (isSaved) R.string.video_player_saved else R.string.video_player_save),
            selected = isSaved,
            onClick = { VideoPlaybackManager.toggleSave() },
        )
        // Share, copy link and add to playlist used to sit behind an overflow dot, so
        // they were three taps away and the row could not grow to hold them. They are
        // buttons like the rest now.
        ActionButton(
            icon = R.drawable.share,
            label = stringResource(R.string.video_player_share),
            selected = false,
            onClick = { VideoPlaybackManager.shareVideo(context) }
        )
        ActionButton(
            icon = R.drawable.link,
            label = stringResource(R.string.video_player_copy_link),
            selected = false,
            onClick = { VideoPlaybackManager.copyVideoLink(context) }
        )
        ActionButton(
            icon = R.drawable.playlist_add,
            label = stringResource(R.string.video_player_add_to_playlist),
            selected = false,
            onClick = { showPlaylistDialog = true }
        )
        DownloadActionButton()
    }

    if (showPlaylistDialog) {
        AddToPlaylistVideoDialog(
            onDismiss = { showPlaylistDialog = false },
        )
    }
}

/**
 * Like and dislike as one segmented pill. They are a single opinion, so they read better
 * as two halves of one control than as two unrelated chips.
 */
@Composable
private fun LikeDislikePill(
    isLiked: Boolean,
    isDisliked: Boolean,
    likeCountText: String?,
) {
    val haptic = LocalHapticFeedback.current
    val selectedContainer = MaterialTheme.colorScheme.secondaryContainer
    val selectedContent = MaterialTheme.colorScheme.onSecondaryContainer
    val idleContent = MaterialTheme.colorScheme.onSurface
    val likeContainer by animateColorAsState(
        targetValue = if (isLiked) selectedContainer else Color.Transparent,
        animationSpec = tween(180),
        label = "likeContainer",
    )
    val dislikeContainer by animateColorAsState(
        targetValue = if (isDisliked) selectedContainer else Color.Transparent,
        animationSpec = tween(180),
        label = "dislikeContainer",
    )
    val likeLabel = stringResource(R.string.video_player_like)
    val dislikeLabel = stringResource(R.string.video_player_dislike)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.height(40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .background(likeContainer)
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        VideoPlaybackManager.toggleLike()
                    }
                    .padding(start = 14.dp, end = 12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_thumb_up),
                    contentDescription = likeLabel,
                    modifier = Modifier.size(18.dp),
                    tint = if (isLiked) selectedContent else idleContent,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = likeCountText?.takeIf { it.isNotBlank() } ?: likeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isLiked) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isLiked) selectedContent else idleContent,
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .background(dislikeContainer)
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        VideoPlaybackManager.toggleDislike()
                    }
                    .padding(start = 12.dp, end = 14.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_thumb_down),
                    contentDescription = dislikeLabel,
                    modifier = Modifier.size(18.dp),
                    tint = if (isDisliked) selectedContent else idleContent,
                )
            }
        }
    }
}

@Composable
private fun AddToPlaylistVideoDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            YouTube.library("FEmusic_liked_playlists").getOrNull()
        }
        playlists = result?.items.orEmpty().filterIsInstance<PlaylistItem>()
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.video_player_add_to_playlist))
        },
        text = {
            when {
                loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.video_player_queue_loading))
                    }
                }
                playlists.isEmpty() -> {
                    Text(stringResource(R.string.video_player_queue_empty))
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(playlists) { playlist ->
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        VideoPlaybackManager.addToPlaylist(playlist.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/**
 * Download control for the video on screen.
 *
 * Shows the real state of the download rather than a toggle that might lie: a progress
 * bar while it fetches, the stored size once it is on the device, and a tap then removes
 * it. The download plays from disk afterwards, so this is the same control for getting
 * a video and for taking it back off the device.
 */
@Composable
private fun DownloadActionButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = VideoPlaybackManager.uiState.collectAsState().value.session
    val videoId = session?.videoId

    LaunchedEffect(videoId) {
        if (videoId != null) VideoDownloadStore.load(context)
    }
    val entries by VideoDownloadStore.entries(context).collectAsState()
    val progressById by VideoDownloadStore.progress(context).collectAsState()

    var showDownloadDialog by remember { mutableStateOf(false) }

    // A download that announces itself needs permission to announce itself. Asked here,
    // at the moment the transfer is asked for, because asking once at startup and being
    // turned down means the prompt never comes round again - and every later download
    // then happens in silence, which reads as the feature not working. The transfer
    // happens either way: the permission only decides whether the system lets it say so.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    fun askForNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        runCatching {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val entry = videoId?.let { id -> entries.firstOrNull { it.videoId == id } }
    val progress = videoId?.let { progressById[it] }

    when {
        progress != null && !progress.done && !progress.failed -> {
            val fraction = if (progress.totalBytes > 0) {
                (progress.downloadedBytes.toFloat() / progress.totalBytes).coerceIn(0f, 1f)
            } else {
                0f
            }
            // Still an ActionButton rather than a dead chip: a transfer can be stopped
            // from here, and a failed one started again, so the control is the same one
            // whichever state the download is in.
            ActionButton(
                icon = R.drawable.download,
                label = when {
                    progress.totalBytes > 0 ->
                        "${(fraction * 100).toInt()}% • " +
                            "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)}"

                    else -> "${stringResource(R.string.video_downloading)} • " +
                        formatBytes(progress.downloadedBytes)
                },
                progress = fraction.takeIf { progress.totalBytes > 0 },
                selected = false,
                onClick = { VideoDownloadStore.cancel(context, videoId ?: return@ActionButton) },
            )
        }

        else -> {
            val downloading = progress?.done == false && progress.failed == false
            // The ring is only as good as the total it is measured against; a stream
            // that does not report its length gives a bar that jumps about, so the
            // figure is left out until one is known.
            val fraction = progress
                ?.takeIf { it.totalBytes > 0 && downloading }
                ?.let { (it.downloadedBytes.toFloat() / it.totalBytes).coerceIn(0f, 1f) }
            ActionButton(
                icon = if (entry != null) R.drawable.check else R.drawable.download,
                label = when {
                    entry != null -> stringResource(R.string.video_downloaded, formatBytes(entry.sizeBytes))
                    downloading && fraction != null ->
                        "${(fraction * 100).toInt()}%"
                    downloading -> stringResource(R.string.video_downloading)
                    progress?.failed == true -> stringResource(R.string.video_download_failed)
                    else -> stringResource(R.string.video_download)
                },
                progress = fraction,
                selected = entry != null,
                // Asks what to fetch rather than taking a quality the user never chose -
                // the same choice YouTube makes, and the one that decides how much of
                // their storage this video takes. A failed download is retried at the
                // quality it was asked for, so the label above is telling the truth.
                onClick = {
                    if (progress?.failed == true && videoId != null) {
                        VideoDownloadStore.retry(context, videoId)
                    } else {
                        showDownloadDialog = true
                    }
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
            },
            onDownload = { quality, codec ->
                val id = videoId ?: return@VideoDownloadDialog
                showDownloadDialog = false
                askForNotificationPermissionIfNeeded()
                scope.launch {
                    // Resolved at the chosen quality and codec, and passed explicitly
                    // rather than through the playback preference: what gets stored on
                    // the device is this choice, and it must not change how the video
                    // currently playing streams.
                    val source = AuraPlayerUtils.getVideoStreamSource(id, quality, codec).getOrNull()
                    if (source == null) {
                        // Nothing to fetch and no transfer to start, so the button would
                        // sit on "Download" with no explanation. Say why instead.
                        Toast.makeText(
                            context,
                            R.string.video_download_unavailable,
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@launch
                    }
                    // The record of what was asked for is written by the store before
                    // the service is started, so a transfer that outlives this process
                    // still knows what it is fetching and can say so.
                    VideoDownloadStore.download(
                        context,
                        VideoDownloadStore.Request(
                            videoId = id,
                            title = session?.title.orEmpty(),
                            channelName = session?.channelName.orEmpty(),
                            thumbnailUrl = session?.videoThumbnail,
                            quality = quality,
                            codec = codec,
                            durationMs = VideoPlaybackManager.uiState.value.durationMs,
                        ),
                    )
                }
            },
        )
    }
}

/**
 * Resolution and codec, as one control.
 *
 * The two belong together because they answer the same question - which stream to ask
 * for - and a stream picker that hid one of them would leave people changing one and
 * assuming the other followed. The settings panel and the download dialog share this,
 * so the choice looks and behaves the same in both.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoFormatPicker(
    quality: VideoQuality,
    codec: VideoCodec,
    onQualityChange: (VideoQuality) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    modifier: Modifier = Modifier,
    /** Wraps the whole picker in scrolling when it is the dialog's only content. */
    scrollable: Boolean = false,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Text(
            text = stringResource(R.string.video_quality),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Wraps rather than scrolls sideways: nine resolutions and four codecs do not
        // fit a phone in one line, and a sideways-scrolling row hides half the ladder.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            VideoQuality.entries.forEach { entry ->
                FormatChip(
                    label = entry.label,
                    selected = entry == quality,
                    onClick = { onQualityChange(entry) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.video_codec),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            VideoCodec.entries.forEach { entry ->
                FormatChip(
                    label = entry.label,
                    selected = entry == codec,
                    onClick = { onCodecChange(entry) },
                )
            }
        }
        if (codec != VideoCodec.CODEC_AUTO) {
            Text(
                text = stringResource(R.string.video_codec_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (scrollable) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun FormatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Download options for the current video: one entry per available quality, and a way to
 * take an existing download back off the device.
 */
@Composable
internal fun VideoDownloadDialog(
    existing: VideoDownloadStore.Entry?,
    onDismiss: () -> Unit,
    onRemove: (VideoDownloadStore.Entry) -> Unit,
    onDownload: (VideoQuality, VideoCodec) -> Unit,
) {
    // Starts on what is already chosen for playback: it is the choice most people would
    // make again, and it makes the dialog's default visible instead of implied.
    var quality by remember { mutableStateOf(VideoPlaybackManager.uiState.value.videoQuality) }
    var codec by remember { mutableStateOf(VideoPlaybackManager.uiState.value.videoCodec) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.video_download)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (existing != null) {
                    Text(
                        text = stringResource(R.string.video_downloaded, formatBytes(existing.sizeBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { onRemove(existing) }) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.remove_download))
                    }
                    HorizontalDivider()
                }
                VideoFormatPicker(
                    quality = quality,
                    codec = codec,
                    onQualityChange = { quality = it },
                    onCodecChange = { codec = it },
                    scrollable = true,
                    modifier = Modifier.heightIn(max = 320.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDownload(quality, codec) }) {
                Text(stringResource(R.string.video_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format("%.1f GB", mb / 1024.0) else String.format("%.0f MB", mb)
}

@Composable
private fun ActionButton(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * How far along the work is, 0 to 1. Null when there is none. The button draws a
     * ring instead of its icon while set: a download that reports nothing until it
     * finishes looks identical to one that has not started, and a video takes long
     * enough for that to be the whole of the wait.
     */
    progress: Float? = null,
) {
    val haptic = LocalHapticFeedback.current
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(180),
        label = "actionContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(180),
        label = "actionContent",
    )
    Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = container,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(start = 14.dp, end = 16.dp)
        ) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                )
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = content
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(animationSpec = tween(220))
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)
        ) {
            // Descriptions come straight from the API as plain text, so any URL in them is
            // otherwise dead - the link is made tappable and handed to the system handler.
            Text(
                text = linkifiedText(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (description.length > 100 || expanded) {
                TextButton(
                    onClick = onToggle,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(if (expanded) R.string.video_player_show_less else R.string.video_player_show_more),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        painter = painterResource(if (expanded) R.drawable.expand_less else R.drawable.expand_more),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private enum class DetailTab(val labelRes: Int) {
    UpNext(R.string.video_player_up_next),
    Comments(R.string.video_player_comments),
}

@Composable
private fun VideoTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    // Pill tabs rather than an underlined tab row: two choices do not need a strip
    // spanning the screen, and the pills match the action chips just above them.
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
    ) {
        DetailTab.entries.forEach { tab ->
            val selected = selectedTab == tab.ordinal
            val container by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = tween(180),
                label = "tabContainer",
            )
            val content by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(180),
                label = "tabContent",
            )
            Surface(
                selected = selected,
                onClick = { onTabSelected(tab.ordinal) },
                shape = RoundedCornerShape(20.dp),
                color = container,
                modifier = Modifier.height(40.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = content,
                    )
                }
            }
        }
    }
}

/** Centered progress block shared by the comments / Up next loading states. */
@Composable
private fun ListSpinnerBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun CommentsErrorBlock(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        TextButton(onClick = { VideoPlaybackManager.retryLoadingComments() }) {
            Text(
                text = stringResource(R.string.video_player_retry),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * One top-level comment plus its replies. The replies are lazily loaded the
 * first time the row is expanded, and rendered inset under their parent so the
 * reply chain is obvious at a glance.
 */
@Composable
private fun CommentThread(
    comment: CommentItem,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val loaded = comment.replies.size
    val claimed = comment.replyCount?.toIntOrNull() ?: 0
    val hasUnloadedReplies = claimed > loaded
    Column(modifier = Modifier.fillMaxWidth()) {
        CommentRow(
            comment = comment,
            pinnedStyle = comment.isPinned,
        )
        when {
            // Nothing fetched yet: offer the tap that starts the request.
            hasUnloadedReplies && loaded == 0 -> Text(
                text = stringResource(
                    R.string.video_player_show_replies,
                    comment.replyCount.orEmpty(),
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 64.dp, top = 2.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        onExpandedChange(true)
                        VideoPlaybackManager.loadCommentReplies(comment.commentId)
                    },
            )
            // Something is loaded, so the row can always be folded away.
            loaded > 0 -> Text(
                text = if (expanded) {
                    stringResource(R.string.video_player_hide_replies, loaded)
                } else {
                    stringResource(R.string.video_player_show_replies, loaded)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 64.dp, top = 2.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        if (!expanded && hasUnloadedReplies) {
                            VideoPlaybackManager.loadCommentReplies(comment.commentId)
                        }
                        onExpandedChange(!expanded)
                    },
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                comment.replies.forEach { reply ->
                    CommentRow(comment = reply, indented = true)
                }
                when {
                    comment.isLoadingReplies -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 64.dp)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    }
                    // Only the first reply page is auto-loaded, so anything past it
                    // needs an explicit tap.
                    !comment.repliesExhausted -> Text(
                        text = stringResource(R.string.video_player_more_replies),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 64.dp, top = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                VideoPlaybackManager.loadCommentReplies(
                                    commentId = comment.commentId,
                                    append = true,
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    indented: Boolean = false,
    pinnedStyle: Boolean = false,
) {
    val startPadding = if (indented) 64.dp else 16.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (pinnedStyle) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                } else {
                    Color.Transparent
                }
            )
            .padding(start = startPadding, end = 16.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(if (indented) 28.dp else 36.dp)
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
                    style = if (indented) {
                        MaterialTheme.typography.bodySmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = comment.authorName,
                        style = if (indented) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (comment.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.video_player_pinned),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Likes sit opposite the author rather than on their own line, which
                // is what keeps the row scannable.
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
                text = comment.content,
                style = if (indented) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            comment.publishedTime?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun AutoplayToggleRow(uiState: VideoPlaybackManager.UiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.video_player_autoplay_next),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.video_player_autoplay_next_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = uiState.autoplayEnabled,
                    onCheckedChange = { VideoPlaybackManager.setAutoplayEnabled(it) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun RecommendationSkeleton() {
    // Shaped like the rows it stands in for, so the list does not jump when they arrive.
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(152.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(placeholder)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(placeholder)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(placeholder)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoRecommendationsBlock() {
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

@Composable
private fun UpNextRow(
    item: RecommendationItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    hasPlayed: Boolean = false,
) {
    // The playing row is lifted onto a tonal card so "where am I in the queue" reads at a
    // glance; the others get the same rounded ripple area without the fill.
    val rowContainer by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(220),
        label = "upNextRowContainer",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(rowContainer)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(152.dp)
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pause),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            } else if (hasPlayed) {
                // Watched already: the picture is dimmed and marked, so a video that
                // has been round once does not read as something still to come.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = stringResource(R.string.video_player_watched),
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.channelName.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!isPlaying) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = stringResource(R.string.more_options),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
    // The same two preferences Player settings writes - the sheet is the second way into
    // them, so the video player can be told to show captions without leaving playback.
    val (subtitlesEnabled, onSubtitlesEnabledChange) = rememberPreference(SubtitlesEnabledKey, true)
    val (subtitleLanguage, onSubtitleLanguageChange) = rememberPreference(SubtitleLanguageKey, "auto")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    // The sheet is pinned to the bottom edge, so without the navigation bar
                    // inset the system bar covers its last row of controls. Applied inside
                    // the sheet so its colour still runs down behind the bar.
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp)
            ) {
                // Grab handle, the usual cue that this is a sheet over the player.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(12.dp))
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

                VideoFormatPicker(
                    quality = uiState.videoQuality,
                    codec = uiState.videoCodec,
                    onQualityChange = { VideoPlaybackManager.setVideoFormat(it, uiState.videoCodec) },
                    onCodecChange = { VideoPlaybackManager.setVideoFormat(uiState.videoQuality, it) },
                )

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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.closed_captions),
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
                        true to stringResource(R.string.captions_on),
                        false to stringResource(R.string.captions_off),
                    ).forEach { (enabled, label) ->
                        Surface(
                            onClick = { onSubtitlesEnabledChange(enabled) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (subtitlesEnabled == enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (subtitlesEnabled == enabled) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.subtitle_language),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    SubtitleLanguageOptions.forEach { (code, name) ->
                        Surface(
                            onClick = { onSubtitleLanguageChange(code) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (subtitleLanguage == code) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (subtitleLanguage == code) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
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

/**
 * Which view type backs the video. A SurfaceView is a separate layer that the compositor has
 * to merge with the app's window on every frame, and while one is on screen the whole window
 * pays that merge cost - scrolling, animations, everything. A TextureView renders the video as
 * an ordinary texture inside the window, so the UI composites in one pass. TextureView is the
 * default; SurfaceView is kept for Android 14+, where the platform's TextureView has known
 * regressions around surface re-creation.
 */
private fun videoSurfaceLayoutRes(): Int =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        R.layout.video_player_view_surface
    } else {
        R.layout.video_player_view
    }

@OptIn(UnstableApi::class)
@Composable
private fun AndroidVideoSurface(player: ExoPlayer, resizeModeOverride: Int) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            (android.view.LayoutInflater.from(ctx)
                .inflate(videoSurfaceLayoutRes(), null, false) as PlayerView).apply {
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
            // Only rebind when something actually changed. Handing PlayerView the same player
            // again makes it rebuild its component listeners and surface plumbing, which is
            // pure work: this update runs on every recomposition of the surface.
            if (view.player !== player) {
                view.player = player
            }
            if (view.resizeMode != resizeModeOverride) {
                view.resizeMode = resizeModeOverride
            }
        },
        onRelease = { view ->
            // Detach from the player: media3's PlayerView registers a component
            // listener it never unregisters, so every expand/collapse cycle leaked
            // a PlayerView + SurfaceView onto the shared player.
            view.player = null
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

/**
 * How long before the end the next video is offered as a tile. Long enough to see it,
 * short enough that acting on it is still a choice rather than a fait accompli.
 */
private const val UP_NEXT_TILE_WINDOW_MS = 40_000L
