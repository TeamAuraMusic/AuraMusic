package com.auramusic.app.tv

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TvPreviewAudioDownloaderTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun appliesResolverHeadersToSequentialProbeAndBodyRequests() = runBlocking {
        val body = ByteArray(256 * 1024) { (it * 13).toByte() }
        val observed = java.util.Collections.synchronizedList(mutableListOf<String?>())
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/audio") { exchange ->
            observed += exchange.requestHeaders.getFirst("X-Preview-Client")
            if (exchange.requestHeaders.getFirst("Range") == "bytes=0-0") {
                exchange.responseHeaders.add("Content-Range", "bytes 0-0/${body.size}")
                exchange.sendResponseHeaders(206, 1)
                exchange.responseBody.use { it.write(body[0].toInt()) }
            } else {
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
        server.start()
        try {
            val output = TvPreviewAudioDownloader.download(
                "http://127.0.0.1:${server.address.port}/audio",
                temporaryFolder.newFolder(),
                "header-source",
                mapOf("X-Preview-Client" to "AuraMusic resolver"),
            )

            assertArrayEquals(body, Files.readAllBytes(output.toPath()))
            assertTrue("Expected both range probe and body requests", observed.size >= 2)
            assertTrue(observed.all { it == "AuraMusic resolver" })
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun staleUrlCleansPartialCacheAndSameSourceCanRecover() = runBlocking {
        val body = ByteArray(384 * 1024) { (it * 23).toByte() }
        val available = AtomicBoolean(false)
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/audio") { exchange ->
            requests.incrementAndGet()
            if (!available.get()) {
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            } else {
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val cacheName = "expired-source"
            val url = "http://127.0.0.1:${server.address.port}/audio"
            var failure: Exception? = null
            try {
                TvPreviewAudioDownloader.download(url, directory, cacheName)
            } catch (error: Exception) {
                failure = error
            }

            assertTrue("A stale source URL must fail cleanly", failure is PreviewAudioHttpException)
            assertEquals(404, (failure as? PreviewAudioHttpException)?.statusCode)
            assertTrue(shouldRefreshPreviewAudioSource(failure!!))
            assertEquals(false, directory.resolve("$cacheName.audio").exists())
            assertEquals(false, directory.resolve("$cacheName.audio.partial").exists())

            available.set(true)
            val recovered = TvPreviewAudioDownloader.download(url, directory, cacheName)
            assertArrayEquals(body, Files.readAllBytes(recovered.toPath()))
            assertEquals(false, directory.resolve("$cacheName.audio.partial").exists())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun classifiesSourceRejectionAfterSuccessfulRangeProbeWithoutRetryingSameUrl() = runBlocking {
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/audio") { exchange ->
            if (requests.incrementAndGet() == 1) {
                exchange.responseHeaders.add("Content-Range", "bytes 0-0/524288")
                exchange.sendResponseHeaders(206, 1)
                exchange.responseBody.use { it.write(0x5a) }
            } else {
                exchange.sendResponseHeaders(403, -1)
                exchange.close()
            }
        }
        server.start()
        try {
            var failure: Exception? = null
            try {
                TvPreviewAudioDownloader.download(
                    "http://127.0.0.1:${server.address.port}/audio",
                    temporaryFolder.newFolder(),
                    "late-expiry",
                )
            } catch (error: Exception) {
                failure = error
            }

            assertEquals(2, requests.get())
            assertEquals(403, (failure as? PreviewAudioHttpException)?.statusCode)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun staleHttpSourceIsResolvedAgainAndDownloaderPublishesRefreshedBytes() = runBlocking {
        val expected = ByteArray(768 * 1024) { (it * 17).toByte() }
        val expiredRequests = AtomicInteger()
        val refreshedRequests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/expired") { exchange ->
            expiredRequests.incrementAndGet()
            exchange.sendResponseHeaders(403, -1)
            exchange.close()
        }
        server.createContext("/refreshed") { exchange ->
            refreshedRequests.incrementAndGet()
            exchange.sendResponseHeaders(200, expected.size.toLong())
            exchange.responseBody.use { it.write(expected) }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val base = "http://127.0.0.1:${server.address.port}"
            val gate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 1)
            val (resolvedUrl, output) = gate.runWithSource(
                resolve = { "$base/expired" },
                refresh = { "$base/refreshed" },
                shouldRefresh = ::shouldRefreshPreviewAudioSource,
            ) { url ->
                TvPreviewAudioDownloader.download(url, directory, "refreshed-source")
            }

            assertEquals("$base/refreshed", resolvedUrl)
            assertEquals(1, expiredRequests.get())
            assertEquals(1, refreshedRequests.get())
            assertArrayEquals(expected, Files.readAllBytes(output.toPath()))
            assertFalse(directory.resolve("refreshed-source.audio.partial").exists())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun expiredSourceDuringParallelRangesRefreshesAndPublishesExactReplacement() = runBlocking {
        val expected = ByteArray(2 * 1024 * 1024) { (it * 29).toByte() }
        val expiredRequests = AtomicInteger()
        val refreshedRequests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newFixedThreadPool(8)
        server.executor = executor
        server.createContext("/expired") { exchange ->
            expiredRequests.incrementAndGet()
            if (exchange.requestHeaders.getFirst("Range") == "bytes=0-0") {
                exchange.responseHeaders.add("Content-Range", "bytes 0-0/${expected.size}")
                exchange.sendResponseHeaders(206, 1)
                exchange.responseBody.use { it.write(expected[0].toInt()) }
            } else {
                exchange.sendResponseHeaders(403, -1)
                exchange.close()
            }
        }
        server.createContext("/refreshed") { exchange ->
            refreshedRequests.incrementAndGet()
            val range = exchange.requestHeaders.getFirst("Range").orEmpty()
            val match = Regex("bytes=([0-9]+)-([0-9]+)").matchEntire(range)
            if (match == null) {
                exchange.sendResponseHeaders(400, -1)
                exchange.close()
            } else {
                val start = match.groupValues[1].toInt()
                val end = match.groupValues[2].toInt()
                val bytes = expected.copyOfRange(start, end + 1)
                exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${expected.size}")
                exchange.sendResponseHeaders(206, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val base = "http://127.0.0.1:${server.address.port}"
            val gate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 1)
            val (_, output) = gate.runWithSource(
                resolve = { "$base/expired" },
                refresh = { "$base/refreshed" },
                shouldRefresh = ::shouldRefreshPreviewAudioSource,
            ) { url ->
                TvPreviewAudioDownloader.download(url, directory, "parallel-refreshed-source")
            }

            assertTrue("Expected an expired range request after the successful probe", expiredRequests.get() > 1)
            assertEquals(5, refreshedRequests.get()) // probe plus four complete byte ranges
            assertArrayEquals(expected, Files.readAllBytes(output.toPath()))
            assertFalse(directory.resolve("parallel-refreshed-source.audio.partial").exists())
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    @Test
    fun resumesAfterShortBodyWithRangeAndPublishesExactFile() = runBlocking {
        val body = ByteArray(256 * 1024) { (it * 31).toByte() }
        val prefixSize = 96 * 1024
        val requestCount = AtomicInteger()
        val retryRange = AtomicReference<String>()
        val retryEncoding = AtomicReference<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/audio") { exchange ->
            if (requestCount.incrementAndGet() == 1) {
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body, 0, prefixSize) }
            } else {
                val range = exchange.requestHeaders.getFirst("Range")
                retryRange.set(range)
                retryEncoding.set(exchange.requestHeaders.getFirst("Accept-Encoding"))
                val offset = range.removePrefix("bytes=").removeSuffix("-").toInt()
                val remainder = body.copyOfRange(offset, body.size)
                exchange.responseHeaders.add("Content-Range", "bytes $offset-${body.lastIndex}/${body.size}")
                exchange.sendResponseHeaders(206, remainder.size.toLong())
                exchange.responseBody.use { it.write(remainder) }
            }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val output = TvPreviewAudioDownloader.download(
                "http://127.0.0.1:${server.address.port}/audio",
                directory,
                "resume-test",
            )

            assertEquals(2, requestCount.get())
            assertEquals("bytes=$prefixSize-", retryRange.get())
            assertEquals("identity", retryEncoding.get())
            assertArrayEquals(body, Files.readAllBytes(output.toPath()))
            assertEquals(false, directory.resolve("resume-test.audio.partial").exists())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun restartsWhenServerIgnoresRangeWithoutDuplicatingPrefix() = runBlocking {
        val body = ByteArray(192 * 1024) { (it * 17).toByte() }
        val requestCount = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/audio") { exchange ->
            if (requestCount.incrementAndGet() == 1) {
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body, 0, 64 * 1024) }
            } else {
                // Deliberately ignore Range and send a complete 200 response.
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val output = TvPreviewAudioDownloader.download(
                "http://127.0.0.1:${server.address.port}/audio",
                directory,
                "restart-test",
            )

            assertEquals(2, requestCount.get())
            assertArrayEquals(body, Files.readAllBytes(output.toPath()))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun downloadsLargeRangeCapableSourceConcurrentlyAndPublishesExactFile() = runBlocking {
        val body = ByteArray(2 * 1024 * 1024) { (it * 13).toByte() }
        val requestCount = AtomicInteger()
        val activeRequests = AtomicInteger()
        val maxActiveRequests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newFixedThreadPool(8)
        server.createContext("/audio") { exchange ->
            val active = activeRequests.incrementAndGet()
            maxActiveRequests.updateAndGet { maxOf(it, active) }
            requestCount.incrementAndGet()
            try {
                val range = exchange.requestHeaders.getFirst("Range")
                if (range == "bytes=0-0") {
                    exchange.responseHeaders.add("Content-Range", "bytes 0-0/${body.size}")
                    exchange.sendResponseHeaders(206, 1)
                    exchange.responseBody.use { it.write(body[0].toInt()) }
                    return@createContext
                }
                val match = Regex("bytes=([0-9]+)-([0-9]+)").matchEntire(range.orEmpty())
                    ?: error("Missing Range header: $range")
                val start = match.groupValues[1].toInt()
                val end = match.groupValues[2].toInt()
                Thread.sleep(150)
                val chunk = body.copyOfRange(start, end + 1)
                exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${body.size}")
                exchange.sendResponseHeaders(206, chunk.size.toLong())
                exchange.responseBody.use { it.write(chunk) }
            } finally {
                activeRequests.decrementAndGet()
            }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val output = TvPreviewAudioDownloader.download(
                "http://127.0.0.1:${server.address.port}/audio",
                directory,
                "parallel-test",
            )

            assertEquals(5, requestCount.get()) // one capability probe + four data ranges
            assertTrue("Expected overlapping byte-range requests", maxActiveRequests.get() >= 2)
            assertArrayEquals(body, Files.readAllBytes(output.toPath()))
            assertEquals(false, directory.resolve("parallel-test.audio.partial").exists())
        } finally {
            server.stop(0)
            (server.executor as java.util.concurrent.ExecutorService).shutdownNow()
        }
    }

    @Test
    fun fallsBackToSequentialTransferAfterParallelRangesFail() = runBlocking {
        val body = ByteArray(2 * 1024 * 1024) { (it * 19).toByte() }
        val rangeFailures = AtomicInteger()
        val sequentialRequests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newFixedThreadPool(8)
        server.createContext("/audio") { exchange ->
            val range = exchange.requestHeaders.getFirst("Range")
            when (range) {
                "bytes=0-0" -> {
                    exchange.responseHeaders.add("Content-Range", "bytes 0-0/${body.size}")
                    exchange.sendResponseHeaders(206, 1)
                    exchange.responseBody.use { it.write(body[0].toInt()) }
                }
                null -> {
                    sequentialRequests.incrementAndGet()
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                else -> {
                    rangeFailures.incrementAndGet()
                    exchange.sendResponseHeaders(503, -1)
                    exchange.close()
                }
            }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val output = TvPreviewAudioDownloader.download(
                "http://127.0.0.1:${server.address.port}/audio",
                directory,
                "parallel-fallback-test",
            )

            assertTrue("Parallel ranges should be retried", rangeFailures.get() >= 3)
            assertEquals(1, sequentialRequests.get())
            assertArrayEquals(body, Files.readAllBytes(output.toPath()))
            assertEquals(false, directory.resolve("parallel-fallback-test.audio.partial").exists())
        } finally {
            server.stop(0)
            (server.executor as java.util.concurrent.ExecutorService).shutdownNow()
        }
    }

    @Test
    fun replacesExistingCacheOnlyAfterCompleteDownload() = runBlocking {
        val oldBody = ByteArray(128 * 1024) { (it * 5).toByte() }
        val newBody = ByteArray(192 * 1024) { (it * 29).toByte() }
        val available = AtomicBoolean(true)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/audio") { exchange ->
            if (!available.get()) {
                exchange.sendResponseHeaders(503, -1)
                exchange.close()
            } else {
                exchange.sendResponseHeaders(200, newBody.size.toLong())
                exchange.responseBody.use { it.write(newBody) }
            }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val cache = directory.resolve("atomic.audio")
            val partial = directory.resolve("atomic.audio.partial")
            cache.writeBytes(oldBody)

            val downloaded = TvPreviewAudioDownloader.download(
                "http://127.0.0.1:${server.address.port}/audio",
                directory,
                "atomic",
            )
            assertEquals(cache, downloaded)
            assertArrayEquals(newBody, cache.readBytes())
            assertEquals(false, partial.exists())

            cache.writeBytes(oldBody)
            available.set(false)
            var failed = false
            try {
                TvPreviewAudioDownloader.download(
                    "http://127.0.0.1:${server.address.port}/audio",
                    directory,
                    "atomic",
                )
            } catch (_: Exception) {
                failed = true
            }
            assertTrue("A failed replacement must report failure", failed)
            assertArrayEquals(oldBody, cache.readBytes())
            assertEquals(false, partial.exists())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun rejectsOversizedDeclaredSourceAndPreservesExistingCache() = runBlocking {
        val oldBody = byteArrayOf(9, 8, 7, 6)
        val declaredLength = 64L * 1024L * 1024L + 1L
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/audio") { exchange ->
            exchange.sendResponseHeaders(200, declaredLength)
            exchange.close()
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val cache = directory.resolve("oversized.audio").apply { writeBytes(oldBody) }
            try {
                TvPreviewAudioDownloader.download(
                    "http://127.0.0.1:${server.address.port}/audio",
                    directory,
                    "oversized",
                )
                fail("Oversized source must not be published")
            } catch (_: CancellationException) {
                throw AssertionError("Unexpected cancellation")
            } catch (_: Exception) {
                // Expected size-bound rejection.
            }
            assertArrayEquals(oldBody, cache.readBytes())
            assertFalse(directory.resolve("oversized.audio.partial").exists())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun cancellationCleansPartialAndKeepsPreviousCompleteCache() = runBlocking {
        val oldBody = byteArrayOf(1, 3, 3, 7)
        val newBody = ByteArray(256 * 1024) { (it * 7).toByte() }
        val prefixSize = 16 * 1024
        val responseStarted = CountDownLatch(1)
        val releaseResponse = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/audio") { exchange ->
            try {
                exchange.sendResponseHeaders(200, newBody.size.toLong())
                exchange.responseBody.use { body ->
                    body.write(newBody, 0, prefixSize)
                    body.flush()
                    responseStarted.countDown()
                    releaseResponse.await(5, TimeUnit.SECONDS)
                    body.write(newBody, prefixSize, newBody.size - prefixSize)
                }
            } catch (_: Exception) {
                exchange.close()
            }
        }
        server.start()
        try {
            val directory = temporaryFolder.newFolder()
            val cache = directory.resolve("cancel.audio").apply { writeBytes(oldBody) }
            val download = async(Dispatchers.IO) {
                TvPreviewAudioDownloader.download(
                    "http://127.0.0.1:${server.address.port}/audio",
                    directory,
                    "cancel",
                )
            }
            assertTrue("Download did not reach the controlled response", responseStarted.await(5, TimeUnit.SECONDS))
            download.cancel()
            releaseResponse.countDown()
            try {
                download.await()
                fail("Canceled download must not complete")
            } catch (_: CancellationException) {
                // Expected cancellation after the blocked read is released.
            }
            assertArrayEquals(oldBody, cache.readBytes())
            assertFalse(directory.resolve("cancel.audio.partial").exists())
        } finally {
            releaseResponse.countDown()
            server.stop(0)
        }
    }
}
