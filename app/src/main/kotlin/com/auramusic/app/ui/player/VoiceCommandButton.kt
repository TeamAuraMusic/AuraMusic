package com.auramusic.app.ui.player

import android.Manifest
import android.content.Context
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.exoplayer.ExoPlayer
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.voice.VoiceCommand
import com.auramusic.app.voice.VoiceCommandDialog
import com.auramusic.app.voice.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    handleSettingsCommand(command, conn, onNavigate)
                }
            }
        )
    }
}

private fun handlePlaybackCommand(command: VoiceCommand, playerConnection: PlayerConnection) {
    val scope = CoroutineScope(Dispatchers.Main)
    scope.launch {
        when (command) {
            is VoiceCommand.Play -> playerConnection.play()
            is VoiceCommand.Pause -> playerConnection.pause()
            is VoiceCommand.TogglePlayPause -> playerConnection.togglePlayPause()
            is VoiceCommand.Next -> playerConnection.seekToNext()
            is VoiceCommand.Previous -> playerConnection.seekToPrevious()
            is VoiceCommand.Shuffle -> {
                withContext(Dispatchers.Main) {
                    val currentShuffle = playerConnection.shuffleModeEnabled.value
                    playerConnection.player.shuffleModeEnabled = !currentShuffle
                }
            }
            is VoiceCommand.ShuffleOn -> {
                withContext(Dispatchers.Main) {
                    playerConnection.player.shuffleModeEnabled = true
                }
            }
            is VoiceCommand.ShuffleOff -> {
                withContext(Dispatchers.Main) {
                    playerConnection.player.shuffleModeEnabled = false
                }
            }
            is VoiceCommand.Repeat -> {
                withContext(Dispatchers.Main) {
                    val player = playerConnection.player
                    player.repeatMode = when (player.repeatMode) {
                        ExoPlayer.REPEAT_MODE_OFF -> ExoPlayer.REPEAT_MODE_ALL
                        ExoPlayer.REPEAT_MODE_ALL -> ExoPlayer.REPEAT_MODE_ONE
                        else -> ExoPlayer.REPEAT_MODE_OFF
                    }
                }
            }
            is VoiceCommand.RepeatOne -> {
                withContext(Dispatchers.Main) {
                    playerConnection.player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                }
            }
            is VoiceCommand.RepeatAll -> {
                withContext(Dispatchers.Main) {
                    playerConnection.player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                }
            }
            is VoiceCommand.RepeatOff -> {
                withContext(Dispatchers.Main) {
                    playerConnection.player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                }
            }
            is VoiceCommand.SeekForward -> {
                withContext(Dispatchers.Main) {
                    val player = playerConnection.player
                    val newPosition = (player.currentPosition + command.milliseconds).coerceAtMost(player.duration)
                    player.seekTo(newPosition)
                }
            }
            is VoiceCommand.SeekBackward -> {
                withContext(Dispatchers.Main) {
                    val player = playerConnection.player
                    val newPosition = (player.currentPosition - command.milliseconds).coerceAtLeast(0)
                    player.seekTo(newPosition)
                }
            }
            is VoiceCommand.VolumeUp -> {
                withContext(Dispatchers.Main) {
                    val player = playerConnection.player
                    player.volume = (player.volume + 0.1f).coerceAtMost(1f)
                }
            }
            is VoiceCommand.VolumeDown -> {
                withContext(Dispatchers.Main) {
                    val player = playerConnection.player
                    player.volume = (player.volume - 0.1f).coerceAtLeast(0f)
                }
            }
            is VoiceCommand.Mute -> playerConnection.setMuted(true)
            is VoiceCommand.Unmute -> playerConnection.setMuted(false)
            is VoiceCommand.SpeedUp -> {
                withContext(Dispatchers.Main) {
                    val player = playerConnection.player
                    val newSpeed = (player.playbackParameters.speed * 1.25f).coerceAtMost(2.0f)
                    player.setPlaybackSpeed(newSpeed)
                }
            }
            is VoiceCommand.SlowDown -> {
                withContext(Dispatchers.Main) {
                    val player = playerConnection.player
                    val newSpeed = (player.playbackParameters.speed * 0.75f).coerceAtLeast(0.5f)
                    player.setPlaybackSpeed(newSpeed)
                }
            }
            is VoiceCommand.ResetSpeed -> {
                withContext(Dispatchers.Main) {
                    playerConnection.player.setPlaybackSpeed(1.0f)
                }
            }
            is VoiceCommand.ToggleLike -> playerConnection.toggleLike()
            is VoiceCommand.ClearQueue -> {
                withContext(Dispatchers.Main) {
                    playerConnection.player.clearMediaItems()
                }
            }
            is VoiceCommand.AddToQueue -> {
                // Already handled in search - add current song to queue
            }
            else -> {}
        }
    }
}

private fun handleSettingsCommand(command: VoiceCommand, playerConnection: PlayerConnection, onNavigate: (String) -> Unit) {
    val context = playerConnection.service as? Context ?: return
    val scope = CoroutineScope(Dispatchers.Main)
    
    scope.launch {
        when (command) {
            is VoiceCommand.SetDarkMode -> {
                val darkModeKey = stringPreferencesKey("darkMode")
                context.dataStore.edit { prefs ->
                    prefs[darkModeKey] = if (command.enabled) "ON" else "OFF"
                }
            }
            is VoiceCommand.ToggleTheme -> {
                val darkModeKey = stringPreferencesKey("darkMode")
                val current = context.dataStore.data.map { prefs ->
                    prefs[darkModeKey] ?: "AUTO"
                }.first()
                val newMode = when (current) {
                    "ON" -> "OFF"
                    "OFF" -> "ON"
                    else -> "ON"
                }
                context.dataStore.edit { prefs ->
                    prefs[darkModeKey] = newMode
                }
            }
            is VoiceCommand.ShowLyrics -> {
                val showLyricsKey = booleanPreferencesKey("showLyrics")
                context.dataStore.edit { prefs ->
                    prefs[showLyricsKey] = true
                }
            }
            is VoiceCommand.HideLyrics -> {
                val showLyricsKey = booleanPreferencesKey("showLyrics")
                context.dataStore.edit { prefs ->
                    prefs[showLyricsKey] = false
                }
            }
            is VoiceCommand.ToggleLyrics -> {
                val showLyricsKey = booleanPreferencesKey("showLyrics")
                val current = context.dataStore.data.map { prefs ->
                    prefs[showLyricsKey] ?: false
                }.first()
                context.dataStore.edit { prefs ->
                    prefs[showLyricsKey] = !current
                }
            }
            is VoiceCommand.EnableVideo -> playerConnection.toggleVideoMode()
            is VoiceCommand.DisableVideo -> playerConnection.toggleVideoMode()
            is VoiceCommand.ToggleVideo -> playerConnection.toggleVideoMode()
            is VoiceCommand.ShowQueue -> withContext(Dispatchers.Main) { onNavigate("queue") }
            is VoiceCommand.OpenHome -> withContext(Dispatchers.Main) { onNavigate("home") }
            is VoiceCommand.OpenLibrary -> withContext(Dispatchers.Main) { onNavigate("library") }
            is VoiceCommand.OpenSearch -> withContext(Dispatchers.Main) { onNavigate("search") }
            is VoiceCommand.OpenSettings -> withContext(Dispatchers.Main) { onNavigate("settings") }
            else -> {}
        }
    }
}
