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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val bluetooth: BluetoothLeAudioManager = BluetoothLeAudioManager(context, scope)
    val speakerMesh: SmartSpeakerMeshManager = SmartSpeakerMeshManager(context, scope)
    val wearable: WearableHapticsManager = WearableHapticsManager(context, scope)
    val car: CarIntegrationManager = CarIntegrationManager(context, scope)
    val proAudio: ProAudioInterfaceManager = ProAudioInterfaceManager(context, scope)

    private val carActive: StateFlow<Boolean> =
        combine(car.enabled, car.isConnected) { enabled, connected -> enabled && connected }
            .stateIn(scope, SharingStarted.Eagerly, false)

    private val proAudioActive: StateFlow<Boolean> =
        combine(proAudio.enabled, proAudio.activeDevice) { enabled, device ->
            enabled && device != null
        }.stateIn(scope, SharingStarted.Eagerly, false)

    private val bluetoothActive: StateFlow<Boolean> =
        bluetooth.devices.map { list -> list.any { it.isConnected } }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Derived top-priority "currently active" hardware. Order is:
     *   Car > Pro Audio > Speaker Mesh > Bluetooth > Wearable > None.
     */
    val activeHardware: StateFlow<ActiveHardware> = combine(
        carActive,
        proAudioActive,
        speakerMesh.meshActive,
        bluetoothActive,
        wearable.enabled,
    ) { carOn, proOn, meshOn, btOn, wearableOn ->
        when {
            carOn -> ActiveHardware.CAR
            proOn -> ActiveHardware.PRO_AUDIO
            meshOn -> ActiveHardware.SPEAKER_MESH
            btOn -> ActiveHardware.BLUETOOTH
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
