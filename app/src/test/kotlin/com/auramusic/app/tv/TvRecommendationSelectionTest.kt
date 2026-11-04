package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TvRecommendationSelectionTest {

    @Test
    fun quickPicksRotationIsStableWithinFourHoursAndAdvancesAfterward() {
        val window = QUICK_PICKS_ROTATION_INTERVAL_MILLIS

        assertEquals(tvRecommendationRotationSeed(0), tvRecommendationRotationSeed(window - 1))
        assertNotEquals(tvRecommendationRotationSeed(0), tvRecommendationRotationSeed(window))
    }

    @Test
    fun prioritizesDistinctQuickPicksThenFillsWithNonduplicateQueueItems() {
        assertEquals(
            listOf("pick-a", "shared", "pick-b", "queue-a", "queue-b"),
            selectTvRecommendationMediaIds(
                queueMediaIds = listOf("queue-a", "shared", "queue-b"),
                quickPickMediaIds = listOf("pick-a", "shared", "pick-a", "pick-b"),
                limit = 5,
            ),
        )
    }

    @Test
    fun fillsTheWholeRowWithQuickPicksWhenAvailable() {
        assertEquals(
            listOf("pick-a", "pick-b", "pick-c"),
            selectTvRecommendationMediaIds(
                queueMediaIds = listOf("queue-a", "queue-b", "queue-c"),
                quickPickMediaIds = listOf("pick-a", "pick-b", "pick-c", "pick-d"),
                limit = 3,
            ),
        )
    }

    @Test
    fun fallsBackToQueueWhenQuickPicksAreUnavailable() {
        assertEquals(
            listOf("queue-a", "queue-a", "queue-b"),
            selectTvRecommendationMediaIds(
                queueMediaIds = listOf("queue-a", "queue-a", "queue-b"),
                quickPickMediaIds = emptyList(),
                limit = 3,
            ),
        )
    }
}
