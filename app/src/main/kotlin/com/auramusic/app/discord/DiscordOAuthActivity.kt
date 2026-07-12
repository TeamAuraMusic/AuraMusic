package com.auramusic.app.discord

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import timber.log.Timber

data class AuthCodeResult(val code: String, val state: String)

class DiscordOAuthActivity : Activity() {

    companion object {
        private const val TAG = "DiscordSvc"

        @Volatile
        private var deferred: CompletableDeferred<AuthCodeResult>? = null

        fun newDeferred(): CompletableDeferred<AuthCodeResult> {
            val d = CompletableDeferred<AuthCodeResult>()
            deferred = d
            return d
        }

        suspend fun awaitCode(timeoutMs: Long = 120_000L): AuthCodeResult {
            val d = deferred ?: throw CancellationException("No pending authorization")
            return withTimeout(timeoutMs) { d.await() }
        }

        fun cancelPending() {
            deferred?.let { d ->
                if (!d.isCompleted) {
                    d.completeExceptionally(CancellationException("Authorization cancelled by user"))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Timber.tag(TAG).i("OAuthActivity: handleIntent action=%s deferred=%s", intent?.action, deferred != null)

        val uri = intent?.data ?: run {
            Timber.tag(TAG).w("OAuthActivity: no URI in intent")
            Toast.makeText(this, "Discord Error: No URI in callback", Toast.LENGTH_LONG).show()
            deferred?.completeExceptionally(
                DiscordAuthException.InvalidGrant("No URI in callback intent")
            )
            finish()
            return
        }

        Timber.tag(TAG).i("OAuthActivity: callback URI=%s", uri)

        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")

        if (error != null) {
            Timber.tag(TAG).w("OAuthActivity: error=%s", error)
            Toast.makeText(this, "Discord Error: $error", Toast.LENGTH_LONG).show()
            deferred?.completeExceptionally(
                DiscordAuthException.UserCancelled("Authorization denied: $error")
            )
            finish()
            return
        }

        if (code == null) {
            Timber.tag(TAG).w("OAuthActivity: missing code in URI=%s", uri)
            Toast.makeText(this, "Discord Error: No auth code received", Toast.LENGTH_LONG).show()
            deferred?.completeExceptionally(
                DiscordAuthException.InvalidGrant("Missing authorization code")
            )
            finish()
            return
        }

        Timber.tag(TAG).i("OAuthActivity: received code (length=%d) state=%s", code.length, state?.take(8) ?: "null")
        Toast.makeText(this, "Discord: Code received, exchanging for token...", Toast.LENGTH_SHORT).show()
        deferred?.complete(AuthCodeResult(code = code, state = state ?: ""))
            ?: run {
                Timber.tag(TAG).e("OAuthActivity: deferred is NULL")
                Toast.makeText(this, "Discord Error: Auth flow not running", Toast.LENGTH_LONG).show()
            }
        finish()
    }
}

