/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    // Upgrade YouTube video thumbnails to higher quality for sharper display
    if (this matches "https://i\\.ytimg\\.com/vi/[^/]+/[^/]+\\.jpg".toRegex()) {
        val quality = when {
            (width ?: 0) >= 1280 || (height ?: 0) >= 720 -> "maxresdefault"
            else -> "hqdefault"
        }
        return replace(Regex("/[^/]+\\.jpg$"), "/$quality.jpg")
    }
    if (width == null && height == null) return this

    // YouTube Music / Google lh3 thumbnails - request high quality
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width ?: 1200
        var h = height ?: 1200
        if (width == null && height != null) w = (h * W) / H
        if (height == null && width != null) h = (w * H) / W
        // Use high quality parameters (p-l90-rj is good balance of quality/size)
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }

    // Artist thumbnails (yt3.ggpht)
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        val target = width ?: height ?: 1200
        return "$this-s$target"
    }

    return this
}

/**
 * Returns a high quality version of the thumbnail URL.
 * Useful for large album art in player and album detail screens.
 */
fun String?.toHighQualityThumbnail(): String? {
    if (this == null) return null
    return this.resize(1200, 1200)
}
