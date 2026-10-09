/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.auramusic.innertube.utils.parseCookieString
import com.auramusic.app.constants.InnerTubeCookieKey
import com.auramusic.app.constants.InnerTubeOAuthTokenKey
import com.auramusic.app.constants.YtmSyncKey
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import kotlinx.coroutines.runBlocking

fun Context.isSyncEnabled(): Boolean {
    return runBlocking {
        dataStore.get(YtmSyncKey, true) && isUserLoggedIn()
    }
}

fun Context.isUserLoggedIn(): Boolean {
    return runBlocking {
        val cookie = dataStore[InnerTubeCookieKey] ?: ""
        val oauthToken = dataStore[InnerTubeOAuthTokenKey] ?: ""
        (("SAPISID" in parseCookieString(cookie)) || oauthToken.isNotBlank()) && isInternetConnected()
    }
}

fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
