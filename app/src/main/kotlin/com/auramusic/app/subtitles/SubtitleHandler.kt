package com.auramusic.app.subtitles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object SubtitleHandler {

    suspend fun parseVtt(content: String): List<SubtitleCue> = withContext(Dispatchers.Default) {
        val cues = mutableListOf<SubtitleCue>()
        try {
            val lines = content.lines()
            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty() || line == "WEBVTT" || line.startsWith("NOTE")) {
                    i++
                    continue
                }
                if (line.contains("-->")) {
                    val parts = line.split("-->")
                    if (parts.size == 2) {
                        val startMs = parseTimestamp(parts[0].trim())
                        val endParts = parts[1].trim().split(" ")
                        val endMs = parseTimestamp(endParts[0])
                        val textLines = mutableListOf<String>()
                        i++
                        while (i < lines.size && lines[i].trim().isNotEmpty()) {
                            textLines.add(lines[i].trim())
                            i++
                        }
                        if (textLines.isNotEmpty() && startMs != null && endMs != null) {
                            cues.add(SubtitleCue(startMs, endMs, textLines.joinToString("\n")))
                        }
                        continue
                    }
                }
                i++
            }
        } catch (e: Exception) {
            Timber.e(e, "SubtitleHandler: Error parsing VTT content")
        }
        cues
    }

    suspend fun parseTimedText(timedText: String): List<SubtitleCue> = withContext(Dispatchers.Default) {
        val cues = mutableListOf<SubtitleCue>()
        try {
            val regex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")
            val lines = timedText.lines()
            var i = 0
            while (i < lines.size) {
                val trimmedLine = lines[i].trim()
                if (trimmedLine.isEmpty()) {
                    i++
                    continue
                }
                val match = regex.find(trimmedLine)
                if (match == null) {
                    i++
                    return@while
                }
                val minutes = match.groupValues[1].toLongOrNull() ?: 0
                val seconds = match.groupValues[2].toLongOrNull() ?: 0
                val millis = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
                val startMs = minutes * 60000 + seconds * 1000 + millis
                val text = match.groupValues[4].trim()
                val endMs = if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    val nextMatch = regex.find(nextLine)
                    if (nextMatch != null) {
                        val nextMinutes = nextMatch.groupValues[1].toLongOrNull() ?: 0
                        val nextSeconds = nextMatch.groupValues[2].toLongOrNull() ?: 0
                        val nextMillis = nextMatch.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
                        nextMinutes * 60000 + nextSeconds * 1000 + nextMillis
                    } else {
                        startMs + 3000
                    }
                } else {
                    startMs + 3000
                }
                if (text.isNotEmpty()) {
                    cues.add(SubtitleCue(startMs, endMs, text))
                }
                i++
            }
        } catch (e: Exception) {
            Timber.e(e, "SubtitleHandler: Error parsing timed text content")
        }
        cues
    }

    suspend fun parseContent(content: String, format: String): List<SubtitleCue> = when {
        format.contains("vtt", ignoreCase = true) || content.startsWith("WEBVTT") -> parseVtt(content)
        else -> parseTimedText(content)
    }

    private fun parseTimestamp(timestamp: String): Long? {
        try {
            val parts = timestamp.split(":")
            return when (parts.size) {
                2 -> {
                    val min = parts[0].toLong()
                    val secParts = parts[1].split(".")
                    val sec = secParts[0].toLong()
                    val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0
                    min * 60000 + sec * 1000 + ms
                }
                3 -> {
                    val hours = parts[0].toLong()
                    val min = parts[1].toLong()
                    val secParts = parts[2].split(".")
                    val sec = secParts[0].toLong()
                    val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0
                    hours * 3600000 + min * 60000 + sec * 1000 + ms
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "SubtitleHandler: Error parsing timestamp $timestamp")
            return null
        }
    }

    fun getActiveCue(cues: List<SubtitleCue>, currentPositionMs: Long): SubtitleCue? {
        return cues.firstOrNull { cue ->
            currentPositionMs >= cue.startMs && currentPositionMs < cue.endMs
        }
    }
}