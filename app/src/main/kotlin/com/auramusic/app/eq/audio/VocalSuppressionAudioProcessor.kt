/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.eq.audio

import android.media.AudioFormat
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Audio processor that suppresses vocals by applying a notch filter in the vocal frequency range.
 * This creates a karaoke effect by reducing the center channel where vocals are typically mixed.
 */
@UnstableApi
class VocalSuppressionAudioProcessor : BaseAudioProcessor() {

    private var vocalSuppressionEnabled = false
    private var suppressionStrength = 0.7f // 0.0 to 1.0, higher = more suppression

    // Biquad filter coefficients for vocal notch filter
    // Targets 80-300Hz range where male vocals dominate, and 200-800Hz for female vocals
    private val vocalNotchFilters = mutableListOf<BiquadFilter>()

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (vocalSuppressionEnabled) {
            initializeFilters(inputAudioFormat.sampleRate)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!vocalSuppressionEnabled || vocalNotchFilters.isEmpty()) {
            // Pass through unchanged
            outputBuffer = inputBuffer
            return
        }

        // Process audio if vocal suppression is enabled
        val inputSize = inputBuffer.remaining()
        val channelCount = inputAudioFormat?.channelCount ?: 2
        val sampleRate = inputAudioFormat?.sampleRate ?: 44100

        // Convert ByteBuffer to float array for processing
        val floatBuffer = ByteBuffer.allocate(inputSize)
        floatBuffer.put(inputBuffer)
        floatBuffer.flip()

        val samples = FloatArray(inputSize / 4) // 4 bytes per float
        floatBuffer.asFloatBuffer().get(samples)

        // Apply vocal suppression to each channel
        for (channel in 0 until channelCount) {
            val channelSamples = samples.filterIndexed { index, _ -> index % channelCount == channel }
            val processedSamples = applyVocalSuppression(channelSamples.toFloatArray(), sampleRate)

            // Put processed samples back
            for (i in processedSamples.indices) {
                samples[channel + i * channelCount] = processedSamples[i]
            }
        }

        // Convert back to ByteBuffer
        val outputByteBuffer = ByteBuffer.allocate(inputSize)
        outputByteBuffer.asFloatBuffer().put(samples)
        outputByteBuffer.flip()

        outputBuffer = outputByteBuffer
    }

    /**
     * Enable vocal suppression with specified strength
     */
    fun enable(strength: Float = 0.7f) {
        suppressionStrength = strength.coerceIn(0f, 1f)
        vocalSuppressionEnabled = true
        flush()
    }

    /**
     * Disable vocal suppression
     */
    fun disable() {
        vocalSuppressionEnabled = false
        vocalNotchFilters.clear()
        flush()
    }

    /**
     * Check if vocal suppression is enabled
     */
    fun isEnabled(): Boolean = vocalSuppressionEnabled

    /**
     * Get current suppression strength
     */
    fun getSuppressionStrength(): Float = suppressionStrength

    private fun initializeFilters(sampleRate: Int) {
        vocalNotchFilters.clear()

        // Create notch filters for vocal frequency ranges
        // Male vocals: ~80-300Hz
        vocalNotchFilters.add(createNotchFilter(150f, 2f, sampleRate))
        // Female vocals: ~200-800Hz
        vocalNotchFilters.add(createNotchFilter(400f, 3f, sampleRate))
        // Harmonics and formants: ~800-2000Hz
        vocalNotchFilters.add(createNotchFilter(1200f, 4f, sampleRate))
    }

    private fun createNotchFilter(centerFreq: Float, q: Float, sampleRate: Int): BiquadFilter {
        val omega = 2 * PI * centerFreq / sampleRate
        val alpha = sin(omega) / (2 * q)
        val cosOmega = cos(omega)

        // Notch filter coefficients
        val b0 = 1f
        val b1 = -2 * cosOmega
        val b2 = 1f
        val a0 = 1 + alpha
        val a1 = -2 * cosOmega
        val a2 = 1 - alpha

        return BiquadFilter(
            b0 / a0, b1 / a0, b2 / a0,
            a1 / a0, a2 / a0
        )
    }

    private fun applyVocalSuppression(samples: FloatArray, sampleRate: Int): FloatArray {
        if (vocalNotchFilters.isEmpty()) return samples

        var processedSamples = samples

        // Apply each notch filter in series
        for (filter in vocalNotchFilters) {
            processedSamples = filter.process(processedSamples)
        }

        // Apply suppression strength (blend original with filtered)
        val dryWetMix = suppressionStrength
        for (i in processedSamples.indices) {
            processedSamples[i] = samples[i] * (1 - dryWetMix) + processedSamples[i] * dryWetMix
        }

        return processedSamples
    }

    /**
     * Simple biquad filter implementation
     */
    private class BiquadFilter(
        val b0: Float,
        val b1: Float,
        val b2: Float,
        val a1: Float,
        val a2: Float
    ) {
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun process(input: FloatArray): FloatArray {
            val output = FloatArray(input.size)

            for (i in input.indices) {
                val x0 = input[i]
                val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2

                output[i] = y0

                // Shift delay lines
                x2 = x1
                x1 = x0
                y2 = y1
                y1 = y0
            }

            return output
        }

        fun reset() {
            x1 = 0f
            x2 = 0f
            y1 = 0f
            y2 = 0f
        }
    }
}