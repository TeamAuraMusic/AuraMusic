/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Car Integration manager – detects when the device is connected to
 * a car infotainment system (via Android Auto / Bluetooth car kit /
 * UiMode) and exposes the state so that:
 *   • the player UI shows the car icon,
 *   • the MusicService keeps the right MediaSession actions enabled
 *     for steering-wheel control (PLAY/PAUSE/SKIP_NEXT/SKIP_PREV/
 *     SEEK/SHUFFLE).
 */
package com.auramusic.app.hardware.car

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

data class CarConnection(
    val name: String,
    val source: String, // "Android Auto" | "Bluetooth" | "UI Mode"
)

class CarIntegrationManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val uiModeManager: UiModeManager? =
        context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connection = MutableStateFlow<CarConnection?>(null)
    val connection: StateFlow<CarConnection?> = _connection.asStateFlow()

    private val _steeringWheelControlsEnabled = MutableStateFlow(true)
    val steeringWheelControlsEnabled: StateFlow<Boolean> = _steeringWheelControlsEnabled.asStateFlow()

    private val _autoPlayOnConnect = MutableStateFlow(false)
    val autoPlayOnConnect: StateFlow<Boolean> = _autoPlayOnConnect.asStateFlow()

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private var receiver: BroadcastReceiver? = null
    private var a2dp: BluetoothA2dp? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = proxy as BluetoothA2dp
                refresh()
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = null
                refresh()
            }
        }
    }

    fun start() {
        if (receiver != null) return
        runCatching { bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP) }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                refresh()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction(UiModeManager.ACTION_ENTER_CAR_MODE)
            addAction(UiModeManager.ACTION_EXIT_CAR_MODE)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to register Car receiver")
        }
        refresh()
    }

    fun stop() {
        runCatching { receiver?.let { context.unregisterReceiver(it) } }
        receiver = null
        a2dp?.let { runCatching { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) } }
        a2dp = null
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        val carModeActive = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_CAR

        // Only treat a Bluetooth car kit as connected when it has an *active*
        // A2DP connection – never based purely on bonded/paired state.
        val carBt: BluetoothDevice? = try {
            a2dp?.connectedDevices?.firstOrNull { dev ->
                val cls = dev.bluetoothClass?.deviceClass
                cls == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                    cls == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
            }
        } catch (_: SecurityException) {
            null
        }

        val info = when {
            carModeActive -> CarConnection("Car Mode", "UI Mode")
            carBt != null -> {
                val name = try { carBt.name } catch (_: SecurityException) { "Car" }
                CarConnection(name ?: "Car", "Bluetooth")
            }
            else -> null
        }
        _connection.value = info
        _isConnected.value = info != null
    }

    fun setEnabled(value: Boolean) { _enabled.value = value }
    fun setSteeringWheelControlsEnabled(value: Boolean) { _steeringWheelControlsEnabled.value = value }
    fun setAutoPlayOnConnect(value: Boolean) { _autoPlayOnConnect.value = value }
}
