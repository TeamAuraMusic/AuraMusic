/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.player

import android.view.ViewGroup
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.auramusic.app.playback.AuraCanvasRepository

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

    // Resolve the URL for the current (title, artist). Cached in the repo.
    LaunchedEffect(title, artist) {
        canvasUrl = null
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

    // Swap the source whenever the resolved URL changes.
    LaunchedEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.player = exoPlayer },
        )
    }
}
