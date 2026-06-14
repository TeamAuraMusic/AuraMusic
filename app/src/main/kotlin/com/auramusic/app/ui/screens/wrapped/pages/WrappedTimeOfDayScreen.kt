/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.wrapped.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.auramusic.app.ui.screens.wrapped.components.AnimatedBackground
import com.auramusic.app.ui.screens.wrapped.components.ShapeType
import com.auramusic.app.ui.theme.bbh_bartle
import kotlinx.coroutines.delay

@Composable
fun WrappedTimeOfDayScreen(
    listeningByTimeOfDay: Map<String, Long>,
    isVisible: Boolean
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(200)
            visible = true
        }
    }

    val timeEmojis = mapOf(
        "Morning" to "\u2600\uFE0F",
        "Afternoon" to "\u26C5",
        "Evening" to "\uD83C\uDF19",
        "Night" to "\u2B50"
    )

    val peakTime = listeningByTimeOfDay.maxByOrNull { it.value }?.key ?: "Night"

    val subtitle = when (peakTime) {
        "Morning" -> "You're an early bird who starts the day with music"
        "Afternoon" -> "Afternoons are your musical sweet spot"
        "Evening" -> "Evenings set the perfect mood for your tunes"
        "Night" -> "Night owl detected - music fuels your late hours"
        else -> "You have unique listening habits"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Line))
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
                    text = "When You Listen",
                    style = TextStyle(
                        fontFamily = bbh_bartle,
                        fontSize = 40.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 400)) + slideInVertically(animationSpec = tween(1000, delayMillis = 400))
            ) {
                Text(
                    text = "${timeEmojis[peakTime]} $peakTime Listener",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 600)) + slideInVertically(animationSpec = tween(1000, delayMillis = 600))
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            val totalMinutes = listeningByTimeOfDay.values.sum().coerceAtLeast(1)

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("Morning", "Afternoon", "Evening", "Night").forEachIndexed { index, timeOfDay ->
                    val minutes = listeningByTimeOfDay[timeOfDay] ?: 0L
                    val percentage = ((minutes.toFloat() / totalMinutes) * 100).toInt()

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(800, delayMillis = 800 + (index * 150))) + slideInVertically(animationSpec = tween(800, delayMillis = 800 + (index * 150)))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${timeEmojis[timeOfDay]}",
                                fontSize = 32.sp
                            )
                            Text(
                                text = timeOfDay,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${percentage}%",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = bbh_bartle
                            )
                            Text(
                                text = "${minutes} min",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
