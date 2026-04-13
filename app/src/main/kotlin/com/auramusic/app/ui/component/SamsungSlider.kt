/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SamsungSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    enabled: Boolean = true,
    trackHeight: Dp = 6.dp,
    expandedTrackHeight: Dp = 12.dp,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }

    val displayValue = if (isDragging) dragValue else normalizedValue

    val animatedHeight by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "trackExpand"
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor

    val baseModifier = modifier
        .fillMaxWidth()
        .height(expandedTrackHeight + 16.dp)

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
            val expandedHeightPx = expandedTrackHeight.toPx()
            val currentHeight = baseHeightPx + (expandedHeightPx - baseHeightPx) * animatedHeight
            val cornerRadius = currentHeight / 2f
            val top = (size.height - currentHeight) / 2f

            // Inactive track (full width pill)
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, top),
                size = Size(size.width, currentHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // Active track (filled portion pill)
            val activeWidth = (size.width * displayValue).coerceAtLeast(currentHeight)
            if (displayValue > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, top),
                    size = Size(activeWidth.coerceAtMost(size.width), currentHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
    }
}
