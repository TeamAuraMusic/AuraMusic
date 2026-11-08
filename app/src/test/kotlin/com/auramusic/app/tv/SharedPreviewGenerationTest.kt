package com.auramusic.app.tv

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreviewGenerationTest {
    @Test
    fun cancelsProducerOnlyAfterLastSharedWaiterLeaves() = runBlocking {
        val generation = SharedPreviewGeneration<String>()
        val started = CompletableDeferred<Unit>()
        val producerCancelled = AtomicBoolean(false)
        assertTrue(generation.tryAddWaiter())
        assertTrue(generation.tryAddWaiter())
        val producer = launch {
            try {
                started.complete(Unit)
                awaitCancellation()
            } finally {
                producerCancelled.set(true)
            }
        }
        generation.attachProducer(producer)
        started.await()

        generation.releaseWaiter()
        assertFalse(producer.isCancelled)
        assertFalse(producerCancelled.get())

        generation.releaseWaiter()
        producer.join()
        assertTrue(producer.isCancelled)
        assertTrue(producerCancelled.get())
    }

    @Test
    fun completedGenerationIsNotCanceledWhenLastWaiterLeaves() = runBlocking {
        val generation = SharedPreviewGeneration<String>()
        val producer = launch { generation.result.complete("ready") }
        assertTrue(generation.tryAddWaiter())
        generation.attachProducer(producer)
        producer.join()

        generation.releaseWaiter()
        assertFalse(producer.isCancelled)
        assertTrue(generation.result.isCompleted)
    }

    @Test
    fun rejectsNewWaitersAfterLastReleaseUntilCleanupCompletes() = runBlocking {
        val generation = SharedPreviewGeneration<String>()
        val started = CompletableDeferred<Unit>()
        assertTrue(generation.tryAddWaiter())
        val producer = launch {
            try {
                started.complete(Unit)
                awaitCancellation()
            } finally {
                generation.markCleanupComplete()
            }
        }
        generation.attachProducer(producer)
        started.await()

        generation.releaseWaiter()
        assertFalse(generation.tryAddWaiter())
        producer.join()
        generation.cleanupComplete.await()
        assertFalse(generation.tryAddWaiter())
    }

    @Test
    fun registryCreatesFreshGenerationAfterAbandonedProducerCleanup() = runBlocking {
        val registry = SharedPreviewGenerationRegistry<String, String>()
        val first = requireNotNull(registry.acquire("track"))
        val started = CompletableDeferred<Unit>()
        val producer = launch {
            try {
                started.complete(Unit)
                awaitCancellation()
            } finally {
                registry.remove("track", first.generation)
                first.generation.markCleanupComplete()
            }
        }
        first.generation.attachProducer(producer)
        started.await()
        first.generation.releaseWaiter()

        val replacement = requireNotNull(registry.acquire("track"))
        assertTrue(replacement.isNew)
        assertNotSame(first.generation, replacement.generation)
        producer.join()

        replacement.generation.result.complete("ready")
        replacement.generation.releaseWaiter()
        registry.remove("track", replacement.generation)
        replacement.generation.markCleanupComplete()
    }

    @Test
    fun concurrentAcquisitionsShareOneRegisteredGeneration() = runBlocking {
        val registry = SharedPreviewGenerationRegistry<String, String>()
        val acquisitions = coroutineScope {
            List(8) {
                async(Dispatchers.Default) {
                    requireNotNull(registry.acquire("same-song"))
                }
            }.awaitAll()
        }
        val shared = acquisitions.first().generation

        assertEquals(1, acquisitions.count { it.isNew })
        acquisitions.forEach { assertSame(shared, it.generation) }
        shared.result.complete("ready")
        acquisitions.forEach { it.generation.releaseWaiter() }
        registry.remove("same-song", shared)
        shared.markCleanupComplete()
    }
}
