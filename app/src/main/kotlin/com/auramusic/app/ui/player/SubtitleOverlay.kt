package com.auramusic.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.subtitles.SubtitleCue
import com.auramusic.app.subtitles.SubtitleStyle

@Composable
fun SubtitleOverlay(
    cues: List<SubtitleCue>,
    currentPositionMs: Long,
    style: SubtitleStyle = SubtitleStyle(),
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!enabled || cues.isEmpty()) return

    var position by remember { mutableLongStateOf(currentPositionMs) }

    LaunchedEffect(currentPositionMs) {
        position = currentPositionMs
    }

    val activeCue = remember(cues, position) {
        cues.firstOrNull { cue ->
            position >= cue.startMs && position < cue.endMs
        }
    }

    activeCue?.let { cue ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = cue.text,
                color = style.textColor,
                fontSize = style.fontSize.sp,
                fontWeight = style.fontWeight,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(style.backgroundColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun SubtitleOverlayFromState(
    cues: List<SubtitleCue>,
    currentPositionMs: Long,
    enabled: Boolean = true,
    fontSize: Float = 16f,
    modifier: Modifier = Modifier
) {
    val style = SubtitleStyle(
        fontSize = fontSize,
        textColor = Color.White,
        backgroundColor = Color.Black.copy(alpha = 0.7f),
        fontWeight = FontWeight.Normal
    )

    SubtitleOverlay(
        cues = cues,
        currentPositionMs = currentPositionMs,
        style = style,
        enabled = enabled,
        modifier = modifier
    )
}