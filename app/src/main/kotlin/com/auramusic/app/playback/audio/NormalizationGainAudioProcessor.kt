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
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Applies a gain to the PCM stream, used for loudness normalization.
 *
 * Unlike [android.media.audiofx.LoudnessEnhancer] this runs inside the app's own audio
 * pipeline, so it does not depend on the device exposing the effect (many Android TV boxes
 * do not, or ignore it for the HDMI output) and it can attenuate as well as boost.
 *
 * Two ways to choose the gain:
 *  - [setGainDb]: a fixed gain, used when YouTube supplied the track's loudness.
 *  - [useMeasuredLoudness]: for tracks YouTube gave no loudness for (common on some clients),
 *    the processor measures the track itself while it plays - average level of the non-silent
 *    audio so far - and moves the gain toward [TARGET_RMS_DB]. The estimate is rough during
 *    the first seconds and settles as the track goes on; the gain moves slowly so it never
 *    pumps. The boost never pushes the peaks seen so far over full scale.
 *
 * Gain changes are smoothed to avoid clicks. Encodings other than 16-bit / float PCM leave the
 * processor inactive.
 */
@UnstableApi
class NormalizationGainAudioProcessor : BaseAudioProcessor() {
    @Volatile
    private var targetGain = 1f

    @Volatile
    private var currentGain = 1f
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT
    private var sampleRate = 48_000
    private var fastRamp = 0.002f
    private var slowRamp = 0.00005f

    // Measurement state. Written from the playback thread (queueInput) and from the thread that
    // calls the public functions below; the flag fields are volatile, the sums are only touched
    // in queueInput after a reset request has been consumed there.
    @Volatile
    private var measuring = false

    @Volatile
    private var resetRequested = false
    private var minGainDb = -15f
    private var maxGainDb = 3f
    private var sumSquares = 0.0
    private var countedSamples = 0L
    private var peak = 0f

    @Volatile
    private var trackId: String? = null

    /** Average level of the track so far in dB below full scale, or null until enough audio was heard. */
    @Volatile
    var measuredLevelDb: Float? = null
        private set

    /** True while the gain is driven by the processor's own measurement instead of a fixed value. */
    val isMeasuring: Boolean get() = measuring

    /** The gain being applied right now, in dB. */
    val currentGainDb: Float get() = 20f * log10(currentGain)

    /** Fixed gain (0 = unchanged) for [trackId]. Safe to call from any thread. */
    fun setGainDb(gainDb: Float, trackId: String?) {
        this.trackId = trackId
        measuring = false
        targetGain = 10f.pow(gainDb / 20f)
    }

    /**
     * Let the processor measure [trackId] itself and pick the gain. Calling it again for the same
     * track keeps the measurement; a different track starts over at 0 dB.
     */
    fun useMeasuredLoudness(trackId: String?, minGainDb: Float, maxGainDb: Float) {
        this.minGainDb = minGainDb
        this.maxGainDb = maxGainDb
        if (!measuring || this.trackId != trackId) {
            this.trackId = trackId
            resetRequested = true
            measuredLevelDb = null
            targetGain = 1f
        }
        measuring = true
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding
        sampleRate = inputAudioFormat.sampleRate
        fastRamp = 1f - exp(-1f / (0.01f * sampleRate))
        slowRamp = 1f - exp(-1f / (0.5f * sampleRate))
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val output = replaceOutputBuffer(remaining)

        val srcOrder = inputBuffer.order()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val frameCount = remaining / (bytesPerSample * channelCount)
        val sampleCount = frameCount * channelCount

        val auto = measuring
        if (auto) {
            if (resetRequested) {
                resetRequested = false
                sumSquares = 0.0
                countedSamples = 0L
                peak = 0f
            }
            analyse(inputBuffer, inputBuffer.position(), sampleCount)
        }

        val target = targetGain
        if (currentGain == target && target == 1f) {
            output.put(inputBuffer)
            output.flip()
            inputBuffer.order(srcOrder)
            return
        }

        val ramp = if (auto) slowRamp else fastRamp
        var gain = currentGain

        repeat(frameCount) {
            if (gain != target) {
                gain += (target - gain) * ramp
                if (abs(target - gain) < 1e-4f) gain = target
            }
            repeat(channelCount) {
                if (encoding == C.ENCODING_PCM_FLOAT) {
                    output.putFloat(inputBuffer.getFloat() * gain)
                } else {
                    val scaled = (inputBuffer.getShort() * gain).roundToInt()
                    output.putShort(scaled.coerceIn(-32768, 32767).toShort())
                }
            }
        }
        currentGain = gain

        // Any trailing bytes that do not form a whole frame are passed through untouched.
        output.put(inputBuffer)
        output.flip()
        inputBuffer.order(srcOrder)
    }

    /** Adds one buffer to the running measurement (absolute reads; the buffer is not consumed). */
    private fun analyse(buffer: ByteBuffer, start: Int, sampleCount: Int) {
        if (sampleCount <= 0) return
        var sum = 0.0
        var blockPeak = 0f
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (i in 0 until sampleCount) {
                val s = buffer.getFloat(start + i * 4)
                sum += (s * s).toDouble()
                val a = abs(s)
                if (a > blockPeak) blockPeak = a
            }
        } else {
            for (i in 0 until sampleCount) {
                val s = buffer.getShort(start + i * 2) / 32768f
                sum += (s * s).toDouble()
                val a = abs(s)
                if (a > blockPeak) blockPeak = a
            }
        }

        // Blocks that are practically silence (gaps, fade-ins) must not drag the average down.
        if (sum / sampleCount > SILENCE_MEAN_SQUARE) {
            sumSquares += sum
            countedSamples += sampleCount
            if (blockPeak > peak) peak = blockPeak
        }

        if (countedSamples < MIN_MEASURE_SECONDS * sampleRate * channelCount) return

        val levelDb = (10.0 * log10(sumSquares / countedSamples)).toFloat()
        measuredLevelDb = levelDb
        var gainDb = (TARGET_RMS_DB - levelDb).coerceIn(minGainDb, maxGainDb)
        if (peak > 0f) {
            // Do not boost into clipping: leave at most the headroom the loudest peak has left.
            gainDb = minOf(gainDb, -20f * log10(peak))
        }
        targetGain = 10f.pow(gainDb / 20f)
    }

    companion object {
        /** Level the measured tracks are brought to (average, dB below full scale). */
        const val TARGET_RMS_DB = -16f
        private const val MIN_MEASURE_SECONDS = 1
        private const val SILENCE_MEAN_SQUARE = 1e-5 // about -50 dBFS
    }
}
