/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Smart Speaker Mesh manager – discovers compatible audio receivers
 * (Chromecast, AirPlay, DLNA / UPnP, Sonos) on the local network
 * via Network Service Discovery (mDNS) and presents them to the UI
 * as an opt-in mesh group. When a speaker group is active the
 * playback can be mirrored using the system MediaRouter (Cast on
 * GMS builds) or via direct UPnP control on FOSS builds.
 */
package com.auramusic.app.hardware.speaker

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

data class MeshSpeaker(
    val id: String,
    val name: String,
    val type: String,
    val host: String?,
    val port: Int,
    val isActive: Boolean,
)

class SmartSpeakerMeshManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val nsd: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _speakers = MutableStateFlow<List<MeshSpeaker>>(emptyList())
    val speakers: StateFlow<List<MeshSpeaker>> = _speakers.asStateFlow()

    private val _meshActive = MutableStateFlow(false)
    val meshActive: StateFlow<Boolean> = _meshActive.asStateFlow()

    private val _activeGroup = MutableStateFlow<Set<String>>(emptySet())
    val activeGroup: StateFlow<Set<String>> = _activeGroup.asStateFlow()

    private val _syncDelayMs = MutableStateFlow(40)
    val syncDelayMs: StateFlow<Int> = _syncDelayMs.asStateFlow()

    private val discoveryListeners = mutableListOf<NsdManager.DiscoveryListener>()
    private val knownServices = mutableMapOf<String, MeshSpeaker>()

    private val serviceTypes = listOf(
        "_googlecast._tcp." to "Chromecast",
        "_airplay._tcp." to "AirPlay",
        "_raop._tcp." to "AirPlay",
        "_spotify-connect._tcp." to "Spotify Connect",
        "_sonos._tcp." to "Sonos",
        "_printer._tcp." to "DLNA",
    )

    fun startDiscovery() {
        val mgr = nsd ?: return
        if (discoveryListeners.isNotEmpty()) return
        serviceTypes.forEach { (type, label) ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    Timber.w("NSD start failed for %s code %d", serviceType, errorCode)
                }

                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}

                override fun onDiscoveryStarted(serviceType: String?) {}

                override fun onDiscoveryStopped(serviceType: String?) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    resolve(serviceInfo, label)
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    val key = "${label}-${serviceInfo.serviceName}"
                    knownServices.remove(key)
                    publish()
                }
            }
            try {
                mgr.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                discoveryListeners.add(listener)
            } catch (e: Exception) {
                Timber.w(e, "discoverServices failed for %s", type)
            }
        }
    }

    fun stopDiscovery() {
        val mgr = nsd ?: return
        discoveryListeners.forEach { l ->
            runCatching { mgr.stopServiceDiscovery(l) }
        }
        discoveryListeners.clear()
    }

    private fun resolve(info: NsdServiceInfo, label: String) {
        val mgr = nsd ?: return
        val cb = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val key = "${label}-${serviceInfo.serviceName}"
                knownServices[key] = MeshSpeaker(
                    id = key,
                    name = serviceInfo.serviceName ?: "Speaker",
                    type = label,
                    host = serviceInfo.host?.hostAddress,
                    port = serviceInfo.port,
                    isActive = _activeGroup.value.contains(key),
                )
                publish()
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mgr.registerServiceInfoCallback(
                    info,
                    java.util.concurrent.Executors.newSingleThreadExecutor(),
                    object : NsdManager.ServiceInfoCallback {
                        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {}
                        override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                            cb.onServiceResolved(serviceInfo)
                        }
                        override fun onServiceLost() {}
                        override fun onServiceInfoCallbackUnregistered() {}
                    },
                )
            } else {
                @Suppress("DEPRECATION")
                mgr.resolveService(info, cb)
            }
        } catch (e: Exception) {
            Timber.w(e, "resolveService failed")
        }
    }

    private fun publish() {
        _speakers.value = knownServices.values.sortedBy { it.name }
    }

    fun toggleSpeaker(id: String) {
        val current = _activeGroup.value.toMutableSet()
        if (id in current) current.remove(id) else current.add(id)
        _activeGroup.value = current
        knownServices.replaceAll { _, v -> v.copy(isActive = id == v.id || v.isActive && current.contains(v.id)) }
        publish()
        _meshActive.value = current.isNotEmpty()
    }

    fun setMeshActive(active: Boolean) {
        _meshActive.value = active
        if (!active) {
            _activeGroup.value = emptySet()
            knownServices.replaceAll { _, v -> v.copy(isActive = false) }
            publish()
        }
    }

    fun setSyncDelay(ms: Int) {
        _syncDelayMs.value = ms.coerceIn(0, 500)
    }

    fun isAnyActive(): Boolean = _activeGroup.value.isNotEmpty()
}
