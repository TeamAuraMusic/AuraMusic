package com.auramusic.app.ui.player

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.voice.VoiceCommand
import com.auramusic.app.voice.VoiceCommandDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun VoiceCommandButton(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showVoiceDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            showVoiceDialog = true
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        FilledIconButton(
            onClick = {
                if (hasPermission) {
                    showVoiceDialog = true
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.size(42.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.mic),
                contentDescription = "Voice command",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showVoiceDialog) {
        VoiceCommandDialog(
            onDismiss = { showVoiceDialog = false },
            onSearch = onSearch,
            onPlaybackCommand = { command ->
                playerConnection?.let { conn ->
                    handlePlaybackCommand(command, conn)
                }
            },
            onSettingsCommand = { command ->
                playerConnection?.let { conn ->
                    handleSettingsCommand(command, conn)
                }
            }
        )
    }
}

private fun handlePlaybackCommand(command: VoiceCommand, playerConnection: com.auramusic.app.PlayerConnection) {
    val player = playerConnection.player
    CoroutineScope(Dispatchers.Main).launch {
        when (command) {
            is VoiceCommand.Play -> player.play()
            is VoiceCommand.Pause -> player.pause()
            is VoiceCommand.TogglePlayPause -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            is VoiceCommand.Next -> player.seekToNext()
            is VoiceCommand.Previous -> player.seekToPrevious()
            is VoiceCommand.Shuffle -> player.shuffleModeEnabled = !player.shuffleModeEnabled
            is VoiceCommand.Repeat -> player.repeatMode = when (player.repeatMode) {
                ExoPlayer.REPEAT_MODE_OFF -> ExoPlayer.REPEAT_MODE_ALL
                ExoPlayer.REPEAT_MODE_ALL -> ExoPlayer.REPEAT_MODE_ONE
                else -> ExoPlayer.REPEAT_MODE_OFF
            }
            is VoiceCommand.VolumeUp -> player.volume = (player.volume + 0.1f).coerceAtMost(1f)
            is VoiceCommand.VolumeDown -> player.volume = (player.volume - 0.1f).coerceAtLeast(0f)
            is VoiceCommand.Mute -> player.volume = 0f
            is VoiceCommand.Unmute -> player.volume = 1f
            is VoiceCommand.ToggleLike -> playerConnection.toggleLike()
            else -> {}
        }
    }
}

private fun handleSettingsCommand(command: VoiceCommand, playerConnection: com.auramusic.app.PlayerConnection) {
    CoroutineScope(Dispatchers.Main).launch {
        when (command) {
            is VoiceCommand.SetDarkMode -> {}
            is VoiceCommand.ToggleTheme -> {}
            is VoiceCommand.ShowLyrics -> {}
            is VoiceCommand.HideLyrics -> {}
            is VoiceCommand.EnableVideo -> playerConnection.toggleVideoMode(enabled = true)
            is VoiceCommand.DisableVideo -> playerConnection.toggleVideoMode(enabled = false)
            else -> {}
        }
    }
}
