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
import com.auramusic.app.canvas.AuraCanvasConfig
import com.auramusic.app.canvas.AuraCanvasResult
import com.auramusic.app.constants.AuraCanvasRemoteEnabledKey
import com.auramusic.app.playback.AuraCanvasRepository
import com.auramusic.app.utils.rememberPreference
import timber.log.Timber

/**
 * Reusable composable that shows either a looping AuraCanvas video
 * or a static image as fallback.
 *
 * Can be used in Player thumbnail, Artist headers, Album art, etc.
 */
@UnstableApi
@Composable
fun AuraCanvasImage(
    title: String?,
    artist: String?,
    staticImageUrl: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    candidateTracks: List<String> = emptyList(),   // Pass top songs for on-demand remote lookup
) {
    val context = LocalContext.current
    var canvasResult by remember { mutableStateOf<AuraCanvasResult?>(null) }
    var isVideoReady by remember { mutableStateOf(false) }

    val (remoteEnabled, _) = rememberPreference(AuraCanvasRemoteEnabledKey, defaultValue = false)

    LaunchedEffect(title, artist, candidateTracks, remoteEnabled) {
        canvasResult = null
        isVideoReady = false

        // 1. Try manifest first (fast, no network for remote)
        val manifestUrl = runCatching {
            AuraCanvasRepository.findCanvasUrl(title, artist)
        }.getOrNull()

        if (manifestUrl != null) {
            canvasResult = AuraCanvasResult(
                url = manifestUrl,
                source = "manifest",
                song = title,
                artist = artist
            )
            return@LaunchedEffect
        }

        // 2. Try remote on-demand only if user enabled it in settings
        if (remoteEnabled) {
            val remote = AuraCanvasConfig.remoteProvider
            if (remote != null && artist != null && candidateTracks.isNotEmpty()) {
                val result = if (title != null) {
                    // Album context
                    remote.getAlbumCanvas(title, artist, candidateTracks)
                } else {
                    // Artist context
                    remote.getArtistCanvas(artist, candidateTracks)
                }
                canvasResult = result
            }
        }
    }

    val url = canvasResult?.url

    if (url != null) {
        // Show canvas video
        CanvasVideo(
            url = url,
            modifier = modifier,
            onReady = { isVideoReady = true }
        )
    } else if (staticImageUrl != null) {
        // Fallback to static image
        AsyncImage(
            model = staticImageUrl,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier.fillMaxSize()
        )
    }
}

@UnstableApi
@Composable
private fun CanvasVideo(
    url: String,
    modifier: Modifier,
    onReady: () -> Unit,
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
                onReady()
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
        label = "canvasFade"
    )

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
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
    )
}
