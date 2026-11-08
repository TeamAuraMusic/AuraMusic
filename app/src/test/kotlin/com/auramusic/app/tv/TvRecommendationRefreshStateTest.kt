package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvRecommendationRefreshStateTest {

    @Test
    fun coalescesRequestsDuringRefreshToNewestStartId() {
        val state = TvRecommendationRefreshState()

        assertTrue(state.request(1))
        assertFalse(state.request(2))
        assertFalse(state.request(3))

        assertEquals(3, state.finish())
        assertEquals(null, state.finish())
        assertTrue(state.request(4))
    }

    @Test
    fun startingQueuedRefreshConsumesRequestsThatArrivedBeforeItsSnapshot() {
        val state = TvRecommendationRefreshState()

        assertTrue(state.request(1))
        assertFalse(state.request(2))
        state.markStarted()

        assertEquals(null, state.finish())
        assertTrue(state.request(3))
    }

    @Test
    fun requestsDuringPublicationCollapseIntoOneTrailingRefresh() {
        val state = TvRecommendationRefreshState()

        assertTrue(state.request(1))
        state.markStarted()
        assertFalse(state.request(2))
        assertFalse(state.request(3))
        assertEquals(3, state.finish())

        state.markStarted()
        assertEquals(null, state.finish())
        assertTrue(state.request(4))
    }
}
