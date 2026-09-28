/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.auramusic.app.R
import com.auramusic.app.utils.compactViewCount
import com.auramusic.app.video.VideoPlaybackManager
import com.auramusic.innertube.models.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen vertical Shorts feed. The current short is played through the
 * shared exo player while [VideoPlaybackManager] keeps the global overlay
 * suppressed (see [VideoPlaybackManager.setOverlaySuppressed]) so it does not
 * render on top of this pager. Swiping up or down advances to the next/previous
 * short once the page settles.
 */
@Composable
fun ShortsVerticalPager(
    shorts: List<YouTubeVideoItem>,
    startIndex: Int,
    channelTitle: String? = null,
    channelAvatarUrl: String? = null,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val player = VideoPlaybackManager.playerOrNull()
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (shorts.size - 1).coerceAtLeast(0)),
        pageCount = { shorts.size },
    )

    BackHandler(onBack = onClose)

    // Keep the global overlay hidden for the lifetime of the pager.
    DisposableEffect(Unit) {
        VideoPlaybackManager.setOverlaySuppressed(true)
        onDispose {
            VideoPlaybackManager.setOverlaySuppressed(false)
        }
    }

    suspend fun playIndex(index: Int) {
        val short = shorts.getOrNull(index) ?: return
        withContext(Dispatchers.Main.immediate) {
            VideoPlaybackManager.playWithDetails(
                context = context,
                videoId = short.videoId,
                title = short.title,
                channelName = short.channelName.ifBlank { channelTitle.orEmpty() },
                channelId = short.channelId,
                channelThumbnail = channelAvatarUrl,
                description = short.description,
                viewCountText = short.viewCountText,
                publishedTimeText = short.publishedTimeText,
                thumbnails = short.thumbnails,
            )
        }
    }

    // Play the short that the pager settles on; do not swap mid-swipe.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .collect { (page, scrolling) ->
                if (!scrolling) {
                    playIndex(page)
                }
            }
    }

    if (shorts.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // The shared player's surface covers the whole pager; the metadata
        // overlay slides on top of it as pages change.
        player?.let {
            ShortsPlayerSurface(player = it)
        }

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val short = shorts[index]
            Box(modifier = Modifier.fillMaxSize()) {
                // Scrim so the floating metadata stays legible over the video.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    channelAvatarUrl?.let { avatar ->
                        AsyncImage(
                            model = avatar,
                            contentDescription = short.channelName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.size(14.dp))
                    }
                    ShortsSideIcon(
                        icon = R.drawable.ic_person,
                        contentDescription = short.channelName.ifBlank { channelTitle.orEmpty() },
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Text(
                        text = short.channelName.ifBlank { channelTitle.orEmpty() }.takeIf { it.isNotBlank() } ?: "@channel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = short.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(
                        short.viewCountText?.let { compactViewCount(it) },
                        short.publishedTimeText,
                    ).joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        Surface(
            onClick = onClose,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier
                .padding(12.dp)
                .size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ShortsSideIcon(icon: Int, contentDescription: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.16f),
        modifier = Modifier.size(42.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ShortsPlayerSurface(player: ExoPlayer) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                setBackgroundColor(android.graphics.Color.BLACK)
                // Zoom crops the frame to fill the screen like the YouTube Shorts player.
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { view ->
            view.player = player
        },
        modifier = Modifier.fillMaxSize(),
    )
}