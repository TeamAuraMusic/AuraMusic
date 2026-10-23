/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

/**
 * Finds the YouTube Music playlist that a pasted share link or raw playlist id points at,
 * so search can open someone's playlist directly instead of searching for the link text.
 */
object PlaylistLinks {
    private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val hostRegex = Regex("""^https?://([^/?#]+)""", RegexOption.IGNORE_CASE)
    private val listParamRegex = Regex("""[?&]list=([A-Za-z0-9_-]+)""")
    private val browsePathRegex = Regex("""/browse/(VL[A-Za-z0-9_-]+)""")

    // User and community playlists (PL), album playlists (OLAK5uy_) and YouTube Music's
    // curated playlists (RDCLAK5uy_). Radio mixes (other RD ids) cannot be opened as a playlist.
    private val playlistIdRegex = Regex("""^(PL|OLAK5uy_|RDCLAK5uy_)[A-Za-z0-9_-]{16,}$""")

    /** Returns the playlist id, without a "VL" browse prefix, or null when [text] is not a playlist link. */
    fun playlistId(text: String): String? {
        val trimmed = text.trim()
        val url = urlRegex.find(trimmed)?.value
        val candidate = if (url != null) {
            val host = hostRegex.find(url)?.groupValues?.get(1)?.lowercase() ?: return null
            if (host != "youtu.be" && host != "youtube.com" && !host.endsWith(".youtube.com")) return null
            listParamRegex.find(url)?.groupValues?.get(1)
                ?: browsePathRegex.find(url)?.groupValues?.get(1)
                ?: return null
        } else {
            trimmed
        }
        return candidate.removePrefix("VL").takeIf { playlistIdRegex.matches(it) }
    }

    fun isAlbumPlaylist(playlistId: String) = playlistId.startsWith("OLAK5uy_")
}
