/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.auramusic.app.constants.SubtitleFontSizeKey
import com.auramusic.app.constants.SubtitleLanguageKey
import com.auramusic.app.constants.SubtitlesEnabledKey
import com.auramusic.app.utils.rememberPreference
import com.auramusic.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private data class CaptionCue(val startMs: Long, val endMs: Long, val text: String)

/**
 * Caption text already downloaded, keyed by video and language.
 *
 * The expanded player is torn down and rebuilt constantly (collapse, expand, music taking the
 * screen back), and without a cache every rebuild would re-request the whole caption track.
 */
private object CaptionStore {
    val text = ConcurrentHashMap<String, String>()
    val attempted = Collections.synchronizedSet(mutableSetOf<String>())
}

private val CueLineRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")

/**
 * The caption line for the moment currently on screen, drawn over the video.
 *
 * The video path builds its media source straight from the stream URL, so the player has no
 * subtitle tracks of its own - captions are fetched here and timed against the playhead
 * instead. The preference is the same one Player settings writes, which keeps a single
 * switch for both the music video player and this overlay.
 *
 * Position is polled but only the cue text is derived from it, so a moving playhead never
 * invalidates anything until the caption actually changes.
 */
@Composable
fun VideoCaptionOverlay(
    videoId: String?,
    player: ExoPlayer,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val subtitlesEnabled by rememberPreference(SubtitlesEnabledKey, true)
    val language by rememberPreference(SubtitleLanguageKey, "auto")
    val fontSize by rememberPreference(SubtitleFontSizeKey, 16f)

    var captionText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId, language, subtitlesEnabled) {
        if (!subtitlesEnabled || videoId.isNullOrBlank()) {
            captionText = null
            return@LaunchedEffect
        }
        val key = "$videoId|$language"
        CaptionStore.text[key]?.let {
            captionText = it
            return@LaunchedEffect
        }
        if (!CaptionStore.attempted.add(key)) {
            // Already tried and failed for this video + language; don't hammer the endpoint
            // on every expand.
            captionText = null
            return@LaunchedEffect
        }
        captionText = null
        val fetched = withContext(Dispatchers.IO) { fetchCaptionText(videoId, language) }
        if (fetched != null) {
            CaptionStore.text[key] = fetched
            captionText = fetched
        }
    }

    val cues = remember(captionText) { parseCues(captionText) }

    var positionMs by remember(videoId) { mutableLongStateOf(player.currentPosition) }
    LaunchedEffect(player, videoId, subtitlesEnabled, cues) {
        // Nothing to time against until a track has actually landed, so a video whose
        // captions are off or unavailable never wakes up for them.
        if (cues.isEmpty()) return@LaunchedEffect
        while (isActive) {
            positionMs = player.currentPosition
            // The playhead only moves while playing; a paused player does not need
            // to wake the main thread at frame rate.
            delay(if (player.isPlaying) 80L else 400L)
        }
    }

    val currentCue by remember(cues) {
        derivedStateOf {
            val position = positionMs
            cues.firstOrNull { position in it.startMs until it.endMs }?.text
        }
    }

    if (!subtitlesEnabled) return
    val cue = currentCue ?: return

    Box(
        modifier = modifier
            .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)
            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = cue,
            color = Color.White,
            fontSize = fontSize.coerceIn(10f, 32f).sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset.Zero,
                    blurRadius = 6f,
                ),
            ),
        )
    }
}

private suspend fun fetchCaptionText(videoId: String, language: String): String? = try {
    val result = YouTube.getCaptionTracksWithDuration(videoId).getOrNull()
    val tracks = result?.first
    if (tracks.isNullOrEmpty()) {
        null
    } else {
        // Same precedence the music player uses: what the user asked for, then English,
        // then anything auto-generated, then whatever exists.
        val track = (if (language != "auto") {
            tracks.firstOrNull { it.languageCode == language }
        } else {
            null
        })
            ?: tracks.firstOrNull { it.languageCode == "en" }
            ?: tracks.firstOrNull { it.kind == "asr" }
            ?: tracks.firstOrNull()
        track?.let {
            YouTube.fetchSubtitleFromCaptionTrack(it.baseUrl)
                .getOrNull()
                ?.takeIf { text -> text.isNotBlank() }
        }
    }
} catch (e: Exception) {
    Timber.e(e, "VideoCaptionOverlay: could not load captions for %s", videoId)
    null
}

/**
 * The caption track comes back as `[mm:ss.mmm]text` lines with no end times, so a cue runs
 * until the next one starts - which is how the rest of the app already reads this format.
 */
private fun parseCues(raw: String?): List<CaptionCue> {
    if (raw.isNullOrBlank()) return emptyList()
    val parsed = ArrayList<Pair<Long, String>>(64)
    for (line in raw.lines()) {
        val match = CueLineRegex.find(line) ?: continue
        val minutes = match.groupValues[1].toLongOrNull() ?: continue
        val seconds = match.groupValues[2].toLongOrNull() ?: continue
        val millis = match.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0L
        val text = match.groupValues[4].trim()
        if (text.isEmpty()) continue
        parsed.add(minutes * 60_000L + seconds * 1_000L + millis to text)
    }
    if (parsed.isEmpty()) return emptyList()
    parsed.sortBy { it.first }
    return parsed.mapIndexed { index, (startMs, text) ->
        val nextStart = parsed.getOrNull(index + 1)?.first
        val endMs = nextStart ?: (startMs + 5_000L)
        CaptionCue(startMs, maxOf(endMs, startMs + 1L), text)
    }
}
