/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Standard TV-styled "primary" button. Adds a focus border + scale so the
 * remote always shows a clear focus indicator.
 */
@Composable
fun TvPrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "tvButtonScale",
    )
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent

    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(),
        modifier = modifier
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(20.dp)),
    ) {
        Text(text = label)
    }
}

/**
 * Outlined variant for secondary actions (Shuffle, Cancel, etc.).
 */
@Composable
fun TvSecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "tvOutlinedButtonScale",
    )
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(20.dp)),
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Animated audio visualizer for the TV player. Shows pulsing bars that
 * animate when [isPlaying] is true and hold still when paused.
 */
@Composable
fun TvAudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    var animationPhase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                animationPhase += 0.15f
                kotlinx.coroutines.delay(50)
            }
        }
    }

    val barSeeds = remember {
        List(barCount) { Random.nextFloat() * 2f + 0.5f }
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = canvasWidth / (barCount * 1.8f)
        val gap = (canvasWidth - barWidth * barCount) / (barCount + 1)

        for (i in 0 until barCount) {
            val normalizedHeight = if (isPlaying) {
                val wave = sin(animationPhase + barSeeds[i] * 6f) * 0.5f + 0.5f
                val jitter = sin(animationPhase * 2.3f + i * 0.7f) * 0.15f
                (wave + jitter).coerceIn(0.08f, 1f)
            } else {
                0.08f
            }

            val barHeight = canvasHeight * normalizedHeight
            val x = gap + i * (barWidth + gap)
            val y = canvasHeight - barHeight

            drawRoundRect(
                color = barColor.copy(alpha = 0.7f + normalizedHeight * 0.3f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

/**
 * Shimmer loading skeleton for a horizontal row of items (e.g., song cards).
 */
@Composable
fun TvLoadingRow(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
    itemWidth: Dp = 180.dp,
    itemHeight: Dp = 220.dp,
) {
    val shimmerAlpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    Row(
        modifier = modifier.padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(itemCount) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(itemHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha)
                        ),
                )
                Box(
                    modifier = Modifier
                        .width(itemWidth * 0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha * 0.8f)
                        ),
                )
                Box(
                    modifier = Modifier
                        .width(itemWidth * 0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha * 0.6f)
                        ),
                )
            }
        }
    }
}

/**
 * Full-screen loading state with a spinner and message.
 */
@Composable
fun TvLoadingScreen(
    message: String = "Loading…",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Error state with a message and optional retry button.
 */
@Composable
fun TvErrorScreen(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                TvPrimaryButton(
                    label = "Try again",
                    onClick = onRetry,
                )
            }
        }
    }
}

/**
 * Empty state shown when a section has no content.
 */
@Composable
fun TvEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
