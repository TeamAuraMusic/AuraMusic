/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Interactive UI for the Hardware Integration & Smart Device Ecosystem.
 * Each section is wired to the corresponding sub-manager exposed by
 * [HardwareIntegrationManager].
 */
package com.auramusic.app.ui.component

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.auramusic.app.LocalHardwareIntegrationManager
import com.auramusic.app.R
import com.auramusic.app.hardware.HardwareIntegrationManager
import com.auramusic.app.hardware.wearable.HapticPattern

@Composable
fun HardwareIntegrationDialog(
    onDismiss: () -> Unit,
) {
    val manager = LocalHardwareIntegrationManager.current ?: run {
        onDismiss(); return
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { manager.bluetooth.refresh() }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(horizontal = 16.dp),
        icon = {
            Icon(
                painter = painterResource(R.drawable.devices),
                contentDescription = null,
            )
        },
        title = { Text("Hardware Integration") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(android.R.string.ok))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Connect and control multiple audio devices for an immersive experience.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                // Request runtime permissions if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !manager.bluetooth.hasBluetoothPermission()
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

                BluetoothLeAudioCard(manager)
                SmartSpeakerMeshCard(manager)
                WearableHapticsCard(manager)
                CarIntegrationCard(manager)
                ProAudioCard(manager)

                Text(
                    "Detected devices update automatically as connections change.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun SectionHeader(
    iconRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HardwareCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@Composable
private fun BluetoothLeAudioCard(manager: HardwareIntegrationManager) {
    val devices by manager.bluetooth.devices.collectAsState()
    val multi by manager.bluetooth.multiDeviceSync.collectAsState()
    val enabled by manager.bluetooth.isEnabled.collectAsState()

    HardwareCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                iconRes = R.drawable.bluetooth,
                title = "Bluetooth LE Audio",
                subtitle = if (enabled) "${devices.count { it.isConnected }} connected · ${devices.size} known"
                else "Bluetooth disabled",
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = multi,
                onCheckedChange = { manager.bluetooth.setMultiDeviceSync(it) },
                enabled = enabled,
            )
        }
        if (devices.isEmpty()) {
            Text(
                if (enabled) "No Bluetooth audio devices currently connected."
                else "Enable Bluetooth and connect a device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            devices.forEach { d ->
                DeviceRow(
                    name = d.name,
                    badge = if (d.supportsLeAudio) "LE" else null,
                    detail = "${d.codec ?: ""}${if (d.isConnected) " · Connected" else ""}",
                    isActive = d.isPrimary,
                    onClick = { manager.bluetooth.setPrimaryDevice(d.address) },
                )
            }
        }
    }
}

@Composable
private fun SmartSpeakerMeshCard(manager: HardwareIntegrationManager) {
    val speakers by manager.speakerMesh.speakers.collectAsState()
    val active by manager.speakerMesh.meshActive.collectAsState()
    val delay by manager.speakerMesh.syncDelayMs.collectAsState()

    HardwareCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                iconRes = R.drawable.speaker_group,
                title = "Smart Speaker Mesh",
                subtitle = "${speakers.size} speaker(s) discovered",
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = active,
                onCheckedChange = { manager.speakerMesh.setMeshActive(it) },
            )
        }
        if (speakers.isEmpty()) {
            Text(
                "Searching network for compatible speakers…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            speakers.forEach { s ->
                DeviceRow(
                    name = s.name,
                    badge = s.type,
                    detail = s.host?.let { "$it:${s.port}" } ?: "",
                    isActive = s.isActive,
                    onClick = { manager.speakerMesh.toggleSpeaker(s.id) },
                )
            }
            Text("Sync delay: ${delay}ms", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = delay.toFloat(),
                onValueChange = { manager.speakerMesh.setSyncDelay(it.toInt()) },
                valueRange = 0f..500f,
            )
        }
    }
}

@Composable
private fun WearableHapticsCard(manager: HardwareIntegrationManager) {
    val enabled by manager.wearable.enabled.collectAsState()
    val pattern by manager.wearable.pattern.collectAsState()
    val intensity by manager.wearable.intensity.collectAsState()
    val bpm by manager.wearable.bpm.collectAsState()

    HardwareCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                iconRes = R.drawable.vibration,
                title = "Wearable Haptics",
                subtitle = if (manager.wearable.hasVibrator())
                    "Beat-synced vibration patterns"
                else "Vibrator unavailable",
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = { manager.wearable.setEnabled(it) },
                enabled = manager.wearable.hasVibrator(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HapticPattern.values().forEach { p ->
                AssistChip(
                    onClick = {
                        manager.wearable.setPattern(p)
                        if (enabled) manager.wearable.preview()
                    },
                    label = { Text(p.displayName) },
                    colors = if (p == pattern) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    } else AssistChipDefaults.assistChipColors(),
                )
            }
        }
        Text("Intensity: ${(intensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = intensity,
            onValueChange = { manager.wearable.setIntensity(it) },
            valueRange = 0f..1f,
        )
        Text("Tempo: $bpm BPM", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = bpm.toFloat(),
            onValueChange = { manager.wearable.setBpm(it.toInt()) },
            valueRange = 40f..200f,
        )
        TextButton(onClick = { manager.wearable.preview() }) {
            Text("Preview")
        }
    }
}

@Composable
private fun CarIntegrationCard(manager: HardwareIntegrationManager) {
    val isConnected by manager.car.isConnected.collectAsState()
    val info by manager.car.connection.collectAsState()
    val steering by manager.car.steeringWheelControlsEnabled.collectAsState()
    val autoPlay by manager.car.autoPlayOnConnect.collectAsState()
    val enabled by manager.car.enabled.collectAsState()

    HardwareCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                iconRes = R.drawable.directions_car,
                title = "Car Integration",
                subtitle = info?.let { "${it.name} (${it.source})" }
                    ?: if (isConnected) "Connected" else "Not connected",
                modifier = Modifier.weight(1f),
            )
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

@Composable
private fun ProAudioCard(manager: HardwareIntegrationManager) {
    val devices by manager.proAudio.devices.collectAsState()
    val enabled by manager.proAudio.enabled.collectAsState()
    val lowLatency by manager.proAudio.lowLatency.collectAsState()
    val nativeRate by manager.proAudio.nativeSampleRate.collectAsState()
    val buffer by manager.proAudio.bufferSizeFrames.collectAsState()

    HardwareCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                iconRes = R.drawable.mic,
                title = "Pro Audio Interface",
                subtitle = "${devices.size} device(s) · ${nativeRate}Hz · ${buffer} frames",
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = { manager.proAudio.setEnabled(it) },
                enabled = devices.isNotEmpty(),
            )
        }
        if (devices.isEmpty()) {
            Text(
                "Connect a USB / HDMI audio interface to enable studio routing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            devices.forEach { d ->
                DeviceRow(
                    name = d.name,
                    badge = d.type,
                    detail = buildString {
                        if (d.sampleRates.isNotEmpty()) append("${d.sampleRates.first()}Hz")
                        if (d.channelCounts.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append("${d.channelCounts.first()}ch")
                        }
                    },
                    isActive = d.isDefault,
                    onClick = { manager.proAudio.setActiveDevice(d.id) },
                )
            }
            ToggleRow(
                label = "Low-latency studio path",
                checked = lowLatency,
                onCheckedChange = { manager.proAudio.setLowLatency(it) },
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

@Composable
private fun DeviceRow(
    name: String,
    badge: String?,
    detail: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent,
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!badge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
