/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.eq.audio

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
 * Audio processor that suppresses vocals by applying notch filters in the vocal frequency range.
 * This creates a karaoke effect by reducing vocal frequencies while preserving instrumental tracks.
 *
 * Uses biquad notch filters targeting male (80-300Hz) and female (200-800Hz) vocal ranges,
 * plus harmonics and formants (800-2000Hz).
 */
@UnstableApi
class VocalSuppressionAudioProcessor : BaseAudioProcessor() {

    private var vocalSuppressionEnabled = false
    private var suppressionStrength = 0.6f // 0.0 to 1.0, higher = more suppression
    private var sampleRate = 44100
    private var channelCount = 2

    // Biquad notch filters for vocal frequency suppression
    private val vocalFilters = mutableListOf<BiquadNotchFilter>()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount

        // Initialize vocal suppression filters
        initializeVocalFilters()

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!vocalSuppressionEnabled || vocalFilters.isEmpty()) {
            // Pass through unchanged
            replaceOutputBuffer(inputBuffer.remaining()).put(inputBuffer)
            return
        }

        // Process audio with vocal suppression
        processAudioBuffer(inputBuffer)
    }

    private fun initializeVocalFilters() {
        vocalFilters.clear()

        // Create notch filters for vocal frequency ranges
        // These target the fundamental frequencies and harmonics of human vocals

        // Male vocal fundamental: ~85-180Hz (lower range)
        vocalFilters.add(BiquadNotchFilter(130f, 1.5f, sampleRate))

        // Male vocal range: ~180-300Hz
        vocalFilters.add(BiquadNotchFilter(240f, 2.0f, sampleRate))

        // Female vocal fundamental: ~165-255Hz
        vocalFilters.add(BiquadNotchFilter(210f, 1.8f, sampleRate))

        // Female vocal range: ~255-400Hz
        vocalFilters.add(BiquadNotchFilter(330f, 2.2f, sampleRate))

        // Shared vocal range: ~400-800Hz (both male/female harmonics)
        vocalFilters.add(BiquadNotchFilter(600f, 2.5f, sampleRate))

        // Formants and harmonics: ~800-1500Hz
        vocalFilters.add(BiquadNotchFilter(1200f, 3.0f, sampleRate))

        // Higher harmonics: ~1500-3000Hz
        vocalFilters.add(BiquadNotchFilter(2200f, 4.0f, sampleRate))
    }

    private fun processAudioBuffer(inputBuffer: ByteBuffer) {
        val inputSize = inputBuffer.remaining()
        val outputBuffer = replaceOutputBuffer(inputSize)

        // Convert ByteBuffer to float samples for processing
        val inputArray = ByteArray(inputSize)
        inputBuffer.get(inputArray)
        inputBuffer.rewind()

        // Process each channel separately
        val samplesPerChannel = inputSize / 2 / channelCount // 2 bytes per sample (16-bit)
        val processedSamples = FloatArray(samplesPerChannel * channelCount)

        for (channel in 0 until channelCount) {
            val channelOffset = channel
            val channelSamples = FloatArray(samplesPerChannel)

            // Extract channel samples
            for (i in 0 until samplesPerChannel) {
                val sampleIndex = (i * channelCount + channel) * 2
                val sample = (inputArray[sampleIndex].toInt() and 0xFF) or
                            (inputArray[sampleIndex + 1].toInt() shl 8)
                // Convert to float (-1.0 to 1.0 range)
                channelSamples[i] = sample.toFloat() / 32768.0f
            }

            // Apply vocal suppression filters
            var filteredSamples = channelSamples
            for (filter in vocalFilters) {
                filteredSamples = filter.process(filteredSamples)
            }

            // Apply suppression strength (blend original with filtered)
            val dryWetMix = suppressionStrength
            for (i in filteredSamples.indices) {
                filteredSamples[i] = channelSamples[i] * (1.0f - dryWetMix) +
                                   filteredSamples[i] * dryWetMix
            }

            // Put processed samples back
            for (i in 0 until samplesPerChannel) {
                val sampleIndex = i * channelCount + channel
                processedSamples[sampleIndex] = filteredSamples[i]
            }
        }

        // Convert back to 16-bit PCM and write to output buffer
        for (i in processedSamples.indices) {
            val sampleInt = (processedSamples[i] * 32767.0f).toInt().coerceIn(-32768, 32767)
            outputBuffer.put((sampleInt and 0xFF).toByte())
            outputBuffer.put((sampleInt shr 8).toByte())
        }

        outputBuffer.flip()
    }

    /**
     * Enable vocal suppression with specified strength
     */
    fun enable(strength: Float = 0.6f) {
        suppressionStrength = strength.coerceIn(0.1f, 1.0f) // Ensure minimum suppression
        vocalSuppressionEnabled = true
        flush()
    }

    /**
     * Disable vocal suppression
     */
    fun disable() {
        vocalSuppressionEnabled = false
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

    /**
     * Biquad notch filter implementation for frequency-specific suppression
     */
    private class BiquadNotchFilter(
        centerFreq: Float,
        q: Float,
        sampleRate: Int
    ) {
        private val a1: Float
        private val a2: Float
        private val b0: Float
        private val b1: Float
        private val b2: Float

        // Filter state
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        init {
            // Calculate filter coefficients for notch filter
            val omega = 2.0f * PI.toFloat() * centerFreq / sampleRate
            val alpha = sin(omega) / (2.0f * q)
            val cosOmega = cos(omega)

            // Notch filter coefficients
            b0 = 1.0f
            b1 = -2.0f * cosOmega
            b2 = 1.0f
            val a0 = 1.0f + alpha
            a1 = -2.0f * cosOmega
            a2 = 1.0f - alpha

            // Normalize by a0
            b0 /= a0
            b1 /= a0
            b2 /= a0
            a1 /= a0
            a2 /= a0
        }

        fun process(input: FloatArray): FloatArray {
            val output = FloatArray(input.size)

            for (i in input.indices) {
                val x0 = input[i]
                val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2

                output[i] = y0

                // Update filter state
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