/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.auramusic.app.BuildConfig
import com.auramusic.app.constants.AccountChannelHandleKey
import com.auramusic.app.constants.AccountEmailKey
import com.auramusic.app.constants.AccountNameKey
import com.auramusic.app.constants.AudioNormalizationKey
import com.auramusic.app.constants.AudioQuality
import com.auramusic.app.constants.AudioQualityKey
import com.auramusic.app.constants.CrossfadeDurationKey
import com.auramusic.app.constants.CrossfadeEnabledKey
import com.auramusic.app.constants.DataSyncIdKey
import com.auramusic.app.constants.InnerTubeCookieKey
import com.auramusic.app.constants.LateNightModeKey
import com.auramusic.app.constants.PreferredLyricsProvider
import com.auramusic.app.constants.SeekExtraSeconds
import com.auramusic.app.constants.SkipSilenceKey
import com.auramusic.app.constants.VideoModeEnabledKey
import com.auramusic.app.constants.VisitorDataKey
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.utils.reportException
import com.auramusic.app.viewmodels.AccountSettingsViewModel
import com.auramusic.app.viewmodels.HomeViewModel
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.utils.parseCookieString
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

/* -------------------------- Login (WebView) -------------------------- */

@SuppressLint("SetJavaScriptEnabled")
@OptIn(DelicateCoroutinesApi::class)
@Composable
 fun TvLoginScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
    BackHandler { onBackClick() }
    var visitorData by rememberPreference(VisitorDataKey, "")
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current

    var webView: WebView? = null
    val backFocus = focusRequester ?: remember { FocusRequester() }
    var webViewFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { backFocus.requestFocus() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp)
                    .focusRequester(remember { FocusRequester() })
                    .onFocusChanged { state ->
                        webViewFocused = state.isFocused
                    }
                    .onPreviewKeyEvent { event ->
                        // Forward D-pad events to WebView for form navigation
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown, Key.DirectionUp,
                                Key.DirectionLeft, Key.DirectionRight -> {
                                    webView?.let { wv ->
                                        // Inject JavaScript to simulate focus navigation in the web form
                                        val direction = when (event.key) {
                                            Key.DirectionDown -> "next"
                                            Key.DirectionUp -> "previous"
                                            Key.DirectionLeft -> "previous"
                                            Key.DirectionRight -> "next"
                                            else -> "next"
                                        }
                                        wv.evaluateJavascript("""
                                            (function() {
                                                var focused = document.activeElement;
                                                if (focused && (focused.tagName === 'INPUT' || focused.tagName === 'BUTTON')) {
                                                    var elements = Array.from(document.querySelectorAll('input[type="email"], input[type="password"], input[type="text"], button, [role="button"]'));
                                                    var idx = elements.indexOf(focused);
                                                    if (idx >= 0) {
                                                        var nextIdx = '$direction' === 'next' ? idx + 1 : idx - 1;
                                                        if (nextIdx >= 0 && nextIdx < elements.length) {
                                                            elements[nextIdx].focus();
                                                            elements[nextIdx].scrollIntoView({behavior: 'smooth', block: 'center'});
                                                        }
                                                    }
                                                }
                                            })();
                                        """, null)
                                        true
                                    } ?: false
                                }
                                Key.DirectionCenter, Key.Enter -> {
                                    webView?.let { wv ->
                                        // Click the currently focused element
                                        wv.evaluateJavascript("""
                                            (function() {
                                                var focused = document.activeElement;
                                                if (focused && (focused.tagName === 'BUTTON' || focused.getAttribute('role') === 'button')) {
                                                    focused.click();
                                                } else if (focused && focused.tagName === 'INPUT') {
                                                    // For input fields, try to find and click the "Next" button
                                                    var nextBtn = document.querySelector('button[type="submit"], #identifierNext button, #passwordNext button, [data-idom-class]');
                                                    if (nextBtn) nextBtn.click();
                                                }
                                            })();
                                        """, null)
                                        true
                                    } ?: false
                                }
                                Key.Back -> {
                                    webView?.let { wv ->
                                        if (wv.canGoBack()) {
                                            wv.goBack()
                                            true
                                        } else {
                                            onBackClick()
                                            true
                                        }
                                    } ?: false
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                // Inject D-pad navigation helper script
                                view.evaluateJavascript("""
                                    (function() {
                                        // Make form elements focusable via D-pad
                                        var style = document.createElement('style');
                                        style.textContent = '*:focus { outline: 3px solid #1a73e8 !important; outline-offset: 2px; }';
                                        document.head.appendChild(style);

                                        // Auto-focus first input field after page load
                                        setTimeout(function() {
                                            var firstInput = document.querySelector('input[type="email"], input[type="text"]');
                                            if (firstInput) firstInput.focus();
                                        }, 1000);
                                    })();
                                """, null)

                                view.loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                                view.loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                                if (url?.startsWith("https://music.youtube.com") == true) {
                                    innerTubeCookie =
                                        CookieManager.getInstance().getCookie(url) ?: ""
                                    coroutineScope.launch {
                                        YouTube.accountInfo().onSuccess {
                                            accountName = it.name
                                            accountEmail = it.email.orEmpty()
                                            accountChannelHandle = it.channelHandle.orEmpty()
                                        }.onFailure {
                                            reportException(it)
                                        }
                                    }
                                }
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            // Enable DOM storage for Google login
                            domStorageEnabled = true
                            databaseEnabled = true
                            // Allow mixed content for login pages
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onRetrieveVisitorData(newVisitorData: String?) {
                                if (newVisitorData != null) {
                                    visitorData = newVisitorData
                                }
                            }

                            @JavascriptInterface
                            fun onRetrieveDataSyncId(newDataSyncId: String?) {
                                if (newDataSyncId != null) {
                                    dataSyncId = newDataSyncId.substringBefore("||")
                                }
                            }
                        }, "Android")
                        // Make WebView focusable for D-pad navigation
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()
                        webView = this
                        loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
                    }
                }
            )

            // Top bar with back button + title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                            onNavigateUp?.invoke()
                            true
                        } else if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                            // Move focus to WebView
                            webView?.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var backFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(56.dp)
                        .focusRequester(backFocus)
                        .onFocusChanged { backFocused = it.isFocused }
                        .border(
                            width = if (backFocused) 3.dp else 0.dp,
                            color = if (backFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape,
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.size(16.dp))
                Text(
                    text = "Sign in to YouTube Music",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}

/* -------------------------- Account Settings -------------------------- */

@Composable
 fun TvAccountSettingsScreen(
     onBackClick: () -> Unit,
     onLoginClick: () -> Unit,
     focusRequester: FocusRequester? = null,
     onNavigateUp: (() -> Unit)? = null,
 ) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    var backButtonFocused by remember { mutableStateOf(false) }
    val firstFocus = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (backButtonFocused) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 64.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            TvSettingsHeader(
                title = "Account",
                onBackClick = onBackClick,
                focusRequester = firstFocus,
                onFocusChange = { backButtonFocused = it },
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoggedIn && accountImageUrl != null) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            )
                        } else {
                            Text(
                                text = accountName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) accountName.ifEmpty { "Signed in" } else "Not signed in",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isLoggedIn)
                                "Library, liked songs, playlists and subscriptions sync from YouTube Music"
                            else
                                "Sign in to sync your liked songs, albums, playlists and subscriptions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            TvSettingsCategoryItem(
                title = if (isLoggedIn) "Sign out" else "Sign in to YouTube Music",
                subtitle = if (isLoggedIn)
                    "Disconnect your account and clear synced content"
                else
                    "Use your Google account to sign in",
                onClick = {
                    if (isLoggedIn) {
                        accountSettingsViewModel.logoutAndClearSyncedContent(
                            context,
                            onInnerTubeCookieChange,
                        )
                    } else {
                        onLoginClick()
                    }
                },
                icon = if (isLoggedIn)
                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.Logout
                else
                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.Login,
            )
        }

        if (isLoggedIn) {
            item {
                TvSettingsCategoryItem(
                    title = "Refresh library",
                    subtitle = "Re-sync liked songs, playlists, and subscribed artists",
                    onClick = {
                        // Re-fetching is automatic on Library screen visit
                    },
                    icon = androidx.compose.material.icons.Icons.Filled.Refresh,
                )
            }
        }
    }
}

