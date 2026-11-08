package com.auramusic.app.tv

internal data class ExistingPreviewProgram(
    val programId: Long,
    val internalProviderId: String?,
    val contentId: String? = null,
    val previewVideoUri: String? = null,
    val previewAudioUri: String? = null,
    val posterArtUri: String? = null,
    val title: String? = null,
    val durationMs: Long = 0L,
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
