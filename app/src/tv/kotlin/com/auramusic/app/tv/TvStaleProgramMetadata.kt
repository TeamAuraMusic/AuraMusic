package com.auramusic.app.tv

internal data class RetainedTvProgramMetadata(
    val internalProviderId: String,
    val contentId: String,
    val previewAudioUri: String?,
    val posterArtUri: String,
    val durationMs: Long,
    val title: String,
    val expiresAtMs: Long,
)

internal fun retainRecentlyDeletedTvPrograms(
    existing: List<RetainedTvProgramMetadata>,
    deleted: List<ExistingPreviewProgram>,
    nowMillis: Long,
    ttlMillis: Long,
    maxEntries: Int,
): List<RetainedTvProgramMetadata> {
    require(ttlMillis > 0L)
    require(maxEntries >= 0)
    val retained = existing
        .filter { it.expiresAtMs > nowMillis }
        .associateByTo(linkedMapOf(), RetainedTvProgramMetadata::internalProviderId)
    val expiry = nowMillis + ttlMillis
    deleted.forEach { program ->
        val providerId = program.internalProviderId?.takeIf(String::isNotBlank) ?: return@forEach
        val contentId = program.contentId?.takeIf(String::isNotBlank) ?: return@forEach
        val artworkUri = program.posterArtUri?.takeIf(String::isNotBlank) ?: return@forEach
        retained[providerId] = RetainedTvProgramMetadata(
            internalProviderId = providerId,
            contentId = contentId,
            previewAudioUri = program.previewAudioUri?.takeIf(String::isNotBlank),
            posterArtUri = artworkUri,
            durationMs = program.durationMs.coerceAtLeast(0L),
            title = program.title.orEmpty(),
            expiresAtMs = expiry,
        )
    }
    return retained.values.sortedByDescending(RetainedTvProgramMetadata::expiresAtMs)
        .take(maxEntries)
}

internal fun findRecentlyDeletedTvProgram(
    retained: List<RetainedTvProgramMetadata>,
    internalProviderId: String,
    nowMillis: Long,
): RetainedTvProgramMetadata? = retained.firstOrNull {
    it.internalProviderId == internalProviderId && it.expiresAtMs > nowMillis
}
