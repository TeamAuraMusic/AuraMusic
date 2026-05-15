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
        val quality = if (width != null && width >= 1280) "maxresdefault" else "hqdefault"
        return replace(Regex("/[^/]+\\.jpg$"), "/$quality.jpg")
    }
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    return this
}
