package com.auramusic.app.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.auramusic.app.R

@Composable
fun VoiceCommandDialog(
    @Suppress("DEPRECATION") viewModel: VoiceCommandViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onPlaybackCommand: (VoiceCommand) -> Unit,
    onSettingsCommand: (VoiceCommand) -> Unit,
    onWakeWordDetected: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startListening { command ->
            when (command) {
                is VoiceCommand.WakeWordDetected -> {
                    onWakeWordDetected()
                    onDismiss()
                }
                is VoiceCommand.Search -> {
                    onSearch(command.query)
                    onDismiss()
                }
                is VoiceCommand.Play, is VoiceCommand.Pause, is VoiceCommand.TogglePlayPause,
                is VoiceCommand.Next, is VoiceCommand.Previous, 
                is VoiceCommand.Shuffle, is VoiceCommand.ShuffleOn, is VoiceCommand.ShuffleOff,
                is VoiceCommand.Repeat, is VoiceCommand.RepeatOne, is VoiceCommand.RepeatAll, is VoiceCommand.RepeatOff,
                is VoiceCommand.VolumeUp, is VoiceCommand.VolumeDown,
                is VoiceCommand.Mute, is VoiceCommand.Unmute,
                is VoiceCommand.SpeedUp, is VoiceCommand.SlowDown, is VoiceCommand.ResetSpeed,
                is VoiceCommand.SeekForward, is VoiceCommand.SeekBackward,
                is VoiceCommand.ToggleLike,
                is VoiceCommand.ClearQueue, is VoiceCommand.AddToQueue -> {
                    onPlaybackCommand(command)
                    onDismiss()
                }
                is VoiceCommand.SetDarkMode, is VoiceCommand.ToggleTheme,
                is VoiceCommand.ShowLyrics, is VoiceCommand.HideLyrics, is VoiceCommand.ToggleLyrics,
                is VoiceCommand.EnableVideo, is VoiceCommand.DisableVideo, is VoiceCommand.ToggleVideo,
                is VoiceCommand.ShowQueue,
                is VoiceCommand.OpenHome, is VoiceCommand.OpenLibrary,
                is VoiceCommand.OpenSearch, is VoiceCommand.OpenSettings -> {
                    onSettingsCommand(command)
                    onDismiss()
                }
                else -> {}
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    VoiceMicIconWithWaves(state = uiState)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = when (uiState) {
                            is VoiceUiState.Idle -> "Tap to speak"
                            is VoiceUiState.Listening -> "Listening..."
                            is VoiceUiState.Processing -> "Processing..."
                            is VoiceUiState.PartialResult -> "\"${(uiState as VoiceUiState.PartialResult).text}\""
                            is VoiceUiState.CommandRecognized -> "Command recognized"
                            is VoiceUiState.Error -> (uiState as VoiceUiState.Error).message
                            else -> "Tap to speak"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (uiState) {
                            is VoiceUiState.Listening -> "Say a command..."
                            is VoiceUiState.Processing -> "Please wait..."
                            else -> "Try: \"Hey Aura play\" • \"Next\" • \"Search songs\"\n\"Dark mode\" • \"Volume up\" • \"Shuffle on\""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Wake Words:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"Hey Aura\" • \"Hello Aura\" • \"Aura\" • \"Ok Aura\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Commands:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• \"Play\" • \"Pause\" • \"Next\" • \"Previous\"\n" +
                                        "• \"Search [song]\" • \"Volume up/down\"\n" +
                                        "• \"Dark mode\" • \"Show lyrics\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceMicIconWithWaves(state: VoiceUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state is VoiceUiState.Listening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val isListening = state is VoiceUiState.Listening
    
    val backgroundColor = when (state) {
        is VoiceUiState.Listening -> MaterialTheme.colorScheme.primary
        is VoiceUiState.Processing -> MaterialTheme.colorScheme.secondary
        is VoiceUiState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Wave circles (only visible when listening)
        if (isListening) {
            for (i in 0..2) {
                val waveScale = 1f + (i * 0.25f) + (scale - 1f) * 0.5f
                val alpha = 0.4f - (i * 0.12f)
                
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(waveScale)
                ) {
                    drawCircle(
                        color = backgroundColor.copy(alpha = alpha.coerceAtLeast(0f)),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        // Main circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(if (isListening) scale else 1f)
                .background(backgroundColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.mic),
                contentDescription = "Voice",
                modifier = Modifier.size(48.dp),
                tint = when (state) {
                    is VoiceUiState.Listening -> Color.White
                    is VoiceUiState.Processing -> Color.White
                    is VoiceUiState.Error -> Color.White
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

sealed class VoiceUiState
