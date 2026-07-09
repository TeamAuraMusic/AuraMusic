package com.auramusic.kizzy.gateway.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// IDENTIFY payload intentionally kept minimal to match the proven-working upstream
// Kizzy client. Extra fields (shard, client_state, presence-in-identify) are omitted:
// "shard" in particular is a bot-only field that Discord validates and can reject on a
// user account. Presence is sent separately via PRESENCE_UPDATE (op 3) after READY.
@Serializable
data class Identify(
    @SerialName("capabilities")
    val capabilities: Int,
    @SerialName("compress")
    val compress: Boolean,
    @SerialName("large_threshold")
    val largeThreshold: Int,
    @SerialName("properties")
    val properties: Properties,
    @SerialName("token")
    val token: String,
) {
    companion object {
        fun String.toIdentifyPayload(
            os: String = "Android",
            browser: String = "Discord Android",
            device: String = "Generic Android Device"
        ) = Identify(
            capabilities = 65,
            compress = false,
            largeThreshold = 100,
            properties = Properties(
                os = os,
                browser = browser,
                device = device
            ),
            token = this
        )
    }
}

@Serializable
data class Properties(
    @SerialName("browser")
    val browser: String,
    @SerialName("device")
    val device: String,
    @SerialName("os")
    val os: String,
)