/* -------------------------- About -------------------------- */

 @Composable
 fun TvAboutScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
    BackHandler { onBackClick() }
    val firstFocus = focusRequester ?: remember { FocusRequester() }
     LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
     var backButtonFocused by remember { mutableStateOf(false) }

     LazyColumn(
         modifier = Modifier
             .fillMaxSize()
             .onPreviewKeyEvent { event ->
                 if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                     if (backButtonFocused) {
                         onNavigateUp?.invoke()
                         true
                     } else {
                         false
                     }
                 } else {
                     false
                 }
             },
         contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
         verticalArrangement = Arrangement.spacedBy(24.dp),
     ) {
        item {
            TvSettingsHeader(
                title = "About",
                onBackClick = onBackClick,
                focusRequester = firstFocus,
                onFocusChange = { backButtonFocused = it },
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                     Text(
                         text = "AuraMusic Tv",
                         style = MaterialTheme.typography.headlineMedium,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.onSurface,
                     )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (BuildConfig.DEBUG) "DEBUG" else "RELEASE",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = BuildConfig.ARCHITECTURE.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Licensed under GPL-3.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Auramusic Project (C) 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/* -------------------------- Storage -------------------------- */

@Composable
 fun TvStorageSettingsScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val firstFocus = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    var backButtonFocused by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (backButtonFocused) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            TvSettingsHeader(
                title = "Storage",
                onBackClick = onBackClick,
                focusRequester = firstFocus,
                onFocusChange = { backButtonFocused = it },
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Storage Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = "AuraMusic TV is designed to prevent storage accumulation like Spotify TV. " +
                            "Lyrics are fetched fresh each time and not cached to disk. Image cache is bounded to prevent growth.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            TvSettingsCategoryItem(
                title = "Clear Cache",
                subtitle = "Clear image cache and temporary files",
                onClick = {
                    // Note: In a real implementation, this would call the clearAccumulatedStorage function
                    // For now, just show a message or implement the clearing logic
                },
                icon = Icons.Filled.CleaningServices,
            )
        }
    }
}

