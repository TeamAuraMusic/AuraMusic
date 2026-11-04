package com.auramusic.app.tv

internal const val QUICK_PICKS_ROTATION_INTERVAL_MILLIS = 4 * 60 * 60 * 1000L

internal fun tvRecommendationRotationSeed(nowMillis: Long): Int =
    (nowMillis / QUICK_PICKS_ROTATION_INTERVAL_MILLIS).toInt()

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
