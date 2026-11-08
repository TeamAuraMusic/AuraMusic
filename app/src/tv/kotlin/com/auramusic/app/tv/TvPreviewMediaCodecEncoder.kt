package com.auramusic.app.tv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.SystemClock
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin

/** Low-memory, surface-free H.264/AAC video generation for Projectivy preview cards. */
internal object TvPreviewMediaCodecEncoder {
    suspend fun encode(artworkFile: File, audioFile: File, durationMs: Long, outputFile: File) {
        val videoFile = File(outputFile.parentFile, "${outputFile.name}.video")
        try {
            try {
                encodeWithSurface(artworkFile, VIDEO_LOOP_DURATION_MS, videoFile)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                videoFile.delete()
                Timber.w(error, "TV preview EGL surface encoder failed; using YUV input fallback")
                encodeWithByteBuffer(artworkFile, VIDEO_LOOP_DURATION_MS, videoFile)
            }
            muxAudio(videoFile, audioFile, durationMs, outputFile)
        } catch (cancelled: CancellationException) {
            outputFile.delete()
            throw cancelled
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        } finally {
            videoFile.delete()
        }
    }

    private suspend fun muxAudio(videoFile: File, audioFile: File, durationMs: Long, outputFile: File) =
        withContext(Dispatchers.IO) {
            require(durationMs > 0L) { "Preview duration must be positive" }
            val jobContext = coroutineContext
            val videoExtractor = MediaExtractor()
            val audioExtractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            var muxerStarted = false
            try {
                videoExtractor.setDataSource(videoFile.absolutePath)
                audioExtractor.setDataSource(audioFile.absolutePath)
                val videoSourceTrack = findTrack(videoExtractor, "video/")
                    ?: error("Generated preview has no video track")
                val audioSourceTrack = findTrack(audioExtractor, "audio/")
                    ?: error("Resolved song source has no audio track")
                val videoFormat = videoExtractor.getTrackFormat(videoSourceTrack)
                val audioFormat = audioExtractor.getTrackFormat(audioSourceTrack)
                val audioMime = audioFormat.getString(MediaFormat.KEY_MIME).orEmpty()
                require(audioMime in SUPPORTED_MUX_AUDIO_MIMES) {
                    "Projectivy preview MP4 cannot mux audio format $audioMime"
                }
                videoExtractor.selectTrack(videoSourceTrack)
                audioExtractor.selectTrack(audioSourceTrack)
                // MediaExtractor may leave the selected track cursor unset for remote
                // formats that contain edit lists. Position both streams explicitly
                // before reading sampleTime or the merge loop can see two empty tracks.
                videoExtractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                audioExtractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                val videoLoopSamples = readVideoLoopSamples(videoExtractor)
                check(videoLoopSamples.first().flags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    "Animated preview loop does not begin with an H.264 sync frame"
                }

                outputFile.parentFile?.mkdirs()
                outputFile.delete()
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val videoTrack = muxer.addTrack(videoFormat)
                val audioTrack = muxer.addTrack(audioFormat)
                muxer.start()
                muxerStarted = true

                val audioBuffer = ByteBuffer.allocate(MAX_COMPRESSED_SAMPLE_BYTES)
                val videoInfo = MediaCodec.BufferInfo()
                val audioInfo = MediaCodec.BufferInfo()
                val durationUs = durationMs * 1_000L
                var videoLoopIndex = 0
                var videoLoopOffsetUs = 0L
                var videoTimeUs = videoLoopSamples.first().presentationTimeUs
                var audioTimeUs = firstNonNegativeSampleTime(audioExtractor)
                var videoSamples = 0
                var audioSamples = 0
                Timber.i(
                    "TV preview mux cursors mime=%s videoTracks=%d audioTracks=%d videoStartUs=%d audioStartUs=%d durationMs=%d",
                    audioMime,
                    videoExtractor.trackCount,
                    audioExtractor.trackCount,
                    videoTimeUs,
                    audioTimeUs,
                    durationMs,
                )

                while (true) {
                    jobContext.ensureActive()
                    if (videoTimeUs >= durationUs) videoTimeUs = -1L
                    if (audioTimeUs >= durationUs) audioTimeUs = -1L
                    if (videoTimeUs < 0L && audioTimeUs < 0L) break

                    if (audioTimeUs < 0L || (videoTimeUs >= 0L && videoTimeUs <= audioTimeUs)) {
                        val sample = videoLoopSamples[videoLoopIndex]
                        videoInfo.set(0, sample.bytes.size, videoTimeUs, sample.flags)
                        muxer.writeSampleData(videoTrack, ByteBuffer.wrap(sample.bytes), videoInfo)
                        videoSamples++
                        videoLoopIndex++
                        if (videoLoopIndex == videoLoopSamples.size) {
                            videoLoopIndex = 0
                            videoLoopOffsetUs += VIDEO_LOOP_DURATION_US
                        }
                        videoTimeUs = videoLoopOffsetUs + videoLoopSamples[videoLoopIndex].presentationTimeUs
                    } else {
                        audioBuffer.clear()
                        val size = audioExtractor.readSampleData(audioBuffer, 0)
                        check(size > 0) { "Could not read selected song audio sample" }
                        audioInfo.set(0, size, audioTimeUs, audioExtractor.sampleFlags)
                        muxer.writeSampleData(audioTrack, audioBuffer, audioInfo)
                        audioSamples++
                        audioExtractor.advance()
                        audioTimeUs = audioExtractor.sampleTime
                    }
                }
                check(videoSamples > 0 && audioSamples > 0) {
                    "Preview mux produced no usable samples: video=$videoSamples audio=$audioSamples " +
                        "videoStartUs=$videoTimeUs audioStartUs=$audioTimeUs"
                }
                muxer.stop()
                muxerStarted = false
                check(outputFile.isFile && outputFile.length() > 0L) { "Audio mux produced an empty preview" }
                Timber.i(
                    "TV preview audio mux complete mime=%s videoSamples=%d audioSamples=%d durationMs=%d bytes=%d",
                    audioMime, videoSamples, audioSamples, durationMs, outputFile.length(),
                )
            } finally {
                if (muxerStarted) runCatching { muxer?.stop() }
                runCatching { muxer?.release() }
                runCatching { videoExtractor.release() }
                runCatching { audioExtractor.release() }
            }
        }

