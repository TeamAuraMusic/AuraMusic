package com.auramusic.app.tv

internal data class PreviewAudioCacheEntry(
    val path: String,
    val sizeBytes: Long,
    val lastAccessMs: Long,
)

/** Returns oldest unprotected completed entries needed to bring the cache under its byte limit. */
internal fun planPreviewAudioCacheEvictions(
    entries: List<PreviewAudioCacheEntry>,
    maxBytes: Long,
    protectedPaths: Set<String>,
): List<String> {
    require(maxBytes >= 0L)
    var bytes = entries.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    val evictions = mutableListOf<String>()
    for (entry in entries.sortedBy(PreviewAudioCacheEntry::lastAccessMs)) {
        if (bytes <= maxBytes) break
        if (entry.path in protectedPaths) continue
        evictions += entry.path
        bytes -= entry.sizeBytes.coerceAtLeast(0L)
    }
    return evictions
}
