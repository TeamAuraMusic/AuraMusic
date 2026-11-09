package com.auramusic.app.tv

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPreviewSourceResolverTest {
    @Test
    fun usesDirectSourceWithoutInvokingFallback() = runBlocking {
        var fallbackCalled = false

        val source = resolveDirectThenFallback(
            direct = { Result.success("direct") },
            fallback = { fallbackCalled = true; "fallback" },
        )

        assertEquals("direct", source)
        assertFalse(fallbackCalled)
    }

    @Test
    fun usesFallbackWhenDirectResolverReturnsFailure() = runBlocking {
        var fallbackCalled = false

        val source = resolveDirectThenFallback(
            direct = { Result.failure(IllegalStateException("direct failed")) },
            fallback = { fallbackCalled = true; "playback-resolver" },
        )

        assertEquals("playback-resolver", source)
        assertTrue(fallbackCalled)
    }

    @Test
    fun cancellationFromDirectResolverDoesNotFallBack() = runBlocking {
        val cancelled = CancellationException("cancelled")
        var fallbackCalled = false
        var caught: CancellationException? = null

        try {
            resolveDirectThenFallback(
                direct = { Result.failure(cancelled) },
                fallback = { fallbackCalled = true; "fallback" },
            )
        } catch (error: CancellationException) {
            caught = error
        }

        assertSame(cancelled, caught)
        assertFalse(fallbackCalled)
    }

    @Test
    fun propagatesFallbackFailure() = runBlocking {
        val expected = IllegalArgumentException("fallback failed")
        var caught: IllegalArgumentException? = null

        try {
            resolveDirectThenFallback(
                direct = { Result.failure(IllegalStateException("direct failed")) },
                fallback = { throw expected },
            )
        } catch (error: IllegalArgumentException) {
            caught = error
        }

        assertSame(expected, caught)
    }
}
