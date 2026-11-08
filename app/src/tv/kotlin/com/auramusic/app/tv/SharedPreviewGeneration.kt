package com.auramusic.app.tv

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/** Shares one preview preparation and cancels its producer after the last waiter leaves. */
internal class SharedPreviewGeneration<T> {
    val result = CompletableDeferred<T>()
    val cleanupComplete = CompletableDeferred<Unit>()
    private var waiterCount = 0
    private var producer: Job? = null
    private var abandoned = false
    private var failed = false

    @Synchronized
    fun tryAddWaiter(): Boolean {
        if (abandoned || failed || result.isCancelled || producer?.isCancelled == true) {
            return false
        }
        waiterCount++
        return true
    }

    @Synchronized
    fun attachProducer(job: Job) {
        check(producer == null) { "Preview generation already has a producer" }
        producer = job
        if (waiterCount == 0 && !result.isCompleted) {
            abandoned = true
            job.cancel()
        }
    }

    @Synchronized
    fun releaseWaiter() {
        check(waiterCount > 0) { "Preview generation waiter count underflow" }
        waiterCount--
        if (waiterCount == 0 && !result.isCompleted) {
            abandoned = true
            producer?.cancel()
        }
    }

    fun markCleanupComplete() {
        cleanupComplete.complete(Unit)
    }

    @Synchronized
    fun markFailed() {
        failed = true
    }
}

internal data class SharedPreviewGenerationAcquisition<T>(
    val generation: SharedPreviewGeneration<T>,
    val isNew: Boolean,
)

/** Replaces failed/abandoned tasks only after their producer has finished cleanup. */
internal class SharedPreviewGenerationRegistry<K, T> {
    private val generations = ConcurrentHashMap<K, SharedPreviewGeneration<T>>()

    suspend fun acquire(
        key: K,
        isReady: () -> Boolean = { false },
    ): SharedPreviewGenerationAcquisition<T>? {
        while (true) {
            if (isReady()) return null
            val candidate = SharedPreviewGeneration<T>()
            check(candidate.tryAddWaiter())
            val existing = generations.putIfAbsent(key, candidate)
            if (existing == null) {
                return SharedPreviewGenerationAcquisition(candidate, isNew = true)
            }
            candidate.releaseWaiter()
            if (existing.tryAddWaiter()) {
                return SharedPreviewGenerationAcquisition(existing, isNew = false)
            }
            existing.cleanupComplete.await()
        }
    }

    fun remove(key: K, generation: SharedPreviewGeneration<T>) {
        generations.remove(key, generation)
    }
}
