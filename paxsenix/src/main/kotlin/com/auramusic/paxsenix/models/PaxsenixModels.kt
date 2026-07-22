package com.auramusic.paxsenix.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val id: String = "",
    val songName: String = "",
    val trackName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val duration: Int? = null,
    val artwork: String? = null,
) {
    val displayName: String get() = trackName.ifBlank { songName }
    val displayArtist: String get() = artistName
}

@Serializable
data class LyricsResponse(
    val type: String? = null,
    val metadata: LyricsMetadata? = null,
    val content: List<LyricsContent> = emptyList(),
    val elrc: String? = null,
    val elrcMultiPerson: String? = null,
    val ttmlContent: String? = null,
    val plain: String? = null,
)

@Serializable
data class LyricsMetadata(
    val artistName: String? = null,
    val trackName: String? = null,
    val albumName: String? = null,
)

@Serializable
data class LyricsContent(
    val timestamp: Double? = null,
    val endtime: Double? = null,
    val duration: Double? = null,
    val structure: String? = null,
    val text: List<LyricText> = emptyList(),
    val background: Boolean = false,
    val oppositeTurn: Boolean = false,
)

@Serializable
data class LyricText(
    val text: String = "",
    val timestamp: Double? = null,
    val endtime: Double? = null,
    val duration: Double? = null,
    val part: Int? = null,
)

@Serializable
data class AppleMusicSearchResponse(
    val results: AppleMusicSearchResults = AppleMusicSearchResults(),
    val resources: AppleMusicResources? = null,
)

@Serializable
data class AppleMusicSearchResults(
    val songs: AppleMusicSongResults? = null,
)

@Serializable
data class AppleMusicSongResults(
    val data: List<AppleMusicSongRef> = emptyList(),
)

@Serializable
data class AppleMusicSongRef(
    val id: String = "",
)

@Serializable
data class AppleMusicResources(
    val songs: Map<String, AppleMusicSongDetail>? = null,
)

@Serializable
data class AppleMusicSongDetail(
    val attributes: AppleMusicSongAttributes = AppleMusicSongAttributes(),
)

@Serializable
data class AppleMusicSongAttributes(
    val name: String = "",
    val artistName: String = "",
    @SerialName("albumName")
    val albumName: String = "",
    @SerialName("durationInMillis")
    val durationInMillis: Long? = null,
    val artwork: AppleMusicArtwork? = null,
)

@Serializable
data class AppleMusicArtwork(
    val url: String = "",
    val width: Int? = null,
    val height: Int? = null,
)
