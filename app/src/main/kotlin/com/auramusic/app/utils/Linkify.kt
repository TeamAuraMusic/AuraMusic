/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

private const val SCHEME_PREFIX = "https://"

// Deliberately narrow: it stops at whitespace and at the brackets/quotes that prose usually
// wraps a URL in, so a trailing sentence period is not swallowed into the link.
private val UrlRegex =
    Regex("""(?:https?://|www\.)[^\s<>"'()\[\]{}]+""")

/**
 * Turns the URLs inside [text] into tappable links.
 *
 * Channel and video descriptions are plain text from the API, so without this every link in
 * them is dead - the user has to copy it out by hand. Compose opens the annotation through
 * the platform Uri handler, which routes to whatever app claims the scheme (browser, mail
 * client, the YouTube app for a youtube.com link, and so on).
 */
fun linkifiedText(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in UrlRegex.findAll(text)) {
        // Prose punctuation right after a URL almost never belongs to the link itself.
        val displayed = match.value.trimEnd('.', ',', ';', ':', '!', '?')
        if (displayed.isEmpty()) continue
        val opened = if (displayed.startsWith("www.")) "$SCHEME_PREFIX$displayed" else displayed
        append(text.substring(cursor, match.range.first))
        withLink(LinkAnnotation.Url(opened)) {
            append(displayed)
        }
        cursor = match.range.first + displayed.length
    }
    append(text.substring(cursor))
}
