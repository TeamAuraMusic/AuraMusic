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
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor

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
        Canvas(modifier = Modifier.fillMaxWidth().height(trackHeight * 3)) {
            val width = size.width
            val trackHeightPx = trackHeight.toPx()
            val centerY = size.height / 2

            val progressWidth = width * displayValue

            drawOceanWaves(
                width = width,
                centerY = centerY,
                trackHeight = trackHeightPx,
                progress = displayValue,
                waveOffset = waveOffset,
                waveformData = waveformData,
                isPlaying = isPlaying,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                progressWidth = progressWidth
            )
        }
    }
}

private fun DrawScope.drawOceanWaves(
    width: Float,
    centerY: Float,
    trackHeight: Float,
    progress: Float,
    waveOffset: Float,
    waveformData: ByteArray?,
    isPlaying: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    progressWidth: Float,
) {
    val waveCount = 4
    val amplitude = trackHeight * 1.5f

    val normalizedData = if (waveformData != null && isPlaying) {
        waveformData.map { sample -> ((sample.toInt() and 0xFF) - 128) / 128f }
    } else {
        List(128) { i ->
            val phase = (i.toFloat() / 128f) * 2f * Math.PI.toFloat() + waveOffset
            sin(phase)
        }
    }

    drawRoundRect(
        color = inactiveColor,
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(width, trackHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2, trackHeight / 2)
    )

    val wavePath = Path()
    val waveHeight = trackHeight * 2f

    wavePath.moveTo(0f, centerY + waveHeight)

    for (i in 0 until (width / 2).toInt()) {
        val x = i.toFloat()
        val dataIndex = ((i.toFloat() / width) * normalizedData.size).toInt().coerceIn(0, normalizedData.size - 1)
        val sampleValue = normalizedData[dataIndex]

        val phase = (i.toFloat() / width) * waveCount * 2f * Math.PI.toFloat() + waveOffset
        val waveY = sin(phase) * amplitude * 0.5f

        val y = centerY + sampleValue * amplitude * 0.8f + waveY

        wavePath.lineTo(x, y.coerceIn(centerY - waveHeight, centerY + waveHeight))
    }

    wavePath.lineTo(progressWidth, centerY)
    wavePath.lineTo(progressWidth, centerY + waveHeight)
    wavePath.close()

    drawPath(
        path = wavePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                activeColor.copy(alpha = 0.8f),
                activeColor.copy(alpha = 0.4f),
                activeColor.copy(alpha = 0.1f)
            ),
            startY = centerY - waveHeight,
            endY = centerY + waveHeight
        )
    )

    val topWavePath = Path()
    topWavePath.moveTo(0f, centerY - trackHeight / 2)

    for (i in 0 until (progressWidth / 2).toInt()) {
        val x = i.toFloat()
        val dataIndex = ((i.toFloat() / width) * normalizedData.size).toInt().coerceIn(0, normalizedData.size - 1)
        val sampleValue = normalizedData[dataIndex]

        val phase = (i.toFloat() / width) * waveCount * 2f * Math.PI.toFloat() + waveOffset
        val waveY = sin(phase) * amplitude * 0.5f

        val y = centerY + sampleValue * amplitude * 0.8f + waveY - trackHeight / 2

        topWavePath.lineTo(x, y.coerceIn(centerY - waveHeight - trackHeight, centerY + waveHeight - trackHeight))
    }

    drawPath(
        path = topWavePath,
        color = activeColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
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
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2
        val trackHeight = 6.dp.toPx()
        val amplitude = trackHeight * 1.5f
        val waveCount = 4

        drawRoundRect(
            color = waveColor.copy(alpha = 0.3f),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(width, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2, trackHeight / 2)
        )

        val wavePath = Path()
        val waveHeight = trackHeight * 2f

        wavePath.moveTo(0f, centerY + waveHeight)

        for (i in 0 until (width / 2).toInt()) {
            val x = i.toFloat()
            val phase = (i.toFloat() / width) * waveCount * 2f * Math.PI.toFloat() + waveOffset
            val waveY = sin(phase) * amplitude * 0.5f
            val sampleValue = sin(phase) * 0.5f
            val y = centerY + sampleValue * amplitude * 0.8f + waveY

            wavePath.lineTo(x, y.coerceIn(centerY - waveHeight, centerY + waveHeight))
        }

        val progressWidth = width * 0.4f
        wavePath.lineTo(progressWidth, centerY)
        wavePath.lineTo(progressWidth, centerY + waveHeight)
        wavePath.close()

        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    waveColor.copy(alpha = 0.8f),
                    waveColor.copy(alpha = 0.4f),
                    waveColor.copy(alpha = 0.1f)
                ),
                startY = centerY - waveHeight,
                endY = centerY + waveHeight
            )
        )
    }
}
