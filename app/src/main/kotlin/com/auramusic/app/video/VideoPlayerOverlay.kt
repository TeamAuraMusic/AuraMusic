package com.auramusic.app.video

import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.auramusic.app.video.VideoPlaybackManager.RecommendationItem

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

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = tween(durationMillis = 250),
        label = "miniDragOffset",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = with(density) { animatedOffset.dp })
                .widthIn(max = 220.dp)
                .fillMaxWidth(0.5f)
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
                                if (dragOffset > 140f) {
                                    VideoPlaybackManager.close()
                                } else {
                                    dragOffset = 0f
                                }
                            },
                            onDragCancel = { dragOffset = 0f },
                        ) { change, dragAmount ->
                            if (dragAmount > 0) {
                                dragOffset = (dragOffset + dragAmount).coerceIn(0f, 420f)
                                change.consume()
                            }
                        }
                    }
            ) {
                AndroidVideoSurface(player)

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(if (state.isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = stringResource(
                                if (state.isPlaying) R.string.pause else R.string.play
                            ),
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }

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

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

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

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLandscape && isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                ) {
                    AndroidVideoSurface(player)
                    TopBarLandscape(
                        modifier = Modifier.align(Alignment.TopCenter),
                        onCollapse = onCollapse,
                        onClose = onClose,
                        title = session.title,
                        channelName = session.channelName,
                    )
                    BottomControlsLandscape(
                        player = player,
                        uiState = uiState,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                ) {
                    VideoInfoSidebar(
                        session = session,
                        uiState = uiState,
                        onToggleLike = { VideoPlaybackManager.toggleLike() },
                        onToggleDislike = { VideoPlaybackManager.toggleDislike() },
                        onToggleSubscribe = { VideoPlaybackManager.toggleSubscribe() },
                        onToggleSave = { VideoPlaybackManager.toggleSave() },
                        onToggleDescription = { VideoPlaybackManager.toggleExpandedDescription() },
                        onRecommendationClick = { videoId ->
                            VideoPlaybackManager.playWithDetails(
                                context = context,
                                videoId = videoId,
                                title = "",
                                channelName = "",
                            )
                        },
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .graphicsLayer {
                            translationY = dragOffset
                            alpha = (1f - (dragOffset / 700f).coerceIn(0f, 1f)).coerceIn(0.3f, 1f)
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffset > 120f) onCollapse() else dragOffset = 0f
                                },
                                onDragCancel = { dragOffset = 0f },
                            ) { change, dragAmount ->
                                if (dragAmount > 0) {
                                    dragOffset = (dragOffset + dragAmount).coerceIn(0f, 800f)
                                    change.consume()
                                }
                            }
                        }
                ) {
                    AndroidVideoSurface(player)
                    TopBarPortrait(
                        modifier = Modifier.align(Alignment.TopCenter),
                        onCollapse = onCollapse,
                        onClose = onClose,
                        title = session.title,
                        channelName = session.channelName,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                ) {
                    VideoInfoSection(
                        session = session,
                        uiState = uiState,
                        onToggleLike = { VideoPlaybackManager.toggleLike() },
                        onToggleDislike = { VideoPlaybackManager.toggleDislike() },
                        onToggleSubscribe = { VideoPlaybackManager.toggleSubscribe() },
                        onToggleSave = { VideoPlaybackManager.toggleSave() },
                        onToggleDescription = { VideoPlaybackManager.toggleExpandedDescription() },
                        onRecommendationClick = { videoId ->
                            VideoPlaybackManager.playWithDetails(
                                context = context,
                                videoId = videoId,
                                title = "",
                                channelName = "",
                            )
                        },
                    )

                    BottomControlsPortrait(
                        player = player,
                        uiState = uiState,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarPortrait(
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
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 10.dp)
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
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
private fun TopBarLandscape(
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
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 10.dp)
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
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
private fun BottomControlsPortrait(
    player: ExoPlayer,
    uiState: VideoPlaybackManager.UiState,
) {
    var scrubPosition by remember { mutableFloatStateOf(-1f) }
    val durationSeconds = uiState.durationMs.toFloat().coerceAtLeast(1f)
    val effectivePosition = if (scrubPosition >= 0f) scrubPosition else uiState.positionMs.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                )
            )
            .padding(top = 14.dp, bottom = 16.dp)
    ) {
        Slider(
            value = effectivePosition.coerceIn(0f, durationSeconds),
            onValueChange = { scrubPosition = it },
            onValueChangeFinished = {
                VideoPlaybackManager.seekTo(scrubPosition.toLong())
                scrubPosition = -1f
            },
            valueRange = 0f..durationSeconds,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(uiState.positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                onClick = { VideoPlaybackManager.togglePlayPause() },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(54.dp)
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
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTime(uiState.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun BottomControlsLandscape(
    player: ExoPlayer,
    uiState: VideoPlaybackManager.UiState,
) {
    var scrubPosition by remember { mutableFloatStateOf(-1f) }
    val durationSeconds = uiState.durationMs.toFloat().coerceAtLeast(1f)
    val effectivePosition = if (scrubPosition >= 0f) scrubPosition else uiState.positionMs.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                )
            )
            .padding(top = 14.dp, bottom = 16.dp)
    ) {
        Slider(
            value = effectivePosition.coerceIn(0f, durationSeconds),
            onValueChange = { scrubPosition = it },
            onValueChangeFinished = {
                VideoPlaybackManager.seekTo(scrubPosition.toLong())
                scrubPosition = -1f
            },
            valueRange = 0f..durationSeconds,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(uiState.positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                onClick = { VideoPlaybackManager.togglePlayPause() },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(54.dp)
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
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTime(uiState.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun VideoInfoSection(
    session: VideoPlaybackManager.VideoSession,
    uiState: VideoPlaybackManager.UiState,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleDescription: () -> Unit,
    onRecommendationClick: (String) -> Unit,
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

        Spacer(modifier = Modifier.height(8.dp))

        ChannelRow(
            channelName = session.channelName,
            channelThumbnail = session.channelThumbnail,
            isSubscribed = uiState.isSubscribed,
            onSubscribeClick = onToggleSubscribe,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionButtonsRow(
            isLiked = uiState.isLiked,
            isDisliked = uiState.isDisliked,
            isSaved = uiState.isSaved,
            onLikeClick = onToggleLike,
            onDislikeClick = onToggleDislike,
            onSaveClick = onToggleSave,
            onShareClick = {},
        )

        Spacer(modifier = Modifier.height(12.dp))

        session.description?.let { desc ->
            if (desc.isNotBlank()) {
                ExpandableDescription(
                    description = desc,
                    expanded = uiState.expandedDescription,
                    onToggle = onToggleDescription,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        VideoTabs(
            session = session,
            uiState = uiState,
            onRecommendationClick = onRecommendationClick,
        )
    }
}

@Composable
private fun VideoInfoSidebar(
    session: VideoPlaybackManager.VideoSession,
    uiState: VideoPlaybackManager.UiState,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleDescription: () -> Unit,
    onRecommendationClick: (String) -> Unit,
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
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        ChannelRow(
            channelName = session.channelName,
            channelThumbnail = session.channelThumbnail,
            isSubscribed = uiState.isSubscribed,
            onSubscribeClick = onToggleSubscribe,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionButtonsRow(
            isLiked = uiState.isLiked,
            isDisliked = uiState.isDisliked,
            isSaved = uiState.isSaved,
            onLikeClick = onToggleLike,
            onDislikeClick = onToggleDislike,
            onSaveClick = onToggleSave,
            onShareClick = {},
        )

        Spacer(modifier = Modifier.height(12.dp))

        session.description?.let { desc ->
            if (desc.isNotBlank()) {
                ExpandableDescription(
                    description = desc,
                    expanded = uiState.expandedDescription,
                    onToggle = onToggleDescription,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        VideoTabs(
            session = session,
            uiState = uiState,
            onRecommendationClick = onRecommendationClick,
        )
    }
}

@Composable
private fun ChannelRow(
    channelName: String,
    channelThumbnail: String?,
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
            if (!channelThumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = channelThumbnail,
                    contentDescription = channelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = channelName,
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
                text = channelName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton(
            icon = if (isLiked) R.drawable.ic_thumb_up else R.drawable.ic_thumb_up,
            contentDescription = stringResource(R.string.video_player_like),
            label = stringResource(R.string.video_player_like),
            selected = isLiked,
            onClick = onLikeClick,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            icon = R.drawable.ic_thumb_down,
            contentDescription = stringResource(R.string.video_player_dislike),
            label = stringResource(R.string.video_player_dislike),
            selected = isDisliked,
            onClick = onDislikeClick,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            icon = R.drawable.share,
            contentDescription = stringResource(R.string.share),
            label = stringResource(R.string.share),
            selected = false,
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            icon = if (isSaved) R.drawable.library_add_check else R.drawable.library_add,
            contentDescription = stringResource(if (isSaved) R.string.video_player_saved else R.string.video_player_save),
            label = stringResource(if (isSaved) R.string.video_player_saved else R.string.video_player_save),
            selected = isSaved,
            onClick = onSaveClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    icon: Int,
    contentDescription: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
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

@Composable
private fun VideoTabs(
    session: VideoPlaybackManager.VideoSession,
    uiState: VideoPlaybackManager.UiState,
    onRecommendationClick: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(1) }

    Column {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {},
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = stringResource(R.string.video_player_comments),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = stringResource(R.string.video_player_up_next),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            if (selectedTab == 0) {
                CommentsPlaceholder()
            } else {
                UpNextList(
                    recommendations = uiState.recommendations,
                    isLoading = uiState.isLoadingRecommendations,
                    onItemClick = onRecommendationClick,
                )
            }
        }
    }
}

@Composable
private fun CommentsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_comment),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.video_player_comments_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun UpNextList(
    recommendations: List<RecommendationItem>,
    isLoading: Boolean,
    onItemClick: (String) -> Unit,
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
        recommendations.isEmpty() -> {
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
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(recommendations, key = { it.videoId }) { item ->
                    UpNextRow(
                        item = item,
                        onClick = { onItemClick(item.videoId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpNextRow(
    item: RecommendationItem,
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
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AndroidVideoSurface(player: ExoPlayer) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            view.player = player
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
