/**
 * AuraMusic Project (C) 2026
 * Licensed under GPL-3.0. See LICENSE file for details.
 */

package com.auramusic.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.WatchEndpoint
import com.auramusic.app.constants.AppBarHeight
import com.auramusic.app.constants.AppLanguageKey
import com.auramusic.app.constants.LastSeenVersionKey
import com.auramusic.app.constants.ChangelogShownForVersionKey
import com.auramusic.app.constants.CheckForUpdatesKey
import com.auramusic.app.constants.DarkModeKey
import com.auramusic.app.constants.DefaultOpenTabKey
import com.auramusic.app.constants.DisableScreenshotKey
import com.auramusic.app.constants.DynamicThemeKey
import com.auramusic.app.constants.EnableHighRefreshRateKey
import com.auramusic.app.constants.ListenTogetherUsernameKey
import com.auramusic.app.constants.MiniPlayerBottomSpacing
import com.auramusic.app.constants.MiniPlayerHeight
import com.auramusic.app.constants.NavigationBarAnimationSpec
import com.auramusic.app.constants.NavigationBarHeight
import com.auramusic.app.constants.PauseSearchHistoryKey
import com.auramusic.app.constants.PureBlackKey
import com.auramusic.app.constants.SYSTEM_DEFAULT
import com.auramusic.app.constants.SelectedThemeColorKey
import com.auramusic.app.constants.SlimNavBarHeight
import com.auramusic.app.constants.SlimNavBarKey
import com.auramusic.app.constants.ListenTogetherAtTopKey
import com.auramusic.app.constants.StopMusicOnTaskClearKey
import com.auramusic.app.constants.UpdateNotificationsEnabledKey
import com.auramusic.app.constants.UpdateVariantKey
import com.auramusic.app.constants.UseNewMiniPlayerDesignKey
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.SearchHistory
import com.auramusic.app.extensions.toEnum
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.app.playback.DownloadUtil
import com.auramusic.app.playback.MusicService
import com.auramusic.app.playback.MusicService.MusicBinder
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.ui.component.AccountSettingsDialog
import com.auramusic.app.ui.component.AppNavigationBar
import com.auramusic.app.ui.component.AppNavigationRail
import com.auramusic.app.ui.component.BottomSheetMenu
import com.auramusic.app.ui.component.BottomSheetPage
import com.auramusic.app.ui.component.LocalBottomSheetPageState
import com.auramusic.app.ui.component.LocalMenuState
import com.auramusic.app.ui.component.rememberBottomSheetState
import com.auramusic.app.ui.component.shimmer.ShimmerTheme
import com.auramusic.app.ui.menu.YouTubeSongMenu
import com.auramusic.app.ui.player.BottomSheetPlayer
import com.auramusic.app.ui.screens.Screens
import com.auramusic.app.ui.screens.navigationBuilder
import com.auramusic.app.ui.screens.settings.ChangelogScreen
import com.auramusic.app.ui.screens.settings.DarkMode
import com.auramusic.app.ui.screens.settings.NavigationTab
import com.auramusic.app.ui.theme.ColorSaver
import com.auramusic.app.ui.theme.DefaultThemeColor
import com.auramusic.app.ui.theme.AuraMusicTheme
import com.auramusic.app.ui.theme.extractThemeColor
import com.auramusic.app.ui.utils.appBarScrollBehavior
import com.auramusic.app.ui.utils.resetHeightOffset
import com.auramusic.app.utils.SyncUtils
import com.auramusic.app.utils.Updater
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.utils.reportException
import com.auramusic.app.utils.setAppLocale
import com.auramusic.app.viewmodels.HomeViewModel
import com.auramusic.app.voice.LocalVoiceCommandController
import com.auramusic.app.voice.VoiceCommandController
import com.auramusic.app.voice.VoiceCommandOverlay
import com.auramusic.app.voice.VoiceCommandViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTION_SEARCH = "com.auramusic.app.action.SEARCH"
        private const val ACTION_LIBRARY = "com.auramusic.app.action.LIBRARY"
    }

    private fun handleDeepLinkIntent(intent: Intent?, navController: NavHostController) {
        if (intent == null) return
        
        val data = intent.data
        if (data != null && data.host == "www.auramusic.site" && data.pathSegments.firstOrNull() == "play") {
            // Deep link: auramusic.site/play/{videoId}
            val videoId = data.pathSegments.getOrNull(1)
            if (videoId != null) {
                lifecycleScope.launch {
                    try {
                        // Try to get song from local database first
                        val database = this@MainActivity.database
                        val localSong = database.song(videoId).first()

                        if (localSong != null) {
                            // Song exists locally, play it directly
                            val playerConnection = this@MainActivity.playerConnection
                            playerConnection?.playQueue(YouTubeQueue.radio(localSong.toMediaMetadata()))
                        } else {
                            // Could not find song, just open the app
                            // User can search for it manually
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils
    
    @Inject
    lateinit var listenTogetherManager: com.auramusic.app.listentogether.ListenTogetherManager

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                try {
                    playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    Timber.tag("MainActivity").d("PlayerConnection created successfully")
                    // Connect Listen Together manager to player
                    listenTogetherManager.setPlayerConnection(playerConnection)
                } catch (e: Exception) {
                    Timber.tag("MainActivity").e(e, "Failed to create PlayerConnection")
                    // Retry after a delay of 500ms
                    lifecycleScope.launch {
                        delay(500)
                        try {
                            playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                            listenTogetherManager.setPlayerConnection(playerConnection)
                        } catch (e2: Exception) {
                            Timber.tag("MainActivity").e(e2, "Failed to create PlayerConnection on retry")
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Disconnect Listen Together manager
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onStart() {
        super.onStart()
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }

        // On Android 12+, we can't start foreground services from background
        // Use BIND_AUTO_CREATE which will create the service if needed
        // The service will call startForeground() in onCreate() when bound
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey, false) && 
            playerConnection?.isPlaying?.value == true && 
            isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize Listen Together manager
        listenTogetherManager.initialize()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        setContent {
            AuraMusicApp(
                latestVersionName = latestVersionName,
                onLatestVersionNameChange = { latestVersionName = it },
                playerConnection = playerConnection,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
            )
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AuraMusicApp(
        latestVersionName: String,
        onLatestVersionNameChange: (String) -> Unit,
        playerConnection: PlayerConnection?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        syncUtils: SyncUtils,
    ) {
        val changelogState = remember { mutableStateOf(false) }
        val checkForUpdates by rememberPreference(CheckForUpdatesKey, defaultValue = true)

        LaunchedEffect(checkForUpdates) {
            if (checkForUpdates) {
                withContext(Dispatchers.IO) {
                    val updatesEnabled = dataStore.get(CheckForUpdatesKey, true)
                    val notifEnabled = dataStore.get(UpdateNotificationsEnabledKey, true)
                    val preferredVariant = dataStore.get(UpdateVariantKey, if (BuildConfig.CAST_AVAILABLE) "gms" else "foss")
                    if (!updatesEnabled) return@withContext
                    
                    Updater.checkForUpdate(forceRefresh = true).onSuccess { (releaseInfo, hasUpdate) ->
                        if (releaseInfo != null) {
                            // Normalize version name - extract version from formats like "AuraMusic v1.0.4" or "v1.0.4"
                            val rawVersion = releaseInfo.versionName
                            val normalizedVersion = try {
                                // Try to find version pattern (e.g., 1.0.4, 1.0.4.5)
                                val versionRegex = Regex("(\\d+\\.\\d+(\\.\\d+)?)")
                                versionRegex.find(rawVersion)?.groupValues?.get(1) ?: rawVersion
                            } catch (e: Exception) {
                                rawVersion
                            }
                            // Always show badge if there's a new version available
                            onLatestVersionNameChange(normalizedVersion)
                            
                            // Check if app was just updated for changelog
                            var lastSeenVersion: String? = null
                            var changelogShownForVersion: String? = null
                            runBlocking {
                                lastSeenVersion = dataStore.data.map { it[LastSeenVersionKey] }.first()
                                changelogShownForVersion = dataStore.data.map { it[ChangelogShownForVersionKey] }.first()
                            }
                            val currentVersion = BuildConfig.VERSION_NAME
                            val isAppUpdated = lastSeenVersion != currentVersion
                            
                            // If app was updated, show changelog if not already shown for this version
                            if (isAppUpdated && changelogShownForVersion != currentVersion) {
                                changelogState.value = true
                            }
                            
                            // If there's a new version available, show notification
                            if (hasUpdate && notifEnabled) {
                                val downloadUrl = Updater.getDownloadUrlForCurrentVariant(releaseInfo, preferredVariant)
                                if (downloadUrl != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, downloadUrl.toUri())

                                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                                        (PendingIntent.FLAG_IMMUTABLE)
                                    val pending = PendingIntent.getActivity(this@MainActivity, 1001, intent, flags)

                                    val notif = NotificationCompat.Builder(this@MainActivity, "updates")
                                        .setSmallIcon(R.drawable.ic_notification_icon)
                                        .setContentTitle(getString(R.string.update_available_title))
                                        .setContentText("New version ${releaseInfo.versionName} available")
                                        .setContentIntent(pending)
                                        .setAutoCancel(true)
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                                        .build()

                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        NotificationManagerCompat.from(this@MainActivity).notify(1001, notif)
                                    }
                                }
                            }
                            
                            // Update last seen version
                            runBlocking {
                                dataStore.edit { 
                                    it[LastSeenVersionKey] = currentVersion
                                }
                            }
                        }
                    }
                }
            } else {
                onLatestVersionNameChange(BuildConfig.VERSION_NAME)
            }
        }

        val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
        val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

        LaunchedEffect(enableHighRefreshRate) {
            val window = this@MainActivity.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val layoutParams = window.attributes
                if (enableHighRefreshRate) {
                    layoutParams.preferredDisplayModeId = 0
                } else {
                    val modes = window.windowManager.defaultDisplay.supportedModes
                    val mode60 = modes.firstOrNull { kotlin.math.abs(it.refreshRate - 60f) < 1f }
                        ?: modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }

                    if (mode60 != null) {
                        layoutParams.preferredDisplayModeId = mode60.modeId
                    }
                }
                window.attributes = layoutParams
            } else {
                val params = window.attributes
                if (enableHighRefreshRate) {
                    params.preferredRefreshRate = 0f
                } else {
                    params.preferredRefreshRate = 60f
                }
                window.attributes = params
            }
        }

        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

        LaunchedEffect(useDarkTheme) {
            setSystemBarAppearance(useDarkTheme)
        }

        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }

        val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
        val selectedThemeColor = Color(selectedThemeColorInt)

        var themeColor by rememberSaveable(stateSaver = ColorSaver) {
            mutableStateOf(selectedThemeColor)
        }

        LaunchedEffect(selectedThemeColor) {
            if (!enableDynamicTheme) {
                themeColor = selectedThemeColor
            }
        }

        LaunchedEffect(playerConnection, enableDynamicTheme, selectedThemeColor) {
            val playerConnection = playerConnection
            if (!enableDynamicTheme || playerConnection == null) {
                themeColor = selectedThemeColor
                return@LaunchedEffect
            }

            playerConnection.service.currentMediaMetadata.collectLatest { song ->
                if (song?.thumbnailUrl != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val result = imageLoader.execute(
                                ImageRequest.Builder(this@MainActivity)
                                    .data(song.thumbnailUrl)
                                    .allowHardware(false)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .networkCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(false)
                                    .build()
                            )
                            themeColor = result.image?.toBitmap()?.extractThemeColor() ?: selectedThemeColor
                        } catch (e: Exception) {
                            // Fallback to default on error
                            themeColor = selectedThemeColor
                        }
                    }
                } else {
                    themeColor = selectedThemeColor
                }
            }
        }

        AuraMusicTheme(
            darkTheme = useDarkTheme,
            pureBlack = pureBlack,
            themeColor = themeColor,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
            ) {
                val focusManager = LocalFocusManager.current
                val density = LocalDensity.current
                val configuration = LocalWindowInfo.current
                val cutoutInsets = WindowInsets.displayCutout
                val windowsInsets = WindowInsets.systemBars
                val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = hiltViewModel()
                val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                // Voice command system
                val voiceCommandViewModel: VoiceCommandViewModel = hiltViewModel()
                val voiceUiState by voiceCommandViewModel.uiState.collectAsState()

                var hasMicPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasMicPermission = granted
                    voiceCommandViewModel.onMicPermissionChanged(granted)
                    if (granted) {
                        voiceCommandViewModel.startManualSession()
                    }
                }

                LaunchedEffect(Unit) {
                    voiceCommandViewModel.onMicPermissionChanged(hasMicPermission)
                    voiceCommandViewModel.onAppForeground()
                }

                DisposableEffect(Unit) {
                    onDispose {
                        voiceCommandViewModel.onAppBackground()
                    }
                }

                LaunchedEffect(playerConnection) {
                    voiceCommandViewModel.bindHandlers(
                        playerConnectionProvider = { playerConnection },
                        onSearch = { query ->
                            navController.navigate("search/$query")
                        },
                        onNavigate = { route ->
                            navController.navigate(route)
                        }
                    )
                }

                val voiceCommandController = remember(voiceCommandViewModel, hasMicPermission) {
                    object : VoiceCommandController {
                        override fun requestManualActivation() {
                            if (hasMicPermission) {
                                voiceCommandViewModel.startManualSession()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                }

                val (listenTogetherAtTop) = rememberPreference(ListenTogetherAtTopKey, defaultValue = true)
                val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                
                // Navigation items - exclude ListenTogether when it's at top (shown in app bar instead)
                val navigationItems = remember(listenTogetherAtTop) {
                    if (listenTogetherAtTop) {
                        listOf(Screens.Home, Screens.Search, Screens.Library)
                    } else {
                        listOf(Screens.Home, Screens.ListenTogether, Screens.Search, Screens.Library)
                    }
                }
                
                val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                val defaultOpenTab = remember {
                    dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                }
                val tabOpenedFromShortcut = remember {
                    when (intent?.action) {
                        ACTION_SEARCH -> NavigationTab.LIBRARY
                        ACTION_LIBRARY -> NavigationTab.SEARCH
                        else -> null
                    }
                }

                val topLevelScreens = remember {
                    listOf(
                        Screens.Home.route,
                        Screens.Library.route,
                        Screens.ListenTogether.route,
                        "settings",
                    )
                }

                val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }

                val onSearch: (String) -> Unit = remember {
                    { searchQuery ->
                        if (searchQuery.isNotEmpty()) {
                            navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")

                            if (dataStore[PauseSearchHistoryKey] != true) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    database.query {
                                        insert(SearchHistory(query = searchQuery))
                                    }
                                }
                            }
                        }
                    }
                }

                // Use derivedStateOf to avoid unnecessary recompositions
                val currentRoute by remember {
                    derivedStateOf { navBackStackEntry?.destination?.route }
                }

                val inSearchScreen by remember {
                    derivedStateOf { currentRoute?.startsWith("search/") == true }
                }
                val navigationItemRoutes = remember(navigationItems) {
                    navigationItems.map { it.route }.toSet()
                }

                val shouldShowNavigationBar = remember(currentRoute, navigationItemRoutes) {
                    currentRoute == null ||
                        navigationItemRoutes.contains(currentRoute) ||
                        currentRoute!!.startsWith("search/")
                }

                val isLandscape = configuration.containerDpSize.width > configuration.containerDpSize.height

                val showRail = isLandscape && !inSearchScreen

                val navPadding = if (shouldShowNavigationBar && !showRail) {
                    if (slimNav) SlimNavBarHeight else NavigationBarHeight
                } else {
                    0.dp
                }

                val navigationBarHeight by animateDpAsState(
                    targetValue = if (shouldShowNavigationBar && !showRail) NavigationBarHeight else 0.dp,
                    animationSpec = NavigationBarAnimationSpec,
                    label = "navBarHeight",
                )

                val playerBottomSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = bottomInset +
                        (if (!showRail && shouldShowNavigationBar) navPadding else 0.dp) +
                        (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                        MiniPlayerHeight,
                    expandedBound = maxHeight,
                )

                val playerAwareWindowInsets = remember(
                    bottomInset,
                    shouldShowNavigationBar,
                    playerBottomSheetState.isDismissed,
                    showRail,
                ) {
                    var bottom = bottomInset
                    if (shouldShowNavigationBar && !showRail) {
                        bottom += NavigationBarHeight
                    }
                    if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                    windowsInsets
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                }
                appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    }
                )

                val topAppBarScrollBehavior = appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    },
                )

                // Navigation tracking
                LaunchedEffect(navBackStackEntry) {
                    if (inSearchScreen) {
                        val searchQuery = withContext(Dispatchers.IO) {
                            if (navBackStackEntry?.arguments?.getString("query")!!.contains("%")) {
                                navBackStackEntry?.arguments?.getString("query")!!
                            } else {
                                URLDecoder.decode(
                                    navBackStackEntry?.arguments?.getString("query")!!,
                                    "UTF-8"
                                )
                            }
                        }
                        onQueryChange(
                            TextFieldValue(
                                searchQuery,
                                TextRange(searchQuery.length)
                            )
                        )
                    } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        onQueryChange(TextFieldValue())
                    }

                    // Reset scroll behavior for main navigation items
                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        if (navigationItems.fastAny { it.route == previousTab }) {
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    topAppBarScrollBehavior.state.resetHeightOffset()

                    // Track previous tab for animations
                    navController.currentBackStackEntry?.destination?.route?.let {
                        setPreviousTab(it)
                    }
                }

                LaunchedEffect(playerConnection) {
                    val player = playerConnection?.player ?: return@LaunchedEffect
                    if (player.currentMediaItem == null) {
                        if (!playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.dismiss()
                        }
                    } else {
                        if (playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.collapseSoft()
                        }
                    }
                }

                DisposableEffect(playerConnection, playerBottomSheetState) {
                    val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int,
                        ) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                mediaItem != null &&
                                playerBottomSheetState.isDismissed
                            ) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }
                    player.addListener(listener)
                    onDispose {
                        player.removeListener(listener)
                    }
                }

                var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(navBackStackEntry) {
                    shouldShowTopBar = navBackStackEntry?.destination?.route in topLevelScreens && 
                        navBackStackEntry?.destination?.route != "settings"
                }

                val coroutineScope = rememberCoroutineScope()
                var sharedSong: SongItem? by remember {
                    mutableStateOf(null)
                }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    if (pendingIntent != null) {
                        handleDeepLinkIntent(pendingIntent!!, navController)
                        pendingIntent = null
                    } else {
                        handleDeepLinkIntent(intent, navController)
                    }
                }

                DisposableEffect(Unit) {
                    val listener = Consumer<Intent> { intent ->
                        handleDeepLinkIntent(intent, navController)
                    }

                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                val currentTitleRes = remember(navBackStackEntry) {
                    when (navBackStackEntry?.destination?.route) {
                        Screens.Home.route -> R.string.home
                        Screens.Search.route -> R.string.search
                        Screens.Library.route -> R.string.filter_library
                        Screens.ListenTogether.route -> R.string.together
                        else -> null
                    }
                }

                var showAccountDialog by remember { mutableStateOf(false) }

                val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

                CompositionLocalProvider(
                    LocalDatabase provides database,
                    LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                    LocalPlayerConnection provides playerConnection,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalDownloadUtil provides downloadUtil,
                    LocalShimmerTheme provides ShimmerTheme,
                    LocalSyncUtils provides syncUtils,
                    LocalListenTogetherManager provides listenTogetherManager,
                    LocalChangelogState provides remember { mutableStateOf(false) },
                    LocalVoiceCommandController provides voiceCommandController,
                ) {

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            AnimatedVisibility(
                                visible = shouldShowTopBar,
                                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 200))
                            ) {
                                Row {
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text = currentTitleRes?.let { stringResource(it) } ?: "",
                                                style = MaterialTheme.typography.titleLarge,
                                            )
                                        },
                                        actions = {
                                            IconButton(onClick = { navController.navigate("history") }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.history),
                                                    contentDescription = stringResource(R.string.history)
                                                )
                                            }
                                            IconButton(onClick = { navController.navigate("stats") }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.stats),
                                                    contentDescription = stringResource(R.string.stats)
                                                )
                                            }
                                            IconButton(onClick = { showAccountDialog = true }) {
                                                BadgedBox(badge = {
                                                    if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                        Badge()
                                                    }
                                                }) {
                                                    if (accountImageUrl != null) {
                                                        AsyncImage(
                                                            model = accountImageUrl,
                                                            contentDescription = stringResource(R.string.account),
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                        )
                                                    } else {
                                                        Icon(
                                                            painter = painterResource(R.drawable.account),
                                                            contentDescription = stringResource(R.string.account),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        scrollBehavior = topAppBarScrollBehavior,
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            scrolledContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                                            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.windowInsetsPadding(
                                            if (showRail) {
                                                WindowInsets(left = NavigationBarHeight)
                                                    .add(cutoutInsets.only(WindowInsetsSides.Start))
                                            } else {
                                                cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                            }
                                        )
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            val onNavItemClick: (Screens, Boolean) -> Unit = remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                                { screen: Screens, isSelected: Boolean ->
                                    if (playerBottomSheetState.isExpanded) {
                                        playerBottomSheetState.collapseSoft()
                                    }

                                    if (isSelected) {
                                        navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                        coroutineScope.launch {
                                            topAppBarScrollBehavior.state.resetHeightOffset()
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                            
                            val onSearchLongClick: () -> Unit = remember(navController) {
                                {
                                    navController.navigate("recognition") {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            // Pre-calculate values for graphicsLayer to avoid reading state during composition
                            val navBarTotalHeight = bottomInset + NavigationBarHeight

                            if (!showRail && currentRoute != "wrapped") {
                                Box {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack
                                    )

                                    AppNavigationBar(
                                        navigationItems = navigationItems,
                                        currentRoute = currentRoute,
                                        onItemClick = onNavItemClick,
                                        pureBlack = pureBlack,
                                        slimNav = slimNav,
                                        onSearchLongClick = onSearchLongClick,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .height(bottomInset + navPadding)
                                            // Use graphicsLayer instead of offset to avoid recomposition
                                            // graphicsLayer runs during draw phase, not composition phase
                                            .graphicsLayer {
                                                val navBarHeightPx = navigationBarHeight.toPx()
                                                val totalHeightPx = navBarTotalHeight.toPx()

                                                translationY = if (navBarHeightPx == 0f) {
                                                    totalHeightPx
                                                } else {
                                                    // Read progress only during draw phase
                                                    val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                    val slideOffset = totalHeightPx * progress
                                                    val hideOffset = totalHeightPx * (1 - navBarHeightPx / NavigationBarHeight.toPx())
                                                    slideOffset + hideOffset
                                                }
                                            }
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(bottomInsetDp)
                                            // Use graphicsLayer for background color changes
                                            .graphicsLayer {
                                                val progress = playerBottomSheetState.progress
                                                alpha = if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                                            }
                                            .background(baseBg)
                                    )
                                }
                            } else {
                                if (currentRoute != "wrapped") {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .height(bottomInsetDp)
                                        // Use graphicsLayer for background color changes
                                        .graphicsLayer {
                                            val progress = playerBottomSheetState.progress
                                            alpha = if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                                        }
                                        .background(baseBg)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            val onRailItemClick: (Screens, Boolean) -> Unit = remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                                { screen: Screens, isSelected: Boolean ->
                                    if (playerBottomSheetState.isExpanded) {
                                        playerBottomSheetState.collapseSoft()
                                    }

                                    if (isSelected) {
                                        navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                        coroutineScope.launch {
                                            topAppBarScrollBehavior.state.resetHeightOffset()
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }

                            val onRailSearchLongClick: () -> Unit = remember(navController) {
                                {
                                    navController.navigate("recognition") {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            if (showRail && currentRoute != "wrapped") {
                                AppNavigationRail(
                                    navigationItems = navigationItems,
                                    currentRoute = currentRoute,
                                    onItemClick = onRailItemClick,
                                    pureBlack = pureBlack,
                                    onSearchLongClick = onRailSearchLongClick
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                // NavHost with animations (Material 3 Expressive style)
                                NavHost(
                                    navController = navController,
                                    startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                        NavigationTab.HOME -> Screens.Home
                                        NavigationTab.LIBRARY -> Screens.Library
                                        else -> Screens.Home
                                    }.route,
                                    // Enter Transition - smoother with smaller offset and longer duration
                                    enterTransition = {
                                        val currentRouteIndex = navigationItems.indexOfFirst {
                                            it.route == targetState.destination.route
                                        }
                                        val previousRouteIndex = navigationItems.indexOfFirst {
                                            it.route == initialState.destination.route
                                        }

                                        if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex)
                                            slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                        else
                                            slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                    },
                                    // Exit Transition - smoother with smaller offset and longer duration
                                    exitTransition = {
                                        val currentRouteIndex = navigationItems.indexOfFirst {
                                            it.route == initialState.destination.route
                                        }
                                        val targetRouteIndex = navigationItems.indexOfFirst {
                                            it.route == targetState.destination.route
                                        }

                                        if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex)
                                            slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                        else
                                            slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                    },
                                    // Pop Enter Transition - smoother with smaller offset and longer duration
                                    popEnterTransition = {
                                        val currentRouteIndex = navigationItems.indexOfFirst {
                                            it.route == targetState.destination.route
                                        }
                                        val previousRouteIndex = navigationItems.indexOfFirst {
                                            it.route == initialState.destination.route
                                        }

                                        if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex)
                                            slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                        else
                                            slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                    },
                                    // Pop Exit Transition - smoother with smaller offset and longer duration
                                    popExitTransition = {
                                        val currentRouteIndex = navigationItems.indexOfFirst {
                                            it.route == initialState.destination.route
                                        }
                                        val targetRouteIndex = navigationItems.indexOfFirst {
                                            it.route == targetState.destination.route
                                        }

                                        if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex)
                                            slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                        else
                                            slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                    },
                                    modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                                ) {
                                    navigationBuilder(
                                        navController = navController,
                                        scrollBehavior = topAppBarScrollBehavior,
                                        latestVersionName = latestVersionName,
                                        activity = this@MainActivity,
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                            }
                        }
                    }

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    BottomSheetPage(
                        state = LocalBottomSheetPageState.current,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    if (showAccountDialog) {
                        AccountSettingsDialog(
                            navController = navController,
                            onDismiss = {
                                showAccountDialog = false
                                homeViewModel.refresh()
                            },
                            latestVersionName = latestVersionName
                        )
                    }

                    val changelogState = LocalChangelogState.current

                    if (changelogState.value) {
                        ChangelogScreen(
                            onDismiss = {
                                changelogState.value = false
                                lifecycleScope.launch {
                                    dataStore.edit {
                                        it[ChangelogShownForVersionKey] = BuildConfig.VERSION_NAME
                                    }
                                }
                            }
                        )
                    }

                    sharedSong?.let { song ->
                        playerConnection?.let {
                            Dialog(
                                onDismissRequest = { sharedSong = null },
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Surface(
                                    modifier = Modifier.padding(24.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = AlertDialogDefaults.containerColor,
                                    tonalElevation = AlertDialogDefaults.TonalElevation,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        YouTubeSongMenu(
                                            song = song,
                                            navController = navController,
                                            onDismiss = { sharedSong = null },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Global voice command overlay (Siri/Gemini-like)
                    VoiceCommandOverlay(
                        state = voiceUiState,
                        onDismiss = { voiceCommandViewModel.dismissOverlay() },
                    )
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        val coroutineScope = lifecycle.coroutineScope

        val listenCode = uri.getQueryParameter("code")
            ?: uri.getQueryParameter("room")
            ?: uri.pathSegments.getOrNull(1)
        val isListenLink = uri.pathSegments.firstOrNull() == "listen" || uri.host?.equals("listen", ignoreCase = true) == true
        if (!listenCode.isNullOrBlank() && isListenLink) {
            val username = dataStore.get(ListenTogetherUsernameKey, "").ifBlank { "Guest" }
            listenTogetherManager.joinRoom(listenCode, username)
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                withContext(Dispatchers.Main) {
                                    navController.navigate("album/$browseId")
                                }
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> uri.lastPathSegment?.let { browseId ->
                navController.navigate("album/$browseId")
            }

            "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                navController.navigate("artist/$artistId")
            }

            "search" -> {
                uri.getQueryParameter("q")?.let {
                    navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                }
            }

            else -> {
                val videoId = when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                    else -> null
                }

                val playlistId = uri.getQueryParameter("list")

                if (videoId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.queue(listOf(videoId), playlistId).onSuccess { queue ->
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                        queue.firstOrNull()?.toMediaMetadata()
                                    )
                                )
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    }
                } else if (playlistId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.queue(null, playlistId).onSuccess { queue ->
                            val firstItem = queue.firstOrNull()
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = firstItem?.id, playlistId = playlistId),
                                        firstItem?.toMediaMetadata()
                                    )
                                )
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalListenTogetherManager = staticCompositionLocalOf<com.auramusic.app.listentogether.ListenTogetherManager?> { null }
val LocalIsPlayerExpanded = compositionLocalOf { false }
val LocalChangelogState = compositionLocalOf { mutableStateOf(false) }
