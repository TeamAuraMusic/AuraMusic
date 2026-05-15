/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Music alarm settings: time picker, repeat, song-source picker
 * (downloads / cached / playlist) and per-source song selection.
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.auramusic.app.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.auramusic.app.LocalDatabase
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.alarm.AlarmScheduler
import com.auramusic.app.alarm.AlarmSource
import com.auramusic.app.constants.AlarmEnabledKey
import com.auramusic.app.constants.AlarmFadeInKey
import com.auramusic.app.constants.AlarmHourKey
import com.auramusic.app.constants.AlarmMinuteKey
import com.auramusic.app.constants.AlarmRepeatDailyKey
import com.auramusic.app.constants.AlarmShuffleKey
import com.auramusic.app.constants.AlarmSnoozeMinutesKey
import com.auramusic.app.constants.AlarmSongIdsKey
import com.auramusic.app.constants.AlarmSourceKey
import com.auramusic.app.constants.AlarmVibrateKey
import com.auramusic.app.constants.AlarmVolumeKey
import com.auramusic.app.constants.SongSortType
import com.auramusic.app.db.entities.Song
import com.auramusic.app.ui.utils.backToMain
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.viewmodels.CachePlaylistViewModel
import com.auramusic.app.ui.component.IconButton as MIconButton

