package com.auramusic.app.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewProgramReconciliationTest {

    @Test
    fun reusesUnchangedProgramsAndOnlyAddsOrRemovesTheDifference() {
        val plan = planPreviewProgramReconciliation(
            existing = listOf(
                ExistingPreviewProgram(10, "auramusic:track:a:0"),
                ExistingPreviewProgram(11, "auramusic:track:stale:0"),
            ),
            requestedInternalIds = listOf("auramusic:track:a:0", "auramusic:track:b:0"),
        )

        assertEquals(listOf(10L to "auramusic:track:a:0"), plan.updates)
        assertEquals(listOf("auramusic:track:b:0"), plan.inserts)
        assertEquals(listOf(11L), plan.deletes)
    }

    @Test
    fun removesLegacyAndDuplicateRowsWithoutReplacingTheRetainedRow() {
        val plan = planPreviewProgramReconciliation(
            existing = listOf(
                ExistingPreviewProgram(20, "auramusic:track:a:0"),
                ExistingPreviewProgram(21, "auramusic:track:a:0"),
                ExistingPreviewProgram(22, null),
            ),
            requestedInternalIds = listOf("auramusic:track:a:0"),
        )

        assertEquals(listOf(20L to "auramusic:track:a:0"), plan.updates)
        assertEquals(emptyList<String>(), plan.inserts)
        assertEquals(listOf(22L, 21L), plan.deletes)
    }
}
