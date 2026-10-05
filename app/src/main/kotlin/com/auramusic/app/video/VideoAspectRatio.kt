/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.util.Rational
import androidx.media3.common.VideoSize

/** Assumed shape for a video whose dimensions are not known yet. */
internal const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f

/**
 * Android clamps the picture-in-picture aspect ratio to this range and throws outside it, so a
 * portrait video has to be narrowed to 1:2.39 before it reaches [Rational].
 */
internal const val MAX_PIP_ASPECT_RATIO = 2.39f

/**
 * Display aspect ratio of a decoded frame, or null when it cannot be determined.
 *
 * Pixel dimensions alone are wrong for anamorphic content, so the frame's pixel aspect ratio is
 * folded in. Guards against YouTube's smallest renditions, which are often rounded
 * (e.g. 426x240 instead of 16:9) and would otherwise letterbox the PiP window slightly.
 */
internal fun VideoSize.toDisplayAspectRatioOrNull(): Float? {
    if (width <= 0 || height <= 0) return null
    val pixelRatio = if (pixelWidthHeightRatio > 0f) pixelWidthHeightRatio else 1f
    return sanitizeDisplayAspectRatio(width * pixelRatio / height)
}

internal fun sanitizeDisplayAspectRatio(aspectRatio: Float): Float =
    if (aspectRatio.isFinite() && aspectRatio > 0f) aspectRatio else DEFAULT_VIDEO_ASPECT_RATIO

/**
 * Aspect ratio clamped to what the platform accepts for a PiP window.
 */
internal fun sanitizePipAspectRatio(aspectRatio: Float): Float =
    sanitizeDisplayAspectRatio(aspectRatio)
        .coerceIn(1f / MAX_PIP_ASPECT_RATIO, MAX_PIP_ASPECT_RATIO)