package com.auramusic.app.tv

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvPreviewAudioTranscoderInstrumentedTest {
    @Test
    fun mp3SourceBecomesSeekableAacOnlyMp4WithFullDurationAndPreservedTone() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = File(context.cacheDir, "preview_440hz_source.mp3")
        val output = File(context.cacheDir, "preview_440hz_output.mp4")
        source.delete()
        output.delete()

        try {
            instrumentation.context.assets.open("preview_440hz.mp3").use { input ->
                source.outputStream().use { outputStream -> input.copyTo(outputStream) }
            }
            TvPreviewAudioTranscoder.transcode(context, source, output)

            assertTrue("AAC MP4 cache must be seekable and non-empty", output.isFile && output.length() > 0L)
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(output.absolutePath)
                assertEquals("Output must contain audio only", 1, extractor.trackCount)
                val format = extractor.getTrackFormat(0)
                assertEquals(MediaFormat.MIMETYPE_AUDIO_AAC, format.getString(MediaFormat.KEY_MIME))
                assertTrue(format.getLong(MediaFormat.KEY_DURATION) in 7_000_000L..9_000_000L)

                extractor.selectTrack(0)
                var bytesRead = 0L
                var lastSampleTimeUs = 0L
                val sampleBuffer = java.nio.ByteBuffer.allocate(16 * 1024)
                while (true) {
                    sampleBuffer.clear()
                    val sampleSize = extractor.readSampleData(sampleBuffer, 0)
                    if (sampleSize < 0) break
                    bytesRead += sampleSize
                    lastSampleTimeUs = extractor.sampleTime
                    extractor.advance()
                }
                assertTrue("Expected encoded audio samples", bytesRead > 0L)
                assertTrue("Full fixture duration must be present", lastSampleTimeUs > 7_000_000L)
            } finally {
                extractor.release()
            }
            assertDecodedTone(output, expectedHz = 440)
        } finally {
            source.delete()
            output.delete()
            File(context.cacheDir, "preview_440hz_output.mp4.transcoding.mp4").delete()
        }
    }

    private fun assertDecodedTone(file: File, expectedHz: Int) {
        val extractor = MediaExtractor()
        val codec: MediaCodec
        val format: MediaFormat
        try {
            extractor.setDataSource(file.absolutePath)
            format = extractor.getTrackFormat(0)
            extractor.selectTrack(0)
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        } finally {
            extractor.release()
        }

        val decoderExtractor = MediaExtractor()
        val monoSamples = ArrayList<Double>()
        var sampleRate = 0
        var channelCount = 0
        var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
        try {
            decoderExtractor.setDataSource(file.absolutePath)
            decoderExtractor.selectTrack(0)
            codec.configure(format, null, null, 0)
            codec.start()

            var inputEnded = false
            var outputEnded = false
            var iterations = 0
            val info = MediaCodec.BufferInfo()
            while (!outputEnded && iterations++ < MAX_DECODE_ITERATIONS) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex)!!
                        input.clear()
                        val size = decoderExtractor.readSampleData(input, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex, 0, size, decoderExtractor.sampleTime, 0,
                            )
                            decoderExtractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        pcmEncoding = outputFormat.getInteger(
                            MediaFormat.KEY_PCM_ENCODING,
                            AudioFormat.ENCODING_PCM_16BIT,
                        )
                    }
                    outputIndex >= 0 -> {
                        if (info.size > 0 && monoSamples.size < MAX_PCM_SAMPLES) {
                            val output = codec.getOutputBuffer(outputIndex)!!.duplicate()
                                .order(ByteOrder.nativeOrder())
                            output.position(info.offset)
                            output.limit(info.offset + info.size)
                            appendMonoSamples(
                                output, pcmEncoding, channelCount, monoSamples,
                            )
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            assertTrue("AAC decoder must reach end of stream", outputEnded)
            assertTrue("Decoder must report an audio format", sampleRate > 0 && channelCount > 0)
        } finally {
            runCatching { codec.stop() }
            codec.release()
            decoderExtractor.release()
        }

        val pcm = monoSamples.toDoubleArray()
        val targetPower = goertzelPower(pcm, sampleRate, expectedHz)
        val competingPower = listOf(250, 700, 1_200, 2_000)
            .maxOf { frequency -> goertzelPower(pcm, sampleRate, frequency) }
        assertTrue("Decoded PCM must contain samples", pcm.size >= 4_096)
        assertTrue("Decoded PCM must contain audible signal energy", targetPower > 1_000.0)
        assertTrue(
            "Decoded PCM should preserve ${expectedHz} Hz tone: target=$targetPower competing=$competingPower",
            targetPower > competingPower * 50.0,
        )
    }

    private fun appendMonoSamples(
        pcm: java.nio.ByteBuffer,
        encoding: Int,
        channels: Int,
        target: MutableList<Double>,
    ) {
        if (channels <= 0) return
        val bytesPerSample = when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> error("Unsupported decoder PCM encoding: $encoding")
        }
        val frameBytes = bytesPerSample * channels
        while (pcm.remaining() >= frameBytes && target.size < MAX_PCM_SAMPLES) {
            var mixed = 0.0
            repeat(channels) {
                mixed += when (encoding) {
                    AudioFormat.ENCODING_PCM_16BIT -> pcm.short.toDouble() / Short.MAX_VALUE
                    AudioFormat.ENCODING_PCM_FLOAT -> pcm.float.toDouble()
                    else -> error("Unsupported decoder PCM encoding: $encoding")
                }
            }
            target += mixed / channels
        }
    }

    private fun goertzelPower(samples: DoubleArray, sampleRate: Int, frequency: Int): Double {
        val blockSize = 4_096
        if (samples.size < blockSize || sampleRate <= 0) return 0.0
        val omega = 2.0 * Math.PI * frequency / sampleRate
        val coefficient = 2.0 * kotlin.math.cos(omega)
        var totalPower = 0.0
        var blocks = 0
        var start = 0
        while (start + blockSize <= samples.size) {
            var previous = 0.0
            var previousPrevious = 0.0
            for (index in start until start + blockSize) {
                val current = samples[index] + coefficient * previous - previousPrevious
                previousPrevious = previous
                previous = current
            }
            totalPower += previous * previous + previousPrevious * previousPrevious -
                coefficient * previous * previousPrevious
            blocks++
            start += blockSize
        }
        return totalPower / blocks
    }

    private companion object {
        const val MAX_PCM_SAMPLES = 176_400
        const val MAX_DECODE_ITERATIONS = 2_000
    }
}
