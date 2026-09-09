/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import java.math.BigInteger
import java.security.MessageDigest

fun makeTimeString(duration: Long?): String {
    if (duration == null || duration < 0) return ""
    var sec = duration / 1000
    val day = sec / 86400
    sec %= 86400
    val hour = sec / 3600
    sec %= 3600
    val minute = sec / 60
    sec %= 60
    return when {
        day > 0 -> "%d:%02d:%02d:%02d".format(day, hour, minute, sec)
        hour > 0 -> "%d:%02d:%02d".format(hour, minute, sec)
        else -> "%d:%02d".format(minute, sec)
    }
}

fun md5(str: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')
}

fun joinByBullet(vararg str: String?) =
    str
        .filterNot {
            it.isNullOrEmpty()
        }.joinToString(separator = " • ")

private val compactNumberRegex = Regex("^([\\d.,]+)(.*)$")

/**
 * Converts a plain view-count text like "9,659 views" into a YouTube-style compact
 * form ("9.7K views"). Texts that are already compact ("12M views"), non-numeric
 * ("No views") or live counters ("1,234 watching") are left untouched.
 */
fun compactViewCount(text: String): String {
    val match = compactNumberRegex.find(text.trim()) ?: return text
    val (numberPart, suffix) = match.destructured
    if (numberPart.any { it == 'K' || it == 'k' || it == 'M' || it == 'm' || it == 'B' || it == 'b' }) {
        return text
    }
    val value = numberPart.replace(",", "").toDoubleOrNull() ?: return text
    fun divisor(unit: String): Double = when (unit) {
        "B" -> 1_000_000_000.0
        "M" -> 1_000_000.0
        else -> 1_000.0
    }
    fun compact(unit: String): String =
        ("%.1f$unit".format(value / divisor(unit))).trimEnd('0').trimEnd('.')
    val compact = when {
        value >= 1_000_000_000 -> compact("B")
        value >= 1_000_000 -> compact("M")
        value >= 1_000 -> compact("K")
        else -> numberPart.trimStart('0').ifEmpty { "0" }
    }
    return compact + suffix
}
