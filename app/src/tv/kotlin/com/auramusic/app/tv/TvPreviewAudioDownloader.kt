package com.auramusic.app.tv

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class PreviewAudioHttpException(val statusCode: Int) : IOException("Preview audio HTTP $statusCode")

internal fun shouldRefreshPreviewAudioSource(error: Throwable): Boolean =
    (error as? PreviewAudioHttpException)?.statusCode in setOf(401, 403, 404, 410)

/** Bounded audio fetch that resumes a partial source after transient connection loss. */
internal object TvPreviewAudioDownloader {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MAX_SOURCE_BYTES = 64L * 1024L * 1024L
    private const val BUFFER_BYTES = 64 * 1024
    private const val MAX_RESUME_RETRIES = 2
    private const val PARALLEL_THRESHOLD_BYTES = 1024L * 1024L
    private const val RANGE_CHUNK_BYTES = 512L * 1024L
    private const val MAX_PARALLEL_RANGES = 4

    suspend fun download(
        url: String,
        directory: File,
        cacheName: String,
        requestHeaders: Map<String, String> = emptyMap(),
    ): File =
        withContext(Dispatchers.IO) {
            val output = File(directory, "$cacheName.audio")
            val partial = File(directory, "$cacheName.audio.partial")
            partial.delete()
            var downloaded = 0L
            var retries = 0
            val jobContext = currentCoroutineContext()
            val probe = probeRange(url, requestHeaders)
            try {
                val rangeLength = probe.rangeLength
                if (rangeLength != null && rangeLength >= PARALLEL_THRESHOLD_BYTES) {
                    try {
                        downloadParallelRanges(url, partial, rangeLength, requestHeaders)
                        check(partial.length() == rangeLength) { "Parallel preview audio download was incomplete" }
                        publishAtomically(partial, output)
                        Timber.i(
                            "TV preview audio parallel download complete bytes=%d ranges=%d concurrency=%d",
                            rangeLength,
                            ((rangeLength + RANGE_CHUNK_BYTES - 1) / RANGE_CHUNK_BYTES).toInt(),
                            MAX_PARALLEL_RANGES,
                        )
                        return@withContext output
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: PreviewAudioHttpException) {
                        partial.delete()
                        throw error
                    } catch (error: IOException) {
                        partial.delete()
                        Timber.w(error, "TV preview parallel range download failed; falling back to sequential transfer")
                    }
                }
                while (true) {
                    jobContext.ensureActive()
                    val requestedOffset = downloaded
                    var connection: HttpURLConnection? = null
                    try {
                        val probeResponse = if (requestedOffset == 0L) probe.initialResponse else null
                        val activeConnection = probeResponse?.also { probe.initialResponse = null }
                            ?: (URL(url).openConnection() as HttpURLConnection).apply {
                                setRequestProperty("Accept-Encoding", "identity")
                                applyRequestHeaders(requestHeaders)
                            }
                        connection = activeConnection
                        activeConnection.connectTimeout = CONNECT_TIMEOUT_MS
                        activeConnection.readTimeout = READ_TIMEOUT_MS
                        activeConnection.instanceFollowRedirects = true
                        activeConnection.applyRequestHeaders(requestHeaders)
                        if (requestedOffset > 0L) {
                            activeConnection.setRequestProperty("Range", "bytes=$requestedOffset-")
                        }

                        val status = activeConnection.responseCode
                        if (status in REFRESHABLE_SOURCE_STATUSES) throw PreviewAudioHttpException(status)
                        check(status == HttpURLConnection.HTTP_OK || status == HTTP_PARTIAL_CONTENT) {
                            "Preview audio HTTP $status"
                        }
                        val responseOffset = when (status) {
                            HTTP_PARTIAL_CONTENT -> {
                                val rangeStart = activeConnection.getHeaderField("Content-Range")
                                    ?.substringAfter("bytes ")
                                    ?.substringBefore('-')
                                    ?.toLongOrNull()
                                if (rangeStart != requestedOffset) {
                                    partial.delete()
                                    downloaded = 0L
                                    throw IOException("Preview audio returned an invalid Content-Range")
                                }
                                requestedOffset
                            }
                            else -> {
                                if (requestedOffset > 0L) {
                                    // Some hosts ignore Range. Restart cleanly; never append a 200 body.
                                    partial.delete()
                                    downloaded = 0L
                                }
                                0L
                            }
                        }
                        val contentLength = activeConnection.contentLengthLong
                        check(contentLength < 0L || responseOffset + contentLength <= MAX_SOURCE_BYTES) {
                            "Preview audio exceeds the configured size bound"
                        }

                        var total = responseOffset
                        BufferedInputStream(activeConnection.inputStream).use { input ->
                            FileOutputStream(partial, responseOffset > 0L).use { sink ->
                                val buffer = ByteArray(BUFFER_BYTES)
                                while (true) {
                                    jobContext.ensureActive()
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    check(total <= MAX_SOURCE_BYTES) {
                                        "Preview audio exceeds the configured size bound"
                                    }
                                    sink.write(buffer, 0, count)
                                }
                                sink.fd.sync()
                            }
                        }
                        if (contentLength >= 0L && total != responseOffset + contentLength) {
                            throw EOFException("Preview audio ended at $total bytes before its declared length")
                        }
                        check(total > 0L) { "Preview audio response was empty" }
                        publishAtomically(partial, output)
                        Timber.i("TV preview audio download complete bytes=%d retries=%d", total, retries)
                        return@withContext output
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: IOException) {
                        if (error is PreviewAudioHttpException) throw error
                        downloaded = partial.length()
                        if (retries >= MAX_RESUME_RETRIES) throw error
                        retries++
                        Timber.w(
                            error,
                            "TV preview audio interrupted; retry=%d resumeOffset=%d",
                            retries,
                            downloaded,
                        )
                    } finally {
                        connection?.disconnect()
                    }
                }
                error("Unreachable audio download state")
            } catch (cancelled: CancellationException) {
                partial.delete()
                throw cancelled
            } catch (error: Exception) {
                partial.delete()
                throw error
            }
        }

