/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * KizzyRPC.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.auramusic.kizzy.rpc

import com.auramusic.kizzy.gateway.DiscordWebSocket
import com.auramusic.kizzy.gateway.entities.presence.Activity
import com.auramusic.kizzy.gateway.entities.presence.Assets
import com.auramusic.kizzy.gateway.entities.presence.Metadata
import com.auramusic.kizzy.gateway.entities.presence.Presence
import com.auramusic.kizzy.gateway.entities.presence.Timestamps
import com.auramusic.kizzy.repository.KizzyRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

/**
 * Modified by Zion Huang
 */
open class KizzyRPC(
    token: String,
    os: String = "Android",
    browser: String = "Discord Android",
    device: String = "Generic Android Device",
    userAgent: String = "Discord-Android/314013;RNA",
    superPropertiesBase64: String? = null
) {
    private val kizzyRepository = KizzyRepository(userAgent, superPropertiesBase64)
    private val discordWebSocket = DiscordWebSocket(token.toGatewayAuthorization(), os, browser, device)

    fun closeRPC() {
        discordWebSocket.close()
    }

    fun isRpcRunning(): Boolean {
        return discordWebSocket.isWebSocketConnected()
    }

    open suspend fun close() {
        discordWebSocket.close()
    }

    suspend fun setActivity(
        name: String,
        state: String?,
        stateUrl: String? = null,
        details: String?,
        detailsUrl: String? = null,
        largeImage: RpcImage?,
        smallImage: RpcImage?,
        largeText: String? = null,
        smallText: String? = null,
        buttons: List<Pair<String, String>>? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        type: Type = Type.LISTENING,
        statusDisplayType: StatusDisplayType = StatusDisplayType.NAME,
        streamUrl: String? = null,
        applicationId: String? = null,
        status: String? = "online",
        since: Long? = null,
    ) {
        if (!isRpcRunning()) {
            discordWebSocket.connect()
        }

        fun presenceWithImages(resolvedImages: Map<String, String> = emptyMap()) = Presence(
            activities = listOf(
                Activity(
                    name = name,
                    state = state,
                    stateUrl = stateUrl,
                    details = details,
                    detailsUrl = detailsUrl,
                    type = type.value,
                    statusDisplayType = statusDisplayType.value,
                    timestamps = Timestamps(startTime, endTime),
                    assets = Assets(
                        largeImage = largeImage?.let { 
                            when (it) {
                                is RpcImage.DiscordImage -> "mp:${it.image}"
                                is RpcImage.ExternalImage -> resolvedImages[it.image]
                            }
                        },
                        smallImage = smallImage?.let { 
                            when (it) {
                                is RpcImage.DiscordImage -> "mp:${it.image}"
                                is RpcImage.ExternalImage -> resolvedImages[it.image]
                            }
                        },
                        largeText = largeText,
                        smallText = smallText
                    ),
                    buttons = buttons?.map { it.first },
                    metadata = Metadata(buttonUrls = buttons?.map { it.second }),
                    applicationId = applicationId,
                    url = streamUrl
                )
            ),
            afk = false,
            since = since,
            status = status ?: "online"
        )

        val images = listOfNotNull(largeImage, smallImage)
        val externalImages = images.filterIsInstance<RpcImage.ExternalImage>()
        val imageUrls = externalImages.map { it.image }

        if (imageUrls.isNotEmpty()) {
            discordWebSocket.sendActivity(presenceWithImages())
        }

        val resolvedImages = if (imageUrls.isEmpty()) {
            emptyMap()
        } else {
            runCatching {
                kizzyRepository.getImages(imageUrls)?.results?.associate { it.originalUrl to it.id } ?: emptyMap()
            }.getOrElse {
                emptyMap()
            }
        }

        if (imageUrls.isEmpty() || resolvedImages.isNotEmpty()) {
            discordWebSocket.sendActivity(presenceWithImages(resolvedImages))
        }
    }

    enum class Type(val value: Int) {
        PLAYING(0),
        STREAMING(1),
        LISTENING(2),
        WATCHING(3),
        COMPETING(5)
    }

    enum class StatusDisplayType(val value: Int) {
        NAME(0),
        STATE(1),
        DETAILS(2)
    }

    companion object {
        suspend fun getUserInfo(
            token: String,
            userAgent: String = "Discord-Android/314013;RNA",
            superPropertiesBase64: String? = null
        ): Result<UserInfo> = runCatching {
            val client = HttpClient()
            try {
                var lastFailure: Throwable? = null
                token.authorizationCandidates().forEach { authorization ->
                    val userInfo = runCatching {
                        val response = client.get("https://discord.com/api/v10/users/@me") {
                            header("Authorization", authorization)
                            header("User-Agent", userAgent)
                            if (superPropertiesBase64 != null) {
                                header("X-Super-Properties", superPropertiesBase64)
                            }
                        }.bodyAsText()
                        val json = JSONObject(response)
                        val id = json.getString("id")
                        val username = json.getString("username")
                        val name = json.optString("global_name")
                            .takeIf { it.isNotBlank() && it != "null" }
                            ?: json.optString("display_name")
                                .takeIf { it.isNotBlank() && it != "null" }
                            ?: username
                        val avatarHash = json.optString("avatar")
                            .takeIf { it.isNotBlank() && it != "null" }
                        val avatarUrl = avatarHash?.let { hash ->
                            val extension = if (hash.startsWith("a_")) "gif" else "png"
                            "https://cdn.discordapp.com/avatars/$id/$hash.$extension?size=128"
                        }

                        UserInfo(username = username, name = name, avatarUrl = avatarUrl)
                    }
                    userInfo.onSuccess { return@runCatching it }
                    lastFailure = userInfo.exceptionOrNull()
                }
                throw lastFailure ?: IllegalStateException("Unable to fetch Discord user info")
            } finally {
                client.close()
            }
        }
    }
}

private fun String.cleanDiscordToken(): String =
    trim().trim('"').trim('\'')

private fun String.toGatewayAuthorization(): String {
    val token = cleanDiscordToken()
    if (token.startsWith("Bearer ", ignoreCase = true)) return token
    if (token.startsWith("Bot ", ignoreCase = true)) return token
    return token
}

private fun String.authorizationCandidates(): List<String> {
    val token = cleanDiscordToken()
    if (token.startsWith("Bearer ", ignoreCase = true) || token.startsWith("Bot ", ignoreCase = true)) {
        return listOf(token)
    }
    return if (token.contains('.')) {
        listOf(token, "Bearer $token")
    } else {
        listOf("Bearer $token", token)
    }
}
