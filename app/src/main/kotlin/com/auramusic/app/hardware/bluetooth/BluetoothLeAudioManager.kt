/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Bluetooth LE Audio manager – discovers, enumerates and synchronizes
 * audio across multiple Bluetooth (Classic + LE) devices using the
 * standard Android Bluetooth APIs. The manager only *observes* and
 * *routes* audio – the actual codec / source playback is performed
 * by the system AudioPolicy through the active media session, so we
 * remain compatible with ExoPlayer.
 */
package com.auramusic.app.hardware.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class BluetoothAudioDevice(
    val address: String,
    val name: String,
    val isConnected: Boolean,
    val supportsLeAudio: Boolean,
    val isPrimary: Boolean,
    val batteryLevel: Int? = null,
    val codec: String? = null,
)

class BluetoothLeAudioManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _devices = MutableStateFlow<List<BluetoothAudioDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothAudioDevice>> = _devices.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _multiDeviceSync = MutableStateFlow(false)
    val multiDeviceSync: StateFlow<Boolean> = _multiDeviceSync.asStateFlow()

    private val _activeDevice = MutableStateFlow<BluetoothAudioDevice?>(null)
    val activeDevice: StateFlow<BluetoothAudioDevice?> = _activeDevice.asStateFlow()

    private var receiver: BroadcastReceiver? = null

    fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun start() {
        if (adapter == null) return
        _isEnabled.value = adapter.isEnabled
        registerReceiver()
        refresh()
    }

    fun stop() {
        runCatching { receiver?.let { context.unregisterReceiver(it) } }
        receiver = null
    }

    private fun registerReceiver() {
        if (receiver != null) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        _isEnabled.value = adapter?.isEnabled == true
                        refresh()
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED,
                    BluetoothDevice.ACTION_ACL_DISCONNECTED,
                    BluetoothDevice.ACTION_NAME_CHANGED,
                    -> refresh()
                    AudioManager.ACTION_HEADSET_PLUG -> refresh()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to register Bluetooth receiver")
        }
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            if (adapter == null || !hasBluetoothPermission()) {
                _devices.value = emptyList()
                _activeDevice.value = null
                return@launch
            }

            val connectedAddrs = audioManager
                .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                }
                .mapNotNull { it.address }
                .toSet()

            val bonded = try {
                adapter.bondedDevices ?: emptySet()
            } catch (e: SecurityException) {
                emptySet()
            }

            val list = bonded
                .filter { dev ->
                    val cls = dev.bluetoothClass?.majorDeviceClass
                    cls == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO ||
                        connectedAddrs.contains(dev.address)
                }
                .map { dev ->
                    val connected = connectedAddrs.contains(dev.address)
                    val supportsLe = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        runCatching {
                            adapter.getProfileProxy(context, null, BluetoothProfile.LE_AUDIO)
                        }.getOrDefault(false) == true
                    BluetoothAudioDevice(
                        address = dev.address,
                        name = dev.name ?: "Bluetooth Device",
                        isConnected = connected,
                        supportsLeAudio = supportsLe,
                        isPrimary = false,
                        codec = if (supportsLe) "LC3" else "SBC/AAC",
                    )
                }
                .sortedByDescending { it.isConnected }

            val withPrimary = if (list.any { it.isConnected }) {
                val first = list.first { it.isConnected }
                list.map { it.copy(isPrimary = it.address == first.address) }
            } else list

            _devices.value = withPrimary
            _activeDevice.value = withPrimary.firstOrNull { it.isPrimary }
        }
    }

    /** Toggle multi-device synchronization (requires LE Audio Auracast on supported devices). */
    fun setMultiDeviceSync(enabled: Boolean) {
        _multiDeviceSync.value = enabled
        if (enabled) {
            // Switch to LE Audio routing where possible. The actual routing is
            // managed by the system; we hint the AudioManager to use Bluetooth
            // and let LE Audio profile pick available sinks.
            try {
                if (audioManager.isBluetoothA2dpOn.not()) {
                    audioManager.startBluetoothSco()
                }
            } catch (e: Exception) {
                Timber.w(e, "startBluetoothSco failed")
            }
        } else {
            runCatching { audioManager.stopBluetoothSco() }
        }
    }

    fun setPrimaryDevice(address: String) {
        _devices.value = _devices.value.map {
            it.copy(isPrimary = it.address == address)
        }
        _activeDevice.value = _devices.value.firstOrNull { it.isPrimary }
    }

    /** Returns true if at least one BT audio sink is currently connected. */
    fun isAnyConnected(): Boolean = _devices.value.any { it.isConnected }
}