/* -------------------------- Updater -------------------------- */

@Composable
 fun TvUpdaterScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
    BackHandler { onBackClick() }
    val firstFocus = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    var backButtonFocused by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var releaseInfo by remember { mutableStateOf<com.auramusic.app.utils.ReleaseInfo?>(null) }
    var hasUpdate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun runCheck(force: Boolean) {
        if (isChecking) return
        scope.launch {
            isChecking = true
            error = null
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.auramusic.app.utils.Updater
                    .checkForUpdate(forceRefresh = force)
                    .onSuccess { (info, available) ->
                        releaseInfo = info
                        hasUpdate = available
                    }
                    .onFailure {
                        error = it.message ?: "Failed to check for updates"
                    }
            }
            isChecking = false
        }
    }

    // Always check on screen open so a freshly published TV release surfaces
    // here without the user having to manually press refresh.
    LaunchedEffect(Unit) { runCheck(force = false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (backButtonFocused) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            TvSettingsHeader(
                title = "Updates",
                onBackClick = onBackClick,
                focusRequester = firstFocus,
                onFocusChange = { backButtonFocused = it },
            )
        }

        // Current version
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Installed version",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "AuraMusic Tv v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Update status
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        isChecking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Checking for updates…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        error != null -> {
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        releaseInfo == null -> {
                            Text(
                                text = "No release information available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        hasUpdate -> {
                            Text(
                                text = "Update available",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "${releaseInfo?.versionName} • released ${
                                    releaseInfo?.releaseDate?.take(10) ?: ""
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val notes = releaseInfo?.description?.takeIf { it.isNotBlank() }
                            if (notes != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 8,
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "You're on the latest version",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Latest released: ${releaseInfo?.versionName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Action buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvUpdaterButton(
                    label = if (isChecking) "Checking…" else "Check again",
                    onClick = { runCheck(force = true) },
                    primary = false,
                )
                if (hasUpdate && releaseInfo != null) {
                    TvUpdaterButton(
                        label = "Download AuraMusic Tv",
                        onClick = {
                            val info = releaseInfo ?: return@TvUpdaterButton
                            val url = com.auramusic.app.utils.Updater
                                .getDownloadUrlForCurrentVariant(info, preferredVariant = "tv")
                            if (url != null) {
                                runCatching {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url),
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            }
                        },
                        primary = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvUpdaterButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
        )
    }
}

/* -------------------------- Playback Settings -------------------------- */

@Composable
fun TvPlaybackSettingsScreen(
    onBackClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
) {
    BackHandler { onBackClick() }
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        com.auramusic.app.constants.AudioQualityKey,
        com.auramusic.app.constants.AudioQuality.AUTO,
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        com.auramusic.app.constants.AudioNormalizationKey, true,
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        com.auramusic.app.constants.SkipSilenceKey, false,
    )
    val (lateNightMode, onLateNightModeChange) = rememberPreference(
        com.auramusic.app.constants.LateNightModeKey, false,
    )
    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(
        com.auramusic.app.constants.CrossfadeEnabledKey, false,
    )
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(
        com.auramusic.app.constants.CrossfadeDurationKey, 5f,
    )
    val (videoModeEnabled, onVideoModeEnabledChange) = rememberPreference(
        com.auramusic.app.constants.VideoModeEnabledKey, true,
    )
    val (seekExtraSeconds, onSeekExtraSecondsChange) = rememberPreference(
        com.auramusic.app.constants.SeekExtraSeconds, false,
    )

    val firstFocus = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    var backButtonFocused by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (backButtonFocused) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TvSettingsHeader(
                title = "Playback",
                onBackClick = onBackClick,
                focusRequester = firstFocus,
                onFocusChange = { backButtonFocused = it },
            )
        }

        item {
            Text(
                text = "AUDIO",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Audio Quality",
                subtitle = when (audioQuality) {
                    com.auramusic.app.constants.AudioQuality.AUTO -> "Automatic (recommended)"
                    com.auramusic.app.constants.AudioQuality.HIGH -> "High quality"
                    com.auramusic.app.constants.AudioQuality.LOW -> "Low quality (saves data)"
                },
                onClick = {
                    val next = when (audioQuality) {
                        com.auramusic.app.constants.AudioQuality.AUTO -> com.auramusic.app.constants.AudioQuality.HIGH
                        com.auramusic.app.constants.AudioQuality.HIGH -> com.auramusic.app.constants.AudioQuality.LOW
                        com.auramusic.app.constants.AudioQuality.LOW -> com.auramusic.app.constants.AudioQuality.AUTO
                    }
                    onAudioQualityChange(next)
                },
                icon = Icons.Filled.Tune,
            )
        }

        item {
            TvContentToggleRow(
                title = "Audio Normalization",
                subtitle = "Normalize volume levels across songs",
                checked = audioNormalization,
                onCheckedChange = onAudioNormalizationChange,
                icon = Icons.Filled.Tune,
            )
        }

        item {
            TvContentToggleRow(
                title = "Skip Silence",
                subtitle = "Automatically skip silent parts of songs",
                checked = skipSilence,
                onCheckedChange = onSkipSilenceChange,
                icon = Icons.Filled.FastForward,
            )
        }

        item {
            TvContentToggleRow(
                title = "Late Night Mode",
                subtitle = "Reduce volume differences for quiet listening",
                checked = lateNightMode,
                onCheckedChange = onLateNightModeChange,
                icon = Icons.Filled.DarkMode,
            )
        }

        item {
            Text(
                text = "CROSSFADE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 12.dp, bottom = 4.dp),
            )
        }

        item {
            TvContentToggleRow(
                title = "Crossfade",
                subtitle = "Smooth transitions between songs",
                checked = crossfadeEnabled,
                onCheckedChange = onCrossfadeEnabledChange,
                icon = Icons.Filled.Tune,
            )
        }

        if (crossfadeEnabled) {
            item {
                TvSliderRow(
                    title = "Crossfade Duration",
                    subtitle = "${crossfadeDuration.toInt()} seconds",
                    value = crossfadeDuration,
                    valueRange = 1f..12f,
                    steps = 10,
                    onValueChange = onCrossfadeDurationChange,
                )
            }
        }

        item {
            Text(
                text = "OTHER",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 12.dp, bottom = 4.dp),
            )
        }

        item {
            TvContentToggleRow(
                title = "Video Mode",
                subtitle = "Play music videos when available",
                checked = videoModeEnabled,
                onCheckedChange = onVideoModeEnabledChange,
                icon = Icons.Filled.MusicNote,
            )
        }

        item {
            TvContentToggleRow(
                title = "Seek Extra Seconds",
                subtitle = "Show 10-second seek buttons on player",
                checked = seekExtraSeconds,
                onCheckedChange = onSeekExtraSecondsChange,
                icon = Icons.Filled.FastForward,
            )
        }

        item {
            Text(
                text = "SPONSORBLOCK",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 12.dp, bottom = 4.dp),
            )
        }

        item {
            val (sbEnabled, onSbEnabledChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockEnabledKey, false,
            )
            TvContentToggleRow(
                title = "SponsorBlock",
                subtitle = if (sbEnabled) "Auto-skip sponsor segments in videos" else "Skip sponsor segments (powered by SponsorBlock)",
                checked = sbEnabled,
                onCheckedChange = onSbEnabledChange,
                icon = Icons.Filled.Block,
            )
        }

        item {
            val (sbSponsor, onSbSponsorChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockSkipSponsorKey, true,
            )
            val (sbSelfPromo, onSbSelfPromoChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockSkipSelfPromoKey, true,
            )
            val (sbInteraction, onSbInteractionChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockSkipInteractionKey, true,
            )
            val (sbIntro, onSbIntroChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockSkipIntroKey, true,
            )
            val (sbOutro, onSbOutroChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockSkipOutroKey, true,
            )
            val (sbPreview, onSbPreviewChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockSkipPreviewKey, true,
            )
            val (sbMusicOffTopic, onSbMusicOffTopicChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockSkipMusicOffTopicKey, true,
            )
            val (sbFiller, onSbFillerChange) = rememberPreference(
                com.auramusic.app.constants.SponsorBlockSkipFillerKey, true,
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Skip Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Choose which segment types to auto-skip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TvContentToggleRow(title = "Sponsor", subtitle = "Paid promotion, not necessarily an ad", checked = sbSponsor, onCheckedChange = onSbSponsorChange, icon = Icons.Filled.Tune)
                    TvContentToggleRow(title = "Self-promotion", subtitle = "Unpaid self-promotion", checked = sbSelfPromo, onCheckedChange = onSbSelfPromoChange, icon = Icons.Filled.Tune)
                    TvContentToggleRow(title = "Interaction reminder", subtitle = "Subscribe, like, etc.", checked = sbInteraction, onCheckedChange = onSbInteractionChange, icon = Icons.Filled.Tune)
                    TvContentToggleRow(title = "Intro", subtitle = "Intro sequences", checked = sbIntro, onCheckedChange = onSbIntroChange, icon = Icons.Filled.Tune)
                    TvContentToggleRow(title = "Outro", subtitle = "End cards/outro", checked = sbOutro, onCheckedChange = onSbOutroChange, icon = Icons.Filled.Tune)
                    TvContentToggleRow(title = "Preview", subtitle = "Recap of what you've seen", checked = sbPreview, onCheckedChange = onSbPreviewChange, icon = Icons.Filled.Tune)
                    TvContentToggleRow(title = "Non-music", subtitle = "Parts where the song is not playing", checked = sbMusicOffTopic, onCheckedChange = onSbMusicOffTopicChange, icon = Icons.Filled.Tune)
                    TvContentToggleRow(title = "Filler", subtitle = "Tangents or filler sections", checked = sbFiller, onCheckedChange = onSbFillerChange, icon = Icons.Filled.Tune)
                }
            }
        }
    }
}

