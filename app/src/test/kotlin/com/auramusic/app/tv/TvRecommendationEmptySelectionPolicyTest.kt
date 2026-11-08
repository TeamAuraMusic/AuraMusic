package com.auramusic.app.tv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvRecommendationEmptySelectionPolicyTest {
    @Test
    fun clearsStaleProgramsWhenBothSourcesWereReadAndSelectionIsEmpty() {
        assertTrue(shouldRemoveStaleTvPrograms(true, true, selectedProgramCount = 0))
    }

    @Test
    fun preservesExistingProgramsWhenEitherSourceSnapshotFailed() {
        assertFalse(shouldRemoveStaleTvPrograms(false, true, selectedProgramCount = 0))
        assertFalse(shouldRemoveStaleTvPrograms(true, false, selectedProgramCount = 0))
    }

    @Test
    fun neverClearsProgramsWhenSelectionContainsItems() {
        assertFalse(shouldRemoveStaleTvPrograms(true, true, selectedProgramCount = 1))
    }
}
