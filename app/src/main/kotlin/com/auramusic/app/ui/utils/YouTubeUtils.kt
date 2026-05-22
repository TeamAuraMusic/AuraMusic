/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    val isGoogleCdn = this.contains("googleusercontent.com") || this.contains("ggpht.com")
    val isYtimg = this.contains("i.ytimg.com")

    if (isGoogleCdn) {
        val w = width ?: height!!
        val h = height ?: width!!

        // Replace existing wNNN-hNNN segments anywhere in the URL/path
        if (this.contains(Regex("w\\d+-h\\d+"))) {
            return this.replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
        }

        // Trim any existing size parameters (=w..., =s..., =h...) so we can rebuild them
        val baseUrl = this.split("=w", "=s", "=h", limit = 2)[0]

        // Use =w-h-p-l90-rj for explicit dimensions / banners (smart cropping, high quality JPEG),
        // otherwise use =s for square-ish images.
        return if ((this.contains("=w") && this.contains("-h")) || (width != null && height != null)) {
            "$baseUrl=w$w-h$h-p-l90-rj"
        } else {
            "$baseUrl=s$w-p-l90-rj"
        }
    }

    if (isYtimg) {
        val target = width ?: height!!
        return when {
            target > 480 -> this
                .replace("hqdefault.jpg", "maxresdefault.jpg")
                .replace("mqdefault.jpg", "maxresdefault.jpg")
                .replace("sddefault.jpg", "maxresdefault.jpg")
                .replace("default.jpg", "maxresdefault.jpg")
            target > 320 -> this
                .replace("mqdefault.jpg", "hqdefault.jpg")
                .replace("default.jpg", "hqdefault.jpg")
            else -> this
        }
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
