/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * HardwareIntegrationManager owns the hardware feature managers
 * (Bluetooth audio + Car) and exposes a derived "active hardware"
 * StateFlow used by the mini-player to swap its icon depending on
 * what is currently connected.
 */
package com.auramusic.app.hardware

import android.content.Context
import com.auramusic.app.hardware.bluetooth.BluetoothLeAudioManager
import com.auramusic.app.hardware.car.CarIntegrationManager
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
    CAR,
}

@Singleton
class HardwareIntegrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val bluetooth: BluetoothLeAudioManager = BluetoothLeAudioManager(context, scope)
    val car: CarIntegrationManager = CarIntegrationManager(context, scope)

    private val carActive: StateFlow<Boolean> =
        combine(car.enabled, car.isConnected) { enabled, connected -> enabled && connected }
            .stateIn(scope, SharingStarted.Eagerly, false)

    private val bluetoothActive: StateFlow<Boolean> =
        bluetooth.devices.map { list -> list.any { it.isConnected } }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Derived top-priority "currently active" hardware.
     * Order: Car > Bluetooth > None.
     */
    val activeHardware: StateFlow<ActiveHardware> = combine(
        carActive,
        bluetoothActive,
    ) { carOn, btOn ->
        when {
            carOn -> ActiveHardware.CAR
            btOn -> ActiveHardware.BLUETOOTH
            else -> ActiveHardware.NONE
        }
    }.stateIn(scope, SharingStarted.Eagerly, ActiveHardware.NONE)

    private var started = false

    fun start() {
        if (started) return
        started = true
        bluetooth.start()
        car.start()
    }

    fun stop() {
        if (!started) return
        started = false
        bluetooth.stop()
        car.stop()
    }

    fun refreshAll() {
        bluetooth.refresh()
        car.refresh()
    }
}
