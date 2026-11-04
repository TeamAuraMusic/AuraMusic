package com.auramusic.app.tv

internal data class ExistingPreviewProgram(
    val programId: Long,
    val internalProviderId: String?,
)

internal data class PreviewProgramReconciliationPlan(
    val updates: List<Pair<Long, String>>,
    val inserts: List<String>,
    val deletes: List<Long>,
)

internal fun planPreviewProgramReconciliation(
    existing: List<ExistingPreviewProgram>,
    requestedInternalIds: List<String>,
): PreviewProgramReconciliationPlan {
    val existingByInternalId = linkedMapOf<String, MutableList<Long>>()
    val deletes = mutableListOf<Long>()

    existing.forEach { program ->
        val internalId = program.internalProviderId
        if (internalId.isNullOrBlank()) {
            deletes += program.programId
        } else {
            existingByInternalId.getOrPut(internalId, ::mutableListOf) += program.programId
        }
    }

    val updates = mutableListOf<Pair<Long, String>>()
    val inserts = mutableListOf<String>()
    requestedInternalIds.forEach { internalId ->
        val programIds = existingByInternalId[internalId]
        if (programIds.isNullOrEmpty()) {
            inserts += internalId
        } else {
            updates += programIds.removeAt(0) to internalId
        }
    }

    existingByInternalId.values.forEach { deletes += it }
    return PreviewProgramReconciliationPlan(updates, inserts, deletes)
}
