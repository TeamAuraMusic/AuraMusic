/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import androidx.compose.animation.core.LinearRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun SamsungSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    enabled: Boolean = true,
    trackHeight: Dp = 4.dp,
    expandedTrackHeight: Dp = 6.dp,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }

    val displayValue = if (isDragging) dragValue else normalizedValue

    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearRepeatable.Easing.Linear),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor

    val baseModifier = modifier
        .fillMaxWidth()
        .height(24.dp)

    val interactiveModifier = if (enabled) {
        baseModifier
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                    val mappedValue = valueRange.start + newValue * (valueRange.endInclusive - valueRange.start)
                    onValueChange(mappedValue)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                        val mappedValue = valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start)
                        onValueChange(mappedValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                        val mappedValue = valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start)
                        onValueChange(mappedValue)
                    }
                )
            }
    } else {
        baseModifier
    }

    Box(
        modifier = interactiveModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseHeightPx = trackHeight.toPx()
            val currentHeight = baseHeightPx
            val centerY = size.height / 2f

            val topTrackY = centerY - currentHeight - 2.dp.toPx()
            val bottomTrackY = centerY + 2.dp.toPx()

            val progressWidth = size.width * displayValue

            val amplitude = if (displayValue < 0.166f) {
                3.dp.toPx()
            } else {
                6.dp.toPx()
            }

            drawWavyTrack(
                y = topTrackY,
                height = currentHeight,
                progress = displayValue,
                waveOffset = waveOffset,
                amplitude = amplitude,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                progressWidth = progressWidth,
                isTop = true
            )

            drawWavyTrack(
                y = bottomTrackY,
                height = currentHeight,
                progress = displayValue,
                waveOffset = waveOffset,
                amplitude = amplitude,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                progressWidth = progressWidth,
                isTop = false
            )
        }
    }
}

private fun DrawScope.drawWavyTrack(
    y: Float,
    height: Float,
    progress: Float,
    waveOffset: Float,
    amplitude: Float,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color,
    progressWidth: Float,
    isTop: Boolean
) {
    val waveCount = 3
    val waveWidth = size.width / waveCount

    val inactivePath = Path()
    inactivePath.moveTo(0f, y + height / 2f)

    for (i in 0..waveCount) {
        val x = i * waveWidth
        val phase = (i.toFloat() / waveCount) * 2f * Math.PI.toFloat() + waveOffset
        val waveY = y + height / 2f + sin(phase).toFloat() * amplitude
        inactivePath.lineTo(x, waveY.coerceIn(y, y + height))
    }

    inactivePath.lineTo(size.width, y + height / 2f)

    drawPath(
        path = inactivePath,
        color = inactiveColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = height)
    )

    if (progressWidth > 0f) {
        val activePath = Path()
        val actualProgressWidth = progressWidth.coerceAtMost(size.width)

        activePath.moveTo(0f, y + height / 2f)

        for (i in 0..waveCount) {
            val x = i * waveWidth
            if (x > actualProgressWidth) break
            val phase = (i.toFloat() / waveCount) * 2f * Math.PI.toFloat() + waveOffset
            val waveY = y + height / 2f + sin(phase).toFloat() * amplitude
            val clampedX = x.coerceAtMost(actualProgressWidth)
            activePath.lineTo(clampedX, waveY.coerceIn(y, y + height))
        }

        activePath.lineTo(actualProgressWidth.coerceAtMost(size.width), y + height / 2f)

        drawPath(
            path = activePath,
            color = activeColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = height)
        )
    }
}
