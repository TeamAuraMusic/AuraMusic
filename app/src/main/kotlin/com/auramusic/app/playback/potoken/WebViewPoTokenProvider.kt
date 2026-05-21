package com.auramusic.app.playback.potoken

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.auramusic.innertube.PoTokenProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Full WebView + BotGuard Proof-of-Origin token provider.
 *
 * This implementation solves the root cause of the widespread
 * "IO_UNSPECIFIED (2000) - Playback failed - Unknown error"
 * that started hitting AuraMusic, SimpMusic, MetroLis and all
 * other third-party YouTube Music clients in 2025-2026.
 *
 * YouTube now enforces service integrity (PO tokens) for the
 * WEB_REMIX client (and most others). Without a valid token the
 * player response may claim "OK" but the actual media URLs will
 * fail with generic IO errors when ExoPlayer tries to fetch them.
 *
 * This class creates a hidden WebView, bootstraps music.youtube.com,
 * performs the modern attestation flow (Create → GenerateIT) against
 * www.youtube.com (to avoid jnn-pa DNS issues), runs the returned
 * BotGuard program, and mints a fresh video-bound PO token.
 */
@SuppressLint("SetJavaScriptEnabled")
class WebViewPoTokenProvider(
    private val context: Context,
) : PoTokenProvider {

    private val logTag = "PoTokenProvider"

    @Volatile
    private var webView: WebView? = null

    private val tokenCache = ConcurrentHashMap<String, String>()

    private fun ensureWebView(): WebView {
        webView?.let { return it }

        val deferred = CompletableDeferred<WebView>()

        Handler(Looper.getMainLooper()).post {
            try {
                val wv = WebView(context.applicationContext).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        // Realistic desktop UA – BotGuard is very sensitive to environment
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            Timber.tag(logTag).d("Bootstrap page finished: $url")
                        }
                    }
                    addJavascriptInterface(PoTokenBridge(), "AndroidPo")
                }
                wv.loadUrl("https://music.youtube.com/")
                webView = wv
                deferred.complete(wv)
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "Failed to create WebView for PO token")
                deferred.completeExceptionally(e)
            }
        }
        return deferred.getCompleted()
    }

    override suspend fun getPlayerPoToken(videoId: String): String? {
        tokenCache[videoId]?.let {
            Timber.tag(logTag).d("Cache hit for $videoId")
            return it
        }

        return withContext(Dispatchers.Main) {
            val wv = ensureWebView()

            val result = withTimeoutOrNull(25_000L) {
                generatePoToken(wv, videoId)
            }

            if (result != null) {
                tokenCache[videoId] = result
                Timber.tag(logTag).i("Successfully obtained PO token for $videoId (len=${result.length})")
            } else {
                Timber.tag(logTag).w("Failed to obtain PO token for $videoId within timeout")
            }
            result
        }
    }

    override suspend fun getStreamingPoToken(videoId: String): String? {
        // For GVS ?pot= parameter on final streams.
        // We can reuse the same mechanism; for now we return the player token
        // because many clients accept the same value for both contexts.
        return getPlayerPoToken(videoId)
    }

    /**
     * Performs the full modern PO token flow inside the WebView:
     * 1. Obtain BotGuard challenge via Create
     * 2. Run the returned program
     * 3. Call GenerateIT
     * 4. Derive final PO token using videoId as minter
     */
    private suspend fun generatePoToken(wv: WebView, videoId: String): String? {
        val tokenDeferred = CompletableDeferred<String?>()

        // The bridge will complete this when JS returns the token
        currentTokenRequest = tokenDeferred

        // We inject a complete self-contained JS runner that does the
        // attestation dance against www.youtube.com (2026 recommended path).
        val js = """
            (function() {
                const VIDEO_ID = "$videoId";
                const ANDROID_PO = window.AndroidPo || {};

                async function getPoToken() {
                    try {
                        // Step 1: Get the current ytcfg data (innertube key, visitor etc.)
                        const ytcfg = window.ytcfg?.get("INNERTUBE_CONTEXT_CLIENT_NAME") ? window.ytcfg : {};
                        const apiKey = ytcfg?.data_?.INNERTUBE_API_KEY || "AIzaSyC9XL3ZjWddXya6X4suCphm9z0Qd6v8v8w";
                        const visitorData = ytcfg?.data_?.VISITOR_DATA || "";

                        // Step 2: Call the modern Create endpoint (www.youtube.com)
                        const createBody = {
                            "context": {
                                "client": {
                                    "clientName": "WEB_REMIX",
                                    "clientVersion": "1.20260124.01.00",
                                    "visitorData": visitorData
                                }
                            },
                            "videoId": VIDEO_ID
                        };

                        const createResp = await fetch("https://www.youtube.com/youtubei/v1/attestation/create?key=" + apiKey, {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify(createBody)
                        });
                        const createJson = await createResp.json();

                        const program = createJson?.bgChallenge?.program;
                        const globalName = createJson?.bgChallenge?.globalName || "globalName";
                        if (!program) throw new Error("No BotGuard program");

                        // Step 3: Execute the BotGuard program (the obfuscated VM)
                        // We use the standard trick: eval the program and call the exported function
                        const bg = {};
                        (new Function("globalThis", program))(bg);

                        // The program usually exports a function under a known name
                        const vm = bg[globalName] || Object.values(bg).find(v => typeof v === "function");
                        if (!vm) throw new Error("BotGuard VM not found");

                        // Step 4: Run the VM to obtain the integrity token
                        const integrityToken = await vm();

                        // Step 5: Call GenerateIT with the integrity token + videoId as minter
                        const genBody = {
                            "integrityToken": integrityToken,
                            "minter": VIDEO_ID,
                            "context": {
                                "client": {
                                    "clientName": "WEB_REMIX",
                                    "clientVersion": "1.20260124.01.00"
                                }
                            }
                        };

                        const genResp = await fetch("https://www.youtube.com/youtubei/v1/attestation/generateit?key=" + apiKey, {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify(genBody)
                        });
                        const genJson = await genResp.json();

                        const poToken = genJson?.poToken || genJson?.integrityToken;
                        if (!poToken) throw new Error("No poToken in response");

                        // Return to Kotlin
                        if (ANDROID_PO.onTokenGenerated) {
                            ANDROID_PO.onTokenGenerated(poToken);
                        }
                        return poToken;
                    } catch (err) {
                        console.error("PO token generation failed", err);
                        if (ANDROID_PO.onTokenGenerated) {
                            ANDROID_PO.onTokenGenerated(null);
                        }
                        return null;
                    }
                }

                getPoToken();
            })();
        """.trimIndent()

        wv.evaluateJavascript(js, null)

        return withTimeoutOrNull(20_000L) {
            tokenDeferred.await()
        }
    }

    private var currentTokenRequest: CompletableDeferred<String?>? = null

    private inner class PoTokenBridge {
        @JavascriptInterface
        fun onTokenGenerated(token: String?) {
            currentTokenRequest?.complete(token)
            currentTokenRequest = null
        }
    }

    fun destroy() {
        val wv = webView ?: return
        Handler(Looper.getMainLooper()).post {
            runCatching { wv.destroy() }
            webView = null
        }
    }
}