@Composable
  fun TvContentSettingsScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
    BackHandler { onBackClick() }

    // Auto-load-more matches the mobile "Auto load more songs" toggle: when
    // the queue reaches the end the app will automatically extend it with
    // related items.
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        key = com.auramusic.app.constants.AutoLoadMoreKey,
        defaultValue = true,
    )
    // SimilarContent matches the mobile "Enable similar content" toggle and
    // controls whether similar songs are surfaced in the queue.
    val (similarContentEnabled, onSimilarContentEnabledChange) = rememberPreference(
        key = com.auramusic.app.constants.SimilarContent,
        defaultValue = true
    )
    val (preferredProvider, onPreferredProviderChange) = rememberEnumPreference(
        key = com.auramusic.app.constants.PreferredLyricsProviderKey,
        defaultValue = PreferredLyricsProvider.BETTER_LYRICS,
    )
    val (enableKugou, onEnableKugouChange) = rememberPreference(
        key = com.auramusic.app.constants.EnableKugouKey,
        defaultValue = true,
    )
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(
        key = com.auramusic.app.constants.EnableLrcLibKey,
        defaultValue = true,
    )
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(
        key = com.auramusic.app.constants.EnableBetterLyricsKey,
        defaultValue = true,
    )
    val (enableSimpMusic, onEnableSimpMusicChange) = rememberPreference(
        key = com.auramusic.app.constants.EnableSimpMusicKey,
        defaultValue = true,
    )
    val (enableRushLyrics, onEnableRushLyricsChange) = rememberPreference(
        key = com.auramusic.app.constants.EnableRushLyricsKey,
        defaultValue = true,
    )

    val firstFocus = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    var backButtonFocused by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (backButtonFocused) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            TvSettingsHeader(
                title = "Content",
                onBackClick = onBackClick,
                focusRequester = firstFocus,
                onFocusChange = { backButtonFocused = it },
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Queue Behavior",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = "Configure how the app behaves when playing songs from Quick Picks, Keep Listening, albums, playlists, and artist pages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Auto load more songs
        item {
            TvContentToggleRow(
                title = "Auto load more songs",
                subtitle = "Automatically add more songs when the end of the queue is reached, if possible",
                checked = autoLoadMore,
                onCheckedChange = onAutoLoadMoreChange,
                icon = Icons.AutoMirrored.Filled.QueueMusic,
            )
        }

        // Enable similar content
        item {
            TvContentToggleRow(
                title = "Enable similar content",
                subtitle = "Show similar songs in the queue",
                checked = similarContentEnabled,
                onCheckedChange = onSimilarContentEnabledChange,
                icon = Icons.Filled.Tune,
            )
        }

        item {
            Text(
                text = "LYRICS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 12.dp, bottom = 4.dp),
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Preferred lyrics provider",
                subtitle = preferredProvider.tvLabel,
                onClick = {
                    val providers = PreferredLyricsProvider.values()
                    val nextIndex = (providers.indexOf(preferredProvider) + 1) % providers.size
                    onPreferredProviderChange(providers[nextIndex])
                },
                icon = Icons.Filled.Lyrics,
            )
        }

        item {
            TvContentToggleRow(
                title = "BetterLyrics",
                subtitle = "Allow BetterLyrics results",
                checked = enableBetterLyrics,
                onCheckedChange = onEnableBetterLyricsChange,
                icon = Icons.Filled.Lyrics,
            )
        }

        item {
            TvContentToggleRow(
                title = "RushLyrics",
                subtitle = "Allow RushLyrics results",
                checked = enableRushLyrics,
                onCheckedChange = onEnableRushLyricsChange,
                icon = Icons.Filled.Lyrics,
            )
        }

        item {
            TvContentToggleRow(
                title = "LrcLib",
                subtitle = "Allow LrcLib results",
                checked = enableLrclib,
                onCheckedChange = onEnableLrclibChange,
                icon = Icons.Filled.Lyrics,
            )
        }

        item {
            TvContentToggleRow(
                title = "KuGou",
                subtitle = "Allow KuGou results",
                checked = enableKugou,
                onCheckedChange = onEnableKugouChange,
                icon = Icons.Filled.Lyrics,
            )
        }

        item {
            TvContentToggleRow(
                title = "SimpMusic",
                subtitle = "Allow SimpMusic results",
                checked = enableSimpMusic,
                onCheckedChange = onEnableSimpMusicChange,
                icon = Icons.Filled.Lyrics,
            )
        }
    }
}

