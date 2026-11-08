package com.auramusic.app.tv

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TvPreviewAudioDownloadGateTest {
    @Test
    fun boundsConcurrentDistinctSongDownloads() = runBlocking {
        val gate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 2)
        val active = AtomicInteger()
        val maximumActive = AtomicInteger()
        val completed = AtomicInteger()

        val downloads = List(8) {
            launch(Dispatchers.Default) {
                gate.run {
                    val nowActive = active.incrementAndGet()
                    maximumActive.updateAndGet { maxOf(it, nowActive) }
                    try {
                        delay(25)
                        completed.incrementAndGet()
                    } finally {
                        active.decrementAndGet()
                    }
                }
            }
        }
        downloads.joinAll()

        assertEquals(2, maximumActive.get())
        assertEquals(8, completed.get())
        assertEquals(0, active.get())
    }

    @Test
    fun resolvesSourceOnlyAfterAcquiringPreparationPermit() = runBlocking {
        val gate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 1)
        val firstResolverEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val secondResolverEntered = CompletableDeferred<Unit>()

        val first = async(Dispatchers.Default) {
            gate.runWithSource(
                resolve = {
                    firstResolverEntered.complete(Unit)
                    releaseFirst.await()
                    "fresh-a"
                },
                block = { it },
            )
        }
        withTimeout(1_000) { firstResolverEntered.await() }

        val second = async(Dispatchers.Default) {
            secondStarted.complete(Unit)
            gate.runWithSource(
                resolve = {
                    secondResolverEntered.complete(Unit)
                    "fresh-b"
                },
                block = { it },
            )
        }
        withTimeout(1_000) { secondStarted.await() }
        yield()
        assertFalse(secondResolverEntered.isCompleted)

        releaseFirst.complete(Unit)
        assertEquals("fresh-a" to "fresh-a", withTimeout(1_000) { first.await() })
        assertEquals("fresh-b" to "fresh-b", withTimeout(1_000) { second.await() })
    }

    @Test
    fun refreshesExpiredSignedSourceOnceAfterAuthorizationFailure() = runBlocking {
        val gate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 1)
        var initialResolutions = 0
        var refreshResolutions = 0
        var downloadAttempts = 0

        val result = gate.runWithSource(
            resolve = { initialResolutions++; "expired-signed-url" },
            refresh = { refreshResolutions++; "refreshed-signed-url" },
            shouldRefresh = ::shouldRefreshPreviewAudioSource,
        ) { source ->
            downloadAttempts++
            if (source == "expired-signed-url") throw PreviewAudioHttpException(403)
            "downloaded-from-$source"
        }

        assertEquals("refreshed-signed-url" to "downloaded-from-refreshed-signed-url", result)
        assertEquals(1, initialResolutions)
        assertEquals(1, refreshResolutions)
        assertEquals(2, downloadAttempts)
    }

    @Test
    fun doesNotRefreshSourceForNonAuthorizationServerFailures() = runBlocking {
        val gate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 1)
        var refreshResolutions = 0
        val result = runCatching {
            gate.runWithSource(
                resolve = { "valid-signed-url" },
                refresh = { refreshResolutions++; "unneeded-refresh" },
                shouldRefresh = ::shouldRefreshPreviewAudioSource,
            ) { throw PreviewAudioHttpException(503) }
        }

        assertEquals(503, (result.exceptionOrNull() as? PreviewAudioHttpException)?.statusCode)
        assertEquals(0, refreshResolutions)
    }

    @Test
    fun doesNotRetryMoreThanOnceWhenRefreshedSourceIsAlsoRejected() = runBlocking {
        val gate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 1)
        var refreshResolutions = 0
        val result = runCatching {
            gate.runWithSource(
                resolve = { "expired-signed-url" },
                refresh = { refreshResolutions++; "also-expired-url" },
                shouldRefresh = ::shouldRefreshPreviewAudioSource,
            ) { throw PreviewAudioHttpException(410) }
        }

        assertEquals(410, (result.exceptionOrNull() as? PreviewAudioHttpException)?.statusCode)
        assertEquals(1, refreshResolutions)
    }

    @Test
    fun cancellationDoesNotResolveAnotherSourceOrRetryDownload() = runBlocking {
        val gate = TvPreviewAudioDownloadGate(maxConcurrentDownloads = 1)
        val downloadStarted = CompletableDeferred<Unit>()
        var refreshResolutions = 0
        var downloadAttempts = 0

        val download = launch {
            gate.runWithSource(
                resolve = { "initial-source" },
                refresh = { refreshResolutions++; "refreshed-source" },
                shouldRefresh = ::shouldRefreshPreviewAudioSource,
            ) {
                downloadAttempts++
                downloadStarted.complete(Unit)
                awaitCancellation()
            }
        }
        withTimeout(1_000) { downloadStarted.await() }
        download.cancelAndJoin()

        assertEquals(1, downloadAttempts)
        assertEquals(0, refreshResolutions)
    }
}
