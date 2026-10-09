/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens

import android.annotation.SuppressLint
import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.auramusic.app.R
import com.auramusic.app.auth.GoogleAccountLogin
import com.auramusic.app.constants.AccountChannelHandleKey
import com.auramusic.app.constants.AccountEmailKey
import com.auramusic.app.constants.AccountNameKey
import com.auramusic.app.constants.DataSyncIdKey
import com.auramusic.app.constants.InnerTubeCookieKey
import com.auramusic.app.constants.VisitorDataKey
import com.auramusic.app.utils.rememberPreference
import com.auramusic.app.utils.reportException
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.utils.parseCookieString
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * The account-credential WebView shared by the full-screen login route and the popup
 * dialog. It signs in through accounts.google.com exactly as before, copies the resulting
 * music.youtube.com cookies into the session, and reports success once the account probe
 * answers — so the popup can close itself the moment the session is real.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebView(
    onSignedIn: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var visitorData by rememberPreference(VisitorDataKey, "")
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    var webView by remember { mutableStateOf<WebView?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val signedInReported = remember { AtomicReference(false) }
    val lastAccountInfoFetchSession = remember { AtomicReference<String?>(null) }
    val latestCookie = remember { AtomicReference(innerTubeCookie) }
    val latestVisitorData = remember { AtomicReference(visitorData) }
    val latestDataSyncId = remember { AtomicReference(dataSyncId) }
    val onSignedInState = remember { AtomicReference(onSignedIn) }
    onSignedInState.set(onSignedIn)

    fun refreshAccountInfoIfReady() {
        val cookie = latestCookie.get().takeIf { "SAPISID" in parseCookieString(it) } ?: return
        val normalizedDataSyncId = latestDataSyncId.get()
            .substringBefore("||")
            .takeIf { it.isNotBlank() && it != "null" }
        val sessionKey = "$cookie|${normalizedDataSyncId.orEmpty()}"
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
                if (signedInReported.getAndSet(true)) {
                    onSignedInState.get().invoke()
                }
            }.onFailure {
                lastAccountInfoFetchSession.set(null)
                reportException(it)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        loadUrl("javascript:Android.onRetrieveVisitorData((window.ytcfg&&ytcfg.get&&ytcfg.get('VISITOR_DATA'))||(window.yt&&yt.config_&&yt.config_.VISITOR_DATA)||'')")
                        loadUrl("javascript:Android.onRetrieveDataSyncId((window.ytcfg&&ytcfg.get&&ytcfg.get('DATASYNC_ID'))||(window.yt&&yt.config_&&yt.config_.DATASYNC_ID)||'')")

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
                    domStorageEnabled = true
                    databaseEnabled = true
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

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}

/**
 * Sign-in dialog: the device's Google account is offered first — one tap, no password —
 * and the credential WebView below it stays available as the fallback whenever the system
 * token is refused, the device holds no Google account, or the user prefers their own.
 */
@Composable
fun LoginPopup(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts = remember { GoogleAccountLogin.deviceAccounts(context) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var signedIn by remember { mutableStateOf(false) }
    LaunchedEffect(signedIn) {
        if (signedIn) onDismiss()
    }

    fun signInWith(account: Account) {
        scope.launch {
            GoogleAccountLogin.completeSignIn(context, account, context.findActivityOrNull())
                .onSuccess { signedIn = true }
                .onFailure {
                    statusMessage = context.getString(R.string.device_account_signin_failed)
                }
        }
    }

    val chooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pickedName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val picked = GoogleAccountLogin.deviceAccounts(context)
            .firstOrNull { it.name == pickedName }
            ?: GoogleAccountLogin.deviceAccounts(context).firstOrNull()
        if (picked != null) {
            signInWith(picked)
        } else {
            statusMessage = context.getString(R.string.device_account_signin_failed)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.login),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                        )
                    }
                }

                if (accounts.isNotEmpty()) {
                    Button(
                        onClick = {
                            if (accounts.size == 1) {
                                signInWith(accounts.first())
                            } else {
                                chooserLauncher.launch(GoogleAccountLogin.accountChooserIntent())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Text(stringResource(R.string.continue_with_google))
                    }
                }

                statusMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.or_sign_in_email),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(Modifier.weight(1f))
                }

                LoginWebView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

internal tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