private val PreferredLyricsProvider.tvLabel: String
    get() = when (this) {
        PreferredLyricsProvider.LRCLIB -> "LrcLib"
        PreferredLyricsProvider.KUGOU -> "KuGou"
        PreferredLyricsProvider.BETTER_LYRICS -> "BetterLyrics"
        PreferredLyricsProvider.SIMPMUSIC -> "SimpMusic"
        PreferredLyricsProvider.RUSH_LYRICS -> "RushLyrics"
    }

/**
 * Standard focusable, clickable toggle row used inside the TV Content
 * settings screen. Wraps a Surface + Switch and routes both card clicks
 * and direct switch toggles to [onCheckedChange].
 */
@Composable
private fun TvContentToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    var rowFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .onFocusChanged { rowFocused = it.isFocused }
            .border(
                width = if (rowFocused) 3.dp else 0.dp,
                color = if (rowFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun TvSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    var rowFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .onFocusChanged { rowFocused = it.isFocused }
            .border(
                width = if (rowFocused) 3.dp else 0.dp,
                color = if (rowFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TvPlaceholderSettings(
    title: String,
    body: String,
    onBackClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
) {
    val firstFocus = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    var backButtonFocused by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (backButtonFocused) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            TvSettingsHeader(
                title = title,
                onBackClick = onBackClick,
                focusRequester = firstFocus,
                onFocusChange = { backButtonFocused = it },
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

/* -------------------------- Shared header -------------------------- */

@Composable
fun TvSettingsHeader(
    title: String,
    onBackClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val backFocus = focusRequester ?: remember { FocusRequester() }
    if (focusRequester == null) {
        LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }
    }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        label = "tvBackScale",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(64.dp)
                .focusRequester(backFocus)
                .onFocusChanged { state ->
                    focused = state.isFocused
                    onFocusChange?.invoke(state.isFocused)
                    if (state.isFocused) onFocused?.invoke()
                }
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .border(
                    width = if (focused) 3.dp else 0.dp,
                    color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape,
                ),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.size(64.dp))
    }
}

/* -------------------------- System Settings -------------------------- */

@Composable
fun TvSystemSettingsScreen(
    onBackClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onNavigateUp: (() -> Unit)? = null,
) {
    BackHandler { onBackClick() }
    val (hdmiCecEnabled, onHdmiCecEnabledChange) = rememberPreference(
        com.auramusic.app.constants.TvHdmiCecEnabledKey, true,
    )
    val (autoPlayOnBoot, onAutoPlayOnBootChange) = rememberPreference(
        com.auramusic.app.constants.TvAutoPlayOnBootKey, false,
    )
    val (screenSaverTimeout, onScreenSaverTimeoutChange) = rememberPreference(
        com.auramusic.app.constants.TvScreenSaverTimeoutKey, 0,
    )
    val (audioOutput, onAudioOutputChange) = rememberPreference(
        com.auramusic.app.constants.TvAudioOutputKey, "auto",
    )

    val firstFocus = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    var backButtonFocused by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    if (backButtonFocused) {
                        onNavigateUp?.invoke()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TvSettingsHeader(
                title = "System",
                onBackClick = onBackClick,
                focusRequester = firstFocus,
                onFocusChange = { backButtonFocused = it },
            )
        }

        item {
            Text(
                text = "TV INTEGRATION",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        item {
            TvContentToggleRow(
                title = "HDMI-CEC Control",
                subtitle = "Allow TV remote to control playback via HDMI-CEC",
                checked = hdmiCecEnabled,
                onCheckedChange = onHdmiCecEnabledChange,
                icon = Icons.Filled.Tune,
            )
        }

        item {
            TvContentToggleRow(
                title = "Auto-play on Boot",
                subtitle = "Resume playback when the TV starts",
                checked = autoPlayOnBoot,
                onCheckedChange = onAutoPlayOnBootChange,
                icon = Icons.Filled.PlayArrow,
            )
        }

        item {
            val (keepScreenOn, onKeepScreenOnChange) = rememberPreference(
                com.auramusic.app.constants.KeepScreenOn, false,
            )
            TvContentToggleRow(
                title = "Keep Screen On",
                subtitle = "Prevent screen from dimming while music plays",
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
                icon = Icons.Filled.DarkMode,
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Screen Saver Timeout",
                subtitle = when (screenSaverTimeout) {
                    0 -> "Never"
                    5 -> "5 minutes"
                    10 -> "10 minutes"
                    15 -> "15 minutes"
                    30 -> "30 minutes"
                    60 -> "1 hour"
                    else -> "Disabled"
                },
                onClick = {
                    val next = when (screenSaverTimeout) {
                        0 -> 5
                        5 -> 10
                        10 -> 15
                        15 -> 30
                        30 -> 60
                        else -> 0
                    }
                    onScreenSaverTimeoutChange(next)
                },
                icon = Icons.Filled.DarkMode,
            )
        }

        item {
            Text(
                text = "AUDIO OUTPUT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }

        item {
            TvSettingsCategoryItem(
                title = "Audio Output",
                subtitle = when (audioOutput) {
                    "auto" -> "Automatic (system default)"
                    "tv_speakers" -> "TV Speakers"
                    "optical" -> "Optical / S/PDIF"
                    "hdmi_arc" -> "HDMI ARC"
                    else -> "Automatic"
                },
                onClick = {
                    val next = when (audioOutput) {
                        "auto" -> "tv_speakers"
                        "tv_speakers" -> "optical"
                        "optical" -> "hdmi_arc"
                        else -> "auto"
                    }
                    onAudioOutputChange(next)
                },
                icon = Icons.Filled.MusicNote,
            )
        }
    }
}
