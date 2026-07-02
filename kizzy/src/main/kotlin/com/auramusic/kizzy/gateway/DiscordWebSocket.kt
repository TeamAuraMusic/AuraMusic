package com.auramusic.kizzy.gateway

import com.auramusic.kizzy.gateway.entities.Heartbeat
import com.auramusic.kizzy.gateway.entities.Identify.Companion.toIdentifyPayload
import com.auramusic.kizzy.gateway.entities.Payload
import com.auramusic.kizzy.gateway.entities.Ready
import com.auramusic.kizzy.gateway.entities.Resume
import com.auramusic.kizzy.gateway.entities.op.OpCode
import com.auramusic.kizzy.gateway.entities.op.OpCode.DISPATCH
import com.auramusic.kizzy.gateway.entities.op.OpCode.HEARTBEAT
import com.auramusic.kizzy.gateway.entities.op.OpCode.HELLO
import com.auramusic.kizzy.gateway.entities.op.OpCode.IDENTIFY
import com.auramusic.kizzy.gateway.entities.op.OpCode.INVALID_SESSION
import com.auramusic.kizzy.gateway.entities.op.OpCode.PRESENCE_UPDATE
import com.auramusic.kizzy.gateway.entities.op.OpCode.RECONNECT
import com.auramusic.kizzy.gateway.entities.op.OpCode.RESUME
import com.auramusic.kizzy.gateway.entities.presence.Presence
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Modified by Zion Huang
 */
