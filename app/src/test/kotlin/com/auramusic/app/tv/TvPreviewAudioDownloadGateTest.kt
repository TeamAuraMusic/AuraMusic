package com.auramusic.app.tv

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.joinAll
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
}
