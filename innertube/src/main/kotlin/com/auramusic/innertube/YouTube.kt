package com.auramusic.innertube

import com.auramusic.innertube.models.AccountInfo
import com.auramusic.innertube.models.YTItem
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.Artist
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.BrowseEndpoint
import com.auramusic.innertube.models.EpisodeItem
import com.auramusic.innertube.models.GridRenderer
import com.auramusic.innertube.models.MediaInfo
import com.auramusic.innertube.models.MusicResponsiveListItemRenderer
import com.auramusic.innertube.models.MusicTwoRowItemRenderer
import com.auramusic.innertube.models.MusicCarouselShelfRenderer
import com.auramusic.innertube.models.MusicShelfRenderer
import com.auramusic.innertube.models.SectionListRenderer
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.PodcastItem
import com.auramusic.innertube.models.SearchSuggestions
import com.auramusic.innertube.models.Run
import com.auramusic.innertube.models.Runs
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.auramusic.innertube.models.YouTubeClient
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.auramusic.innertube.models.YouTubeClient.Companion.MOBILE
import com.auramusic.innertube.models.YouTubeLocale
import com.auramusic.innertube.models.getContinuation
import com.auramusic.innertube.models.getItems
import com.auramusic.innertube.models.oddElements
import com.auramusic.innertube.models.response.AccountMenuResponse
import com.auramusic.innertube.models.response.BrowseResponse
import com.auramusic.innertube.models.response.CreatePlaylistResponse
import com.auramusic.innertube.models.response.EditPlaylistResponse
import com.auramusic.innertube.models.response.FeedbackResponse
import com.auramusic.innertube.models.response.GetQueueResponse
import com.auramusic.innertube.models.response.GetSearchSuggestionsResponse
import com.auramusic.innertube.models.response.GetTranscriptResponse
import com.auramusic.innertube.models.response.ImageUploadResponse
import com.auramusic.innertube.models.response.NextResponse
import com.auramusic.innertube.models.response.PlayerResponse
import com.auramusic.innertube.models.response.SearchResponse
import com.auramusic.innertube.pages.AlbumPage
import com.auramusic.innertube.pages.ArtistItemsContinuationPage
import com.auramusic.innertube.pages.ArtistItemsPage
import com.auramusic.innertube.pages.ArtistPage
import com.auramusic.innertube.pages.ChartsPage
import com.auramusic.innertube.pages.BrowseResult
import com.auramusic.innertube.pages.ExplorePage
import com.auramusic.innertube.pages.HistoryPage
import com.auramusic.innertube.pages.HomePage
import com.auramusic.innertube.pages.LibraryContinuationPage
import com.auramusic.innertube.pages.LibraryPage
import com.auramusic.innertube.pages.MoodAndGenres
import com.auramusic.innertube.pages.NewReleaseAlbumPage
import com.auramusic.innertube.pages.NextPage
import com.auramusic.innertube.pages.NextResult
import com.auramusic.innertube.pages.PlaylistContinuationPage
import com.auramusic.innertube.pages.PlaylistPage
import com.auramusic.innertube.pages.RelatedPage
import com.auramusic.innertube.pages.SearchPage
import com.auramusic.innertube.pages.SearchResult
import com.auramusic.innertube.pages.SearchSuggestionPage
import com.auramusic.innertube.pages.SearchSummary
import com.auramusic.innertube.pages.SearchSummaryPage
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.Proxy
import kotlin.random.Random

