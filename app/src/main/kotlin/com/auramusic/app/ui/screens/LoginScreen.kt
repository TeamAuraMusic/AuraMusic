/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.utils.parseCookieString
import com.auramusic.app.LocalPlayerAwareWindowInsets
import com.auramusic.app.R
import com.auramusic.app.constants.AccountChannelHandleKey
import com.auramusic.app.constants.AccountEmailKey
import com.auramusic.app.constants.AccountNameKey
import com.auramusic.app.constants.DataSyncIdKey
import com.auramusic.app.constants.InnerTubeCookieKey
import com.auramusic.app.constants.VisitorDataKey
import com.auramusic.app.ui.component.IconButton
import com.auramusic.app.ui.utils.backToMain
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.utils.reportException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    var visitorData by rememberPreference(VisitorDataKey, "")
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    var webView: WebView? = null
    val lastAccountInfoFetchSession = remember { AtomicReference<String?>(null) }
    val latestCookie = remember { AtomicReference(innerTubeCookie) }
    val latestVisitorData = remember { AtomicReference(visitorData) }
    val latestDataSyncId = remember { AtomicReference(dataSyncId) }

    fun refreshAccountInfoIfReady() {
        val cookie = latestCookie.get().takeIf { "SAPISID" in parseCookieString(it) } ?: return
        val normalizedDataSyncId = latestDataSyncId.get()
            .substringBefore("||")
            .takeIf { it.isNotBlank() && it != "null" }
            ?: return
        val sessionKey = "$cookie|$normalizedDataSyncId"
        if (lastAccountInfoFetchSession.get() == sessionKey) return
        lastAccountInfoFetchSession.set(sessionKey)

        YouTube.cookie = cookie
        YouTube.visitorData = latestVisitorData.get().takeIf { it.isNotBlank() }
        YouTube.dataSyncId = normalizedDataSyncId

        coroutineScope.launch {
            YouTube.accountInfo().onSuccess {
                accountName = it.name
                accountEmail = it.email.orEmpty()
                accountChannelHandle = it.channelHandle.orEmpty()
            }.onFailure {
                lastAccountInfoFetchSession.set(null)
                reportException(it)
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                        loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                        if (url?.startsWith("https://music.youtube.com") == true) {
                            CookieManager.getInstance().flush()
                            val cookie = CookieManager.getInstance().getCookie(url).orEmpty()
                            latestCookie.set(cookie)
                            innerTubeCookie = cookie
                            YouTube.cookie = cookie
                            YouTube.visitorData = latestVisitorData.get().takeIf { it.isNotBlank() }
                            YouTube.dataSyncId = latestDataSyncId.get().takeIf { it.isNotBlank() }
                            refreshAccountInfoIfReady()
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
                        if (!newVisitorData.isNullOrBlank() && newVisitorData != "null") {
                            latestVisitorData.set(newVisitorData)
                            visitorData = newVisitorData
                            YouTube.visitorData = newVisitorData
                            refreshAccountInfoIfReady()
                        }
                    }
                    @JavascriptInterface
                    fun onRetrieveDataSyncId(newDataSyncId: String?) {
                        val normalizedDataSyncId = newDataSyncId
                            ?.substringBefore("||")
                            ?.takeIf { it.isNotBlank() && it != "null" }
                        if (normalizedDataSyncId != null) {
                            latestDataSyncId.set(normalizedDataSyncId)
                            dataSyncId = normalizedDataSyncId
                            YouTube.dataSyncId = normalizedDataSyncId
                            refreshAccountInfoIfReady()
                        }
                    }
                }, "Android")
                webView = this
                loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
            }
        }
    )

    TopAppBar(
        title = { Text(stringResource(R.string.login)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
