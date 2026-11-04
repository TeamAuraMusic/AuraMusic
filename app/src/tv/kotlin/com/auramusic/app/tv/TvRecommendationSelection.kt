package com.auramusic.app.tv

/** Publishes distinct personalized picks first, then uses the saved queue to fill gaps. */
internal fun selectTvRecommendationMediaIds(
    queueMediaIds: List<String>,
    quickPickMediaIds: List<String>,
    limit: Int,
): List<String> {
    if (limit <= 0) return emptyList()

    val quickPickIds = quickPickMediaIds.asSequence()
        .filter(String::isNotBlank)
        .distinct()
        .take(limit)
        .toList()
    val selectedIds = quickPickIds.toSet()
    val queueIds = queueMediaIds.asSequence()
        .filter(String::isNotBlank)
        .filter { it !in selectedIds }
        .take(limit - quickPickIds.size)
        .toList()

    return quickPickIds + queueIds
}