/**
 * Parse useful data with [InnerTube] sending requests.
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
object YouTube {
    private val innerTube = InnerTube()

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) {
            innerTube.locale = value
        }
    var visitorData: String?
        get() = innerTube.visitorData
        set(value) {
            innerTube.visitorData = value
        }
    var dataSyncId: String?
        get() = innerTube.dataSyncId
        set(value) {
            innerTube.dataSyncId = value
        }
    var cookie: String?
        get() = innerTube.cookie
        set(value) {
            innerTube.cookie = value
        }
    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) {
            innerTube.proxy = value
        }

    var proxyAuth: String?
        get() = innerTube.proxyAuth
        set(value) {
            innerTube.proxyAuth = value
        }
    var useLoginForBrowse: Boolean
        get() = innerTube.useLoginForBrowse
        set(value) {
            innerTube.useLoginForBrowse = value
        }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> = runCatching {
        val response = innerTube.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
        SearchSuggestions(
            queries = response.contents?.getOrNull(0)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull { content ->
                content.searchSuggestionRenderer?.suggestion?.runs?.joinToString(separator = "") { it.text }
            }.orEmpty(),
            recommendedItems = response.contents?.getOrNull(1)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull {
                it.musicResponsiveListItemRenderer?.let { renderer ->
                    SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                }
            }.orEmpty()
        )
    }

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> = runCatching {
        val response = innerTube.search(WEB_REMIX, query).body<SearchResponse>()
        SearchSummaryPage(
            summaries = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { it ->
                if (it.musicCardShelfRenderer != null)
                    SearchSummary(
                        title = it.musicCardShelfRenderer.header?.musicCardShelfHeaderBasicRenderer?.title?.runs?.firstOrNull()?.text ?: YouTubeConstants.DEFAULT_TOP_RESULT,
                        items = listOfNotNull(SearchSummaryPage.fromMusicCardShelfRenderer(it.musicCardShelfRenderer))
                            .plus(
                                it.musicCardShelfRenderer.contents
                                    ?.mapNotNull { it.musicResponsiveListItemRenderer }
                                    ?.mapNotNull(SearchSummaryPage.Companion::fromMusicResponsiveListItemRenderer)
                                    .orEmpty()
                            )
                            .distinctBy { it.id }
                            .ifEmpty { null } ?: return@mapNotNull null
                    )
                else
                    SearchSummary(
                        title = it.musicShelfRenderer?.title?.runs?.firstOrNull()?.text ?: YouTubeConstants.DEFAULT_OTHER_RESULTS,
                        items = it.musicShelfRenderer?.contents?.getItems()
                            ?.mapNotNull {
                                SearchSummaryPage.fromMusicResponsiveListItemRenderer(it)
                            }
                            ?.distinctBy { it.id }
                            ?.ifEmpty { null } ?: return@mapNotNull null
                    )
            }!!
        )
    }

    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> = runCatching {
        val response = innerTube.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
        SearchResult(
            items = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.contents?.getItems()?.mapNotNull {
                    SearchPage.toYTItem(it)
                }.orEmpty(),
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.continuations?.getContinuation()
        )
    }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> = runCatching {
        val response = innerTube.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
        val items = response.continuationContents?.musicShelfContinuation?.contents
            ?.mapNotNull {
                SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
            } ?: emptyList()
        SearchResult(
            items = items,
            continuation = if (items.isEmpty()) null else response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation()
        )
    }

    suspend fun album(browseId: String, withSongs: Boolean = true): Result<AlbumPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        if (browseId.contains("FEmusic_library_privately_owned_release_detail")) {
            val playlistId =
                response.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons?.firstOrNull()?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.playlistId!!
            val albumItem = AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = response.header.musicDetailHeaderRenderer.title.runs?.firstOrNull()?.text!!,
                artists = response.header.musicDetailHeaderRenderer.subtitle.runs?.filter { it.navigationEndpoint != null }?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                },
                year = response.header.musicDetailHeaderRenderer.subtitle.runs?.lastOrNull()?.text?.toIntOrNull(),
                thumbnail = response.header.musicDetailHeaderRenderer.thumbnail.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()!!.url,
                explicit = false, // TODO: Extract explicit badge for albums from YouTube response
            )
            return@runCatching AlbumPage(
                album = albumItem,
                songs = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.getItems()?.mapNotNull {
                    AlbumPage.getSong(it, albumItem)
                }!!.toMutableList(),
                otherVersions = emptyList()
            )
        } else {
            val playlistId =
                response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')!!
            val albumItem = AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                artists = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.straplineTextOne?.runs?.oddElements()
                    ?.map {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                        )
                    }!!,
                year = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                thumbnail = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url!!,
                explicit = false, // TODO: Extract explicit badge for albums from YouTube response
            )
            return@runCatching AlbumPage(
                album = albumItem,
                songs = if (withSongs) albumSongs(
                    playlistId, albumItem
                ).getOrThrow() else emptyList(),
                otherVersions = response.contents.twoColumnBrowseResultsRenderer.secondaryContents?.sectionListRenderer?.contents?.getOrNull(
                    1
                )?.musicCarouselShelfRenderer?.contents
                    ?.mapNotNull { it.musicTwoRowItemRenderer }
                    ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                    .orEmpty()
            )
        }
    }

    suspend fun albumSongs(playlistId: String, album: AlbumItem? = null): Result<List<SongItem>> = runCatching {
        var response = innerTube.browse(WEB_REMIX, "VL$playlistId").body<BrowseResponse>()
        val songs = response.contents?.twoColumnBrowseResultsRenderer
            ?.secondaryContents?.sectionListRenderer
            ?.contents?.firstOrNull()
            ?.musicPlaylistShelfRenderer?.contents?.getItems()
            ?.mapNotNull {
                AlbumPage.getSong(it, album)
            }!!
            .toMutableList()
        var continuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
            .contents.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.getContinuation()
        val seenContinuations = mutableSetOf<String>()
        var requestCount = 0
        val maxRequests = 50 // Prevent excessive API calls
        
        while (continuation != null && requestCount < maxRequests) {
            // Prevent infinite loops by tracking seen continuations
            if (continuation in seenContinuations) {
                break
            }
            seenContinuations.add(continuation)
            requestCount++
            
            response = innerTube.browse(
                client = WEB_REMIX,
                continuation = continuation,
            ).body<BrowseResponse>()
            songs += response.onResponseReceivedActions?.firstOrNull()?.appendContinuationItemsAction?.continuationItems?.getItems()?.mapNotNull {
                AlbumPage.getSong(it, album)
            }.orEmpty()
            continuation = response.continuationContents?.musicPlaylistShelfContinuation?.continuations?.getContinuation()
        }
        songs
    }

    suspend fun artist(browseId: String): Result<ArtistPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()

        fun mapRuns(runs: List<Run>?): List<Run>? = runs?.map { run ->
            Run(
                text = run.text,
                navigationEndpoint = run.navigationEndpoint
            )
        }

        val descriptionRuns = response.contents?.sectionListRenderer?.contents
            ?.firstOrNull { it.musicDescriptionShelfRenderer != null }
            ?.musicDescriptionShelfRenderer?.description?.runs
            ?.let(::mapRuns)
            ?: response.header?.musicImmersiveHeaderRenderer?.description?.runs?.let(::mapRuns)

        ArtistPage(
            artist = ArtistItem(
                id = browseId,
                title = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                thumbnail = response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                channelId = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.channelId,
                playEndpoint = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer
                    ?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.overlay?.musicItemThumbnailOverlayRenderer
                    ?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                shuffleEndpoint = response.header?.musicImmersiveHeaderRenderer?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint
                    ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
                        ?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                radioEndpoint = response.header?.musicImmersiveHeaderRenderer?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint
            ),
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
            description = descriptionRuns?.joinToString(separator = "") { it.text },
                subscriberCountText = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton2
                    ?.subscribeButtonRenderer?.subscriberCountWithSubscribeText?.runs?.firstOrNull()?.text
                    ?: response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer
                        ?.longSubscriberCountText?.runs?.firstOrNull()?.text
                    ?: response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer
                        ?.shortSubscriberCountText?.runs?.firstOrNull()?.text,
            monthlyListenerCount = response.header?.musicImmersiveHeaderRenderer?.monthlyListenerCount?.runs?.firstOrNull()?.text,
            descriptionRuns = descriptionRuns
        )
    }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        val sectionContent = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
        
        val gridRenderer = sectionContent?.gridRenderer
        val musicCarouselShelfRenderer = sectionContent?.musicCarouselShelfRenderer
        val musicPlaylistShelfRenderer = sectionContent?.musicPlaylistShelfRenderer
        val musicShelfRenderer = sectionContent?.musicShelfRenderer
        
        when {
            gridRenderer != null -> {
                ArtistItemsPage(
                    title = gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    items = gridRenderer.items.mapNotNull {
                        it.musicTwoRowItemRenderer?.let { renderer ->
                            ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                        }
                    },
                    continuation = gridRenderer.continuations?.getContinuation()
                )
            }
            musicCarouselShelfRenderer != null -> {
                ArtistItemsPage(
                    title = musicCarouselShelfRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                    items = musicCarouselShelfRenderer.contents.mapNotNull { content ->
                        content.musicTwoRowItemRenderer?.let { renderer ->
                            ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                        } ?: content.musicResponsiveListItemRenderer?.let { renderer ->
                            ArtistItemsPage.fromMusicResponsiveListItemRenderer(renderer)
                        }
                    },
                    continuation = null
                )
            }
            musicShelfRenderer != null -> {
                ArtistItemsPage(
                    title = musicShelfRenderer.title?.runs?.firstOrNull()?.text 
                        ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text 
                        ?: "",
                    items = musicShelfRenderer.contents?.getItems()?.mapNotNull {
                        ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                    } ?: emptyList(),
                    continuation = musicShelfRenderer.continuations?.getContinuation()
                )
            }
            else -> {
                ArtistItemsPage(
                    title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: "",
                    items = musicPlaylistShelfRenderer?.contents?.getItems()?.mapNotNull {
                        ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                    } ?: emptyList(),
                    continuation = musicPlaylistShelfRenderer?.contents?.getContinuation()
                )
            }
        }
    }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()

        when {
            response.continuationContents?.gridContinuation != null -> {
                val gridContinuation = response.continuationContents.gridContinuation
                val items = gridContinuation.items.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { renderer ->
                        ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                    }
                }
                ArtistItemsContinuationPage(
                    items = items,
                    continuation = if (items.isEmpty()) null else gridContinuation.continuations?.getContinuation()
                )
            }

            response.continuationContents?.musicPlaylistShelfContinuation != null -> {
                val musicPlaylistShelfContinuation = response.continuationContents.musicPlaylistShelfContinuation
                val items = musicPlaylistShelfContinuation.contents.getItems().mapNotNull {
                    ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                }
                ArtistItemsContinuationPage(
                    items = items,
                    continuation = if (items.isEmpty()) null else musicPlaylistShelfContinuation.continuations?.getContinuation()
                )
            }

            else -> {
                val continuationItems = response.onResponseReceivedActions?.firstOrNull()
                    ?.appendContinuationItemsAction?.continuationItems
                val items = continuationItems?.getItems()?.mapNotNull {
                    ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                } ?: emptyList()
                ArtistItemsContinuationPage(
                    items = items,
                    continuation = if (items.isEmpty()) null else continuationItems?.getContinuation()
                )
            }
        }
    }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "VL$playlistId",
            setLogin = true
        ).body<BrowseResponse>()
        val base = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
        val header = base?.musicResponsiveHeaderRenderer ?: base?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer

        val editable = base?.musicEditablePlaylistDetailHeaderRenderer != null

        PlaylistPage(
            playlist = PlaylistItem(
                id = playlistId,
                title = header?.title?.runs?.firstOrNull()?.text!!,
                author = header.straplineTextOne?.runs?.firstOrNull()?.let {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                },
                songCountText = header.secondSubtitle?.runs?.firstOrNull()?.text,
                thumbnail = header.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url!!,
                playEndpoint = null,
                shuffleEndpoint = header.buttons.lastOrNull()?.menuRenderer?.items?.firstOrNull()?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,
                radioEndpoint = header.buttons.getOrNull(2)?.menuRenderer?.items?.find {
                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                isEditable = editable
            ),
            songs = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.getItems()?.mapNotNull {
                    PlaylistPage.fromMusicResponsiveListItemRenderer(it)
                } ?: emptyList(),
            songsContinuation = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.getContinuation(),
            continuation = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.continuations?.getContinuation()
        )
    }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            continuation = continuation,
            browseId = "",
            setLogin = true
        ).body<BrowseResponse>()

        val mainContents: List<MusicShelfRenderer.Content> = response.continuationContents?.sectionListContinuation?.contents
            ?.mapNotNull { content: SectionListRenderer.Content -> content.musicPlaylistShelfRenderer?.contents }
            ?.flatten()
            ?: emptyList()

        val appendedContents: List<MusicShelfRenderer.Content> = response.onResponseReceivedActions
            ?.firstOrNull()
            ?.appendContinuationItemsAction
            ?.continuationItems
            .orEmpty()

        val allContents = mainContents + appendedContents

        val songs = allContents
            .mapNotNull { content: MusicShelfRenderer.Content -> content.musicResponsiveListItemRenderer }
            .mapNotNull { renderer -> PlaylistPage.fromMusicResponsiveListItemRenderer(renderer) }

        val nextContinuation = if (songs.isEmpty()) null else {
            response.continuationContents
                ?.sectionListContinuation
                ?.continuations
                ?.getContinuation()
                ?: response.continuationContents
                    ?.musicPlaylistShelfContinuation
                    ?.continuations
                    ?.getContinuation()
                ?: response.continuationContents
                    ?.musicShelfContinuation
                    ?.continuations
                    ?.getContinuation()
                ?: response.onResponseReceivedActions
                    ?.firstOrNull()
                    ?.appendContinuationItemsAction
                    ?.continuationItems
                    ?.getContinuation()
        }

        PlaylistContinuationPage(
            songs = songs,
            continuation = nextContinuation
        )
    }

    suspend fun home(continuation: String? = null, params: String? = null): Result<HomePage> = runCatching {
        if (continuation != null) {
            return@runCatching homeContinuation(continuation).getOrThrow()
        }

        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_home", params = params).body<BrowseResponse>()
        val continuation = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.continuations?.getContinuation()
        val sectionListRender = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer
        val sections = sectionListRender?.contents!!
            .mapNotNull { it.musicCarouselShelfRenderer }
            .mapNotNull {
                HomePage.Section.fromMusicCarouselShelfRenderer(it)
            }.toMutableList()
        val chips = sectionListRender.header?.chipCloudRenderer?.chips?.mapNotNull { HomePage.Chip.fromChipCloudChipRenderer(it) }
        HomePage(chips, sections, continuation)
    }

    private suspend fun homeContinuation(continuation: String): Result<HomePage> = runCatching {
        val response =
            innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
        val continuation =
            response.continuationContents?.sectionListContinuation?.continuations?.getContinuation()
        HomePage(
            null,
            response.continuationContents?.sectionListContinuation?.contents
            ?.mapNotNull { it.musicCarouselShelfRenderer }
            ?.mapNotNull {
                HomePage.Section.fromMusicCarouselShelfRenderer(it)
            }.orEmpty(), continuation
        )
    }

    suspend fun explore(): Result<ExplorePage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_explore").body<BrowseResponse>()
        val sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        val knownBrowseIds = setOf("FEmusic_new_releases_albums", "FEmusic_moods_and_genres")

        // Extract remaining carousel sections as generic sections (podcasts, mixes, etc.)
        val otherSections = sections?.filter { content ->
            val browseId = content.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId
            content.musicCarouselShelfRenderer != null && browseId !in knownBrowseIds
        }?.mapNotNull { content ->
            content.musicCarouselShelfRenderer?.let { renderer ->
                val title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return@let null
                val items = renderer.contents.mapNotNull { item ->
                    item.musicTwoRowItemRenderer?.let { RelatedPage.Companion.fromMusicTwoRowItemRenderer(it) }
                }
                if (items.isEmpty()) null else title to items
            }
        }.orEmpty()

        ExplorePage(
            newReleaseAlbums = sections?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_new_releases_albums"
            }?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer).orEmpty(),
            moodAndGenres = sections?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_moods_and_genres"
            }?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicNavigationButtonRenderer }
                ?.mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer)
                .orEmpty(),
            podcasts = otherSections.filter { it.first.contains("podcast", ignoreCase = true) }
                .map { PodcastsPage.PodcastSection(it.first, it.second) },
            mixes = otherSections.filter { it.first.contains("mix", ignoreCase = true) || it.first.contains("playlist", ignoreCase = true) }
                .map { MixesPage.MixSection(it.first, it.second) },
        )
    }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_new_releases_albums").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
            ?.mapNotNull { it.musicTwoRowItemRenderer }
            ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
            .orEmpty()
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents!!
            .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
    }

    suspend fun podcasts(): Result<PodcastsPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_podcasts").body<BrowseResponse>()
        PodcastsPage(
            featured = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
                content.musicCarouselShelfRenderer?.let { renderer ->
                    val title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return@let null
                    val items = renderer.contents.mapNotNull { item ->
                        item.musicTwoRowItemRenderer?.let { RelatedPage.Companion.fromMusicTwoRowItemRenderer(it) }
                    }.filterNotNull()
                    if (items.isEmpty()) null else PodcastsPage.PodcastSection(title, items)
                }
            }?.filterNotNull()?.orEmpty() ?: emptyList()
        )
    }

    suspend fun mixes(): Result<MixesPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_mixes").body<BrowseResponse>()
        MixesPage(
            mixes = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
                content.musicCarouselShelfRenderer?.let { renderer ->
                    renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text?.let { title ->
                        val items = renderer.contents.mapNotNull { item ->
                            item.musicTwoRowItemRenderer?.let { RelatedPage.Companion.fromMusicTwoRowItemRenderer(it) }
                        }.filterNotNull()
                        if (items.isEmpty()) null else MixesPage.MixSection(title, items)
                    }
                }
            }?.filterNotNull()?.orEmpty() ?: emptyList()
        )
    }

    suspend fun browse(browseId: String, params: String?): Result<BrowseResult> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = browseId, params = params).body<BrowseResponse>()
        BrowseResult(
            title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
            items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
                when {
                    content.gridRenderer != null -> {
                        BrowseResult.Item(
                            title = content.gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                            items = content.gridRenderer.items
                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                        )
                    }

                    content.musicCarouselShelfRenderer != null -> {
                        BrowseResult.Item(
                            title = content.musicCarouselShelfRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                            items = content.musicCarouselShelfRenderer.contents
                                .mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                        )
                    }

                    else -> null
                }
            }.orEmpty()
        )
    }

    suspend fun library(browseId: String, tabIndex: Int = 0) = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = browseId,
            setLogin = true
        ).body<BrowseResponse>()

        val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs

        val contents = if (tabs != null && tabs.size >= tabIndex) {
            tabs[tabIndex].tabRenderer.content?.sectionListRenderer?.contents?.firstOrNull()
        }
        else {
            null
        }

        when {
            contents?.gridRenderer != null -> {
                LibraryPage(
                    items = contents.gridRenderer.items
                        .mapNotNull (GridRenderer.Item::musicTwoRowItemRenderer)
                        .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) },
                    continuation = contents.gridRenderer.continuations?.getContinuation()
                )
            }

            else -> { // contents?.musicShelfRenderer != null
                LibraryPage(
                    items = contents?.musicShelfRenderer?.contents!!
                        .mapNotNull (MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                        .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) },
                    continuation = contents.musicShelfRenderer.continuations?.getContinuation()
                )
            }
        }
    }

    suspend fun libraryContinuation(continuation: String) = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()

        val contents = response.continuationContents

        when {
            contents?.gridContinuation != null -> {
                LibraryContinuationPage(
                    items = contents.gridContinuation.items
                        .mapNotNull (GridRenderer.Item::musicTwoRowItemRenderer)
                        .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) },
                    continuation = contents.gridContinuation.continuations?.getContinuation()
                )
            }

            else -> { // contents?.musicShelfContinuation != null
                LibraryContinuationPage(
                    items = contents?.musicShelfContinuation?.contents!!
                        .mapNotNull (MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                        .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) },
                    continuation = contents.musicShelfContinuation.continuations?.getContinuation()
                )
            }
        }
    }

    suspend fun libraryRecentActivity(): Result<LibraryPage> = runCatching {
        val continuation = LibraryFilter.FILTER_RECENT_ACTIVITY.value

        val response = innerTube.browse(
            client = WEB_REMIX,
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()

        val gridItems = response.continuationContents?.sectionListContinuation?.contents?.firstOrNull()
            ?.gridRenderer?.items
        
        if (gridItems == null) {
            return@runCatching LibraryPage(
                items = emptyList(),
                continuation = null
            )
        }
        
        val items = gridItems.mapNotNull {
            it.musicTwoRowItemRenderer?.let { renderer ->
                LibraryPage.fromMusicTwoRowItemRenderer(renderer)
            }
        }.toMutableList()

        /*
         * We need to fetch the artist page when accessing the library because it allows to have
         * a proper playEndpoint, which is needed to correctly report the playing indicator in
         * the home page.
         *
         * Despite this, we need to use the old thumbnail because it's the proper format for a
         * square picture, which is what we need.
         */
        items.forEachIndexed { index, item ->
            if (item is ArtistItem) {
                artist(item.id).getOrNull()?.artist?.let { fetchedArtist ->
                    items[index] = fetchedArtist.copy(thumbnail = item.thumbnail)
                }
            }
        }

        LibraryPage(
            items = items,
            continuation = null
        )
    }

    suspend fun getChartsPage(continuation: String? = null): Result<ChartsPage> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_charts",
            params = "ggMGCgQIgAQ%3D",
            continuation = continuation
        ).body<BrowseResponse>()

        val sections = mutableListOf<ChartsPage.ChartSection>()

        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.forEach { content ->

                content.musicCarouselShelfRenderer?.let { renderer ->
                    val title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: return@forEach

                    val items = renderer.contents.mapNotNull { item ->
                        when {
                            item.musicResponsiveListItemRenderer != null ->
                                RelatedPage.fromMusicResponsiveListItemRenderer(item.musicResponsiveListItemRenderer)
                            item.musicTwoRowItemRenderer != null ->
                                RelatedPage.fromMusicTwoRowItemRenderer(item.musicTwoRowItemRenderer)
                            else -> null
                        }
                    }.filterNotNull()

                    if (items.isNotEmpty()) {
                        sections.add(ChartsPage.ChartSection(title, items, ChartsPage.ChartType.GENRE))
                    }
                }
            }

        ChartsPage(sections, response.continuationContents?.sectionListContinuation?.continuations?.getContinuation())
    }

    suspend fun getTop100Charts(countryCode: String = "US"): Result<Top100ChartsPage> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_charts",
            params = "ggMGCgQIbRibJGAkQChAKEAcQBDA%3D",
            continuation = null
        ).body<BrowseResponse>()

        val topItems = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.musicCarouselShelfRenderer?.contents?.mapNotNull { item ->
                when {
                    item.musicResponsiveListItemRenderer != null ->
                        RelatedPage.fromMusicResponsiveListItemRenderer(item.musicResponsiveListItemRenderer)
                    item.musicTwoRowItemRenderer != null ->
                        RelatedPage.fromMusicTwoRowItemRenderer(item.musicTwoRowItemRenderer)
                    else -> null
                }
            }?.filterNotNull()?.take(100)?.orEmpty() ?: emptyList()

        Top100ChartsPage(countryCode, topItems)
    }

    suspend fun musicHistory() = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_history",
            setLogin = true
        ).body<BrowseResponse>()

        HistoryPage(
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull {
                    it.musicShelfRenderer?.let { musicShelfRenderer ->
                        HistoryPage.fromMusicShelfRenderer(musicShelfRenderer)
                    }
                }
        )
    }

    suspend fun likeVideo(videoId: String, like: Boolean) = runCatching {
        if (like)
            innerTube.likeVideo(WEB_REMIX, videoId)
        else
            innerTube.unlikeVideo(WEB_REMIX, videoId)
    }

    suspend fun likePlaylist(playlistId: String, like: Boolean) = runCatching {
        if (like)
            innerTube.likePlaylist(WEB_REMIX, playlistId)
        else
            innerTube.unlikePlaylist(WEB_REMIX, playlistId)
    }

    suspend fun subscribeChannel(channelId: String, subscribe: Boolean) = runCatching {
        if (subscribe)
            innerTube.subscribeChannel(WEB_REMIX, channelId)
        else
            innerTube.unsubscribeChannel(WEB_REMIX, channelId)
    }

    suspend fun getChannelId(browseId: String): String {
        artist(browseId).onSuccess {
            return it.artist.channelId ?: ""
        }
        return ""
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String) = runCatching {
        innerTube.addToPlaylist(WEB_REMIX, playlistId, videoId)
    }

    suspend fun addPlaylistToPlaylist(playlistId: String, addPlaylistId: String) = runCatching {
        innerTube.addPlaylistToPlaylist(WEB_REMIX, playlistId, addPlaylistId)
    }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoId: String) = runCatching {
        innerTube.removeFromPlaylist(WEB_REMIX, playlistId, videoId, setVideoId)
    }

    suspend fun moveSongPlaylist(playlistId: String, setVideoId: String, successorSetVideoId: String?) = runCatching {
        innerTube.moveSongPlaylist(WEB_REMIX, playlistId, setVideoId, successorSetVideoId)
    }

    fun createPlaylist(title: String) = runBlocking {
        innerTube.createPlaylist(WEB_REMIX, title).body<CreatePlaylistResponse>().playlistId
    }

    suspend fun renamePlaylist(playlistId: String, name: String) = runCatching {
        innerTube.renamePlaylist(WEB_REMIX, playlistId, name)
    }

    suspend fun uploadCustomThumbnailLink(playlistId: String, image: ByteArray) = runCatching {
        val uploadUrl = innerTube.getUploadCustomThumbnailLink(WEB_REMIX, image.size).headers["x-guploader-uploadid"]
        val blobReq = innerTube.uploadCustomThumbnail(
            WEB_REMIX,
            uploadUrl!!,
            image
        )
        val blobId = Json.decodeFromString<ImageUploadResponse>(blobReq.bodyAsText()).encryptedBlobId
        innerTube.setThumbnailPlaylist(WEB_REMIX, playlistId, blobId).body<EditPlaylistResponse>().newHeader?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
    }

    suspend fun removeThumbnailPlaylist(playlistId: String) = runCatching {
        innerTube.removeThumbnailPlaylist(WEB_REMIX, playlistId).body<EditPlaylistResponse>().newHeader?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
    }

    suspend fun deletePlaylist(playlistId: String) = runCatching {
        innerTube.deletePlaylist(WEB_REMIX, playlistId)
    }

    suspend fun player(videoId: String, playlistId: String? = null, client: YouTubeClient, signatureTimestamp: Int? = null): Result<PlayerResponse> = runCatching {
        innerTube.player(client, videoId, playlistId, signatureTimestamp).body<PlayerResponse>()
    }

    suspend fun registerPlayback(playlistId: String? = null, playbackTracking: String) = runCatching {
        val cpn = (1..16).map {
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"[Random.Default.nextInt(
                0,
                64
            )]
        }.joinToString("")

        val playbackUrl = playbackTracking.replace(
            "https://s.youtube.com",
            "https://music.youtube.com",
        )

        innerTube.registerPlayback(
            url = playbackUrl,
            playlistId = playlistId,
            cpn = cpn
        )
    }

    suspend fun next(endpoint: WatchEndpoint, continuation: String? = null): Result<NextResult> = runCatching {
        val response = innerTube.next(
            WEB_REMIX,
            endpoint.videoId,
            endpoint.playlistId,
            endpoint.playlistSetVideoId,
            endpoint.index,
            endpoint.params,
            continuation).body<NextResponse>()
        val playlistPanelRenderer = response.continuationContents?.playlistPanelContinuation
            ?: response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer
                ?.watchNextTabbedResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.musicQueueRenderer
                ?.content?.playlistPanelRenderer!!
        val title = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.musicQueueRenderer
            ?.header?.musicQueueHeaderRenderer?.subtitle?.runs?.firstOrNull()?.text
        val items = playlistPanelRenderer.contents.mapNotNull { content ->
            content.playlistPanelVideoRenderer
                ?.let(NextPage::fromPlaylistPanelVideoRenderer)
                ?.let { it to content.playlistPanelVideoRenderer.selected }
        }
        val songs = items.map { it.first }
        val currentIndex = items.indexOfFirst { it.second }.takeIf { it != -1 }

        // load automix items
        playlistPanelRenderer.contents.lastOrNull()?.automixPreviewVideoRenderer?.content?.automixPlaylistVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.let { watchPlaylistEndpoint ->
            return@runCatching next(watchPlaylistEndpoint).getOrThrow().let { result ->
                result.copy(
                    title = title,
                    items = songs + result.items,
                    lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint,
                    relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint,
                    currentIndex = currentIndex,
                    endpoint = watchPlaylistEndpoint
                )
            }
        }
        NextResult(
            title = title,
            items = songs,
            currentIndex = currentIndex,
            lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint,
            relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint,
            continuation = playlistPanelRenderer.continuations?.getContinuation(),
            endpoint = endpoint
        )
    }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        response.contents?.sectionListRenderer?.contents
            ?.firstOrNull { it.musicDescriptionShelfRenderer != null }
            ?.musicDescriptionShelfRenderer?.description?.runs
            ?.joinToString(separator = "") { it.text }
    }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId).body<BrowseResponse>()
        val songs = mutableListOf<SongItem>()
        val albums = mutableListOf<AlbumItem>()
        val artists = mutableListOf<ArtistItem>()
        val playlists = mutableListOf<PlaylistItem>()
        response.contents?.sectionListRenderer?.contents?.forEach { sectionContent ->
            sectionContent.musicCarouselShelfRenderer?.contents?.forEach { content ->
                when (val item = content.musicResponsiveListItemRenderer?.let(RelatedPage.Companion::fromMusicResponsiveListItemRenderer)
                    ?: content.musicTwoRowItemRenderer?.let(RelatedPage.Companion::fromMusicTwoRowItemRenderer)) {
                    is SongItem -> if (content.musicResponsiveListItemRenderer?.overlay
                            ?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchEndpoint?.watchEndpointMusicSupportedConfigs
                            ?.watchEndpointMusicConfig?.musicVideoType == MUSIC_VIDEO_TYPE_ATV
                    ) songs.add(item)

                    is AlbumItem -> albums.add(item)
                    is ArtistItem -> artists.add(item)
                    is PlaylistItem -> playlists.add(item)
                    is PodcastItem -> {}
                    is EpisodeItem -> {}
                    null -> {}
                }
            }
        }
        RelatedPage(songs, albums, artists, playlists)
    }

    suspend fun queue(videoIds: List<String>? = null, playlistId: String? = null): Result<List<SongItem>> = runCatching {
        if (videoIds != null) {
            assert(videoIds.size <= MAX_GET_QUEUE_SIZE) // Max video limit
        }
        innerTube.getQueue(WEB_REMIX, videoIds, playlistId).body<GetQueueResponse>().queueDatas
            .mapNotNull {
                it.content.playlistPanelVideoRenderer?.let { renderer ->
                    NextPage.fromPlaylistPanelVideoRenderer(renderer)
                }
            }
    }

    suspend fun transcript(videoId: String): Result<String> = runCatching {
        val response = innerTube.getTranscript(WEB, videoId).body<GetTranscriptResponse>()
        response.actions?.firstOrNull()?.updateEngagementPanelAction?.content?.transcriptRenderer?.body?.transcriptBodyRenderer?.cueGroups?.joinToString(separator = "\n") { group ->
            val time = group.transcriptCueGroupRenderer.cues[0].transcriptCueRenderer.startOffsetMs
            val text = group.transcriptCueGroupRenderer.cues[0].transcriptCueRenderer.cue.simpleText
                .trim('♪')
                .trim(' ')
            "[%02d:%02d.%03d]$text".format(time / 60000, (time / 1000) % 60, time % 1000)
        }!!
    }

    /**
     * Get caption tracks from the player response (like SmartTube does).
     * Returns a list of available caption tracks with their URLs.
     * Tries WEB client first, then falls back to ANDROID/MOBILE for YouTube Music content.
     * Also returns video duration in milliseconds for proper subtitle timing.
     */
    suspend fun getCaptionTracksWithDuration(videoId: String): Result<Pair<List<PlayerResponse.Captions.PlayerCaptionsTracklistRenderer.CaptionTrack>, Long?>> = runCatching {
        // Try WEB client first
        val webResponse = innerTube.player(WEB, videoId, null, null).body<PlayerResponse>()
        val webTracks = webResponse.captions?.playerCaptionsTracklistRenderer?.captionTracks
        val webDuration = webResponse.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000)
        
        if (!webTracks.isNullOrEmpty()) {
            return@runCatching webTracks to webDuration
        }

        // Fallback to ANDROID/MOBILE client which may have more captions available
        val mobileResponse = innerTube.player(MOBILE, videoId, null, null).body<PlayerResponse>()
        val mobileTracks = mobileResponse.captions?.playerCaptionsTracklistRenderer?.captionTracks
        val mobileDuration = mobileResponse.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000)
        
        if (!mobileTracks.isNullOrEmpty()) {
            return@runCatching mobileTracks to mobileDuration
        }
        
        // Last fallback to WEB_REMIX
        val remixResponse = innerTube.player(WEB_REMIX, videoId, null, null).body<PlayerResponse>()
        val remixTracks = remixResponse.captions?.playerCaptionsTracklistRenderer?.captionTracks
        val remixDuration = remixResponse.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000)
        
        (remixTracks ?: emptyList()) to (remixDuration ?: mobileDuration ?: webDuration)
    }
    
    /**
     * Get caption tracks from the player response (legacy function, returns only tracks).
     * @deprecated Use getCaptionTracksWithDuration instead for video duration support.
     */
    suspend fun getCaptionTracks(videoId: String): Result<List<PlayerResponse.Captions.PlayerCaptionsTracklistRenderer.CaptionTrack>> = runCatching {
        // Try WEB client first
        val webResponse = innerTube.player(WEB, videoId, null, null).body<PlayerResponse>()
        val webTracks = webResponse.captions?.playerCaptionsTracklistRenderer?.captionTracks
        if (!webTracks.isNullOrEmpty()) return@runCatching webTracks

        // Fallback to WEB_REMIX for YouTube Music content
        val remixResponse = innerTube.player(WEB_REMIX, videoId, null, null).body<PlayerResponse>()
        remixResponse.captions?.playerCaptionsTracklistRenderer?.captionTracks ?: emptyList()
    }
    
    /**
     * Get caption tracks using ANDROID client as primary (more reliable for captions).
     */
    suspend fun getCaptionTracksAndroid(videoId: String): Result<List<PlayerResponse.Captions.PlayerCaptionsTracklistRenderer.CaptionTrack>> = runCatching {
        // Try ANDROID/MOBILE client first - typically has more captions
        val mobileResponse = innerTube.player(MOBILE, videoId, null, null).body<PlayerResponse>()
        val mobileTracks = mobileResponse.captions?.playerCaptionsTracklistRenderer?.captionTracks
        if (!mobileTracks.isNullOrEmpty()) return@runCatching mobileTracks
        
        // Fallback to WEB
        val webResponse = innerTube.player(WEB, videoId, null, null).body<PlayerResponse>()
        webResponse.captions?.playerCaptionsTracklistRenderer?.captionTracks ?: emptyList()
    }

    /**
     * Fetch subtitle content from a caption track URL in JSON3 format.
     * Returns timed text as "[MM:SS.mmm]text" lines (same format as transcript).
     */
    suspend fun fetchSubtitleFromCaptionTrack(captionTrackUrl: String): Result<String> = runCatching {
        // Ensure URL has proper format and domain
        val fullUrl = if (captionTrackUrl.startsWith("http")) {
            captionTrackUrl
        } else {
            "https://youtube.com$captionTrackUrl"
        }
        
        val url = if ("fmt=" in fullUrl) fullUrl else "$fullUrl&fmt=json3"
        val responseText = innerTube.getUrl(url).bodyAsText()
        
        // Check if response is an error page (starts with < which indicates HTML)
        if (responseText.trim().startsWith("<")) {
            error("Invalid subtitle response (HTML error page)")
        }
        
        val json = Json.parseToJsonElement(responseText).jsonObject
        val events = json["events"]?.jsonArray ?: error("No events in subtitle response")
        buildString {
            for (event in events) {
                val obj = event.jsonObject
                val startMs = obj["tStartMs"]?.jsonPrimitive?.longOrNull ?: continue
                val segs = obj["segs"]?.jsonArray ?: continue
                val text = segs.joinToString("") {
                    it.jsonObject["utf8"]?.jsonPrimitive?.contentOrNull ?: ""
                }.trim().replace("\n", " ")
                if (text.isNotEmpty()) {
                    appendLine("[%02d:%02d.%03d]$text".format(startMs / 60000, (startMs / 1000) % 60, startMs % 1000))
                }
            }
        }.trim()
    }

    fun convertTimedTextToVtt(timedText: String): String {
        val lines = timedText.lines().filter { it.isNotBlank() }
        val cues = mutableListOf<String>()
        for (i in lines.indices) {
            val line = lines[i]
            val match = Regex("""\[(\d{2}):(\d{2})\.(\d{3})\](.*)""").find(line) ?: continue
            val min = match.groupValues[1].toInt()
            val sec = match.groupValues[2].toInt()
            val ms = match.groupValues[3].toInt()
            val text = match.groupValues[4].trim()
            val startMs = min * 60000 + sec * 1000 + ms
            val endMs = if (i + 1 < lines.size) {
                val nextLine = lines[i + 1]
                val nextMatch = Regex("""\[(\d{2}):(\d{2})\.(\d{3})\].*""").find(nextLine)
                if (nextMatch != null) {
                    val nextMin = nextMatch.groupValues[1].toInt()
                    val nextSec = nextMatch.groupValues[2].toInt()
                    val nextMs = nextMatch.groupValues[3].toInt()
                    nextMin * 60000 + nextSec * 1000 + nextMs
                } else {
                    startMs + 5000 // default 5s
                }
            } else {
                startMs + 5000
            }
            val startTime = formatTime(startMs)
            val endTime = formatTime(endMs)
            cues.add("$startTime --> $endTime\n$text")
        }
        return "WEBVTT\n\n" + cues.joinToString("\n\n")
    }
    
    /**
     * Convert timed text to VTT format with optional video duration.
     * Uses video duration for the last cue's end time instead of default 5s.
     */
    fun convertTimedTextToVttWithDuration(timedText: String, videoDurationMs: Long?): String {
        val lines = timedText.lines().filter { it.isNotBlank() }
        val cues = mutableListOf<String>()
        for (i in lines.indices) {
            val line = lines[i]
            val match = Regex("""\[(\d{2}):(\d{2})\.(\d{3})\](.*)""").find(line) ?: continue
            val min = match.groupValues[1].toInt()
            val sec = match.groupValues[2].toInt()
            val ms = match.groupValues[3].toInt()
            val text = match.groupValues[4].trim()
            val startMs = min * 60000 + sec * 1000 + ms
            val endMs = if (i + 1 < lines.size) {
                val nextLine = lines[i + 1]
                val nextMatch = Regex("""\[(\d{2}):(\d{2})\.(\d{3})\].*""").find(nextLine)
                if (nextMatch != null) {
                    val nextMin = nextMatch.groupValues[1].toInt()
                    val nextSec = nextMatch.groupValues[2].toInt()
                    val nextMs = nextMatch.groupValues[3].toInt()
                    nextMin * 60000 + nextSec * 1000 + nextMs
                } else {
                    startMs + 5000
                }
            } else {
                // Use video duration for last cue if available, otherwise default 5s
                (videoDurationMs?.toInt() ?: startMs + 5000).coerceAtLeast(startMs + 1000)
            }
            val startTime = formatTime(startMs)
            val endTime = formatTime(endMs)
            cues.add("$startTime --> $endTime\n$text")
        }
        return "WEBVTT\n\n" + cues.joinToString("\n\n")
    }

    private fun formatTime(ms: Int): String {
        val h = ms / 3600000
        val m = (ms % 3600000) / 60000
        val s = (ms % 60000) / 1000
        val mmm = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", h, m, s, mmm)
    }

    fun filterCaptionTracksByLanguage(
        tracks: List<PlayerResponse.Captions.PlayerCaptionsTracklistRenderer.CaptionTrack>,
        desiredLanguages: Set<String>
    ): List<PlayerResponse.Captions.PlayerCaptionsTracklistRenderer.CaptionTrack> {
        if (desiredLanguages.isEmpty()) {
            return tracks
        }
        val filtered = tracks.filter { it.languageCode in desiredLanguages }
        if (filtered.isEmpty()) {
            timber.log.Timber.w("filterCaptionTracksByLanguage: No tracks found for languages $desiredLanguages, returning all tracks")
            return tracks
        }
        val prioritized = filtered.sortedByDescending { track ->
            when (track.kind) {
                null -> 2
                "asr" -> 0
                else -> 1
            }
        }
        return prioritized
    }

    suspend fun visitorData(): Result<String> = runCatching {
        Json.parseToJsonElement(innerTube.getSwJsData().bodyAsText().substring(5))
            .jsonArray[0]
            .jsonArray[2]
            .jsonArray.first {
                (it as? JsonPrimitive)?.contentOrNull?.let { candidate ->
                    VISITOR_DATA_REGEX.containsMatchIn(candidate)
                } ?: false
            }
            .jsonPrimitive.content
    }

    suspend fun accountInfo(): Result<AccountInfo> = runCatching {
        innerTube.accountMenu(WEB_REMIX).body<AccountMenuResponse>()
            .actions[0].openPopupAction.popup.multiPageMenuRenderer
            .header?.activeAccountHeaderRenderer
            ?.toAccountInfo()!!
    }

    suspend fun feedback(tokens: List<String>): Result<Boolean> = runCatching {
        innerTube.feedback(WEB_REMIX, tokens).body<FeedbackResponse>().feedbackResponses.all { it.isProcessed }
    }

    /**
     * Add a song to library by fetching fresh feedback tokens from the next endpoint
     * This is more reliable than using cached tokens which might be stale
     */
    suspend fun addSongToLibrary(videoId: String): Result<Boolean> = runCatching {
        // Get fresh song data with menu tokens using next endpoint
        val nextResult = next(WatchEndpoint(videoId = videoId)).getOrThrow()
        val song = nextResult.items.find { it.id == videoId }
            ?: throw Exception("Song not found in next response")
        
        val addToken = song.libraryAddToken
            ?: throw Exception("Add to library token not available")
        
        feedback(listOf(addToken)).getOrThrow()
    }

    /**
     * Remove a song from library by fetching fresh feedback tokens from the next endpoint
     */
    suspend fun removeSongFromLibrary(videoId: String): Result<Boolean> = runCatching {
        // Get fresh song data with menu tokens using next endpoint
        val nextResult = next(WatchEndpoint(videoId = videoId)).getOrThrow()
        val song = nextResult.items.find { it.id == videoId }
            ?: throw Exception("Song not found in next response")
        
        val removeToken = song.libraryRemoveToken
            ?: throw Exception("Remove from library token not available")
        
        feedback(listOf(removeToken)).getOrThrow()
    }

    /**
     * Toggle song library status - adds if not in library, removes if in library
     * Uses fresh tokens fetched from the API for reliability
     */
    suspend fun toggleSongLibrary(videoId: String, addToLibrary: Boolean): Result<Boolean> = runCatching {
        if (addToLibrary) {
            addSongToLibrary(videoId).getOrThrow()
        } else {
            removeSongFromLibrary(videoId).getOrThrow()
        }
    }

    suspend fun getMediaInfo(videoId: String): Result<MediaInfo> = runCatching {
        return innerTube.getMediaInfo(videoId)
    }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FILTER_PODCAST = SearchFilter("EgWKAQIoAWoKEAkQChAFEAMQBA%3D%3D")
            }
    }

    @JvmInline
    value class LibraryFilter(val value: String) {
        companion object {
            val FILTER_RECENT_ACTIVITY = LibraryFilter("4qmFsgIrEhdGRW11c2ljX2xpYnJhcnlfbGFuZGluZxoQZ2dNR0tnUUlCaEFCb0FZQg%3D%3D")
            val FILTER_RECENTLY_PLAYED = LibraryFilter("4qmFsgIrEhdGRW11c2ljX2xpYnJhcnlfbGFuZGluZxoQZ2dNR0tnUUlCUkFCb0FZQg%3D%3D")
            val FILTER_PLAYLISTS_ALPHABETICAL = LibraryFilter("4qmFsgIrEhdGRW11c2ljX2xpa2VkX3BsYXlsaXN0cxoQZ2dNR0tnUUlBUkFBb0FZQg%3D%3D")
            val FILTER_PLAYLISTS_RECENTLY_SAVED = LibraryFilter("4qmFsgIrEhdGRW11c2ljX2xpa2VkX3BsYXlsaXN0cxoQZ2dNR0tnUUlBQkFCb0FZQg%3D%3D")
        }
    }

    const val MAX_GET_QUEUE_SIZE = 1000

    private val VISITOR_DATA_REGEX = Regex("^Cg[t|s]")

    fun getNewPipeStreamUrls(videoId: String): List<Pair<Int, String>> {
        return emptyList()
    }

    suspend fun newPipePlayer(
        videoId: String,
        tempRes: PlayerResponse,
    ): PlayerResponse? {
        return null
    }
}

data class PodcastsPage(
    val featured: List<PodcastSection>
) {
    data class PodcastSection(
        val title: String,
        val items: List<YTItem>
    )
}

data class MixesPage(
    val mixes: List<MixSection>
) {
    data class MixSection(
        val title: String,
        val items: List<YTItem>
    )
}

data class Top100ChartsPage(
    val countryCode: String,
    val items: List<YTItem>
)
