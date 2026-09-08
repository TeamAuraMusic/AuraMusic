/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.videos

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.ui.PlayerView
import com.auramusic.app.R
import com.auramusic.app.utils.FlowPlayerUtils
import com.auramusic.flow.FlowVideo
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Standalone regular-YouTube video player. This is intentionally separate from
 * [com.auramusic.app.playback.MusicService] - it has its own ExoPlayer and is
 * used from the Videos tab, so watching a video never touches the music queue.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val videoId = remember {
        val encoded = navController.currentBackStackEntry?.arguments?.getString("videoId") ?: ""
        URLDecoder.decode(encoded, "UTF-8")
    }

    var title by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Load the stream + metadata once
    LaunchedEffect(videoId) {
        isLoading = true
        error = null
        withContext(Dispatchers.IO) {
            FlowPlayerUtils.getVideoDetails(videoId).onSuccess { details ->
                title = details.title
            }
        }
        FlowPlayerUtils.getVideoStreamSource(videoId).fold(
            onSuccess = { source ->
                runCatching {
                    val factory = ProgressiveMediaSource.Factory(
                        DefaultHttpDataSource.Factory(),
                        ExtractorsFactory {
                            arrayOf(
                                MatroskaExtractor(),
                                FragmentedMp4Extractor(),
                                Mp4Extractor()
                            )
                        }
                    )
                    val mediaSource = when (source) {
                        is FlowVideo.VideoStreamSource.Single -> {
                            val mediaItem = MediaItem.Builder()
                                .setUri(source.url)
                                .setMimeType(source.mimeType)
                                .setMediaId(videoId)
                                .build()
                            factory.createMediaSource(mediaItem)
                        }
                        is FlowVideo.VideoStreamSource.Merged -> {
                            val videoMediaItem = MediaItem.Builder()
                                .setUri(source.videoUrl)
                                .setMimeType(source.videoMimeType)
                                .setMediaId(videoId + "_video")
                                .build()
                            val videoSource = factory.createMediaSource(videoMediaItem)
                            val audioMediaItem = MediaItem.Builder()
                                .setUri(source.audioUrl)
                                .setMimeType(source.audioMimeType)
                                .setMediaId(videoId + "_audio")
                                .build()
                            val audioSource = factory.createMediaSource(audioMediaItem)
                            MergingMediaSource(true, true, videoSource, audioSource)
                        }
                    }
                    player.setMediaSource(mediaSource)
                    player.prepare()
                }.onFailure { e ->
                    error = e.message ?: "Playback failed"
                }
            },
            onFailure = { e ->
                error = e.message ?: "Could not load video"
            }
        )
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                enabled = !isLoading && error == null,
                onClick = { showControls = !showControls }
            )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.playback_error),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = error ?: "",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.dismiss),
                        tint = Color.White
                    )
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        keepScreenOn = true
                    }
                },
                update = { playerView ->
                    playerView.player = player
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay controls (top bar) when showControls is true
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.dismiss),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = title ?: videoId,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}