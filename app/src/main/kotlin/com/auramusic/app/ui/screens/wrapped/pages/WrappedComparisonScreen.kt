/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.wrapped.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.ui.screens.wrapped.WrappedConstants
import com.auramusic.app.ui.screens.wrapped.components.AnimatedBackground
import com.auramusic.app.ui.screens.wrapped.components.ShapeType
import com.auramusic.app.ui.theme.bbh_bartle
import kotlinx.coroutines.delay

@Composable
fun WrappedComparisonScreen(
    currentMinutes: Long,
    previousMonthMinutes: Long,
    currentUniqueSongs: Int,
    previousMonthUniqueSongs: Int,
    currentUniqueArtists: Int,
    previousMonthUniqueArtists: Int,
    monthOverMonthChange: Float,
    isVisible: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    val animatedChange = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(200)
            visible = true
            animatedChange.animateTo(
                targetValue = monthOverMonthChange,
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            )
        }
    }

    val isUp = monthOverMonthChange >= 0
    val changeText = if (isUp) "+${String.format("%.0f", monthOverMonthChange)}%" else "${String.format("%.0f", monthOverMonthChange)}%"
    val verdict = when {
        monthOverMonthChange >= 50 -> "Your music listening exploded!"
        monthOverMonthChange >= 20 -> "You've been vibing more this month"
        monthOverMonthChange >= 5 -> "Steady and growing"
        monthOverMonthChange >= -5 -> "Consistent as always"
        monthOverMonthChange >= -20 -> "Taking it a bit easier this month"
        else -> "A quieter month for music"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 200)) + slideInVertically(animationSpec = tween(1000, delayMillis = 200))
            ) {
                Text(
                    text = "Month over Month",
                    style = TextStyle(
                        fontFamily = bbh_bartle,
                        fontSize = 40.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 400)) + slideInVertically(animationSpec = tween(1000, delayMillis = 400))
            ) {
                Text(
                    text = WrappedConstants.displayMonthYear,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 600)) + slideInVertically(animationSpec = tween(1000, delayMillis = 600))
            ) {
                Text(
                    text = changeText,
                    style = TextStyle(
                        fontFamily = bbh_bartle,
                        fontSize = 80.sp,
                        color = if (isUp) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 800)) + slideInVertically(animationSpec = tween(1000, delayMillis = 800))
            ) {
                Text(
                    text = verdict,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Comparison rows
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ComparisonRow(
                    label = "Listening Time",
                    current = "${currentMinutes} min",
                    previous = "${previousMonthMinutes} min",
                    isVisible = visible,
                    delayMs = 1000
                )
                ComparisonRow(
                    label = "Unique Songs",
                    current = "$currentUniqueSongs",
                    previous = "$previousMonthUniqueSongs",
                    isVisible = visible,
                    delayMs = 1200
                )
                ComparisonRow(
                    label = "Unique Artists",
                    current = "$currentUniqueArtists",
                    previous = "$previousMonthUniqueArtists",
                    isVisible = visible,
                    delayMs = 1400
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    current: String,
    previous: String,
    isVisible: Boolean,
    delayMs: Int
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(800, delayMillis = delayMs)) + slideInVertically(animationSpec = tween(800, delayMillis = delayMs))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Text(
                    text = "This month",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = current,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = bbh_bartle
            )
        }
        // Previous month line
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Last month",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
            Text(
                text = previous,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}