open class DiscordWebSocket(
    private val token: String,
    private val os: String = "Android",
    private val browser: String = "Discord Android",
    private val device: String = "Generic Android Device",
) : CoroutineScope {
    private val logger = Logger.getLogger(DiscordWebSocket::class.java.name)
    private val gatewayUrl = "wss://gateway.discord.gg/?v=10&encoding=json"
    private var websocket: DefaultClientWebSocketSession? = null
    private var sequence = 0
    private var sessionId: String? = null
    private var heartbeatInterval = 0L
    private var resumeGatewayUrl: String? = null
    private var heartbeatJob: Job? = null
    private var connected = false
    private var ready = false
    private var client: HttpClient = HttpClient {
        install(WebSockets)
    }
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var reconnectionJob: Job? = null
    private var currentReconnectDelay = INITIAL_RECONNECT_DELAY

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Default

    suspend fun connect(): Boolean {
        if (connected && ready) {
            logger.info("Gateway already connected and ready.")
            return true
        }
        ready = false
        reconnectionJob?.cancel()
        var success = false
        reconnectionJob = launch {
            try {
                val url = resumeGatewayUrl ?: gatewayUrl
                logger.info("Connecting to Discord Gateway at $url")
                websocket = client.webSocketSession(url) {
                    header("User-Agent", "Discord-Android/314013;RNA")
                    header("Accept-Language", "en-US")
                    header("Cache-Control", "no-cache")
                    header("Pragma", "no-cache")
                }
                connected = true
                logger.info("Successfully connected to Discord Gateway.")
                currentReconnectDelay = INITIAL_RECONNECT_DELAY
                success = true
                // start receiving messages
                websocket!!.incoming.receiveAsFlow()
                    .collect {
                        when (it) {
                            is Frame.Text -> {
                                val jsonString = it.readText()
                                onMessage(json.decodeFromString(jsonString))
                            }

                            else -> {}
                        }
                    }
                handleClose()
            } catch (e: Exception) {
                logger.severe("Gateway connection error: ${e.stackTraceToString()}")
                scheduleReconnection()
            }
        }
        return success
    }

    private fun scheduleReconnection() {
        if (reconnectionJob?.isActive == true) {
            return
        }
        heartbeatJob?.cancel()
        connected = false
        ready = false
        reconnectionJob = launch {
            delay(currentReconnectDelay)
            logger.info("Attempting to reconnect...")
            connect()
            currentReconnectDelay = (currentReconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
        }
    }


    private suspend fun handleClose() {
        heartbeatJob?.cancel()
        connected = false
        ready = false
        val close = websocket?.closeReason?.await()
        logger.warning("Gateway closed with code: ${close?.code}, reason: ${close?.message}, can_reconnect: ${close?.code?.toInt() == 4000}")
        if (close?.code?.toInt() == 4000) {
            delay(200.milliseconds)
            connect()
        } else
            scheduleReconnection()
    }

    private suspend fun onMessage(payload: Payload) {
        logger.info("Gateway received: op=${payload.op}, seq=${payload.s}, event=${payload.t}")
        payload.s?.let {
            sequence = it
        }
        when (payload.op) {
            DISPATCH -> payload.handleDispatch()
            HEARTBEAT -> sendHeartBeat()
            RECONNECT -> reconnectWebSocket()
            INVALID_SESSION -> handleInvalidSession()
            HELLO -> payload.handleHello()
            else -> {}
        }
    }

    open fun Payload.handleDispatch() {
        when (this.t.toString()) {
            "READY" -> {
                val ready = json.decodeFromJsonElement<Ready>(this.d!!)
                sessionId = ready.sessionId
                resumeGatewayUrl = ready.resumeGatewayUrl + "/?v=10&encoding=json"
                logger.info("Gateway READY: resume_gateway_url updated to $resumeGatewayUrl, session_id updated to $sessionId")
                connected = true
                this@DiscordWebSocket.ready = true
                return
            }

            "RESUMED" -> {
                connected = true
                ready = true
                logger.info("Gateway: Session Resumed")
            }

            else -> {}
        }
    }

    private suspend inline fun handleInvalidSession() {
        ready = false
        logger.warning("Gateway: Handling Invalid Session. Sending Identify after 150ms")
        delay(150)
        sendIdentify()
    }

    private suspend inline fun Payload.handleHello() {
        if (sequence > 0 && !sessionId.isNullOrBlank()) {
            sendResume()
        } else {
            sendIdentify()
        }
        heartbeatInterval = json.decodeFromJsonElement<Heartbeat>(this.d!!).heartbeatInterval
        logger.info("Gateway: Setting heartbeatInterval=$heartbeatInterval")
        startHeartbeatJob(heartbeatInterval)
    }

    private suspend fun sendHeartBeat() {
        logger.info("Gateway: Sending $HEARTBEAT with seq: $sequence")
        send(
            op = HEARTBEAT,
            d = if (sequence == 0) "null" else sequence.toString(),
        )
    }

    private suspend inline fun reconnectWebSocket() {
        websocket?.close(
            CloseReason(
                code = 4000,
                message = "Attempting to reconnect"
            )
        )
    }

    private suspend fun sendIdentify() {
        logger.info("Gateway: Sending $IDENTIFY")
        send(
            op = IDENTIFY,
            d = token.toIdentifyPayload(
                os = os,
                browser = browser,
                device = device
            )
        )
    }

    private suspend fun sendResume() {
        logger.info("Gateway: Sending $RESUME")
        send(
            op = RESUME,
            d = Resume(
                seq = sequence,
                sessionId = sessionId,
                token = token
            )
        )
    }

    private fun startHeartbeatJob(interval: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = launch {
            while (isActive) {
                sendHeartBeat()
                delay(interval)
            }
        }
    }

    private fun isSocketConnectedToAccount(): Boolean {
        return ready && websocket?.isActive == true
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun isWebSocketConnected(): Boolean {
        return websocket?.incoming != null && websocket?.outgoing?.isClosedForSend == false
    }

    private suspend inline fun <reified T> send(op: OpCode, d: T?) {
        if (websocket?.isActive == true) {
            val payload = json.encodeToString(
                Payload(
                    op = op,
                    d = json.encodeToJsonElement(d),
                )
            )
            if (op == IDENTIFY) {
                logger.info("Gateway sending payload: [REDACTED IDENTIFY PAYLOAD]")
            } else {
                logger.info("Gateway sending payload: $payload")
            }
            websocket?.send(Frame.Text(payload))
        }
    }

    fun close() {
        reconnectionJob?.cancel()
        heartbeatJob?.cancel()
        heartbeatJob = null
        this.cancel()
        resumeGatewayUrl = null
        sessionId = null
        connected = false
        ready = false
        runBlocking {
            websocket?.close()
            logger.severe("Gateway: Connection to gateway closed")
        }
    }

    suspend fun sendActivity(presence: Presence) {
        val becameReady = withTimeoutOrNull(15.seconds) {
            while (!isSocketConnectedToAccount()) {
                delay(10.milliseconds)
            }
            true
        } == true
        if (!becameReady) {
            logger.warning("Gateway: Timed out waiting for READY before sending presence")
            return
        }
        logger.info("Gateway: Sending $PRESENCE_UPDATE")
        send(
            op = PRESENCE_UPDATE,
            d = presence
        )
    }
    companion object {
        private val INITIAL_RECONNECT_DELAY = 1.seconds
        private val MAX_RECONNECT_DELAY = 60.seconds
    }
}