    private fun findTrack(extractor: MediaExtractor, mimePrefix: String): Int? =
        (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                ?.startsWith(mimePrefix, ignoreCase = true) == true
        }

    private fun readVideoLoopSamples(extractor: MediaExtractor): List<PendingSample> {
        val samples = ArrayList<PendingSample>()
        val buffer = ByteBuffer.allocate(MAX_COMPRESSED_SAMPLE_BYTES)
        var sampleTimeUs = firstNonNegativeSampleTime(extractor)
        while (sampleTimeUs >= 0L) {
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            check(size > 0) { "Could not read generated preview loop sample" }
            val bytes = ByteArray(size)
            buffer.position(0)
            buffer.get(bytes)
            samples += PendingSample(
                isVideo = true,
                presentationTimeUs = sampleTimeUs,
                flags = extractor.sampleFlags,
                bytes = bytes,
            )
            if (!extractor.advance()) break
            sampleTimeUs = extractor.sampleTime
            while (sampleTimeUs < 0L && sampleTimeUs != -1L) {
                if (!extractor.advance()) {
                    sampleTimeUs = -1L
                    break
                }
                sampleTimeUs = extractor.sampleTime
            }
        }
        check(samples.isNotEmpty()) { "Generated preview animation loop has no video samples" }
        Timber.i("TV preview loop prepared frames=%d cycleMs=%d", samples.size, VIDEO_LOOP_DURATION_MS)
        return samples
    }

    /** Skip encoder-delay samples with negative timestamps; only -1 marks extractor EOS. */
    private fun firstNonNegativeSampleTime(extractor: MediaExtractor): Long {
        var sampleTimeUs = extractor.sampleTime
        while (sampleTimeUs < 0L && sampleTimeUs != -1L) {
            if (!extractor.advance()) return -1L
            sampleTimeUs = extractor.sampleTime
        }
        return sampleTimeUs
    }

