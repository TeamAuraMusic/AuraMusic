/**
 * AuraMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.component

import android.media.audiofx.Visualizer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioVisualizerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = androidx.compose.material3.SliderDefaults.colors(),
    enabled: Boolean = true,
    audioSessionId: Int = 0,
    isPlaying: Boolean = true,
    trackHeight: Dp = 6.dp,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    var waveformData by remember { mutableStateOf<ByteArray?>(null) }
    var visualizer by remember { mutableStateOf<Visualizer?>(null) }

    val displayValue = if (isDragging) dragValue else normalizedValue

    DisposableEffect(audioSessionId) {
        if (audioSessionId == 0) {
            return@DisposableEffect onDispose { }
        }

        val visualizerInstance = Visualizer(audioSessionId)
        visualizer = visualizerInstance

        visualizerInstance.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) {
                    waveformData = waveform
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                }
            },
            Visualizer.getMaxCaptureRate() / 2,
            true,
            false
        )

        visualizerInstance.enabled = true

        onDispose {
            visualizerInstance.enabled = false
            visualizerInstance.release()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor
    val thumbColor = colors.thumbColor

    val baseModifier = modifier
        .fillMaxWidth()
        .height(32.dp)

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
        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            val width = size.width
            val centerY = size.height / 2
            val barHeight = trackHeight.toPx()

            val progressWidth = width * displayValue

            drawSmoothProgressBar(
                width = width,
                centerY = centerY,
                barHeight = barHeight,
                progress = displayValue,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            drawSmoothOceanWaves(
                width = width,
                centerY = centerY,
                barHeight = barHeight,
                progress = displayValue,
                waveOffset = waveOffset,
                waveformData = waveformData,
                isPlaying = isPlaying,
                activeColor = activeColor
            )

            if (isDragging || enabled) {
                val thumbRadius = barHeight * 0.8f
                drawCircle(
                    color = thumbColor ?: activeColor,
                    radius = thumbRadius,
                    center = Offset(progressWidth.coerceIn(thumbRadius, width - thumbRadius), centerY)
                )
            }
        }
    }
}

private fun DrawScope.drawSmoothProgressBar(
    width: Float,
    centerY: Float,
    barHeight: Float,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    val progressWidth = width * progress

    drawRoundRect(
        color = inactiveColor,
        topLeft = Offset(0f, centerY - barHeight / 2),
        size = Size(width, barHeight),
        cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
    )

    drawRoundRect(
        color = activeColor,
        topLeft = Offset(0f, centerY - barHeight / 2),
        size = Size(progressWidth.coerceAtMost(width), barHeight),
        cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
    )
}

private fun DrawScope.drawSmoothOceanWaves(
    width: Float,
    centerY: Float,
    barHeight: Float,
    progress: Float,
    waveOffset: Float,
    waveformData: ByteArray?,
    isPlaying: Boolean,
    activeColor: Color,
) {
    val progressWidth = width * progress
    if (progressWidth <= 0f) return

    val waveCount = 2
    val waveAmplitude = barHeight * 0.4f

    val normalizedData = if (waveformData != null && isPlaying) {
        waveformData.map { sample -> ((sample.toInt() and 0xFF) - 128) / 128f }
    } else {
        List(64) { i ->
            val phase = (i.toFloat() / 64f) * 2f * Math.PI.toFloat() + waveOffset
            (sin(phase) + 1f) / 2f
        }
    }

    val wavePath = Path()
    val waveTop = centerY - barHeight * 0.6f

    wavePath.moveTo(0f, waveTop)

    for (i in 0 until progressWidth.toInt()) {
        val x = i.toFloat()
        val dataIndex = ((x / width) * normalizedData.size).toInt().coerceIn(0, normalizedData.size - 1)
        val sampleValue = normalizedData[dataIndex]

        val phase = (x / width) * waveCount * 2f * Math.PI.toFloat() + waveOffset
        val smoothWave = sin(phase) * waveAmplitude * 0.3f

        val y = waveTop - sampleValue * waveAmplitude * 0.5f - smoothWave.toFloat()
        wavePath.lineTo(x, y)
    }

    wavePath.lineTo(progressWidth, waveTop)
    wavePath.lineTo(0f, waveTop)
    wavePath.close()

    drawPath(
        path = wavePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                activeColor.copy(alpha = 0.9f),
                activeColor.copy(alpha = 0.6f),
                activeColor.copy(alpha = 0.3f)
            ),
            startY = waveTop - waveAmplitude,
            endY = waveTop
        )
    )
}

@Composable
fun AudioVisualizerPreview(
    modifier: Modifier = Modifier,
    waveColor: Color = Color.White,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavePreview")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2
        val barHeight = 6.dp.toPx()
        val waveCount = 2
        val waveAmplitude = barHeight * 0.4f

        drawRoundRect(
            color = waveColor.copy(alpha = 0.3f),
            topLeft = Offset(0f, centerY - barHeight / 2),
            size = Size(width, barHeight),
            cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
        )

        val progressWidth = width * 0.4f

        drawRoundRect(
            color = waveColor.copy(alpha = 0.5f),
            topLeft = Offset(0f, centerY - barHeight / 2),
            size = Size(progressWidth, barHeight),
            cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
        )

        val wavePath = Path()
        val waveTop = centerY - barHeight * 0.6f

        wavePath.moveTo(0f, waveTop)

        for (i in 0 until progressWidth.toInt()) {
            val x = i.toFloat()
            val phase = (x / width) * waveCount * 2f * Math.PI.toFloat() + waveOffset
            val smoothWave = sin(phase) * waveAmplitude * 0.3f
            val sampleValue = (sin(phase + waveOffset) + 1f) / 2f

            val y = waveTop - sampleValue * waveAmplitude * 0.5f - smoothWave.toFloat()
            wavePath.lineTo(x, y)
        }

        wavePath.lineTo(progressWidth, waveTop)
        wavePath.lineTo(0f, waveTop)
        wavePath.close()

        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    waveColor.copy(alpha = 0.9f),
                    waveColor.copy(alpha = 0.6f),
                    waveColor.copy(alpha = 0.3f)
                ),
                startY = waveTop - waveAmplitude,
                endY = waveTop
            )
        )
    }
}
