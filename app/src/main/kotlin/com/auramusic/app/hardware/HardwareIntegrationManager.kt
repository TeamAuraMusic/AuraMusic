/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * HardwareIntegrationManager owns the five hardware feature managers
 * and exposes a derived "active hardware" StateFlow used by the
 * MiniPlayer to swap its icon depending on which device is currently
 * driving audio (or vibrating in sync, in the case of wearables).
 */
package com.auramusic.app.hardware

import android.content.Context
import com.auramusic.app.hardware.audio.ProAudioInterfaceManager
import com.auramusic.app.hardware.bluetooth.BluetoothLeAudioManager
import com.auramusic.app.hardware.car.CarIntegrationManager
import com.auramusic.app.hardware.speaker.SmartSpeakerMeshManager
import com.auramusic.app.hardware.wearable.WearableHapticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** Highest-priority hardware that the mini-player should reflect. */
enum class ActiveHardware {
    NONE,
    BLUETOOTH,
    SPEAKER_MESH,
    WEARABLE,
    CAR,
    PRO_AUDIO,
}

@Singleton
class HardwareIntegrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob())

    val bluetooth: BluetoothLeAudioManager = BluetoothLeAudioManager(context, scope)
    val speakerMesh: SmartSpeakerMeshManager = SmartSpeakerMeshManager(context, scope)
    val wearable: WearableHapticsManager = WearableHapticsManager(context, scope)
    val car: CarIntegrationManager = CarIntegrationManager(context, scope)
    val proAudio: ProAudioInterfaceManager = ProAudioInterfaceManager(context, scope)

    /**
     * Derived top-priority "currently active" hardware. Order is:
     *   Car > Pro Audio > Speaker Mesh > Bluetooth > Wearable > None.
     */
    @Suppress("UNCHECKED_CAST")
    val activeHardware: StateFlow<ActiveHardware> = combine(
        listOf(
            car.isConnected,
            proAudio.activeDevice,
            proAudio.enabled,
            speakerMesh.meshActive,
            bluetooth.activeDevice,
            wearable.enabled,
        )
    ) { values ->
        val carConnected = values[0] as Boolean
        val proDevice = values[1]
        val proEnabled = values[2] as Boolean
        val meshOn = values[3] as Boolean
        val bt = values[4]
        val wearableOn = values[5] as Boolean
        when {
            carConnected -> ActiveHardware.CAR
            proDevice != null && proEnabled -> ActiveHardware.PRO_AUDIO
            meshOn -> ActiveHardware.SPEAKER_MESH
            bt != null && (bt as com.auramusic.app.hardware.bluetooth.BluetoothAudioDevice).isConnected -> ActiveHardware.BLUETOOTH
            wearableOn -> ActiveHardware.WEARABLE
            else -> ActiveHardware.NONE
        }
    }.stateIn(scope, SharingStarted.Eagerly, ActiveHardware.NONE)

    private var started = false

    fun start() {
        if (started) return
        started = true
        bluetooth.start()
        car.start()
        proAudio.start()
        speakerMesh.startDiscovery()
    }

    fun stop() {
        if (!started) return
        started = false
        bluetooth.stop()
        car.stop()
        proAudio.stop()
        speakerMesh.stopDiscovery()
        wearable.stop()
    }

    fun refreshAll() {
        bluetooth.refresh()
        car.refresh()
        proAudio.refresh()
    }
}
