/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.player

import android.widget.Toast
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import com.auramusic.innertube.YouTube
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.auramusic.app.LocalListenTogetherManager
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.CropAlbumArtKey
import com.auramusic.app.constants.HidePlayerThumbnailKey
import com.auramusic.app.constants.PlayerBackgroundStyle
import com.auramusic.app.constants.PlayerBackgroundStyleKey
import com.auramusic.app.constants.PlayerHorizontalPadding
import com.auramusic.app.constants.SeekExtraSeconds
import com.auramusic.app.constants.SubtitlesEnabledKey
import com.auramusic.app.constants.SubtitleLanguageKey
import com.auramusic.app.constants.SwipeThumbnailKey
import com.auramusic.app.constants.ThumbnailCornerRadius
import com.auramusic.app.constants.VideoLyricsEnabledKey
import com.auramusic.app.constants.VideoQuality
import com.auramusic.app.constants.VideoQualityKey
import com.auramusic.app.listentogether.RoomRole
import com.auramusic.app.ui.component.CastButton
import com.auramusic.app.utils.FlowPlayerUtils
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Pre-calculated thumbnail dimensions to avoid repeated calculations during recomposition.
 * All values are computed once and cached.
 */
@Immutable
data class ThumbnailDimensions(
    val itemWidth: Dp,
    val containerSize: Dp,
    val thumbnailSize: Dp,
    val cornerRadius: Dp
)

/**
 * Cached media items data to prevent recalculation on every recomposition.
 */
@Immutable
data class MediaItemsData(
    val items: List<MediaItem>,
    val currentIndex: Int
)

/**
 * Calculate thumbnail dimensions once based on container size.
 * This function is marked as @Stable to indicate it produces stable results.
 * In landscape mode, uses the smaller dimension (height) to ensure square thumbnail fits.
 */
@Stable
private fun calculateThumbnailDimensions(
    containerWidth: Dp,
    containerHeight: Dp = containerWidth,
    horizontalPadding: Dp = PlayerHorizontalPadding,
    cornerRadius: Dp = ThumbnailCornerRadius,
    isLandscape: Boolean = false
): ThumbnailDimensions {
    // In landscape, use height as the constraining dimension for a square thumbnail
    val effectiveSize = if (isLandscape) {
        minOf(containerWidth, containerHeight) - (horizontalPadding * 2)
    } else {
        containerWidth - (horizontalPadding * 2)
    }
    return ThumbnailDimensions(
        itemWidth = containerWidth,
        containerSize = containerWidth,
        thumbnailSize = effectiveSize,
        cornerRadius = cornerRadius * 2
    )
}

/**
 * Get media items for the thumbnail carousel.
 * Calculates previous, current, and next items based on shuffle mode.
 */
