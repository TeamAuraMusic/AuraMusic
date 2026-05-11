/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Bluetooth LE Audio manager – uses BluetoothProfile proxies (A2DP,
 * HEADSET, LE_AUDIO) as the single source of truth for connected
 * audio devices, and ACTION_CONNECTION_STATE_CHANGED broadcasts to
 * react to live connection changes. This catches every bonded BT
 * audio sink including necklaces, earbuds, hearing aids and LE
 * Audio devices.
 */
package com.auramusic.app.hardware.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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

    private var a2dp: BluetoothA2dp? = null
    private var headset: BluetoothHeadset? = null
    private var leAudio: BluetoothLeAudio? = null

    private var selectedPrimaryAddress: String? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dp = proxy as BluetoothA2dp
                BluetoothProfile.HEADSET -> headset = proxy as BluetoothHeadset
                BluetoothProfile.LE_AUDIO -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    leAudio = proxy as BluetoothLeAudio
                }
            }
            refresh()
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dp = null
                BluetoothProfile.HEADSET -> headset = null
                BluetoothProfile.LE_AUDIO -> leAudio = null
            }
            refresh()
        }
    }

    fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val adapter = this.adapter ?: return
        _isEnabled.value = adapter.isEnabled
        registerReceiver()

        if (hasBluetoothPermission()) {
            runCatching { adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP) }
            runCatching { adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    adapter.getProfileProxy(context, profileListener, BluetoothProfile.LE_AUDIO)
                }
            }
        }
        refresh()
    }

    fun stop() {
        runCatching { receiver?.let { context.unregisterReceiver(it) } }
        receiver = null
        a2dp?.let { runCatching { adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) } }
        headset?.let { runCatching { adapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) } }
        leAudio?.let { runCatching { adapter?.closeProfileProxy(BluetoothProfile.LE_AUDIO, it) } }
        a2dp = null
        headset = null
        leAudio = null
    }

    private fun registerReceiver() {
        if (receiver != null) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        _isEnabled.value = adapter?.isEnabled == true
                    }
                }
                refresh()
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addAction("android.bluetooth.action.LE_AUDIO_CONNECTION_STATE_CHANGED")
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
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
            if (adapter?.isEnabled != true || !hasBluetoothPermission()) {
                _devices.value = emptyList()
                _activeDevice.value = null
                return@launch
            }

            val byAddress = linkedMapOf<String, MutableSet<Int>>()
            val byDevice = linkedMapOf<String, BluetoothDevice>()

            fun add(profile: Int, list: List<BluetoothDevice>) {
                list.forEach { d ->
                    val addr = d.address ?: return@forEach
                    byAddress.getOrPut(addr) { linkedSetOf() }.add(profile)
                    byDevice[addr] = d
                }
            }

            try { add(BluetoothProfile.A2DP, a2dp?.connectedDevices.orEmpty()) } catch (_: SecurityException) {}
            try { add(BluetoothProfile.HEADSET, headset?.connectedDevices.orEmpty()) } catch (_: SecurityException) {}
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try { add(BluetoothProfile.LE_AUDIO, leAudio?.connectedDevices.orEmpty()) } catch (_: SecurityException) {}
            }

            val list = byAddress.map { (addr, profiles) ->
                val dev = byDevice.getValue(addr)
                val name = try { dev.name } catch (_: SecurityException) { null }
                val supportsLe = BluetoothProfile.LE_AUDIO in profiles
                BluetoothAudioDevice(
                    address = addr,
                    name = name ?: "Bluetooth Device",
                    isConnected = true,
                    supportsLeAudio = supportsLe,
                    isPrimary = selectedPrimaryAddress == addr,
                    codec = when {
                        supportsLe -> "LE Audio"
                        BluetoothProfile.A2DP in profiles -> "A2DP"
                        BluetoothProfile.HEADSET in profiles -> "HFP/HSP"
                        else -> null
                    },
                )
            }.sortedBy { it.name }

            val normalized = if (list.any { it.isPrimary }) {
                list
            } else if (list.isNotEmpty()) {
                list.mapIndexed { i, d -> d.copy(isPrimary = i == 0) }
            } else list

            _devices.value = normalized
            _activeDevice.value = normalized.firstOrNull { it.isPrimary }
        }
    }

    /** Toggle multi-device synchronization. Routing remains system-driven. */
    fun setMultiDeviceSync(enabled: Boolean) {
        _multiDeviceSync.value = enabled
    }

    fun setPrimaryDevice(address: String) {
        selectedPrimaryAddress = address
        refresh()
    }

    fun isAnyConnected(): Boolean = _devices.value.any { it.isConnected }
}
