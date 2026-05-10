/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Pro Audio Interface manager – enumerates USB / HDMI / DisplayPort
 * audio devices, exposes their sample-rate / channel capabilities and
 * lets the user toggle the low-latency studio routing path.
 */
package com.auramusic.app.hardware.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProAudioDevice(
    val id: Int,
    val name: String,
    val type: String,
    val sampleRates: List<Int>,
    val channelCounts: List<Int>,
    val isDefault: Boolean,
)

class ProAudioInterfaceManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _devices = MutableStateFlow<List<ProAudioDevice>>(emptyList())
    val devices: StateFlow<List<ProAudioDevice>> = _devices.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _lowLatency = MutableStateFlow(true)
    val lowLatency: StateFlow<Boolean> = _lowLatency.asStateFlow()

    private val _activeDevice = MutableStateFlow<ProAudioDevice?>(null)
    val activeDevice: StateFlow<ProAudioDevice?> = _activeDevice.asStateFlow()

    private val _bufferSizeFrames = MutableStateFlow(
        runCatching {
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toInt()
        }.getOrNull() ?: 256
    )
    val bufferSizeFrames: StateFlow<Int> = _bufferSizeFrames.asStateFlow()

    private val _nativeSampleRate = MutableStateFlow(
        runCatching {
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toInt()
        }.getOrNull() ?: AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
    )
    val nativeSampleRate: StateFlow<Int> = _nativeSampleRate.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) { refresh() }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) { refresh() }
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun stop() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }

    fun refresh() {
        val list = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                    it.type == AudioDeviceInfo.TYPE_AUX_LINE ||
                    it.type == AudioDeviceInfo.TYPE_HDMI ||
                    it.type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                    it.type == AudioDeviceInfo.TYPE_DOCK
            }
            .map { d ->
                ProAudioDevice(
                    id = d.id,
                    name = d.productName?.toString() ?: typeLabel(d.type),
                    type = typeLabel(d.type),
                    sampleRates = d.sampleRates?.toList()?.sortedDescending() ?: emptyList(),
                    channelCounts = d.channelCounts?.toList()?.sortedDescending() ?: emptyList(),
                    isDefault = false,
                )
            }
        val withDefault = if (list.isNotEmpty()) {
            list.mapIndexed { idx, d -> d.copy(isDefault = idx == 0) }
        } else list
        _devices.value = withDefault
        _activeDevice.value = withDefault.firstOrNull { it.isDefault }
    }

    private fun typeLabel(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
        AudioDeviceInfo.TYPE_AUX_LINE -> "Aux Line"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
        AudioDeviceInfo.TYPE_DOCK -> "Dock"
        else -> "Audio Device"
    }

    fun setEnabled(value: Boolean) { _enabled.value = value }
    fun setLowLatency(value: Boolean) { _lowLatency.value = value }
    fun setActiveDevice(id: Int) {
        _devices.value = _devices.value.map { it.copy(isDefault = it.id == id) }
        _activeDevice.value = _devices.value.firstOrNull { it.isDefault }
    }

    fun isAnyConnected(): Boolean = _devices.value.isNotEmpty()
}
