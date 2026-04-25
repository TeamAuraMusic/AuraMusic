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
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Karaoke / "Sing"-style vocal remover.
 *
 * Lead vocals in commercial stereo masters are mixed dead-centre, so they
 * appear with equal amplitude/phase in L and R. Anything else with stereo
 * width (guitars, keys, hats, reverb tails) sits in the side channel.
 *
 * Algorithm (full centre-channel cancellation with bass preservation):
 *
 *   mid  = (L + R) / 2          centred content (vocals, kick, bass)
 *   side = (L - R) / 2          stereo content (instruments)
 *   bass = lowPass(mid, 180 Hz) keep kick + bass guitar audible
 *
 *   keptMid = bass + (mid - bass) * (1 - strength)
 *   outL    = keptMid + side
 *   outR    = keptMid - side
 *
 * At strength = 1.0 every centred sound above 180 Hz is removed —
 * lead vocals disappear and only the instrumental survives, exactly
 * like Apple Music's Sing mode for tracks without an ML stem.
 */
@UnstableApi
class VocalSuppressionAudioProcessor : BaseAudioProcessor() {

    @Volatile private var vocalSuppressionEnabled = false
    @Volatile private var suppressionStrength = 1.0f

    private var sampleRate = 44100
    private var channelCount = 2

    // Low-pass on the centre signal: anything below this stays in the mix
    // even at full suppression so the song still has bass/kick.
    private var bassKeepLowPass: BiquadFilter? = null

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount

        bassKeepLowPass = if (channelCount == 2) {
            BiquadFilter.lowPass(180f, sampleRate, 0.707f)
        } else null

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // Pass-through when disabled, or when the source is mono — centre
        // cancellation requires two distinct channels.
        if (!vocalSuppressionEnabled || channelCount != 2) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        processStereo(inputBuffer)
    }

    private fun processStereo(inputBuffer: ByteBuffer) {
        val byteCount = inputBuffer.remaining()
        // 2 bytes/sample, 2 channels => 4 bytes per stereo frame.
        val frameCount = byteCount / 4

        val output = replaceOutputBuffer(byteCount)

        val srcOrder = inputBuffer.order()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        val lp = bassKeepLowPass!!
        val keepCenter = 1f - suppressionStrength.coerceIn(0f, 1f)

        for (i in 0 until frameCount) {
            val l = inputBuffer.short.toFloat()
            val r = inputBuffer.short.toFloat()

            val mid = (l + r) * 0.5f
            val side = (l - r) * 0.5f

            // Bass-band of the mid signal — never suppressed.
            val bass = lp.process(mid)
            // Everything above the bass cut-off in the mid channel is
            // attenuated by `strength`. At full strength only `bass`
            // survives in the centre.
            val keptMid = bass + (mid - bass) * keepCenter

            var outL = keptMid + side
            var outR = keptMid - side

            // Safety clamp.
            if (outL > 32767f) outL = 32767f else if (outL < -32768f) outL = -32768f
            if (outR > 32767f) outR = 32767f else if (outR < -32768f) outR = -32768f

            output.putShort(outL.toInt().toShort())
            output.putShort(outR.toInt().toShort())
        }

        inputBuffer.order(srcOrder)
        output.flip()
    }

    override fun onFlush() {
        bassKeepLowPass?.reset()
    }

    override fun onReset() {
        bassKeepLowPass = null
    }

    fun enable(strength: Float = 1.0f) {
        suppressionStrength = strength.coerceIn(0f, 1f)
        vocalSuppressionEnabled = true
        flush()
    }

    fun disable() {
        vocalSuppressionEnabled = false
        flush()
    }

    fun isEnabled(): Boolean = vocalSuppressionEnabled
    fun getSuppressionStrength(): Float = suppressionStrength

    /** RBJ biquad — only the LP variant is needed here. */
    private class BiquadFilter(
        private val b0: Float,
        private val b1: Float,
        private val b2: Float,
        private val a1: Float,
        private val a2: Float,
    ) {
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun process(x0: Float): Float {
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
            return y0
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }

        companion object {
            fun lowPass(freq: Float, sampleRate: Int, q: Float): BiquadFilter {
                val w0 = 2f * PI.toFloat() * freq / sampleRate
                val cosW = cos(w0)
                val alpha = sin(w0) / (2f * q)
                val a0 = 1f + alpha
                val b0 = ((1f - cosW) / 2f) / a0
                val b1 = (1f - cosW) / a0
                val b2 = ((1f - cosW) / 2f) / a0
                val a1 = (-2f * cosW) / a0
                val a2 = (1f - alpha) / a0
                return BiquadFilter(b0, b1, b2, a1, a2)
            }
        }
    }
}