    private suspend fun encodeWithSurface(artworkFile: File, durationMs: Long, outputFile: File) =
        withContext(Dispatchers.IO) {
            require(durationMs > 0L) { "Preview duration must be positive" }
            val jobContext = coroutineContext
            val artwork = BitmapFactory.decodeFile(artworkFile.absolutePath)
                ?: error("Preview artwork could not be decoded")
            var videoCodec: MediaCodec? = null
            var inputSurface: android.view.Surface? = null
            var renderer: TvPreviewEglRenderer? = null
            var muxer: MediaMuxer? = null
            var muxerStarted = false
            var videoStarted = false
            try {
                val videoInfo = selectSurfaceVideoEncoder()
                videoCodec = MediaCodec.createByCodecName(videoInfo.name)
                val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE)
                    setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, GOP_SECONDS)
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                    setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3)
                }
                videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = videoCodec.createInputSurface()
                videoCodec.start()
                videoStarted = true
                renderer = TvPreviewEglRenderer(inputSurface, artwork)
                inputSurface = null // Renderer owns and releases the codec input surface.
                outputFile.parentFile?.mkdirs()
                outputFile.delete()
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val frameCount = ceil(durationMs / 1_000.0 * FRAME_RATE).toInt().coerceAtLeast(1)
                var videoFramesQueued = 1
                var videoInputEnded = false
                var videoOutputEnded = false
                var videoOutputFormat: MediaFormat? = null
                val pendingSamples = ArrayList<PendingSample>(2)
                val videoInfoBuffer = MediaCodec.BufferInfo()

                renderer.drawFrame(0, framePtsUs(0))
                val formatWaitStarted = SystemClock.elapsedRealtime()
                while (videoOutputFormat == null) {
                    jobContext.ensureActive()
                    var madeProgress = false
                    when (val status = videoCodec.dequeueOutputBuffer(videoInfoBuffer, CODEC_TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            videoOutputFormat = videoCodec.outputFormat
                            madeProgress = true
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER, MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                        else -> if (status >= 0) {
                            stashOutput(videoCodec, status, videoInfoBuffer, isVideo = true, pendingSamples)
                            madeProgress = true
                        }
                    }
                    check(SystemClock.elapsedRealtime() - formatWaitStarted < FORMAT_TIMEOUT_MS) {
                        "Surface MediaCodec did not provide an output format"
                    }
                    if (!madeProgress) Thread.sleep(IDLE_SLEEP_MS)
                }

                val videoTrack = muxer.addTrack(videoOutputFormat!!)
                muxer.start()
                muxerStarted = true
                pendingSamples.sortedBy(PendingSample::presentationTimeUs)
                    .forEach { sample -> writePending(muxer, videoTrack, sample) }
                pendingSamples.clear()

                var lastLoggedFrame = 0
                while (!videoOutputEnded) {
                    jobContext.ensureActive()
                    var madeProgress = false
                    if (videoFramesQueued < frameCount) {
                        renderer.drawFrame(videoFramesQueued, framePtsUs(videoFramesQueued))
                        videoFramesQueued++
                        madeProgress = true
                    } else if (!videoInputEnded) {
                        videoCodec.signalEndOfInputStream()
                        videoInputEnded = true
                        madeProgress = true
                    }

                    drainOutput(videoCodec, videoInfoBuffer, muxer, videoTrack, isVideo = true).also {
                        madeProgress = it.first || madeProgress
                        videoOutputEnded = videoOutputEnded || it.second
                    }
                    if (videoFramesQueued / FRAME_RATE >= lastLoggedFrame + 30) {
                        lastLoggedFrame = videoFramesQueued / FRAME_RATE
                        Timber.d("TV preview EGL encodedFrames=%d/%d", videoFramesQueued, frameCount)
                    }
                    if (!madeProgress) Thread.sleep(IDLE_SLEEP_MS)
                }
                muxer.stop()
                muxerStarted = false
                check(outputFile.isFile && outputFile.length() > 0L) { "EGL MediaCodec produced an empty preview" }
                Timber.i(
                    "TV preview EGL surface mux complete encoder=%s bytes=%d durationMs=%d audio=added-by-source-mux",
                    videoInfo.name, outputFile.length(), durationMs,
                )
            } finally {
                if (muxerStarted) runCatching { muxer?.stop() }
                runCatching { muxer?.release() }
                runCatching { renderer?.close() }
                runCatching { inputSurface?.release() }
                if (videoStarted) runCatching { videoCodec?.stop() }
                runCatching { videoCodec?.release() }
                artwork.recycle()
            }
        }

    private fun selectSurfaceVideoEncoder(): MediaCodecInfo {
        val candidates = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) } &&
                runCatching {
                    info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).colorFormats
                        .contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                }.getOrDefault(false)
        }
        return candidates.sortedBy { info ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.isHardwareAccelerated) 0 else 1
        }.firstOrNull() ?: error("No H.264 encoder accepts Surface input")
    }

    private suspend fun encodeWithByteBuffer(artworkFile: File, durationMs: Long, outputFile: File) =
        withContext(Dispatchers.IO) {
            require(durationMs > 0L) { "Preview duration must be positive" }
            val jobContext = coroutineContext
            val artwork = BitmapFactory.decodeFile(artworkFile.absolutePath)
                ?: error("Preview artwork could not be decoded")
            val frame = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(WIDTH * HEIGHT)
            val canvas = Canvas(frame)
            val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(188, 8, 10, 20)
            }
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 74, 166)
                style = Paint.Style.STROKE
                strokeWidth = 3.2f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val panel = RectF(102f, 126f, 218f, 169f)
            val waveform = Path()
            val source = centerCrop(artwork.width, artwork.height, WIDTH, HEIGHT)
            val destination = Rect(0, 0, WIDTH, HEIGHT)
            var videoCodec: MediaCodec? = null
            var audioCodec: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var muxerStarted = false
            try {
                val videoInfo = selectVideoEncoder()
                videoCodec = MediaCodec.createByCodecName(videoInfo.name)
                audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                Timber.i(
                    "TV preview MediaCodec encoder video=%s audio=%s durationMs=%d size=%dx%d fps=%d",
                    videoInfo.name,
                    audioCodec.name,
                    durationMs,
                    WIDTH,
                    HEIGHT,
                    FRAME_RATE,
                )

                val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                    setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE)
                    setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, GOP_SECONDS)
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                    setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3)
                }
                val audioFormat = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC,
                    AUDIO_SAMPLE_RATE,
                    AUDIO_CHANNELS,
                ).apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_CHUNK_BYTES)
                    setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                }
                videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                videoCodec.start()
                audioCodec.start()

                outputFile.parentFile?.mkdirs()
                outputFile.delete()
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val frameCount = ceil(durationMs / 1_000.0 * FRAME_RATE).toInt().coerceAtLeast(1)
                val targetAudioFrames = ceil(durationMs / 1_000.0 * AUDIO_SAMPLE_RATE).toLong()
                var videoFramesQueued = 0
                var audioFramesQueued = 0L
                var videoInputEnded = false
                var audioInputEnded = false
                var videoOutputEnded = false
                var audioOutputEnded = false
                var videoTrack = -1
                var audioTrack = -1
                var videoOutputFormat: MediaFormat? = null
                var audioOutputFormat: MediaFormat? = null
                val pendingSamples = ArrayList<PendingSample>(2)
                val videoInfoBuffer = MediaCodec.BufferInfo()
                val audioInfoBuffer = MediaCodec.BufferInfo()

                // Prime each codec once, then wait for both output formats before starting MP4 muxing.
                queueVideoFrame(videoCodec, 0, frame, canvas, artwork, source, destination, artworkPaint,
                    panel, panelPaint, waveform, wavePaint, pixels)
                videoFramesQueued = 1
                queueAudioChunk(audioCodec, 0L, targetAudioFrames)
                audioFramesQueued = min(AUDIO_CHUNK_FRAMES.toLong(), targetAudioFrames)
                val formatWaitStarted = SystemClock.elapsedRealtime()
                while (videoOutputFormat == null || audioOutputFormat == null) {
                    jobContext.ensureActive()
                    var madeProgress = false
                    if (videoOutputFormat == null) {
                        val status = videoCodec.dequeueOutputBuffer(videoInfoBuffer, CODEC_TIMEOUT_US)
                        when {
                            status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                videoOutputFormat = videoCodec.outputFormat
                                madeProgress = true
                            }
                            status >= 0 -> {
                                stashOutput(videoCodec, status, videoInfoBuffer, isVideo = true, pendingSamples)
                                madeProgress = true
                            }
                        }
                    }
                    if (audioOutputFormat == null) {
                        val status = audioCodec.dequeueOutputBuffer(audioInfoBuffer, CODEC_TIMEOUT_US)
                        when {
                            status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                audioOutputFormat = audioCodec.outputFormat
                                madeProgress = true
                            }
                            status >= 0 -> {
                                stashOutput(audioCodec, status, audioInfoBuffer, isVideo = false, pendingSamples)
                                madeProgress = true
                            }
                        }
                    }
                    check(SystemClock.elapsedRealtime() - formatWaitStarted < FORMAT_TIMEOUT_MS) {
                        "MediaCodec did not provide both output formats"
                    }
                    if (!madeProgress) Thread.sleep(IDLE_SLEEP_MS)
                }

                videoTrack = muxer.addTrack(videoOutputFormat!!)
                audioTrack = muxer.addTrack(audioOutputFormat!!)
                muxer.start()
                muxerStarted = true
                pendingSamples.sortedBy(PendingSample::presentationTimeUs).forEach { sample ->
                    writePending(muxer, if (sample.isVideo) videoTrack else audioTrack, sample)
                }
                pendingSamples.clear()

                var lastLoggedSecond = 0
                while (!videoOutputEnded || !audioOutputEnded) {
                    jobContext.ensureActive()
                    var madeProgress = false

                    if (!videoInputEnded) {
                        val input = videoCodec.dequeueInputBuffer(0)
                        if (input >= 0) {
                            if (videoFramesQueued < frameCount) {
                                queueVideoFrame(
                                    videoCodec,
                                    videoFramesQueued,
                                    frame,
                                    canvas,
                                    artwork,
                                    source,
                                    destination,
                                    artworkPaint,
                                    panel,
                                    panelPaint,
                                    waveform,
                                    wavePaint,
                                    pixels,
                                    input,
                                )
                                videoFramesQueued++
                            } else {
                                videoCodec.queueInputBuffer(input, 0, 0,
                                    framePtsUs(videoFramesQueued), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                videoInputEnded = true
                            }
                            madeProgress = true
                        }
                    }

                    if (!audioInputEnded) {
                        val videoHorizonUs = if (videoFramesQueued == 0) 0L
                        else framePtsUs(videoFramesQueued - 1) + AUDIO_LEAD_US
                        while (audioPtsUs(audioFramesQueued) <= videoHorizonUs) {
                            val input = audioCodec.dequeueInputBuffer(0)
                            if (input < 0) break
                            if (audioFramesQueued < targetAudioFrames) {
                                val count = queueAudioChunk(audioCodec, audioFramesQueued, targetAudioFrames, input)
                                audioFramesQueued += count
                            } else {
                                audioCodec.queueInputBuffer(input, 0, 0,
                                    audioPtsUs(audioFramesQueued), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                audioInputEnded = true
                            }
                            madeProgress = true
                        }
                    }

                    madeProgress = drainOutput(videoCodec, videoInfoBuffer, muxer, videoTrack, isVideo = true)
                        .also { videoOutputEnded = videoOutputEnded || it.second }
                        .first || madeProgress
                    madeProgress = drainOutput(audioCodec, audioInfoBuffer, muxer, audioTrack, isVideo = false)
                        .also { audioOutputEnded = audioOutputEnded || it.second }
                        .first || madeProgress

                    val encodedSecond = (videoFramesQueued / FRAME_RATE)
                    if (encodedSecond >= lastLoggedSecond + 30) {
                        lastLoggedSecond = encodedSecond
                        Timber.d("TV preview MediaCodec encodedFrames=%d/%d audioFrames=%d/%d",
                            videoFramesQueued, frameCount, audioFramesQueued, targetAudioFrames)
                    }
                    if (!madeProgress) Thread.sleep(IDLE_SLEEP_MS)
                }

                muxer.stop()
                muxerStarted = false
                check(outputFile.isFile && outputFile.length() > 0L) { "MediaMuxer produced an empty preview" }
                Timber.i("TV preview MediaCodec mux complete bytes=%d durationMs=%d", outputFile.length(), durationMs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                if (muxerStarted) runCatching { muxer?.stop() }
                runCatching { muxer?.release() }
                runCatching { videoCodec?.stop() }
                runCatching { audioCodec?.stop() }
                runCatching { videoCodec?.release() }
                runCatching { audioCodec?.release() }
                frame.recycle()
                artwork.recycle()
            }
        }

    private fun selectVideoEncoder(): MediaCodecInfo {
        val matching = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            .filter { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) } &&
                    runCatching {
                        info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).colorFormats
                            .contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                    }.getOrDefault(false)
            }
        val selected = matching.sortedBy { info ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.isHardwareAccelerated) 0 else 1
        }.firstOrNull() ?: error("No H.264 encoder accepts flexible YUV420 input")
        Timber.i("TV preview available YUV H.264 encoders=%s", matching.joinToString { it.name })
        return selected
    }

    private fun queueVideoFrame(
        codec: MediaCodec,
        frameIndex: Int,
        frame: Bitmap,
        canvas: Canvas,
        artwork: Bitmap,
        source: Rect,
        destination: Rect,
        artworkPaint: Paint,
        panel: RectF,
        panelPaint: Paint,
        waveform: Path,
        wavePaint: Paint,
        pixels: IntArray,
        inputIndex: Int = codec.dequeueInputBuffer(CODEC_TIMEOUT_US),
    ) {
        require(inputIndex >= 0) { "No H.264 input buffer available" }
        canvas.drawBitmap(artwork, source, destination, artworkPaint)
        canvas.drawRoundRect(panel, 22f, 22f, panelPaint)
        waveform.reset()
        for (point in 0..WAVE_POINTS) {
            val x = panel.left + 10f + point * ((panel.width() - 20f) / WAVE_POINTS)
            val envelope = 0.26f + 0.74f * sin(point * Math.PI / WAVE_POINTS).toFloat()
            val phase = frameIndex * (Math.PI / 2.0) + point * 0.72
            val y = panel.centerY() + (12f * envelope * sin(phase)).toFloat()
            if (point == 0) waveform.moveTo(x, y) else waveform.lineTo(x, y)
        }
        canvas.drawPath(waveform, wavePaint)
        frame.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        val image = codec.getInputImage(inputIndex) ?: error("H.264 encoder returned no flexible input image")
        try {
            writeYuv420(pixels, image.planes, WIDTH, HEIGHT)
        } finally {
            image.close()
        }
        codec.queueInputBuffer(
            inputIndex,
            0,
            WIDTH * HEIGHT * 3 / 2,
            framePtsUs(frameIndex),
            0,
        )
    }

    private fun queueAudioChunk(
        codec: MediaCodec,
        startFrame: Long,
        targetFrames: Long,
        inputIndex: Int = codec.dequeueInputBuffer(CODEC_TIMEOUT_US),
    ): Long {
        require(inputIndex >= 0) { "No AAC input buffer available" }
        if (startFrame >= targetFrames) {
            codec.queueInputBuffer(inputIndex, 0, 0, audioPtsUs(startFrame), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return 0L
        }
        val buffer = codec.getInputBuffer(inputIndex) ?: error("AAC encoder returned no input buffer")
        buffer.clear()
        val maxFrames = buffer.remaining() / (AUDIO_CHANNELS * PCM_BYTES_PER_SAMPLE)
        val frames = min(AUDIO_CHUNK_FRAMES.toLong(), min(maxFrames.toLong(), targetFrames - startFrame)).toInt()
        require(frames > 0) { "AAC input buffer is too small" }
        val bytes = frames * AUDIO_CHANNELS * PCM_BYTES_PER_SAMPLE
        repeat(bytes) { buffer.put(0) }
        codec.queueInputBuffer(inputIndex, 0, bytes, audioPtsUs(startFrame), 0)
        return frames.toLong()
    }

    private fun writeYuv420(pixels: IntArray, planes: Array<android.media.Image.Plane>, width: Int, height: Int) {
        require(planes.size == 3) { "Flexible YUV420 input must expose three planes" }
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        for (row in 0 until height) {
            val rowStart = row * width
            for (column in 0 until width) {
                val color = pixels[rowStart + column]
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                val y = ((66 * red + 129 * green + 25 * blue + 128) shr 8) + 16
                yBuffer.put(row * yPlane.rowStride + column * yPlane.pixelStride, y.coerceIn(0, 255).toByte())
            }
        }
        for (row in 0 until height step 2) {
            for (column in 0 until width step 2) {
                var red = 0
                var green = 0
                var blue = 0
                repeat(2) { dy -> repeat(2) { dx ->
                    val color = pixels[(row + dy) * width + column + dx]
                    red += Color.red(color)
                    green += Color.green(color)
                    blue += Color.blue(color)
                } }
                red /= 4
                green /= 4
                blue /= 4
                val u = ((-38 * red - 74 * green + 112 * blue + 128) shr 8) + 128
                val v = ((112 * red - 94 * green - 18 * blue + 128) shr 8) + 128
                val x = column / 2
                val y = row / 2
                uBuffer.put(y * uPlane.rowStride + x * uPlane.pixelStride, u.coerceIn(0, 255).toByte())
                vBuffer.put(y * vPlane.rowStride + x * vPlane.pixelStride, v.coerceIn(0, 255).toByte())
            }
        }
    }

    private fun drainOutput(
        codec: MediaCodec,
        info: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        track: Int,
        isVideo: Boolean,
    ): Pair<Boolean, Boolean> {
        var progress = false
        var ended = false
        while (true) {
            val index = codec.dequeueOutputBuffer(info, 0)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> return progress to ended
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> error("Codec output format changed after MP4 mux start")
                index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                index >= 0 -> {
                    val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        val output = codec.getOutputBuffer(index) ?: error("Codec returned an empty output buffer")
                        output.position(info.offset)
                        output.limit(info.offset + info.size)
                        muxer.writeSampleData(track, output, info)
                    }
                    codec.releaseOutputBuffer(index, false)
                    progress = true
                    if (eos) {
                        ended = true
                        return progress to ended
                    }
                    if (isVideo && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) progress = true
                }
                else -> return progress to ended
            }
        }
    }

    private fun stashOutput(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo,
        isVideo: Boolean,
        pending: MutableList<PendingSample>,
    ) {
        try {
            if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                val output = codec.getOutputBuffer(index) ?: error("Codec returned an empty output buffer")
                output.position(info.offset)
                output.limit(info.offset + info.size)
                val bytes = ByteArray(info.size)
                output.get(bytes)
                pending += PendingSample(isVideo, info.presentationTimeUs, info.flags, bytes)
            }
        } finally {
            codec.releaseOutputBuffer(index, false)
        }
    }

    private fun writePending(muxer: MediaMuxer, track: Int, sample: PendingSample) {
        val info = MediaCodec.BufferInfo().apply {
            set(0, sample.bytes.size, sample.presentationTimeUs, sample.flags)
        }
        muxer.writeSampleData(track, ByteBuffer.wrap(sample.bytes), info)
    }

    private fun centerCrop(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Rect {
        val targetRatio = targetWidth.toFloat() / targetHeight
        val sourceRatio = sourceWidth.toFloat() / sourceHeight
        return if (sourceRatio > targetRatio) {
            val cropWidth = (sourceHeight * targetRatio).toInt()
            val left = (sourceWidth - cropWidth) / 2
            Rect(left, 0, left + cropWidth, sourceHeight)
        } else {
            val cropHeight = (sourceWidth / targetRatio).toInt()
            val top = (sourceHeight - cropHeight) / 2
            Rect(0, top, sourceWidth, top + cropHeight)
        }
    }

    private fun framePtsUs(frame: Int): Long = frame * FRAME_INTERVAL_US
    private fun audioPtsUs(frame: Long): Long = frame * 1_000_000L / AUDIO_SAMPLE_RATE

    private data class PendingSample(
        val isVideo: Boolean,
        val presentationTimeUs: Long,
        val flags: Int,
        val bytes: ByteArray,
    )

    private const val WIDTH = 640
    private const val HEIGHT = 360
    private const val FRAME_RATE = 24
    private const val FRAME_INTERVAL_US = 1_000_000L / FRAME_RATE
    private const val VIDEO_BIT_RATE = 800_000
    private const val GOP_SECONDS = 1
    private const val VIDEO_LOOP_DURATION_MS = 2_000L
    private const val VIDEO_LOOP_DURATION_US = VIDEO_LOOP_DURATION_MS * 1_000L
    private const val AUDIO_SAMPLE_RATE = 48_000
    private const val AUDIO_CHANNELS = 2
    private const val AUDIO_BIT_RATE = 32_000
    private const val MAX_COMPRESSED_SAMPLE_BYTES = 2 * 1024 * 1024
    private const val AUDIO_CHUNK_FRAMES = 1_024
    private const val PCM_BYTES_PER_SAMPLE = 2
    private const val AUDIO_CHUNK_BYTES = AUDIO_CHUNK_FRAMES * AUDIO_CHANNELS * PCM_BYTES_PER_SAMPLE
    private const val AUDIO_LEAD_US = 500_000L
    private const val WAVE_POINTS = 48
    private const val CODEC_TIMEOUT_US = 10_000L
    private const val FORMAT_TIMEOUT_MS = 10_000L
    private const val IDLE_SLEEP_MS = 1L
    private val SUPPORTED_MUX_AUDIO_MIMES = setOf(
        MediaFormat.MIMETYPE_AUDIO_AAC,
        "audio/opus",
    )
}
