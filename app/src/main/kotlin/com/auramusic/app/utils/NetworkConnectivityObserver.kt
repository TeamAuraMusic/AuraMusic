/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Simple NetworkConnectivityObserver based on OuterTune's implementation
 * Provides network connectivity monitoring for auto-play functionality
 */
class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = Channel<Boolean>(Channel.CONFLATED)
    val networkStatus = _networkStatus.receiveAsFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkStatus.trySend(true)
        }

        override fun onLost(network: Network) {
            _networkStatus.trySend(false)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            // A VPN can come up after Wi‑Fi and changes the capabilities of the active
            // network without firing onAvailable again. Republish the state so a player
            // that parked itself waiting for a connection can retry.
            _networkStatus.trySend(isConnected(capabilities))
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            // Do not require NOT_RESTRICTED: VPN networks are not always granted it,
            // and a working VPN route is still a usable connection for playback.
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Fallback: assume connected if registration fails
            _networkStatus.trySend(true)
        }
        
        // Send initial state
        val isInitiallyConnected = isCurrentlyConnected()
        _networkStatus.trySend(isInitiallyConnected)
    }

    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    
    /**
     * Check current connectivity state synchronously
     */
    fun isCurrentlyConnected(): Boolean {
        return try {
            isConnected(connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * INTERNET is enough. A VPN network is often never marked VALIDATED because
     * Android's captive-portal probe is blocked or rewritten by the tunnel, yet
     * HTTPS through it works. Requiring VALIDATED made the player treat such a
     * network as offline and wait forever instead of retrying the stream.
     */
    private fun isConnected(capabilities: NetworkCapabilities?): Boolean =
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
