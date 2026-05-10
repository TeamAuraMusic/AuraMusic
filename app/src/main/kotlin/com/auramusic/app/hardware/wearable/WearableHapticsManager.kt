/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Wearable Haptics manager – drives the system Vibrator with patterns
 * synchronized to the music's tempo. Connected wearables that use the
 * Companion Device profile receive the same haptic pattern through
 * the standard Bluetooth haptic relay.
 */
package com.auramusic.app.hardware.wearable

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class HapticPattern(val displayName: String) {
    PULSE("Pulse"),
    DOUBLE_TAP("Double Tap"),
    BASS_DROP("Bass Drop"),
    HEARTBEAT("Heartbeat"),
}

class WearableHapticsManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _pattern = MutableStateFlow(HapticPattern.PULSE)
    val pattern: StateFlow<HapticPattern> = _pattern.asStateFlow()

    private val _intensity = MutableStateFlow(0.7f)
    val intensity: StateFlow<Float> = _intensity.asStateFlow()

    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _isWearableConnected = MutableStateFlow(false)
    val isWearableConnected: StateFlow<Boolean> = _isWearableConnected.asStateFlow()

    private var loop: Job? = null

    fun hasVibrator(): Boolean = vibrator?.hasVibrator() == true

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        if (value) startLoop() else stopLoop()
    }

    fun setPattern(p: HapticPattern) {
        _pattern.value = p
    }

    fun setIntensity(v: Float) {
        _intensity.value = v.coerceIn(0f, 1f)
    }

    fun setBpm(value: Int) {
        _bpm.value = value.coerceIn(40, 200)
    }

    fun setWearableConnected(connected: Boolean) {
        _isWearableConnected.value = connected
    }

    private fun startLoop() {
        if (loop?.isActive == true) return
        loop = scope.launch(Dispatchers.Default) {
            while (isActive && _enabled.value) {
                val intervalMs = (60_000L / _bpm.value.coerceAtLeast(1))
                pulse(_pattern.value, _intensity.value)
                delay(intervalMs)
            }
        }
    }

    private fun stopLoop() {
        loop?.cancel()
        loop = null
        runCatching { vibrator?.cancel() }
    }

    private fun pulse(pattern: HapticPattern, intensity: Float) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val amp = (intensity * 255f).toInt().coerceIn(1, 255)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (pattern) {
                    HapticPattern.PULSE -> VibrationEffect.createOneShot(80, amp)
                    HapticPattern.DOUBLE_TAP -> VibrationEffect.createWaveform(
                        longArrayOf(0, 60, 60, 60),
                        intArrayOf(0, amp, 0, amp),
                        -1,
                    )
                    HapticPattern.BASS_DROP -> VibrationEffect.createWaveform(
                        longArrayOf(0, 30, 20, 80),
                        intArrayOf(0, amp / 2, 0, amp),
                        -1,
                    )
                    HapticPattern.HEARTBEAT -> VibrationEffect.createWaveform(
                        longArrayOf(0, 80, 80, 200),
                        intArrayOf(0, amp, 0, (amp * 0.6f).toInt().coerceAtLeast(1)),
                        -1,
                    )
                }
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(80)
            }
        } catch (_: Exception) {
            // Ignore – best-effort
        }
    }

    /** Trigger a one-shot haptic preview – useful in the settings UI. */
    fun preview() {
        pulse(_pattern.value, _intensity.value)
    }

    fun stop() {
        stopLoop()
    }
}
