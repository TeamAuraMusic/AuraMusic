/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.player

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.auramusic.app.playback.AuraCanvasRepository
import timber.log.Timber

/**
 * Looping, muted MP4 overlay that renders the matching AuraCanvas video
 * on top of the album-art thumbnail when one is available.
 *
 * Uses a dedicated ExoPlayer (NOT the main playback player) so audio is untouched.
 * Renders nothing while no canvas URL is resolved.
 */
@UnstableApi
@Composable
fun AuraCanvasOverlay(
    title: String?,
    artist: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var canvasUrl by remember { mutableStateOf<String?>(null) }
    var isVideoReady by remember { mutableStateOf(false) }

    // Resolve the URL for the current (title, artist). Cached in the repo.
    LaunchedEffect(title, artist) {
        canvasUrl = null
        isVideoReady = false
        canvasUrl = runCatching {
            AuraCanvasRepository.findCanvasUrl(title, artist)
        }.getOrNull()
    }

    val url = canvasUrl ?: return

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    // Attach listener for errors and first frame
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.e("AuraCanvas playback error: ${error.errorCodeName} - ${error.message}")
                isVideoReady = false
            }

            override fun onRenderedFirstFrame() {
                isVideoReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Swap the source whenever the resolved URL changes.
    LaunchedEffect(url) {
        isVideoReady = false
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "canvasFade"
    )

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                AspectRatioFrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    val textureView = TextureView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    addView(textureView)
                    exoPlayer.setVideoTextureView(textureView)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
            update = { /* no-op */ }
        )
    }
}
