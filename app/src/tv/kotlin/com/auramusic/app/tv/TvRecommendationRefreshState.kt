package com.auramusic.app.tv

/** Coalesces refresh requests received during an active or queued publication. */
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

    /** A worker is starting now and will read the latest state, so older queued requests collapse. */
    fun markStarted() {
        refreshing = true
        pendingStartId = null
    }

    /** Returns the newest queued start ID, or null when the service can stop. */
    fun finish(): Int? {
        val nextStartId = pendingStartId
        pendingStartId = null
        if (nextStartId == null) refreshing = false
        return nextStartId
    }
}
