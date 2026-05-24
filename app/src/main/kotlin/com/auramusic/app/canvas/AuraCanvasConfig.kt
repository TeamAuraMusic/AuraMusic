package com.auramusic.app.canvas

/**
 * Central configuration for Aura Canvas providers.
 * This is where you plug in your self-hosted Render instance (or any remote backend).
 */
object AuraCanvasConfig {

    /**
     * Remote backend URL (e.g. your self-hosted Spotify Canvas API on Render).
     * Leave empty or null to disable remote on-demand lookups.
     */
    const val REMOTE_BASE_URL: String = "https://auramusiccanvasserver.onrender.com"

    /**
     * Optional API key for your remote backend.
     * Only needed if you added Bearer token protection when deploying on Render.
     */
    val REMOTE_API_KEY: String? = null   // e.g. "sk-auramusic-abc123"

    /**
     * The RemoteCanvasProvider instance used for on-demand artist/album lookups.
     * Returns null if REMOTE_BASE_URL is not configured.
     */
    val remoteProvider: RemoteCanvasProvider? by lazy {
        if (REMOTE_BASE_URL.isNotBlank()) {
            RemoteCanvasProvider(
                baseUrl = REMOTE_BASE_URL,
                apiKey = REMOTE_API_KEY
            )
        } else {
            null
        }
    }

    /**
     * Default provider chain used across the app.
     * Order: Manifest (fast) → Remote (on-demand via your sp_dc)
     */
    val defaultProvider: AuraCanvasProvider by lazy {
        createDefaultAuraCanvasProvider(
            manifestProvider = ManifestAuraCanvasProvider(),
            spotifyDirectProvider = remoteProvider
        )
    }
}
