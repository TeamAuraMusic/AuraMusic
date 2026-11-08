package com.auramusic.app.tv

import android.content.Context
import android.net.Uri
import androidx.tvprovider.media.tv.TvContractCompat

/** Program metadata used to scope the overlay and restore artwork when Projectivy's video surface is blank. */
internal object TvPreviewFocusTargets {
    data class Program(val title: String, val artwork: Uri?, val channelNames: Set<String>)

    fun isInChannelRow(expectedChannelNames: Set<String>, ancestorDescriptions: Iterable<String?>): Boolean =
        expectedChannelNames.isNotEmpty() &&
            ancestorDescriptions.any { it?.trim() in expectedChannelNames }

    fun unambiguousArtworkUri(candidates: List<String?>): String? = candidates.distinct().singleOrNull()

    fun publishedPrograms(context: Context): Map<String, Program> {
        val resolver = context.contentResolver
        val channels = buildList {
            resolver.query(
                TvContractCompat.Channels.CONTENT_URI,
                arrayOf(
                    TvContractCompat.Channels._ID,
                    TvContractCompat.Channels.COLUMN_PACKAGE_NAME,
                    TvContractCompat.Channels.COLUMN_DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(TvContractCompat.Channels._ID)
                val packageColumn = cursor.getColumnIndexOrThrow(TvContractCompat.Channels.COLUMN_PACKAGE_NAME)
                val nameColumn = cursor.getColumnIndexOrThrow(TvContractCompat.Channels.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    if (cursor.getString(packageColumn) == context.packageName) {
                        val name = cursor.getString(nameColumn)?.takeIf(String::isNotBlank) ?: continue
                        add(cursor.getLong(idColumn) to name)
                    }
                }
            }
        }
        val programsByTitle = linkedMapOf<String, MutableList<Program>>()
        for ((channelId, channelName) in channels) {
            resolver.query(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                arrayOf(TITLE_COLUMN, POSTER_ART_URI_COLUMN),
                null,
                null,
                null,
            )?.use { cursor ->
                val titleColumn = cursor.getColumnIndexOrThrow(TITLE_COLUMN)
                val artworkColumn = cursor.getColumnIndex(POSTER_ART_URI_COLUMN)
                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleColumn)?.takeIf(String::isNotBlank) ?: continue
                    val artwork = artworkColumn.takeIf { it >= 0 }
                        ?.let(cursor::getString)
                        ?.takeIf(String::isNotBlank)
                        ?.let(Uri::parse)
                    programsByTitle.getOrPut(title, ::mutableListOf) +=
                        Program(title, artwork, setOf(channelName))
                }
            }
        }
        return programsByTitle.mapValues { (title, programs) ->
            val artwork = unambiguousArtworkUri(programs.map { it.artwork?.toString() })
                ?.let(Uri::parse)
            Program(title, artwork, programs.flatMapTo(linkedSetOf()) { it.channelNames })
        }
    }

    private const val TITLE_COLUMN = "title"
    private const val POSTER_ART_URI_COLUMN = "poster_art_uri"
}