@Composable
fun AlarmSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (alarmEnabled, onAlarmEnabledChange) = rememberPreference(AlarmEnabledKey, false)
    val (alarmHour, onAlarmHourChange) = rememberPreference(AlarmHourKey, 7)
    val (alarmMinute, onAlarmMinuteChange) = rememberPreference(AlarmMinuteKey, 0)
    val (repeatDaily, onRepeatDailyChange) = rememberPreference(AlarmRepeatDailyKey, true)
    val (sourceName, onSourceNameChange) = rememberPreference(AlarmSourceKey, AlarmSource.DOWNLOADS.name)
    val source = AlarmSource.fromName(sourceName)
    val (songIdsCsv, onSongIdsCsvChange) = rememberPreference(AlarmSongIdsKey, "")
    val (vibrate, onVibrateChange) = rememberPreference(AlarmVibrateKey, true)
    val (volume, onVolumeChange) = rememberPreference(AlarmVolumeKey, 0.85f)
    val (shuffle, onShuffleChange) = rememberPreference(AlarmShuffleKey, false)
    val (snoozeMinutes, onSnoozeMinutesChange) = rememberPreference(AlarmSnoozeMinutesKey, 5)
    val (fadeIn, onFadeInChange) = rememberPreference(AlarmFadeInKey, true)

    val selectedSongIds = remember(songIdsCsv) {
        songIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var showSongPicker by remember { mutableStateOf(false) }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (alarmEnabled) {
            AlarmScheduler.schedule(
                context,
                AlarmScheduler.computeNextTrigger(alarmHour, alarmMinute),
            )
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user choice respected */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        // Big time card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (alarmEnabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "%02d:%02d".format(alarmHour, alarmMinute),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (alarmEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (alarmEnabled) {
                        val target = AlarmScheduler.computeNextTrigger(alarmHour, alarmMinute)
                        "Rings " + relativeTime(target)
                    } else "Tap to edit time",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Enable alarm", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = alarmEnabled,
                        onCheckedChange = { enabled ->
                            onAlarmEnabledChange(enabled)
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    !AlarmScheduler.canScheduleExact(context)
                                ) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData(android.net.Uri.parse("package:${context.packageName}"))
                                    runCatching { exactAlarmLauncher.launch(intent) }
                                } else {
                                    AlarmScheduler.schedule(
                                        context,
                                        AlarmScheduler.computeNextTrigger(alarmHour, alarmMinute),
                                    )
                                }
                            } else {
                                AlarmScheduler.cancel(context)
                            }
                        },
                    )
                }
            }
        }

        // Source picker
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Wake up to", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AlarmSource.values().forEach { s ->
                        FilterChip(
                            selected = source == s,
                            onClick = {
                                onSourceNameChange(s.name)
                                onSongIdsCsvChange("")
                            },
                            label = { Text(s.displayName) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showSongPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedSongIds.isEmpty()) "Choose songs"
                        else "${selectedSongIds.size} song(s) selected",
                    )
                }
            }
        }

        // Behaviour
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Behaviour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                ToggleRow(label = "Repeat daily", checked = repeatDaily, onCheckedChange = onRepeatDailyChange)
                ToggleRow(label = "Shuffle songs", checked = shuffle, onCheckedChange = onShuffleChange)
                ToggleRow(label = "Vibrate", checked = vibrate, onCheckedChange = onVibrateChange)
                ToggleRow(label = "Fade-in volume", checked = fadeIn, onCheckedChange = onFadeInChange)
            }
        }

        // Volume / snooze
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Volume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Alarm volume: ${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(value = volume, onValueChange = onVolumeChange, valueRange = 0.1f..1f)
                Text("Snooze duration: $snoozeMinutes min", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = snoozeMinutes.toFloat(),
                    onValueChange = { onSnoozeMinutesChange(it.toInt()) },
                    valueRange = 1f..30f,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = alarmHour,
            initialMinute = alarmMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Alarm time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAlarmHourChange(pickerState.hour)
                    onAlarmMinuteChange(pickerState.minute)
                    showTimePicker = false
                    if (alarmEnabled) {
                        AlarmScheduler.schedule(
                            context,
                            AlarmScheduler.computeNextTrigger(pickerState.hour, pickerState.minute),
                        )
                    }
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }

    if (showSongPicker) {
        AlarmSongPickerDialog(
            source = source,
            initialSelected = selectedSongIds,
            onDismiss = { showSongPicker = false },
            onConfirm = { picked ->
                onSongIdsCsvChange(picked.joinToString(","))
                showSongPicker = false
            },
        )
    }

    TopAppBar(
        title = { Text(stringRes(R.string.alarm_title)) },
        navigationIcon = {
            MIconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AlarmSongPickerDialog(
    source: AlarmSource,
    initialSelected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val database = LocalDatabase.current
    var selected by remember(source) { mutableStateOf(initialSelected) }

    val songs by when (source) {
        AlarmSource.DOWNLOADS -> database
            .downloadedSongs(SongSortType.CREATE_DATE, true)
            .collectAsState(initial = emptyList())
        AlarmSource.CACHED -> {
            val vm: CachePlaylistViewModel = hiltViewModel()
            vm.cachedSongs.collectAsState()
        }
        AlarmSource.PLAYLIST -> database
            .downloadedSongs(SongSortType.CREATE_DATE, true)
            .collectAsState(initial = emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick songs (${selected.size})") },
        text = {
            if (songs.isEmpty()) {
                Text(
                    when (source) {
                        AlarmSource.DOWNLOADS -> "No downloaded songs."
                        AlarmSource.CACHED -> "No cached songs."
                        AlarmSource.PLAYLIST -> "No songs in your playlists."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(songs, key = { it.song.id }) { song ->
                        SongPickerRow(
                            song = song,
                            checked = song.song.id in selected,
                            onToggle = {
                                selected = if (song.song.id in selected) {
                                    selected - song.song.id
                                } else selected + song.song.id
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected.toList()) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SongPickerRow(
    song: Song,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artists = song.artists.joinToString { it.name }
            if (artists.isNotBlank()) {
                Text(
                    artists,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun relativeTime(targetMillis: Long): String {
    val deltaMs = (targetMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    val totalMin = deltaMs / 60_000
    val hours = totalMin / 60
    val minutes = totalMin % 60
    return when {
        hours == 0L && minutes <= 1L -> "in a moment"
        hours == 0L -> "in $minutes min"
        hours < 24 && minutes == 0L -> "in ${hours}h"
        hours < 24 -> "in ${hours}h ${minutes}m"
        else -> "tomorrow"
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
