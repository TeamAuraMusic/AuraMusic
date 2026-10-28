/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * One UI 9 style seek bar: a row of waveform bars that fill with the active colour (the song's
 * colour in the player) as it plays, and sway gently while playing. Tap or drag to seek.
 *
 * [seed] shapes the bars, so each song keeps its own waveform.
 */
@Composable
fun WaveformSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    enabled: Boolean = true,
    isPlaying: Boolean = false,
    seed: Any? = null,
) {
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val shown = if (dragFraction >= 0f) dragFraction else fraction

    val heights = remember(seed) {
        val random = Random(seed?.hashCode() ?: 0)
        List(BAR_COUNT) { 0.3f + random.nextFloat() * 0.7f }
    }
    val phase by rememberInfiniteTransition(label = "waveform").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "waveformPhase",
    )

    fun valueAt(f: Float) = valueRange.start + f * span

    val gestures = if (enabled) {
        Modifier
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    onValueChange(valueAt((offset.x / size.width).coerceIn(0f, 1f)))
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragFraction = (it.x / size.width).coerceIn(0f, 1f)
                        onValueChange(valueAt(dragFraction))
                    },
                    onDragEnd = {
                        dragFraction = -1f
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = { dragFraction = -1f },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(valueAt(dragFraction))
                        change.consume()
                    },
                )
            }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(gestures),
    ) {
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
        heights.forEachIndexed { index, base ->
            val sway = if (isPlaying) 0.82f + 0.18f * sin(phase + index * 0.45f) else 1f
            val barHeight = size.height * base * sway
            val played = (index + 0.5f) / heights.size <= shown
            drawRoundRect(
                color = if (played) colors.activeTrackColor else colors.inactiveTrackColor,
                topLeft = Offset(index * (barWidth + gap), (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

private const val BAR_COUNT = 56
