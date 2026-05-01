/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.auramusic.app.constants.DataSyncIdKey
import com.auramusic.app.constants.InnerTubeCookieKey
import com.auramusic.app.constants.VisitorDataKey
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
    var visitorData by rememberPreference(VisitorDataKey, "")
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var webView: WebView? = null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                                loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

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
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var backFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(56.dp)
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
    val context = LocalContext.current
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                    onNavigateUp?.invoke()
                    true
                } else {
                    false
                }
            },
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            TvSettingsHeader(title = "Account", onBackClick = onBackClick)
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
            )
        }
    }
}

/* -------------------------- About -------------------------- */

 @Composable
 fun TvAboutScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
     LazyColumn(
         modifier = Modifier
             .fillMaxSize()
             .onPreviewKeyEvent { event ->
                 if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                     onNavigateUp?.invoke()
                     true
                 } else {
                     false
                 }
             },
         contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
         verticalArrangement = Arrangement.spacedBy(24.dp),
     ) {
        item {
            TvSettingsHeader(title = "About", onBackClick = onBackClick)
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
                        text = "AuraMusic",
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

/* -------------------------- Updater -------------------------- */

@Composable
 fun TvUpdaterScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            TvSettingsHeader(title = "Updates", onBackClick = onBackClick)
        }
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
                        text = "Current version",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "AuraMusic TV checks for updates automatically. " +
                            "Visit the GitHub releases page on a phone to manually download a newer build.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/* -------------------------- Playback / Content placeholders -------------------------- */

@Composable
 fun TvPlaybackSettingsScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
    TvPlaceholderSettings(
        title = "Playback",
        body = "Audio quality, gapless playback and other playback settings " +
            "follow the values configured on the mobile app for now.",
        onBackClick = onBackClick,
    )
}

@Composable
 fun TvContentSettingsScreen(onBackClick: () -> Unit, focusRequester: FocusRequester? = null, onNavigateUp: (() -> Unit)? = null) {
    TvPlaceholderSettings(
        title = "Content",
        body = "Content preferences (hide explicit, hide videos, sync rules) follow the values configured on the mobile app for now.",
        onBackClick = onBackClick,
    )
}

@Composable
private fun TvPlaceholderSettings(title: String, body: String, onBackClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { TvSettingsHeader(title = title, onBackClick = onBackClick) }
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
fun TvSettingsHeader(title: String, onBackClick: () -> Unit) {
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }
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
                .onFocusChanged { focused = it.isFocused }
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
