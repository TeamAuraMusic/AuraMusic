package com.auramusic.innertube.pages

import com.auramusic.innertube.models.Album
import com.auramusic.innertube.models.Artist
import com.auramusic.innertube.models.MusicResponsiveListItemRenderer
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.oddElements
import com.auramusic.innertube.utils.parseTime

data class PlaylistPage(
    val playlist: PlaylistItem,
    val songs: List<SongItem>,
    val songsContinuation: String?,
    val continuation: String?,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            // Extract library tokens using the new method that properly handles multiple toggle items
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)
            val watchEndpoint = PageHelper.watchEndpoint(renderer)

            return SongItem(
                id = PageHelper.videoId(renderer) ?: return null,
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()?.text ?: return null,
                artists = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                }.orEmpty(),
                album = renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                    )
                },
                duration = renderer.fixedColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text?.parseTime(),
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = watchEndpoint,
                // A playlist row is still a playable song without its playlist-item
                // id. Newer responses often omit playlistSetVideoId; treating it as
                // mandatory dropped every row and made the whole playlist look empty.
                // Callers that need it (remove/reorder) already check for null.
                setVideoId = PageHelper.playlistSetVideoId(renderer),
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken
            )
        }
    }
}
