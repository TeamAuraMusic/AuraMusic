/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.player

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
 * [2] Next lyric line preview
 * [3] Glow/shadow effect for readability
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoLyricsOverlay(
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val player = playerConnection.player

    // Check if video subtitles are enabled
    val (videoLyricsEnabled, _) = rememberPreference(
        VideoLyricsEnabledKey,
        defaultValue = true
    )
    val subtitleLanguage by rememberPreference(
        SubtitleLanguageKey,
        defaultValue = "en"
    )

    if (!videoLyricsEnabled) return

    // Fetch YouTube subtitles: try caption tracks first (like SmartTube), fallback to transcript
    var transcriptText by remember { mutableStateOf<String?>(null) }
    var isLoadingCaptions by remember { mutableStateOf(false) }
    var captionError by remember { mutableStateOf<String?>(null) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }

    // Use the actual video ID being played (may differ from song ID when video was found via search)
    val activeVideoId by playerConnection.currentVideoId.collectAsState()
    val videoModeEnabled by playerConnection.videoModeEnabled.collectAsState()

    LaunchedEffect(mediaMetadata?.id, activeVideoId) {
        val rawVideoId = (if (videoModeEnabled) activeVideoId else null)
            ?: mediaMetadata?.id
            ?: return@LaunchedEffect
        // Strip any suffixes like "_video" that might be added for video mode
        val videoId = rawVideoId.removeSuffix("_video").ifEmpty { rawVideoId }
        
        timber.log.Timber.d("VideoLyricsOverlay: Fetching captions for videoId=$videoId (activeVideoId=$activeVideoId, songId=${mediaMetadata?.id}, videoMode=$videoModeEnabled)")
        
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