    /** Same-directory rename replaces an existing cache entry atomically on Android/Linux. */
    private fun publishAtomically(partial: File, output: File) {
        check(partial.renameTo(output)) { "Could not atomically finalize preview audio" }
    }

    /** Keeps an ignored-Range 200 response for sequential download instead of discarding its body. */
    private suspend fun probeRange(url: String, requestHeaders: Map<String, String>): RangeProbe {
        val connection = URL(url).openConnection() as HttpURLConnection
        var handedOff = false
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.applyRequestHeaders(requestHeaders)
            connection.setRequestProperty("Range", "bytes=0-0")
            val status = connection.responseCode
            if (status in REFRESHABLE_SOURCE_STATUSES) throw PreviewAudioHttpException(status)
            if (status == HttpURLConnection.HTTP_OK) {
                handedOff = true
                return RangeProbe(initialResponse = connection)
            }
            if (status != HTTP_PARTIAL_CONTENT) return RangeProbe()
            val contentRange = connection.getHeaderField("Content-Range") ?: return RangeProbe()
            val match = CONTENT_RANGE.matchEntire(contentRange) ?: return RangeProbe()
            val start = match.groupValues[1].toLongOrNull() ?: return RangeProbe()
            val end = match.groupValues[2].toLongOrNull() ?: return RangeProbe()
            val total = match.groupValues[3].toLongOrNull() ?: return RangeProbe()
            if (start != 0L || end != 0L || total < PARALLEL_THRESHOLD_BYTES || total > MAX_SOURCE_BYTES) {
                return RangeProbe()
            }
            if (connection.contentLengthLong != 1L) return RangeProbe()
            val firstByte = connection.inputStream.use { it.read() }
            return RangeProbe(rangeLength = total.takeIf { firstByte >= 0 })
        } catch (error: IOException) {
            if (error is PreviewAudioHttpException) throw error
            // Probe failure must not prevent the established sequential/resume path.
            Timber.w(error, "TV preview range capability probe failed; using sequential download")
            return RangeProbe()
        } finally {
            if (!handedOff) connection.disconnect()
        }
    }

    private suspend fun downloadParallelRanges(
        url: String,
        partial: File,
        totalBytes: Long,
        requestHeaders: Map<String, String>,
    ) = coroutineScope {
        partial.parentFile?.mkdirs()
        val randomAccessFile = RandomAccessFile(partial, "rw")
        val channel = randomAccessFile.channel
        try {
            randomAccessFile.setLength(totalBytes)
            val semaphore = Semaphore(MAX_PARALLEL_RANGES)
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()
            var start = 0L
            while (start < totalBytes) {
                val rangeStart = start
                val rangeEnd = minOf(totalBytes - 1L, rangeStart + RANGE_CHUNK_BYTES - 1L)
                jobs += async(Dispatchers.IO) {
                    semaphore.withPermit {
                        downloadRange(url, channel, rangeStart, rangeEnd, totalBytes, requestHeaders)
                    }
                }
                start = rangeEnd + 1L
            }
            jobs.forEach { it.await() }
            channel.force(true)
        } finally {
            channel.close()
            randomAccessFile.close()
        }
    }

    private suspend fun downloadRange(
        url: String,
        channel: FileChannel,
        start: Long,
        end: Long,
        total: Long,
        requestHeaders: Map<String, String>,
    ) {
        val expectedBytes = end - start + 1L
        val jobContext = currentCoroutineContext()
        var lastError: IOException? = null
        repeat(MAX_RESUME_RETRIES + 1) { attempt ->
            jobContext.ensureActive()
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.applyRequestHeaders(requestHeaders)
                connection.setRequestProperty("Range", "bytes=$start-$end")
                val status = connection.responseCode
                if (status in REFRESHABLE_SOURCE_STATUSES) throw PreviewAudioHttpException(status)
                if (status != HTTP_PARTIAL_CONTENT) {
                    throw IOException("Parallel preview audio range returned HTTP $status")
                }
                val match = CONTENT_RANGE.matchEntire(connection.getHeaderField("Content-Range").orEmpty())
                    ?: throw IOException("Parallel preview audio returned an invalid Content-Range")
                val responseStart = match.groupValues[1].toLongOrNull()
                val responseEnd = match.groupValues[2].toLongOrNull()
                val responseTotal = match.groupValues[3].toLongOrNull()
                if (responseStart != start || responseEnd != end || responseTotal != total) {
                    throw IOException("Parallel preview audio range did not match the requested bytes")
                }
                if (connection.contentLengthLong >= 0L && connection.contentLengthLong != expectedBytes) {
                    throw IOException("Parallel preview audio range had an invalid body length")
                }
                var position = start
                var received = 0L
                BufferedInputStream(connection.inputStream).use { input ->
                    val bytes = ByteArray(BUFFER_BYTES)
                    while (received < expectedBytes) {
                        jobContext.ensureActive()
                        val count = input.read(bytes, 0, minOf(bytes.size.toLong(), expectedBytes - received).toInt())
                        if (count < 0) throw java.io.EOFException("Preview audio range ended early")
                        var buffer = ByteBuffer.wrap(bytes, 0, count)
                        while (buffer.hasRemaining()) position += channel.write(buffer, position)
                        received += count
                    }
                    if (input.read() != -1) throw IOException("Preview audio range exceeded its declared body length")
                }
                return
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                if (error is PreviewAudioHttpException) throw error
                lastError = error
                if (attempt == MAX_RESUME_RETRIES) throw error
            } finally {
                connection.disconnect()
            }
        }
        throw lastError ?: IOException("Parallel preview audio range failed")
    }

    private const val HTTP_PARTIAL_CONTENT = 206
    private val REFRESHABLE_SOURCE_STATUSES = setOf(401, 403, 404, 410)
    private val CONTENT_RANGE = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)")

    private data class RangeProbe(
        val rangeLength: Long? = null,
        var initialResponse: HttpURLConnection? = null,
    )

    private fun HttpURLConnection.applyRequestHeaders(headers: Map<String, String>) {
        headers.forEach { (name, value) -> setRequestProperty(name, value) }
    }
}
