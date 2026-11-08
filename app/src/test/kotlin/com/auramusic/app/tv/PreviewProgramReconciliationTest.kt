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

    @Test
    fun confirmedEmptySelectionDeletesEveryExistingProgram() {
        val plan = planPreviewProgramReconciliation(
            existing = listOf(
                ExistingPreviewProgram(30, "auramusic:track:a:0"),
                ExistingPreviewProgram(31, "auramusic:track:b:0"),
                ExistingPreviewProgram(32, null),
            ),
            requestedInternalIds = emptyList(),
        )

        assertEquals(emptyList<Pair<Long, String>>(), plan.updates)
        assertEquals(emptyList<String>(), plan.inserts)
        assertEquals(listOf(32L, 30L, 31L), plan.deletes)
    }

    @Test
    fun preservesStableIdsWhileReorderingAndApplyingAddRemoveChanges() {
        val plan = planPreviewProgramReconciliation(
            existing = listOf(
                ExistingPreviewProgram(40, "auramusic:track:a:0"),
                ExistingPreviewProgram(41, "auramusic:track:b:0"),
                ExistingPreviewProgram(42, "auramusic:track:c:0"),
            ),
            requestedInternalIds = listOf(
                "auramusic:track:c:0",
                "auramusic:track:a:0",
                "auramusic:track:d:0",
            ),
        )

        assertEquals(
            listOf(42L to "auramusic:track:c:0", 40L to "auramusic:track:a:0"),
            plan.updates,
        )
        assertEquals(listOf("auramusic:track:d:0"), plan.inserts)
        assertEquals(listOf(41L), plan.deletes)
    }
}