@Stable
private fun getMediaItems(
    player: Player,
    swipeThumbnail: Boolean
): MediaItemsData {
    val timeline = player.currentTimeline
    val currentIndex = player.currentMediaItemIndex
    val shuffleModeEnabled = player.shuffleModeEnabled
    
    val currentMediaItem = try {
        player.currentMediaItem
    } catch (e: Exception) { null }
    
    val previousMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (previousIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(previousIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val nextMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (nextIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(nextIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val items = listOfNotNull(previousMediaItem, currentMediaItem, nextMediaItem)
    val currentMediaIndex = items.indexOf(currentMediaItem)
    
    return MediaItemsData(items, currentMediaIndex)
}

/**
 * Get text color based on player background style.
 * Computed once per background style change.
 */
@Stable
@Composable
private fun getTextColor(playerBackground: PlayerBackgroundStyle): Color {
    return when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: () -> Boolean = { true },
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    // Collect states
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    
    // Video mode state
    val videoModeEnabled by playerConnection.videoModeEnabled.collectAsState()
    val availableSubtitles by playerConnection.availableSubtitles.collectAsState()
    val selectedSubtitleIndex by playerConnection.selectedSubtitleIndex.collectAsState()
    val subtitlesEnabled by rememberPreference(SubtitlesEnabledKey, true)

    // Preferences - computed once
    // Disable swipe for Listen Together guests
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    
    // Pre-calculate text color based on background style
    val textBackgroundColor = getTextColor(playerBackground)
    
    // Grid state
    val thumbnailLazyGridState = rememberLazyGridState()
    
    // Calculate media items data - memoized
    val mediaItemsData by remember(
        playerConnection.player.currentMediaItemIndex,
        playerConnection.player.shuffleModeEnabled,
        swipeThumbnail,
        mediaMetadata
    ) {
        derivedStateOf {
            getMediaItems(playerConnection.player, swipeThumbnail)
        }
    }
    
    val mediaItems = mediaItemsData.items
    val currentMediaIndex = mediaItemsData.currentIndex

    // Snap behavior - created once per grid state
    val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
        ThumbnailSnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize / 2f - itemSize / 2f)
            },
            velocityThreshold = 500f
        )
    }

    // Current item tracking - derived state for efficiency
    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    // Handle swipe to change song
    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail || itemScrollOffset != 0 || currentMediaIndex < 0) return@LaunchedEffect

        if (currentItem > currentMediaIndex && canSkipNext) {
            playerConnection.player.seekToNext()
        } else if (currentItem < currentMediaIndex && canSkipPrevious) {
            playerConnection.player.seekToPreviousMediaItem()
        }
    }

    // Update position when song changes
    LaunchedEffect(mediaMetadata, canSkipPrevious, canSkipNext) {
        val index = maxOf(0, currentMediaIndex)
        if (index >= 0 && index < mediaItems.size) {
            try {
                thumbnailLazyGridState.animateScrollToItem(index)
            } catch (e: Exception) {
                thumbnailLazyGridState.scrollToItem(index)
            }
        }
    }

    LaunchedEffect(playerConnection.player.currentMediaItemIndex) {
        val index = mediaItemsData.currentIndex
        if (index >= 0 && index != currentItem) {
            thumbnailLazyGridState.scrollToItem(index)
        }
    }

    // Seek effect state
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .graphicsLayer {
                // Use hardware layer for entire Thumbnail to ensure smooth 120Hz animations
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        // Error view
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    retry = playerConnection.player::prepare,
                )
            }
        }

        // Main thumbnail view
        AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isLandscape) Modifier.statusBarsPadding() else Modifier),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
            ) {
                // Now Playing header - hide in landscape mode
                if (!isLandscape) {
                    ThumbnailHeader(
                        queueTitle = queueTitle,
                        albumTitle = mediaMetadata?.album?.title,
                        textColor = textBackgroundColor
                    )
                }
                
                // Thumbnail content
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = if (isLandscape) {
                        Modifier.weight(1f, false)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    // Calculate dimensions once per size change, considering landscape mode
                    val dimensions = remember(maxWidth, maxHeight, isLandscape) {
                        calculateThumbnailDimensions(
                            containerWidth = maxWidth,
                            containerHeight = maxHeight,
                            isLandscape = isLandscape
                        )
                    }

                    // Remember the onSeek callback to prevent recomposition
                    val onSeekCallback = remember {
                        { direction: String, showEffect: Boolean ->
                            seekDirection = direction
                            showSeekEffect = showEffect
                        }
                    }
                    
                    // Derive scroll enabled state to prevent unnecessary recomposition
                    val isScrollEnabled by remember(swipeThumbnail) {
                        derivedStateOf { swipeThumbnail && isPlayerExpanded() }
                    }
                    
                    LazyHorizontalGrid(
                        state = thumbnailLazyGridState,
                        rows = GridCells.Fixed(1),
                        flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                        userScrollEnabled = isScrollEnabled,
                        modifier = when {
                            videoModeEnabled -> Modifier.fillMaxSize() // Video fills entire player area
                            isLandscape -> Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                            else -> Modifier.fillMaxSize()
                        }
                    ) {
                        items(
                            items = mediaItems,
                            key = { item -> 
                                item.mediaId.ifEmpty { "unknown_${item.hashCode()}" }
                            }
                        ) { item ->
                            ThumbnailItem(
                                item = item,
                                dimensions = dimensions,
                                hidePlayerThumbnail = hidePlayerThumbnail,
                                cropAlbumArt = cropAlbumArt,
                                textBackgroundColor = textBackgroundColor,
                                layoutDirection = layoutDirection,
                                onSeek = onSeekCallback,
                                playerConnection = playerConnection,
                                context = context,
                                isLandscape = isLandscape,
                                isListenTogetherGuest = isListenTogetherGuest,
                                currentMediaId = mediaMetadata?.id,
                                currentMediaThumbnail = mediaMetadata?.thumbnailUrl,
                                videoModeEnabled = videoModeEnabled,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header component showing "Now Playing" and queue/album title.
 */
@Composable
private fun ThumbnailHeader(
    queueTitle: String?,
    albumTitle: String?,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
        ) {
            // Listen Together indicator
            if (listenTogetherRoleState?.value != RoomRole.NONE) {
                Text(
                    text = if (listenTogetherRoleState?.value == RoomRole.HOST) "Hosting Listen Together" else "Listening Together",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            } else {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            }
            val playingFrom = queueTitle ?: albumTitle
            if (!playingFrom.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playingFrom,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

/**
 * Individual thumbnail item in the carousel.
 */
@Composable
private fun ThumbnailItem(
    item: MediaItem,
    dimensions: ThumbnailDimensions,
    hidePlayerThumbnail: Boolean,
    cropAlbumArt: Boolean,
    textBackgroundColor: Color,
    layoutDirection: LayoutDirection,
    onSeek: (String, Boolean) -> Unit,
    playerConnection: com.auramusic.app.playback.PlayerConnection,
    context: android.content.Context,
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    currentMediaId: String? = null,
    currentMediaThumbnail: String? = null,
    videoModeEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)
    var skipMultiplier by remember { mutableIntStateOf(1) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .then(
                if (isLandscape) {
                    Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                } else {
                    Modifier
                        .width(dimensions.itemWidth)
                        .fillMaxSize()
                }
            )
            .padding(horizontal = PlayerHorizontalPadding)
            .then(
                if (videoModeEnabled) Modifier
                else Modifier.graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (isListenTogetherGuest) return@detectTapGestures

                        val currentPosition = playerConnection.player.currentPosition
                        val duration = playerConnection.player.duration

                        val now = System.currentTimeMillis()
                        if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                            skipMultiplier++
                        } else {
                            skipMultiplier = 1
                        }
                        lastTapTime = now

                        val skipAmount = 5000 * skipMultiplier

                        val isLeftSide = (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)

                        if (isLeftSide) {
                            playerConnection.player.seekTo((currentPosition - skipAmount).coerceAtLeast(0))
                            onSeek(context.getString(R.string.seek_backward_dynamic, skipAmount / 1000), true)
                        } else {
                            playerConnection.player.seekTo((currentPosition + skipAmount).coerceAtMost(duration))
                            onSeek(context.getString(R.string.seek_forward_dynamic, skipAmount / 1000), true)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (videoModeEnabled && item.mediaId == currentMediaId) {
                        Modifier.fillMaxSize() // Video fills entire player area
                    } else {
                        Modifier.size(dimensions.thumbnailSize)
                    }
                )
                .clip(RoundedCornerShape(dimensions.cornerRadius))
        ) {
            if (hidePlayerThumbnail) {
                HiddenThumbnailPlaceholder(textBackgroundColor = textBackgroundColor)
            } else {
                val artworkUriToUse = if (item.mediaId == currentMediaId && !currentMediaThumbnail.isNullOrBlank()) {
                    currentMediaThumbnail
                } else {
                    item.mediaMetadata.artworkUri?.toString()
                }

                ThumbnailImage(
                    artworkUri = artworkUriToUse,
                    cropArtwork = cropAlbumArt,
                    videoModeEnabled = videoModeEnabled && item.mediaId == currentMediaId,
                )
            }
            
            // Cast button at top corner of thumbnail
            CastButton(
                modifier = Modifier
                    .align(if (videoModeEnabled) Alignment.TopStart else Alignment.TopEnd)
                    .padding(8.dp),
                tintColor = textBackgroundColor
            )
        }
    }
}

/**
 * Placeholder shown when thumbnail is hidden.
 */
@Composable
private fun HiddenThumbnailPlaceholder(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = stringResource(R.string.hide_player_thumbnail),
            tint = textBackgroundColor.copy(alpha = 0.7f),
            modifier = Modifier.size(120.dp)
        )
    }
}

/**
 * Actual thumbnail image with caching and hardware layer rendering.
 * Also displays video when video mode is enabled with enhanced controls.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThumbnailImage(
    artworkUri: String?,
    cropArtwork: Boolean,
    videoModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val isVideoSwitching by playerConnection.isVideoSwitching.collectAsState()
    val (subtitlesEnabled, onSubtitlesEnabledChange) = rememberPreference(SubtitlesEnabledKey, true)
    
    // [4] Auto-hide controls state
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // [6] Pinch-to-zoom resize mode
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    
    // [8] Brightness/Volume gesture state
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }
    var volumeLevel by remember { mutableFloatStateOf(1f) }
    
    // Initialize brightness from system
    LaunchedEffect(Unit) {
        try {
            val systemBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 128
            )
            brightnessLevel = systemBrightness / 255f
        } catch (_: Exception) {}
        volumeLevel = player.volume
    }
    
    // [4] Auto-hide controls after 3 seconds
    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls && videoModeEnabled) {
            delay(3000)
            if (System.currentTimeMillis() - lastInteractionTime >= 2900) {
                showControls = false
            }
        }
    }
    
    // [7] Progress tracking for gradient bar
    var playerPosition by remember { mutableLongStateOf(0L) }
    var playerDuration by remember { mutableLongStateOf(1L) }
    LaunchedEffect(videoModeEnabled) {
        if (videoModeEnabled) {
            while (isActive) {
                playerPosition = player.currentPosition
                playerDuration = player.duration.coerceAtLeast(1L)
                delay(200)
            }
        } else {
            playerPosition = 0L
            playerDuration = 1L
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (videoModeEnabled) Modifier
                else Modifier.graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
            )
            .background(if (videoModeEnabled) Color.Black else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (isVideoSwitching && !videoModeEnabled) {
            // Show loading animation while video is being fetched
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator()
            }
        } else if (videoModeEnabled) {
            // Video player view - fix: use RESIZE_MODE_ZOOM for full fill
            // Native ExoPlayer subtitle rendering
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false // We use our own controls
                        this.resizeMode = resizeMode
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        // Show subtitle button to let users toggle captions
                        setShowSubtitleButton(true)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { playerView ->
                    playerView.player = player
                    playerView.resizeMode = resizeMode
                    playerView.requestLayout()
                }
            )
            
            // [4] Tap to show/hide controls + [8] Brightness/Volume swipe gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                lastInteractionTime = System.currentTimeMillis()
                                showControls = true
                                val isLeftSide = offset.x < size.width / 2
                                if (isLeftSide) showBrightnessOverlay = true
                                else showVolumeOverlay = true
                            },
                            onDragEnd = {
                                showBrightnessOverlay = false
                                showVolumeOverlay = false
                            },
                            onDragCancel = {
                                showBrightnessOverlay = false
                                showVolumeOverlay = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount.y / size.height
                                if (showBrightnessOverlay) {
                                    brightnessLevel = (brightnessLevel + delta).coerceIn(0.01f, 1f)
                                    val window = (context as? android.app.Activity)?.window
                                    window?.let {
                                        val params = it.attributes
                                        params.screenBrightness = brightnessLevel
                                        it.attributes = params
                                    }
                                } else if (showVolumeOverlay) {
                                    volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                                    player.volume = volumeLevel
                                }
                            }
                        )
                    }
            )
            
            // [8] Brightness overlay (left side)
            AnimatedVisibility(
                visible = showBrightnessOverlay,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                GestureIndicator(
                    icon = R.drawable.contrast,
                    level = brightnessLevel,
                    label = "${(brightnessLevel * 100).roundToInt()}%"
                )
            }
            
            // [8] Volume overlay (right side)
            AnimatedVisibility(
                visible = showVolumeOverlay,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                GestureIndicator(
                    icon = R.drawable.volume_up,
                    level = volumeLevel,
                    label = "${(volumeLevel * 100).roundToInt()}%"
                )
            }
            
            // [7] Video progress gradient bar at top
            val progress by animateFloatAsState(
                targetValue = if (playerDuration > 0) playerPosition.toFloat() / playerDuration.toFloat() else 0f,
                animationSpec = tween(250, easing = LinearEasing),
                label = "progress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Background track
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.2f))
                )
                // Progress with gradient
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                )
            }
            
            // [4] Auto-hide settings button
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(400)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                VideoSettingsButton(
                    resizeMode = resizeMode,
                    onResizeModeChange = { resizeMode = it },
                    onInteraction = { lastInteractionTime = System.currentTimeMillis() },
                    subtitlesEnabled = subtitlesEnabled,
                    onSubtitlesToggle = { onSubtitlesEnabledChange(!subtitlesEnabled) }
                )
            }
            
            VideoLyricsOverlay(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
            

        } else {
            // Album art image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = if (cropArtwork) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * [8] Gesture indicator for brightness/volume swipe
 */
@Composable
private fun GestureIndicator(
    icon: Int,
    level: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(8.dp))
        // Vertical progress bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(80.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(level)
                    .align(Alignment.BottomCenter)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Single settings button for video controls.
 * Contains video quality, resize mode (pinch-to-zoom) options.
 * YouTube-style compact menu design
 */
@Composable
private fun VideoSettingsButton(
    resizeMode: Int,
    onResizeModeChange: (Int) -> Unit,
    onInteraction: () -> Unit,
    subtitlesEnabled: Boolean = true,
    onSubtitlesToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    val (videoQuality, onVideoQualityChange) = rememberEnumPreference(
        VideoQualityKey,
        defaultValue = VideoQuality.QUALITY_720P
    )

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                expanded = true
                showQualityMenu = false
                onInteraction()
            },
            modifier = Modifier
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = "Video settings",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        if (!showQualityMenu) {
            // Main menu - YouTube style
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Quality option
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.settings),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                    tint = Color.White
                                )
                                Text("Quality")
                            }
                            Text(
                                text = when (videoQuality) {
                                    VideoQuality.QUALITY_360P -> "360p"
                                    VideoQuality.QUALITY_480P -> "480p"
                                    VideoQuality.QUALITY_720P -> "720p"
                                    VideoQuality.QUALITY_1080P -> "1080p"
                                },
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    },
onClick = {
                        showQualityMenu = true
                    }
                )
                
                // Captions toggle
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_subtitles),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                    tint = Color.White
                                )
                                Text(stringResource(R.string.captions))
                            }
                            Text(
                                text = if (subtitlesEnabled) stringResource(R.string.captions_on) else stringResource(R.string.captions_off),
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        onSubtitlesToggle()
                        expanded = false
                    }
                )
                
                // Video fit option
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.fullscreen),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                    tint = Color.White
                                )
                                Text("Video fit")
                            }
                            Text(
                                text = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                                    else -> "Fit"
                                },
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        // Cycle between Fit and Stretch
                        val nextMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        onResizeModeChange(nextMode)
                        expanded = false
                    }
                )
            }
        } else {
            // Quality submenu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { 
                    expanded = false
                    showQualityMenu = false
                }
            ) {
                // Back button
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                tint = Color.White
                            )
                            Text("Quality")
                        }
                    },
                    onClick = {
                        showQualityMenu = false
                    }
                )
                
                VideoQuality.entries.forEach { quality ->
                    val label = when (quality) {
                        VideoQuality.QUALITY_360P -> "360p"
                        VideoQuality.QUALITY_480P -> "480p"
                        VideoQuality.QUALITY_720P -> "720p"
                        VideoQuality.QUALITY_1080P -> "1080p"
                    }
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label)
                                if (quality == videoQuality) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onVideoQualityChange(quality)
                            FlowPlayerUtils.setPreferredVideoQuality(quality)
                            expanded = false
                            showQualityMenu = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * [5] Enhanced seek effect overlay with ripple circles and seek arrows
 */
