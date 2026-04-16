package com.auramusic.app.voice

import androidx.compose.animation.core.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.auramusic.app.R

@Composable
fun VoiceCommandDialog(
    viewModel: VoiceCommandViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onPlaybackCommand: (VoiceCommand) -> Unit,
    onSettingsCommand: (VoiceCommand) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startListening { command ->
            when (command) {
                is VoiceCommand.Search -> {
                    onSearch(command.query)
                    onDismiss()
                }
                is VoiceCommand.Play, is VoiceCommand.Pause, is VoiceCommand.TogglePlayPause,
                is VoiceCommand.Next, is VoiceCommand.Previous, is VoiceCommand.Shuffle,
                is VoiceCommand.Repeat, is VoiceCommand.VolumeUp, is VoiceCommand.VolumeDown,
                is VoiceCommand.Mute, is VoiceCommand.Unmute, is VoiceCommand.ToggleLike -> {
                    onPlaybackCommand(command)
                    onDismiss()
                }
                is VoiceCommand.SetDarkMode, is VoiceCommand.ToggleTheme,
                is VoiceCommand.ShowLyrics, is VoiceCommand.HideLyrics,
                is VoiceCommand.EnableVideo, is VoiceCommand.DisableVideo -> {
                    onSettingsCommand(command)
                    onDismiss()
                }
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
                    // Close button
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

                    // Microphone icon with animation
                    VoiceMicIcon(state = uiState)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Status text
                    Text(
                        text = when (uiState) {
                            is VoiceUiState.Idle -> "Tap to speak"
                            is VoiceUiState.Listening -> "Listening..."
                            is VoiceUiState.Processing -> "Processing..."
                            is VoiceUiState.PartialResult -> "\"${(uiState as VoiceUiState.PartialResult).text}\""
                            is VoiceUiState.CommandRecognized -> "Command recognized"
                            is VoiceUiState.Error -> (uiState as VoiceUiState.Error).message
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hint text
                    Text(
                        text = when (uiState) {
                            is VoiceUiState.Listening -> "Say a command..."
                            is VoiceUiState.Processing -> "Please wait..."
                            else -> "Try: \"Play\", \"Next\", \"Search songs\", \"Dark mode\""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Command examples
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
                                text = "Commands:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• \"Play\" • \"Pause\" • \"Next\" • \"Previous\"\n" +
                                        "• \"Search [song name]\" • \"Volume up/down\"\n" +
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
fun VoiceMicIcon(state: VoiceUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state is VoiceUiState.Listening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val backgroundColor = when (state) {
        is VoiceUiState.Listening -> MaterialTheme.colorScheme.primary
        is VoiceUiState.Processing -> MaterialTheme.colorScheme.secondary
        is VoiceUiState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(if (state is VoiceUiState.Listening) scale else 1f)
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

sealed class VoiceUiState {
    data object Idle : VoiceUiState()
    data class Listening(val amplitude: Float = 0f) : VoiceUiState()
    data object Processing : VoiceUiState()
    data class PartialResult(val text: String) : VoiceUiState()
    data object CommandRecognized : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}
