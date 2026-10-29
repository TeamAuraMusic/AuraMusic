/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.auramusic.app.ui.component.AuraCanvasImage
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.layout.ContentScale
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.EpisodeItem
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.PodcastItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.app.LocalDatabase
import com.auramusic.app.LocalListenTogetherManager
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.AppBarHeight
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.constants.ShowArtistDescriptionKey
import com.auramusic.app.constants.ShowArtistSubscriberCountKey
import com.auramusic.app.constants.ShowMonthlyListenersKey
import com.auramusic.app.db.entities.ArtistEntity
import com.auramusic.app.extensions.toMediaItem
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.app.playback.queues.ListQueue
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.ui.component.AlbumGridItem
import com.auramusic.app.ui.component.CollectionActionRow
import com.auramusic.app.ui.component.CollectionHeroBackdrop
import com.auramusic.app.ui.component.CollectionSectionHeader
import com.auramusic.app.ui.component.CollectionSubscribeButton
import com.auramusic.app.ui.component.ExpandableText
import com.auramusic.app.ui.component.HeroIconButton
import com.auramusic.app.ui.component.HeroPillButton
import com.auramusic.app.ui.component.HideOnScrollFAB
import com.auramusic.app.ui.component.IconButton
import com.auramusic.app.ui.component.LinkSegment
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.MetaChip
import com.auramusic.app.ui.component.SongListItem
import com.auramusic.app.ui.component.YouTubeGridItem
import com.auramusic.app.ui.component.YouTubeListItem
import com.auramusic.app.ui.component.rememberArtworkAccent
import com.auramusic.app.ui.component.shimmer.ButtonPlaceholder
import com.auramusic.app.ui.component.shimmer.ListItemPlaceHolder
import com.auramusic.app.ui.component.shimmer.ShimmerHost
import com.auramusic.app.ui.component.shimmer.TextPlaceholder
import com.auramusic.app.ui.menu.AlbumMenu
import com.auramusic.app.ui.menu.SongMenu
import com.auramusic.app.ui.menu.YouTubeAlbumMenu
import com.auramusic.app.ui.menu.YouTubeArtistMenu
import com.auramusic.app.ui.menu.YouTubePlaylistMenu
import com.auramusic.app.ui.menu.YouTubeSongMenu
import com.auramusic.app.ui.utils.backToMain
import com.auramusic.app.ui.utils.fadingEdge
import com.auramusic.app.ui.utils.isScrollingUp
import com.auramusic.app.ui.utils.resize
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val hiddenPlaylistIds by rememberPreference(com.auramusic.app.constants.HiddenPlaylistIdsKey, emptySet())
    // Hidden playlists stay out of the "featured on", "fans might also like" and community rows.
    val artistPage = viewModel.artistPage?.let { page ->
        if (hiddenPlaylistIds.isEmpty()) page
        else page.copy(
            sections = page.sections
                .map { section -> section.copy(items = section.items.filterNot { it is PlaylistItem && it.id in hiddenPlaylistIds }) }
                .filter { it.items.isNotEmpty() },
        )
    }
    // Set while the artist-to-channel link is being resolved, so the button can say so
    // rather than appearing to do nothing.
    var openingChannel by remember { mutableStateOf(false) }
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showArtistDescription by rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val showArtistSubscriberCount by rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val showMonthlyListeners by rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    // The hero is drawn under the transparent top bar, so it is pulled up by the bar's height.
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topBarHeight = systemBarsTopPadding + AppBarHeight
    val topBarHeightPx = with(density) { topBarHeight.roundToPx() }

    val heroThumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
    val accent = rememberArtworkAccent(heroThumbnail)

    // Laid-out height of the hero (below the top bar), used to fade the bar in once the
    // artist name has scrolled underneath it.
    var heroHeightPx by remember { mutableIntStateOf(0) }
    val appBarAlpha by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                heroHeightPx <= 0 -> 0f
                else -> ((lazyListState.firstVisibleItemScrollOffset - heroHeightPx * 0.55f) / (heroHeightPx * 0.3f))
                    .coerceIn(0f, 1f)
            }
        }
    }
    val transparentAppBar by remember {
        derivedStateOf { appBarAlpha < 0.01f }
    }

    // Grid items carry 12dp of padding of their own, so 4dp here gives every shelf a 16dp edge.
    val shelfContentPadding = WindowInsets.systemBars
        .only(WindowInsetsSides.Horizontal)
        .add(WindowInsets(left = 4.dp, right = 4.dp))
        .asPaddingValues()

    LaunchedEffect(libraryArtist) {
        // always show local page for local artists. Show local page remote artist when offline
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (artistPage == null && !showLocal) {
                item(key = "shimmer") {
                    ShimmerHost(
                        modifier = Modifier.drawUnderTopBar(topBarHeightPx),
                    ) {
                        // Hero image placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topBarHeight + 320.dp),
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shimmer()
                                    .background(MaterialTheme.colorScheme.onSurface)
                                    .fadingEdge(
                                        top = topBarHeight,
                                        bottom = 200.dp,
                                    ),
                            )
                            // Overline and artist name placeholders, bottom-left like the hero
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                            ) {
                                TextPlaceholder(
                                    height = 12.dp,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth(0.2f),
                                )
                                TextPlaceholder(
                                    height = 40.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                )
                            }
                        }

                        // Shuffle / Radio pill placeholders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ButtonPlaceholder(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                            )
                            ButtonPlaceholder(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                            )
                        }

                        // Subscribe pill and icon button placeholders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ButtonPlaceholder(
                                modifier = Modifier
                                    .width(132.dp)
                                    .height(44.dp),
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface),
                                )
                            }
                        }

                        // Section header placeholder
                        TextPlaceholder(
                            height = 22.dp,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
                                .fillMaxWidth(0.4f),
                        )

                        // Songs List Placeholder
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                item(key = "header") {
                    val thumbnail = heroThumbnail
                    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name
                    val subscriberCount = artistPage?.subscriberCountText
                        ?.takeIf { !showLocal && showArtistSubscriberCount && it.isNotBlank() }
                    val monthlyListeners = artistPage?.monthlyListenerCount
                        ?.takeIf { !showLocal && showMonthlyListeners && it.isNotBlank() }
                    val backgroundColor = MaterialTheme.colorScheme.background

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Immersive hero: full-bleed artist photo under the top bar with a
                        // parallax drift, fading into the background behind the name.
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { heroHeightPx = it.height }
                                .drawUnderTopBar(topBarHeightPx)
                                .clipToBounds(),
                        ) {
                            val heroHeight = if (thumbnail != null) {
                                (maxWidth * 1.05f).coerceIn(380.dp, 560.dp)
                            } else {
                                topBarHeight + 220.dp
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(heroHeight),
                            ) {
                                if (thumbnail != null) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .graphicsLayer {
                                                val scrolled = if (lazyListState.firstVisibleItemIndex == 0) {
                                                    lazyListState.firstVisibleItemScrollOffset.toFloat()
                                                } else {
                                                    0f
                                                }
                                                translationY = scrolled * 0.5f
                                                if (size.height > 0f) {
                                                    alpha = 1f - (scrolled / size.height).coerceIn(0f, 1f) * 0.6f
                                                }
                                            },
                                    ) {
                                        @UnstableApi
                                        AuraCanvasImage(
                                            title = null,
                                            artist = artistName,
                                            staticImageUrl = thumbnail.resize(1200, 1200).toString(),
                                            contentScale = ContentScale.Crop,
                                            candidateTracks = librarySongs.take(10).mapNotNull { it.song.id }, // for on-demand remote lookup
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    // Accent wash towards the bottom of the photo
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.35f to Color.Transparent,
                                                    1f to accent.copy(alpha = 0.28f),
                                                ),
                                            ),
                                    )
                                    // Scrim under the top bar and a strong fade into the background
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0f to backgroundColor.copy(alpha = 0.55f),
                                                    0.28f to Color.Transparent,
                                                    0.5f to Color.Transparent,
                                                    0.78f to backgroundColor.copy(alpha = 0.72f),
                                                    1f to backgroundColor,
                                                ),
                                            ),
                                    )
                                } else {
                                    CollectionHeroBackdrop(
                                        imageUrl = null,
                                        accent = accent,
                                        modifier = Modifier.matchParentSize(),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.person),
                                            contentDescription = null,
                                            tint = accent.copy(alpha = 0.35f),
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(96.dp),
                                        )
                                    }
                                }

                                // Overline, name and stats, bottom-left over the fade
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                        .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.sort_by_artist).uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accent,
                                        letterSpacing = 2.sp,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = artistName ?: "Unknown",
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            shadow = Shadow(
                                                color = backgroundColor.copy(alpha = 0.6f),
                                                blurRadius = 24f,
                                            ),
                                        ),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (subscriberCount != null || monthlyListeners != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            subscriberCount?.let { MetaChip(it) }
                                            monthlyListeners?.let { MetaChip(it) }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Primary playback actions
                        val shuffleEndpoint = artistPage?.artist?.shuffleEndpoint
                        val radioEndpoint = artistPage?.artist?.radioEndpoint
                        val showOnlineShuffle = !showLocal && !isGuest && shuffleEndpoint != null
                        val showLocalShuffle = showLocal && !isGuest && librarySongs.isNotEmpty()
                        val showRadio = !showLocal && !isGuest && radioEndpoint != null

                        if (showOnlineShuffle || showLocalShuffle || showRadio) {
                            CollectionActionRow(
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                if (showOnlineShuffle || showLocalShuffle) {
                                    HeroPillButton(
                                        text = stringResource(R.string.shuffle),
                                        icon = R.drawable.shuffle,
                                        onClick = {
                                            if (showOnlineShuffle) {
                                                shuffleEndpoint?.let {
                                                    playerConnection.playQueue(YouTubeQueue(it))
                                                }
                                            } else {
                                                val shuffledSongs = librarySongs.shuffled()
                                                if (shuffledSongs.isNotEmpty()) {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                            items = shuffledSongs.map { it.toMediaItem() }
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        accent = accent,
                                        filled = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (showRadio) {
                                    HeroPillButton(
                                        text = stringResource(R.string.radio),
                                        icon = R.drawable.radio,
                                        onClick = {
                                            radioEndpoint?.let {
                                                playerConnection.playQueue(YouTubeQueue(it))
                                            }
                                        },
                                        accent = accent,
                                        filled = false,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        // Secondary actions: subscribe, open channel and the artist menu
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CollectionSubscribeButton(
                                subscribed = libraryArtist?.artist?.bookmarkedAt != null,
                                accent = accent,
                                onClick = {
                                    database.transaction {
                                        val artist = libraryArtist?.artist
                                        if (artist != null) {
                                            update(artist.toggleLike())
                                        } else {
                                            artistPage?.artist?.let {
                                                insert(
                                                    ArtistEntity(
                                                        id = it.id,
                                                        name = it.title,
                                                        channelId = it.channelId,
                                                        thumbnailUrl = it.thumbnail,
                                                    ).toggleLike()
                                                )
                                            }
                                        }
                                    }
                                },
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Straight to the same artist on the video side. The
                            // channel id comes from the library when it is there
                            // and from the artist page otherwise, so this works for
                            // any artist rather than only the saved ones.
                            val artistChannelId = libraryArtist?.artist?.channelId
                                ?: artistPage?.artist?.channelId
                            if (artistChannelId != null) {
                                HeroIconButton(
                                    // The same mark the channel screen uses to
                                    // step across to the artist, so the two
                                    // buttons read as one pair.
                                    icon = R.drawable.person,
                                    contentDescription = stringResource(R.string.open_channel),
                                    enabled = !openingChannel,
                                    onClick = {
                                        // An artist's channel id is often an
                                        // auto-generated one, which opens a
                                        // generated page rather than the artist's.
                                        // Resolved by name and subscriber count
                                        // before leaving this screen.
                                        coroutineScope.launch {
                                            openingChannel = true
                                            val resolved =
                                                com.auramusic.app.video.ArtistChannelResolver.resolve(
                                                    artistName = libraryArtist?.artist?.name
                                                        ?: artistPage?.artist?.title.orEmpty(),
                                                    artistChannelId = artistChannelId,
                                                    artistSubscribersText = artistPage?.subscriberCountText
                                                        ?: libraryArtist?.artist?.subscriberCountText,
                                                )
                                            openingChannel = false
                                            resolved?.let {
                                                navController.navigate("youtube_channel/${it.channelId}")
                                            }
                                        }
                                    },
                                    // While the link resolves, say so rather than
                                    // appearing to do nothing.
                                    iconContent = if (openingChannel) {
                                        {
                                            CircularProgressIndicator(
                                                color = accent,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }

                            artistPage?.artist?.let { artistItem ->
                                HeroIconButton(
                                    icon = R.drawable.more_vert,
                                    contentDescription = stringResource(R.string.more_options),
                                    onClick = {
                                        menuState.show {
                                            YouTubeArtistMenu(
                                                artist = artistItem,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // About Artist Section (subscriber and monthly listener counts sit in the hero)
                if (!showLocal && showArtistDescription) {
                    val description = artistPage?.description
                    val descriptionRuns = artistPage?.descriptionRuns

                    if (!description.isNullOrEmpty() || !descriptionRuns.isNullOrEmpty()) {
                        item(key = "about_artist") {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.info),
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.about_artist),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Expands and collapses on tap, with its own show more / less.
                                    ExpandableText(
                                        text = description.orEmpty(),
                                        runs = descriptionRuns?.map {
                                            LinkSegment(
                                                text = it.text,
                                                url = it.navigationEndpoint?.urlEndpoint?.url
                                            )
                                        },
                                        collapsedMaxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }

                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_title") {
                            ArtistSectionHeader(
                                title = stringResource(R.string.songs),
                                accent = accent,
                                modifier = Modifier.animateItem(),
                                onMoreClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                }
                            )
                        }

                        val filteredLibrarySongs = if (hideExplicit) {
                            librarySongs.filter { !it.song.explicit }
                        } else {
                            librarySongs
                        }
                        itemsIndexed(
                            items = filteredLibrarySongs,
                            key = { index, item -> "local_song_${item.id}_$index" }
                        ) { index, song ->
                            SongListItem(
                                song = song,
                                showInLibraryIcon = true,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                        items = librarySongs.map { it.toMediaItem() },
                                                        startIndex = index
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (libraryAlbums.isNotEmpty()) {
                        item(key = "local_albums_title") {
                            ArtistSectionHeader(
                                title = stringResource(R.string.albums),
                                accent = accent,
                                modifier = Modifier.animateItem(),
                                onMoreClick = {
                                    navController.navigate("artist/${viewModel.artistId}/albums")
                                }
                            )
                        }

                        item(key = "local_albums_list") {
                            val filteredLibraryAlbums = if (hideExplicit) {
                                libraryAlbums.filter { !it.album.explicit }
                            } else {
                                libraryAlbums
                            }
                            LazyRow(
                                contentPadding = shelfContentPadding,
                            ) {
                                items(
                                    items = filteredLibraryAlbums,
                                    key = { "local_album_${it.id}_${filteredLibraryAlbums.indexOf(it)}" }
                                ) { album ->
                                    AlbumGridItem(
                                        album = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${album.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = album,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "section_${section.title}") {
                                ArtistSectionHeader(
                                    title = section.title,
                                    accent = accent,
                                    modifier = Modifier.animateItem(),
                                    onMoreClick = section.moreEndpoint?.let {
                                        {
                                            navController.navigate(
                                                "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                            items(
                                items = section.items.distinctBy { it.id },
                                key = { "youtube_song_${it.id}" },
                            ) { song ->
                                YouTubeListItem(
                                    item = song as SongItem,
                                    isActive = mediaMetadata?.id == song.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = song.id),
                                                            song.toMediaMetadata()
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        } else {
                            item(key = "section_list_${section.title}") {
                                LazyRow(
                                    contentPadding = shelfContentPadding,
                                ) {
                                    items(
                                        items = section.items.distinctBy { it.id },
                                        key = { "youtube_album_${it.id}" },
                                    ) { item ->
                                        YouTubeGridItem(
                                            item = item,
                                            isActive = when (item) {
                                                is SongItem -> mediaMetadata?.id == item.id
                                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                else -> false
                                            },
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            thumbnailRatio = 1f, // Use square thumbnails for all items in horizontal scroll
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        when (item) {
                                                            is SongItem ->
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata()
                                                                    ),
                                                                )

                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                            is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                                            is EpisodeItem -> playerConnection.playQueue(
                                                                YouTubeQueue.radio(item.toMediaMetadata())
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            when (item) {
                                                                is SongItem ->
                                                                    YouTubeSongMenu(
                                                                        song = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is AlbumItem ->
                                                                    YouTubeAlbumMenu(
                                                                        albumItem = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is ArtistItem ->
                                                                    YouTubeArtistMenu(
                                                                        artist = item,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is PlaylistItem ->
                                                                    YouTubePlaylistMenu(
                                                                        playlist = item,
                                                                        coroutineScope = coroutineScope,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                                is PodcastItem ->
                                                                    YouTubePlaylistMenu(
                                                                        playlist = item.asPlaylistItem(),
                                                                        coroutineScope = coroutineScope,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                                is EpisodeItem ->
                                                                    YouTubeSongMenu(
                                                                        song = item.asSongItem(),
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                            }
                                                        }
                                                    },
                                                )
                                                .animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val isScrollingUp = lazyListState.isScrollingUp()
        val showLocalFab = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true
        
        // Library/Local Toggle FAB
        HideOnScrollFAB(
            visible = showLocalFab,
            lazyListState = lazyListState,
            icon = if (showLocal) R.drawable.language else R.drawable.library_music,
            onClick = {
                showLocal = showLocal.not()
                if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM()
            }
        )
        
        // Play All FAB (Stacked above Library/Local FAB if visible)
        val canPlayAll = !isGuest && (
            (showLocal && librarySongs.isNotEmpty()) || 
            (!showLocal && artistPage?.sections?.any { 
                (it.items.firstOrNull() as? SongItem)?.album != null 
            } == true)
        )

        if (canPlayAll) {
             androidx.compose.animation.AnimatedVisibility(
                visible = isScrollingUp,
                enter = androidx.compose.animation.slideInVertically { it * 2 },
                exit = androidx.compose.animation.slideOutVertically { it * 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    )
                    // Add padding to position it above the other FAB (56dp height + 16dp padding + 8dp spacing)
                    // If the other FAB is visible.
                    .padding(bottom = if (showLocalFab) 64.dp else 0.dp)
            ) {
                val onPlayAllClick: () -> Unit = {
                     if (showLocal) {
                         if (librarySongs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                    items = librarySongs.map { it.toMediaItem() }
                                )
                            )
                        }
                    } else if (artistPage != null) {
                        val songSection = artistPage.sections.find { section ->
                            (section.items.firstOrNull() as? SongItem)?.album != null
                        }
                        
                        val moreEndpoint = songSection?.moreEndpoint
                        if (moreEndpoint != null) {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val result = YouTube.artistItems(moreEndpoint).getOrNull()
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    if (result != null && result.items.isNotEmpty()) {
                                        val songs = result.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = artistPage.artist.title,
                                                items = songs
                                            )
                                        )
                                    } else {
                                        // Fallback to loaded items
                                        val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                        if (songs.isNotEmpty()) {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = artistPage.artist.title,
                                                    items = songs
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            } else if (songSection != null) {
                            // Use loaded items if no more endpoint
                            val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artistPage.artist.title,
                                    items = songs
                                )
                            )
                        } else {
                            // Fallback to shuffle endpoint (stripped) if no song section found
                            val shuffleEndpoint = artistPage.artist.shuffleEndpoint
                            if (shuffleEndpoint != null) {
                                val endpoint = if (shuffleEndpoint.playlistId != null) {
                                    WatchEndpoint(
                                        playlistId = shuffleEndpoint.playlistId,
                                        params = null, // Remove shuffle params to play in order
                                        videoId = null // Ensure videoId is null to start from beginning of playlist
                                    )
                                } else {
                                    shuffleEndpoint
                                }
                                playerConnection.playQueue(YouTubeQueue(endpoint))
                            }
                        }
                    }
                }

                if (showLocalFab) {
                     androidx.compose.material3.SmallFloatingActionButton(
                        modifier = Modifier.padding(16.dp).offset(x = (-4).dp), // Align center with standard FAB (56dp vs 48dp)
                        onClick = onPlayAllClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All",
                        )
                    }
                } else {
                    androidx.compose.material3.FloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        onClick = onPlayAllClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }


        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }

    // Transparent over the hero; a solid surface and the artist name fade in once the
    // name has scrolled under the bar.
    val appBarSurface = MaterialTheme.colorScheme.surface
    TopAppBar(
        title = {
            if (!transparentAppBar) {
                Text(
                    text = artistPage?.artist?.title ?: libraryArtist?.artist?.name.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = appBarAlpha },
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    viewModel.artistPage?.artist?.shareLink?.let { link ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Artist Link", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(
                    painterResource(R.drawable.link),
                    contentDescription = null,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = appBarSurface.copy(alpha = appBarAlpha),
            scrolledContainerColor = appBarSurface.copy(alpha = appBarAlpha),
        )
    )
}

/**
 * Lays the content out [shiftPx] higher than its slot and reports that much less height,
 * so the hero sits under the transparent top bar without leaving a gap below it.
 */
private fun Modifier.drawUnderTopBar(shiftPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, (placeable.height - shiftPx).coerceAtLeast(0)) {
        placeable.place(0, -shiftPx)
    }
}

/** Accent section header; the whole row and the chevron open [onMoreClick] when given. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistSectionHeader(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onMoreClick: (() -> Unit)? = null,
) {
    CollectionSectionHeader(
        title = title,
        accent = accent,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onMoreClick != null) { onMoreClick?.invoke() },
        trailing = if (onMoreClick != null) {
            {
                Surface(
                    onClick = onMoreClick,
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.navigate_next),
                            contentDescription = stringResource(R.string.see_more),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        } else {
            null
        },
    )
}
