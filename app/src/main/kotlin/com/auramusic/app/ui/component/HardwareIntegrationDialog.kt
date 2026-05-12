/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Audio device picker shown from the mini-player. Lists every audio
 * output the system currently exposes (Bluetooth, wired headphones,
 * USB, HDMI, dock, phone speaker) plus a Car Integration section
 * with steering-wheel and auto-play options.
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.auramusic.app.ui.component

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.core.content.ContextCompat
import com.auramusic.app.LocalHardwareIntegrationManager
import com.auramusic.app.R
import com.auramusic.app.hardware.HardwareIntegrationManager

enum class AudioOutputType {
    BLUETOOTH,
    WIRED_HEADPHONES,
    USB_HEADSET,
    HDMI,
    DOCK,
    PHONE_SPEAKER,
    OTHER,
}

data class AudioOutput(
    val id: Int,
    val name: String,
    val type: AudioOutputType,
    val isConnected: Boolean,
)

@Composable
fun HardwareIntegrationDialog(
    onDismiss: () -> Unit,
) {
    val manager = LocalHardwareIntegrationManager.current ?: run {
        onDismiss(); return
    }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var outputs by remember { mutableStateOf(loadAudioOutputs(audioManager)) }

    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var isUserDragging by remember { mutableStateOf(false) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        manager.bluetooth.refresh()
        outputs = loadAudioOutputs(audioManager)
    }

    DisposableEffect(Unit) {
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION" && !isUserDragging) {
                    val stream = intent.getIntExtra(
                        "android.media.EXTRA_VOLUME_STREAM_TYPE", -1
                    )
                    if (stream == AudioManager.STREAM_MUSIC) {
                        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    }
                }
            }
        }
        val deviceReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                outputs = loadAudioOutputs(audioManager)
                manager.bluetooth.refresh()
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    volumeReceiver,
                    IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
                    Context.RECEIVER_EXPORTED,
                )
                context.registerReceiver(
                    deviceReceiver,
                    IntentFilter().apply {
                        addAction(AudioManager.ACTION_HEADSET_PLUG)
                        addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
                        addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
                    },
                    Context.RECEIVER_EXPORTED,
                )
            } else {
                context.registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
                context.registerReceiver(
                    deviceReceiver,
                    IntentFilter().apply {
                        addAction(AudioManager.ACTION_HEADSET_PLUG)
                        addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
                        addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
                    },
                )
            }
        } catch (_: Exception) {
        }

        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                outputs = loadAudioOutputs(audioManager)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                outputs = loadAudioOutputs(audioManager)
            }
        }
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))

        onDispose {
            runCatching { context.unregisterReceiver(volumeReceiver) }
            runCatching { context.unregisterReceiver(deviceReceiver) }
            runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Audio devices",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            // Volume control
            VolumeSlider(
                value = currentVolume,
                max = maxVolume,
                onChange = {
                    isUserDragging = true
                    currentVolume = it
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        it.toInt(),
                        0,
                    )
                },
                onChangeFinished = { isUserDragging = false },
            )

            // Permission chip
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                AssistChip(
                    onClick = {
                        btPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN,
                            ),
                        )
                    },
                    label = { Text("Grant Bluetooth permission") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.bluetooth),
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }

            if (outputs.isEmpty()) {
                Text(
                    "No audio devices detected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                outputs.forEach { device ->
                    AudioOutputRow(device)
                }
            }

            Spacer(Modifier.height(8.dp))
            CarIntegrationSection(manager)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VolumeSlider(
    value: Float,
    max: Int,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.volume_up),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Slider(
                value = value,
                onValueChange = onChange,
                onValueChangeFinished = onChangeFinished,
                valueRange = 0f..max.toFloat(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${((value / max) * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AudioOutputRow(device: AudioOutput) {
    val containerColor = if (device.isConnected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (device.isConnected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    val iconRes = when (device.type) {
        AudioOutputType.BLUETOOTH -> R.drawable.bluetooth
        AudioOutputType.WIRED_HEADPHONES -> R.drawable.headset
        AudioOutputType.USB_HEADSET -> R.drawable.headset
        AudioOutputType.HDMI -> R.drawable.tv
        AudioOutputType.DOCK -> R.drawable.devices
        AudioOutputType.PHONE_SPEAKER -> R.drawable.smartphone
        AudioOutputType.OTHER -> R.drawable.speaker_group
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = containerColor,
        tonalElevation = if (device.isConnected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(onContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (device.isConnected) "Connected" else "Available",
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun CarIntegrationSection(manager: HardwareIntegrationManager) {
    val isConnected by manager.car.isConnected.collectAsState()
    val info by manager.car.connection.collectAsState()
    val steering by manager.car.steeringWheelControlsEnabled.collectAsState()
    val autoPlay by manager.car.autoPlayOnConnect.collectAsState()
    val enabled by manager.car.enabled.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.directions_car),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Car Integration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = info?.let { "${it.name} (${it.source})" }
                            ?: if (isConnected) "Connected" else "Not connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { manager.car.setEnabled(it) },
                )
            }
            ToggleRow(
                label = "Steering wheel controls",
                checked = steering,
                onCheckedChange = { manager.car.setSteeringWheelControlsEnabled(it) },
            )
            ToggleRow(
                label = "Auto-play on connect",
                checked = autoPlay,
                onCheckedChange = { manager.car.setAutoPlayOnConnect(it) },
            )
        }
    }
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

private fun loadAudioOutputs(audioManager: AudioManager): List<AudioOutput> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
    val list = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val mapped = list.mapNotNull { d ->
        val type = when (d.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioOutputType.BLUETOOTH
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioOutputType.WIRED_HEADPHONES
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> AudioOutputType.USB_HEADSET
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC -> AudioOutputType.HDMI
            AudioDeviceInfo.TYPE_DOCK -> AudioOutputType.DOCK
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputType.PHONE_SPEAKER
            else -> return@mapNotNull null
        }
        val nameSource: CharSequence = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            d.productName ?: ""
        } else ""
        val name = nameSource.toString().ifBlank {
            when (type) {
                AudioOutputType.PHONE_SPEAKER -> "This phone"
                AudioOutputType.BLUETOOTH -> "Bluetooth Device"
                AudioOutputType.WIRED_HEADPHONES -> "Wired Headphones"
                AudioOutputType.USB_HEADSET -> "USB Audio"
                AudioOutputType.HDMI -> "HDMI"
                AudioOutputType.DOCK -> "Dock"
                AudioOutputType.OTHER -> "Audio Device"
            }
        }
        AudioOutput(
            id = d.id,
            name = name,
            type = type,
            isConnected = true,
        )
    }
    val priority = mapOf(
        AudioOutputType.BLUETOOTH to 0,
        AudioOutputType.USB_HEADSET to 1,
        AudioOutputType.WIRED_HEADPHONES to 2,
        AudioOutputType.HDMI to 3,
        AudioOutputType.DOCK to 4,
        AudioOutputType.PHONE_SPEAKER to 5,
        AudioOutputType.OTHER to 6,
    )
    return mapped
        .distinctBy { "${it.type}-${it.name}" }
        .sortedBy { priority[it.type] ?: 99 }
}