@Composable
private fun SeekEffectOverlay(
    seekDirection: String,
    isForward: Boolean = true,
    modifier: Modifier = Modifier
) {
    val rippleAlpha = remember { Animatable(0.6f) }
    val rippleScale = remember { Animatable(0.5f) }
    
    LaunchedEffect(seekDirection) {
        rippleScale.snapTo(0.5f)
        rippleAlpha.snapTo(0.6f)
        launch { rippleScale.animateTo(1.5f, tween(600)) }
        rippleAlpha.animateTo(0f, tween(600))
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Ripple circle
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = rippleScale.value
                    scaleY = rippleScale.value
                    alpha = rippleAlpha.value
                }
        ) {
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = size.minDimension / 2
            )
        }
        // Seek info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isForward) R.drawable.fast_forward else R.drawable.fast_forward
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { if (!isForward) scaleX = -1f }
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = seekDirection,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Video lyrics overlay displayed at bottom like YouTube subtitles.
 * Enabled when video is playing and subtitles are toggled on. Features:
 * [1] Animated fade transitions between lyric lines
 * [2] Next lyric line preview
 * [3] Glow/shadow effect for readability
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoLyricsOverlay(
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val player = playerConnection.player

    // Check if video mode is enabled
    val videoModeEnabled by playerConnection.videoModeEnabled.collectAsState()
    if (!videoModeEnabled) return

    // Check if video subtitles are enabled
    val (videoLyricsEnabled, _) = rememberPreference(
        VideoLyricsEnabledKey,
        defaultValue = true
    )
    val subtitleLanguage by rememberPreference(
        SubtitleLanguageKey,
        defaultValue = "auto"
    )

    if (!videoLyricsEnabled) return

    // Fetch YouTube subtitles: try caption tracks first (like SmartTube), fallback to transcript
    var transcriptText by remember { mutableStateOf<String?>(null) }
    var isLoadingCaptions by remember { mutableStateOf(false) }
    var captionError by remember { mutableStateOf<String?>(null) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }

    // Track which video IDs we've already attempted to fetch
    var attemptedVideoIds by remember { mutableStateOf(setOf<String>()) }

    // Use the actual video ID being played
    val activeVideoId by playerConnection.currentVideoId.collectAsState()

    // Only fetch captions when video ID actually changes AND video mode is enabled
    LaunchedEffect(activeVideoId, subtitleLanguage) {
        // Only proceed if video mode is enabled and we have a video ID
        val videoId = activeVideoId ?: return@LaunchedEffect
        
        // Skip if already attempted for this video ID
        if (videoId in attemptedVideoIds) {
            timber.log.Timber.d("VideoLyricsOverlay: Already attempted for videoId=$videoId, skipping")
            return@LaunchedEffect
        }
        
        timber.log.Timber.d("VideoLyricsOverlay: Fetching captions for videoId=$videoId")
        
        // Add to attempted set first to prevent re-fetching
        attemptedVideoIds = attemptedVideoIds + videoId
        
        transcriptText = null
        captionError = null
        isLoadingCaptions = true
        videoDurationMs = 0L
        
        delay(500) // Slightly longer delay to ensure player is ready
        withContext(Dispatchers.IO) {
            try {
                // Try caption tracks first (extracted from player response like SmartTube)
                // Use version that returns video duration for proper VTT conversion
                val captionResult = YouTube.getCaptionTracksWithDuration(videoId)
                val captionResultPair = captionResult.getOrNull()
                val captionTracks = captionResultPair?.first
                videoDurationMs = captionResultPair?.second ?: 0L
                timber.log.Timber.d("VideoLyricsOverlay: Caption tracks found: ${captionTracks?.size ?: 0}, duration: ${videoDurationMs}ms")
                
                if (!captionTracks.isNullOrEmpty()) {
                    // Prefer user-selected language, then English, then auto-generated, then first available
                    val captionTrack = if (subtitleLanguage != "auto") {
                        captionTracks.firstOrNull { it.languageCode == subtitleLanguage }
                    } else null
                        ?: captionTracks.firstOrNull { it.languageCode == "en" }
                        ?: captionTracks.firstOrNull { it.kind == "asr" }
                        ?: captionTracks.firstOrNull()
                    if (captionTrack != null) {
                        timber.log.Timber.d("VideoLyricsOverlay: Using caption track: lang=${captionTrack.languageCode}, kind=${captionTrack.kind}")
                        val fetchResult = YouTube.fetchSubtitleFromCaptionTrack(captionTrack.baseUrl)
                        fetchResult.onSuccess { text ->
                            timber.log.Timber.d("VideoLyricsOverlay: Caption text length: ${text.length}")
                            if (text.isNotEmpty()) {
                                transcriptText = text
                            }
                        }.onFailure { e ->
                            timber.log.Timber.e(e, "VideoLyricsOverlay: Failed to fetch caption track content")
                            captionError = e.message
                        }
                    }
                } else {
                    timber.log.Timber.d("VideoLyricsOverlay: No caption tracks available, trying transcript fallback")
                    captionError = "No caption tracks available"
                }
                
                // Fallback to transcript API if caption tracks didn't work
                if (transcriptText == null) {
                    timber.log.Timber.d("VideoLyricsOverlay: Trying transcript fallback for videoId=$videoId")
                    val transcriptResult = YouTube.transcript(videoId)
                    transcriptResult.onSuccess { text ->
                        timber.log.Timber.d("VideoLyricsOverlay: Transcript text length: ${text.length}")
                        transcriptText = text
                    }.onFailure { e ->
                        timber.log.Timber.e(e, "VideoLyricsOverlay: Transcript fallback also failed")
                        if (captionError == null) {
                            captionError = "Transcript: ${e.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "VideoLyricsOverlay: Exception during caption fetching")
                captionError = e.message
            } finally {
                isLoadingCaptions = false
                // Show result via Toast
                if (transcriptText != null && transcriptText!!.isNotEmpty()) {
                    Toast.makeText(context, "Captions loaded successfully", Toast.LENGTH_SHORT).show()
                } else if (captionError != null) {
                    Toast.makeText(context, "Captions failed: $captionError", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val lyricsText = remember(transcriptText) {
        if (!transcriptText.isNullOrEmpty()) transcriptText else null
    }

    data class LyricLine(val timeMs: Long, val text: String)

    val parsedLines = remember(lyricsText) {
        if (lyricsText.isNullOrBlank()) return@remember emptyList<LyricLine>()
        val result = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")
        for (line in lyricsText.lines()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            val match = regex.find(trimmedLine) ?: continue
            val minutes = match.groupValues[1].toLongOrNull() ?: 0
            val seconds = match.groupValues[2].toLongOrNull() ?: 0
            val millis = match.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0
            val timeMs = minutes * 60_000 + seconds * 1000 + millis
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                result.add(LyricLine(timeMs, text))
            }
        }
        result.sortedBy { it.timeMs }
    }

    // Real-time player position tracking with lyrics offset
    var playerPosition by remember { mutableLongStateOf(player.currentPosition) }
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
    
    LaunchedEffect(mediaMetadata?.id, lyricsOffset) {
        while (isActive) {
            playerPosition = player.currentPosition - lyricsOffset
            delay(16) // Match frame rate for tight video lyrics sync
        }
    }

    // Find current and next lyric line index
    val currentIndex = remember(parsedLines, playerPosition) {
        if (parsedLines.isEmpty()) return@remember -1
        var bestIdx = -1
        for (i in parsedLines.indices) {
            if (playerPosition >= parsedLines[i].timeMs) {
                bestIdx = i
            } else break
        }
        bestIdx
    }

    val currentLine = if (currentIndex >= 0) parsedLines[currentIndex].text else null
    // [2] Next lyric line preview
    val nextLine = if (currentIndex >= 0 && currentIndex + 1 < parsedLines.size) parsedLines[currentIndex + 1].text else null

    // [3] Glow text style with shadow
    val glowStyle = TextStyle(
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        shadow = Shadow(
            color = Color.Black,
            offset = Offset(0f, 0f),
            blurRadius = 12f
        )
    )

    // Show loading indicator while fetching captions
    if (isLoadingCaptions) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxWidth()
        ) {
            ContainedLoadingIndicator()
        }
    } else if (currentLine != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            // [1] Current line with animated fade transition
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.55f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // [3] Double-render for glow effect
                    Text(
                        text = currentLine,
                        style = glowStyle.copy(
                            shadow = Shadow(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                offset = Offset(0f, 0f),
                                blurRadius = 20f
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = currentLine,
                        style = glowStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // [2] Next line preview - dimmed, smaller
            if (nextLine != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = nextLine,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(0f, 0f),
                            blurRadius = 8f
                        )
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
        }
    }
}
