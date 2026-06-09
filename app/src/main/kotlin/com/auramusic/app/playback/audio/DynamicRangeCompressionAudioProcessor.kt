/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Gentle real-time compressor for late-night listening.
 *
 * It raises quiet detail and tames sudden peaks so music stays intelligible at
 * low volume without large jumps waking people nearby. The processor is kept
 * intentionally conservative and only touches PCM 16-bit playback; encoded
 * passthrough streams (Dolby/DTS) remain untouched for receivers to decode.
 */
@UnstableApi
class DynamicRangeCompressionAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var enabled: Boolean = false
        set(value) {
            field = value
            flush()
        }

    private var sampleRate = 44100
    private var channelCount = 2
    private var envelope = 0f
    private var smoothedGain = 1f
    private var attackCoeff = 0f
    private var releaseCoeff = 0f
    private var gainSmoothCoeff = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        attackCoeff = timeConstant(8f)
        releaseCoeff = timeConstant(140f)
        gainSmoothCoeff = timeConstant(18f)

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val output = replaceOutputBuffer(remaining)

        if (!enabled || channelCount <= 0) {
            output.put(inputBuffer)
            output.flip()
            return
        }

        val srcOrder = inputBuffer.order()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        val frameCount = remaining / 2 / channelCount
        val invShortMax = 1f / 32768f
        val makeupGain = dbToLinear(7.5f)
        val noiseFloor = dbToLinear(-52f)

        repeat(frameCount) {
            var peak = 0f
            val samples = FloatArray(channelCount)
            repeat(channelCount) { channel ->
                val sample = inputBuffer.short.toFloat() * invShortMax
                samples[channel] = sample
                peak = max(peak, abs(sample))
            }

            val coeff = if (peak > envelope) attackCoeff else releaseCoeff
            envelope = coeff * envelope + (1f - coeff) * peak

            val targetGain = compressorGain(envelope, makeupGain, noiseFloor)
            smoothedGain = gainSmoothCoeff * smoothedGain + (1f - gainSmoothCoeff) * targetGain

            repeat(channelCount) { channel ->
                val processed = softLimit(samples[channel] * smoothedGain)
                output.putShort((processed * 32767f).toInt().coerceIn(-32768, 32767).toShort())
            }
        }

        inputBuffer.order(srcOrder)
        output.flip()
    }

    override fun onFlush() {
        envelope = 0f
        smoothedGain = 1f
    }

    private fun compressorGain(envelope: Float, makeupGain: Float, noiseFloor: Float): Float {
        if (envelope < noiseFloor) return 1f

        val levelDb = linearToDb(envelope)
        val compressedDb = when {
            levelDb > -12f -> (levelDb + 12f) * (1f / 5f) - 12f
            levelDb > -30f -> (levelDb + 30f) * (1f / 2.6f) - 30f
            else -> levelDb
        }
        val attenuation = dbToLinear(compressedDb - levelDb)

        return (attenuation * makeupGain).coerceIn(0.25f, 3.25f)
    }

    private fun softLimit(value: Float): Float {
        val absValue = abs(value)
        if (absValue <= 0.92f) return value

        val sign = if (value < 0f) -1f else 1f
        val over = absValue - 0.92f
        val limited = 0.92f + over / (1f + over * 8f)
        return sign * limited.coerceAtMost(0.995f)
    }

    private fun timeConstant(milliseconds: Float): Float =
        exp(-1f / ((milliseconds / 1000f) * sampleRate))

    private fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)

    private fun linearToDb(value: Float): Float =
        (20f * ln(value.coerceAtLeast(1e-7f)) / ln(10f))
}
