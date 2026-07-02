package com.auramusic.innertube.pages

import com.auramusic.innertube.models.Album
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.Artist
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.auramusic.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.auramusic.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_USER_CHANNEL
import com.auramusic.innertube.models.EpisodeItem
import com.auramusic.innertube.models.MusicCardShelfRenderer
import com.auramusic.innertube.models.MusicResponsiveListItemRenderer
import com.auramusic.innertube.models.MusicTwoRowItemRenderer
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.PodcastItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.YTItem
import com.auramusic.innertube.models.clean
import com.auramusic.innertube.models.filterExplicit
import com.auramusic.innertube.models.filterVideoSongs
import com.auramusic.innertube.models.oddElements
import com.auramusic.innertube.models.splitBySeparator
import com.auramusic.innertube.utils.parseTime
import com.auramusic.innertube.YouTubeConstants

data class SearchSummary(
    val title: String,
    val items: List<YTItem>,
)

data class SearchSummaryPage(
    val summaries: List<SearchSummary>,
) {
    fun filterExplicit(enabled: Boolean) =
        if (enabled) {
            SearchSummaryPage(
                summaries.mapNotNull { s ->
                    SearchSummary(
                        title = s.title,
                        items =
                            s.items.filterExplicit().ifEmpty {
                                return@mapNotNull null
                            },
                    )
                },
            )
        } else {
            this
        }

    fun filterVideoSongs(disableVideos: Boolean) =
        if (disableVideos) {
            SearchSummaryPage(
                summaries.mapNotNull { s ->
                    SearchSummary(
                        title = s.title,
                        items =
                            s.items.filterVideoSongs(true).ifEmpty {
                                return@mapNotNull null
                            },
                    )
                },
            )
        } else {
            this
        }

    companion object {
        fun fromMusicCardShelfRenderer(renderer: MusicCardShelfRenderer): YTItem? {
            val subtitle = renderer.subtitle.runs?.splitBySeparator()
            return when {
                renderer.onTap.watchEndpoint != null -> {
                    SongItem(
                        id = renderer.onTap.watchEndpoint.videoId ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            subtitle?.getOrNull(1)?.oddElements()?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            } ?: return null,
                        album =
                            subtitle.getOrNull(2)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                                Album(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                                )
                            },
                        duration =
                            subtitle
                                .lastOrNull()
                                ?.firstOrNull()
                                ?.text
                                ?.parseTime(),
                        musicVideoType = renderer.onTap.musicVideoType,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit =
                            renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                    )
                }

                renderer.onTap.browseEndpoint?.isArtistEndpoint == true -> {
                    ArtistItem(
                        id = renderer.onTap.browseEndpoint.browseId,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint =
                            renderer.buttons
                                .find { it.buttonRenderer.icon?.iconType == "MUSIC_SHUFFLE" }
                                ?.buttonRenderer
                                ?.command
                                ?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint =
                            renderer.buttons
                                .find { it.buttonRenderer.icon?.iconType == "MIX" }
                                ?.buttonRenderer
                                ?.command
                                ?.watchPlaylistEndpoint ?: return null,
                    )
                }

                renderer.onTap.browseEndpoint?.isAlbumEndpoint == true -> {
                    AlbumItem(
                        browseId = renderer.onTap.browseEndpoint.browseId,
                        playlistId =
                            renderer.buttons
                                .firstOrNull()
                                ?.buttonRenderer
                                ?.command
                                ?.anyWatchEndpoint
                                ?.playlistId ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            subtitle?.getOrNull(1)?.oddElements()?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            } ?: return null,
                        year = null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit =
                            renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                    )
                }

                renderer.onTap.browseEndpoint?.isPlaylistEndpoint == true -> {
                    PlaylistItem(
                        id =
                            renderer.onTap.browseEndpoint.browseId
                                .removePrefix("VL"),
                        title =
                            renderer.header?.musicCardShelfHeaderBasicRenderer?.title?.runs
                                ?.joinToString(separator = "") { it.text }
                                ?: return null,
                        author =
                            Artist(
                                id = null,
                                name = renderer.subtitle.runs?.joinToString { it.text } ?: return null,
                            ),
                        songCountText = null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        playEndpoint =
                            renderer.buttons
                                .find { it.buttonRenderer.icon?.iconType == "PLAY_ARROW" }
                                ?.buttonRenderer
                                ?.command
                                ?.watchPlaylistEndpoint
                                ?: return null,
                        shuffleEndpoint =
                            renderer.buttons
                                .find { it.buttonRenderer.icon?.iconType == "MUSIC_SHUFFLE" }
                                ?.buttonRenderer
                                ?.command
                                ?.watchPlaylistEndpoint
                                ?: return null,
                        radioEndpoint = null,
                    )
                }

                else -> null
            }
        }

        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
            val secondaryLine =
                renderer.flexColumns
                    .getOrNull(1)
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs
                    ?.splitBySeparator()
                    ?: return null
            val thirdLine =
                renderer.flexColumns
                    .getOrNull(2)
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs
                    ?.splitBySeparator()
                    ?: emptyList()
            val listRun = (secondaryLine + thirdLine).clean()
            return when {
                renderer.isSong -> {
                    val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)
                    val watchEndpoint = PageHelper.watchEndpoint(renderer)

                    SongItem(
                        id = PageHelper.videoId(renderer) ?: return null,
                        title =
                            renderer.flexColumns
                                .firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists = listRun.getOrNull(0)?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        } ?: return null,
                        album = listRun.getOrNull(1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                            )
                        },
                        duration =
                            secondaryLine
                                .lastOrNull()
                                ?.firstOrNull()
                                ?.text
                                ?.parseTime(),
                        musicVideoType = renderer.musicVideoType,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit =
                            renderer.badges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                        endpoint = watchEndpoint,
                        libraryAddToken = libraryTokens.addToken,
                        libraryRemoveToken = libraryTokens.removeToken
                    )
                }

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        title =
                            renderer.flexColumns
                                .firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: return null,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                                ?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint =
                            renderer.menu.menuRenderer.items
                                .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                                ?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                    )
                }

                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        playlistId =
                            renderer.overlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?.playlistId
                                ?: return null,
                        title =
                            renderer.flexColumns
                                .firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            secondaryLine.getOrNull(1)?.oddElements()?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            } ?: return null,
                        year =
                            secondaryLine
                                .getOrNull(2)
                                ?.firstOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit =
                            renderer.badges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                    )
                }

                renderer.isPlaylist -> {
                    PlaylistItem(
                        id =
                            renderer.navigationEndpoint
                                ?.browseEndpoint
                                ?.browseId
                                ?.removePrefix("VL") ?: return null,
                        title =
                            renderer.flexColumns
                                .firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        author =
                            secondaryLine.getOrNull(1)?.firstOrNull()?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            } ?: return null,
                        songCountText =
                            renderer.flexColumns
                                .getOrNull(1)
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.lastOrNull()
                                ?.text ?: return null,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        playEndpoint =
                            renderer.overlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                        shuffleEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                                ?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint =
                            renderer.menu.menuRenderer.items
                                .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                                ?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                    )
                }

                renderer.isEpisode -> {
                    val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

                    val firstSubtitle = secondaryLine.getOrNull(0)?.firstOrNull()?.text
                    val isUnfilteredSearch = firstSubtitle == "Episode"
                    val podcastIndex = if (isUnfilteredSearch) 2 else 1

                    EpisodeItem(
                        id = renderer.playlistItemData?.videoId
                            ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                            ?: return null,
                        title =
                            renderer.flexColumns
                                .firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer
                                ?.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        author = null,
                        podcast =
                            secondaryLine.getOrNull(podcastIndex)?.firstOrNull()?.takeIf {
                                it.navigationEndpoint?.browseEndpoint != null
                            }?.let {
                                Album(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                                )
                            },
                        duration =
                            secondaryLine
                                .lastOrNull()
                                ?.firstOrNull()
                                ?.text
                                ?.parseTime(),
                        publishDateText =
                            secondaryLine
                                .getOrNull(if (isUnfilteredSearch) 1 else 0)
                                ?.firstOrNull()
                                ?.text,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit =
                            renderer.badges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                        endpoint = renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.watchEndpoint,
                        libraryAddToken = libraryTokens.addToken,
                        libraryRemoveToken = libraryTokens.removeToken,
                    )
                }

                else -> null
            }
        }

        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isSong -> {
                    val subtitleRuns = renderer.subtitle?.runs?.oddElements() ?: return null
                    val watchEndpoint = PageHelper.watchEndpoint(renderer)
                    SongItem(
                        id = watchEndpoint?.videoId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = subtitleRuns.filter { run ->
                            run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true ||
                            (run.navigationEndpoint?.browseEndpoint != null &&
                             run.navigationEndpoint.browseEndpoint.browseId.startsWith("MPREb_") != true)
                        }.map { run ->
                            Artist(
                                name = run.text,
                                id = run.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        }.ifEmpty {
                            subtitleRuns.firstOrNull()?.let { run ->
                                listOf(Artist(name = run.text, id = null))
                            } ?: emptyList()
                        },
                        album = subtitleRuns.firstOrNull {
                            it.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPREb_") == true
                        }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                            )
                        },
                        duration = null,
                        musicVideoType = renderer.musicVideoType,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                            ?: return null,
                        explicit = renderer.subtitleBadges?.any {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } == true,
                        endpoint = watchEndpoint
                    )
                }

                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        },
                        year = null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                            ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }

                renderer.isPlaylist -> {
                    PlaylistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL")
                            ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        author = Artist(
                            name = renderer.subtitle?.runs?.firstOrNull()?.text ?: return null,
                            id = null
                        ),
                        songCountText = null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                            ?: return null,
                        playEndpoint = renderer.thumbnailOverlay
                            ?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                            ?: return null,
                        radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                            ?: return null,
                    )
                }

                else -> null
            }
        }

        fun groupItemsByType(items: List<YTItem>): List<SearchSummary> {
            val grouped =
                items.groupBy { item ->
                    when (item) {
                        is EpisodeItem -> YouTubeConstants.SECTION_EPISODES
                        is PodcastItem -> YouTubeConstants.SECTION_PODCASTS
                        is AlbumItem -> YouTubeConstants.SECTION_ALBUMS
                        is ArtistItem -> if (item.isProfile) YouTubeConstants.SECTION_PROFILES else YouTubeConstants.SECTION_ARTISTS
                        is PlaylistItem -> YouTubeConstants.SECTION_PLAYLISTS
                        is SongItem -> when {
                            item.isEpisode -> YouTubeConstants.SECTION_EPISODES
                            item.isVideoSong -> YouTubeConstants.SECTION_VIDEOS
                            else -> YouTubeConstants.SECTION_SONGS
                        }
                    }
                }

            val sectionOrder =
                listOf(
                    YouTubeConstants.SECTION_SONGS, YouTubeConstants.SECTION_VIDEOS, YouTubeConstants.SECTION_ALBUMS, YouTubeConstants.SECTION_ARTISTS, YouTubeConstants.SECTION_PLAYLISTS,
                    YouTubeConstants.SECTION_PODCASTS, YouTubeConstants.SECTION_EPISODES, YouTubeConstants.SECTION_PROFILES, YouTubeConstants.DEFAULT_OTHER_RESULTS,
                )

            return sectionOrder.mapNotNull { sectionName ->
                grouped[sectionName]?.takeIf { it.isNotEmpty() }?.let { groupItems ->
                    SearchSummary(title = sectionName, items = groupItems)
                }
            }
        }
    }
}