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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioVisualizerView(
    audioSessionId: Int,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    waveColor: Color = Color.White,
    height: Dp = 48.dp,
    progress: Float = 0f,
) {
    var waveformData by remember { mutableStateOf<ByteArray?>(null) }
    var visualizer by remember { mutableStateOf<Visualizer?>(null) }

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
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val displayWaveData = waveformData ?: run {
        if (isPlaying) {
            ByteArray(128) { i -> ((sin(i * 0.3 + waveOffset) * 40 + 40).toInt() and 0xFF).toByte() }
        } else {
            null
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val centerY = size.height / 2

        val amplitude = size.height * 0.35f

        if (displayWaveData == null || !isPlaying) {
            val progressWidth = width * progress.coerceIn(0f, 1f)

            val path = Path()
            val waveCount = 3
            val waveWidth = width / waveCount

            path.moveTo(0f, centerY)
            for (i in 0 until waveCount * 10) {
                val x = i * waveWidth / 10f
                val phase = (i.toFloat() / (waveCount * 10)) * 2f * Math.PI.toFloat() + waveOffset
                val y = centerY + sin(phase).toFloat() * amplitude * 0.3f
                path.lineTo(x, y)
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(waveColor.copy(alpha = 0.3f), waveColor.copy(alpha = 0.6f))
                ),
                style = Stroke(width = 3.dp.toPx())
            )

            if (progressWidth > 0f) {
                val activePath = Path()
                activePath.moveTo(0f, centerY)
                for (i in 0 until (waveCount * 10 * progress).toInt()) {
                    val x = i * waveWidth / 10f
                    val phase = (i.toFloat() / (waveCount * 10)) * 2f * Math.PI.toFloat() + waveOffset
                    val y = centerY + sin(phase).toFloat() * amplitude * 0.3f
                    activePath.lineTo(x, y)
                }

                drawPath(
                    path = activePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(waveColor.copy(alpha = 0.6f), waveColor)
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            return@Canvas
        }

        val step = width / displayWaveData.size
        val normalizedData = displayWaveData.map { sample ->
            ((sample.toInt() and 0xFF) - 128) / 128f
        }

        val progressPoint = (progress * normalizedData.size).toInt()

        val path = Path()
        path.moveTo(0f, centerY)

        for (i in normalizedData.indices) {
            val x = i * step
            val phase = (i.toFloat() / normalizedData.size) * 2f * Math.PI.toFloat() + waveOffset
            val sampleValue = normalizedData[i]
            val y = centerY + sampleValue * amplitude + sin(phase).toFloat() * amplitude * 0.2f
            path.lineTo(x, y.coerceIn(centerY - amplitude, centerY + amplitude))
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(waveColor.copy(alpha = 0.3f), waveColor.copy(alpha = 0.7f), waveColor)
            ),
            style = Stroke(width = 3.dp.toPx())
        )

        if (progressPoint > 0 && progressPoint < normalizedData.size) {
            val activePath = Path()
            activePath.moveTo(0f, centerY)

            for (i in 0 until progressPoint) {
                val x = i * step
                val phase = (i.toFloat() / normalizedData.size) * 2f * Math.PI.toFloat() + waveOffset
                val sampleValue = normalizedData[i]
                val y = centerY + sampleValue * amplitude + sin(phase).toFloat() * amplitude * 0.2f
                activePath.lineTo(x, y.coerceIn(centerY - amplitude, centerY + amplitude))
            }

            drawPath(
                path = activePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(waveColor.copy(alpha = 0.7f), waveColor)
                ),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun AudioVisualizerPreview(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    waveColor: Color = Color.White,
    height: Dp = 48.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavePreview")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val centerY = size.height / 2
        val amplitude = size.height * 0.35f

        val waveCount = 3
        val waveWidth = width / waveCount

        val path = Path()
        path.moveTo(0f, centerY)

        for (i in 0 until waveCount * 10) {
            val x = i * waveWidth / 10f
            val phase = (i.toFloat() / (waveCount * 10)) * 2f * Math.PI.toFloat() + waveOffset
            val y = centerY + sin(phase).toFloat() * amplitude * 0.3f
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(waveColor.copy(alpha = 0.3f), waveColor.copy(alpha = 0.6f))
            ),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
