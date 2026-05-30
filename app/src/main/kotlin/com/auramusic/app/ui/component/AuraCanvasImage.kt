/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.compose.AsyncImage
import com.auramusic.app.constants.AuraCanvasEnabledKey
import com.auramusic.app.playback.AuraCanvasRepository
import com.auramusic.app.utils.rememberPreference
import timber.log.Timber

/**
 * Reusable composable that shows either a looping AuraCanvas video
 * or a static image as fallback (the static image always renders, the video
 * fades in on top once ready).
 *
 * `candidateTracks` is kept for source compatibility with existing callers
 * but is no longer used – the repository now resolves canvases server-side
 * directly from (title, artist, album).
 */
@UnstableApi
@Composable
fun AuraCanvasImage(
    title: String?,
    artist: String?,
    staticImageUrl: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    album: String? = null,
    durationMs: Long? = null,
    @Suppress("UNUSED_PARAMETER") candidateTracks: List<String> = emptyList(),
) {
    val (enabled, _) = rememberPreference(AuraCanvasEnabledKey, defaultValue = false)
    var canvasUrl by remember(title, artist, album, durationMs) { mutableStateOf<String?>(null) }

    LaunchedEffect(title, artist, album, durationMs, enabled) {
        canvasUrl = null
        if (!enabled) return@LaunchedEffect
        canvasUrl = runCatching {
            AuraCanvasRepository.findCanvasUrl(title, artist, album, durationMs)
        }.getOrNull()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (staticImageUrl != null) {
            AsyncImage(
                model = staticImageUrl,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
        val url = canvasUrl
        if (url != null) {
            CanvasVideo(url = url, modifier = Modifier.fillMaxSize())
        }
    }
}

@UnstableApi
@Composable
private fun CanvasVideo(
    url: String,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.e("AuraCanvasVideo error: ${error.errorCodeName} - ${error.message}")
                isReady = false
            }

            override fun onRenderedFirstFrame() {
                isReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(url) {
        isReady = false
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isReady) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "canvasFade",
    )

    AndroidView(
        factory = { ctx ->
            AspectRatioFrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                val textureView = TextureView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
                addView(textureView)
                exoPlayer.setVideoTextureView(textureView)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha),
    )
}
