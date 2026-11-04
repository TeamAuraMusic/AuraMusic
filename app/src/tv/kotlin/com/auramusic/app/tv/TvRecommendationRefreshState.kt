package com.auramusic.app.tv

/** Coalesces overlapping service starts into one follow-up refresh. */
internal class TvRecommendationRefreshState {
    private var refreshing = false
    private var pendingStartId: Int? = null

    /** Returns true when the caller should start a refresh now. */
    fun request(startId: Int): Boolean {
        if (refreshing) {
            pendingStartId = startId
            return false
        }

        refreshing = true
        return true
    }

    /** Returns the newest queued start ID, or null when the service can stop. */
    fun finish(): Int? {
        val nextStartId = pendingStartId
        pendingStartId = null
        if (nextStartId == null) refreshing = false
        return nextStartId
    }
}
