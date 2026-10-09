/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import com.auramusic.app.constants.AccountChannelHandleKey
import com.auramusic.app.constants.AccountEmailKey
import com.auramusic.app.constants.AccountNameKey
import com.auramusic.app.constants.InnerTubeOAuthTokenKey
import com.auramusic.app.constants.OAuthAccountKey
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import com.auramusic.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

/**
 * Password-free sign-in through the Google accounts the device already knows.
 *
 * The account manager hands out an OAuth bearer token for the YouTube scope and InnerTube
 * authenticates with it directly (`Authorization: Bearer`), so neither a WebView nor a
 * typed password is involved. The token is probed against `account/account_menu` before it
 * is trusted: a rejected token never replaces an existing session. Tokens live roughly an
 * hour, so [silentRefresh] re-mints the stored account's token on app start.
 */
object GoogleAccountLogin {
    const val ACCOUNT_TYPE = "com.google"
    private const val YOUTUBE_SCOPE = "oauth2:https://www.googleapis.com/auth/youtube"

    /** Google accounts currently on the device; empty when none are signed in. */
    fun deviceAccounts(context: Context): List<Account> =
        runCatching {
            AccountManager.get(context).getAccountsByType(ACCOUNT_TYPE).toList()
        }.getOrDefault(emptyList())

    /**
     * System account chooser, for devices holding more than one Google account. The result
     * carries the picked account's name in [AccountManager.KEY_ACCOUNT_NAME].
     */
    fun accountChooserIntent(): Intent = AccountManager.newChooseAccountIntent(
        null,
        null,
        arrayOf(ACCOUNT_TYPE),
        true,
        null,
        null,
        null,
        null,
    )

    /**
     * Gets a bearer token for [account] (first use consents through [activity]), proves it
     * works against InnerTube, then persists it as the logged-in session. On any failure
     * the previous session is left exactly as it was.
     */
    suspend fun completeSignIn(
        context: Context,
        account: Account,
        activity: Activity?,
    ): Result<Unit> {
        val token = try {
            getAuthToken(context, account, activity)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val previousToken = YouTube.oauthToken
        YouTube.oauthToken = token
        val info = YouTube.accountInfo().getOrElse {
            // The server rejected it: leave whatever session existed untouched.
            YouTube.oauthToken = previousToken
            return Result.failure(it)
        }
        withContext(Dispatchers.IO) {
            context.dataStore.edit { settings ->
                settings[InnerTubeOAuthTokenKey] = token
                settings[OAuthAccountKey] = account.name
                settings[AccountNameKey] = info.name
                settings[AccountEmailKey] = info.email.orEmpty()
                settings[AccountChannelHandleKey] = info.channelHandle.orEmpty()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Re-mints the stored account's token after its lifetime lapses. Runs at app start; a
     * still-valid token comes back unchanged, and a device that lost its Google account
     * drops the dead session instead of failing every request. Never throws.
     */
    suspend fun silentRefresh(context: Context) {
        withContext(Dispatchers.IO) {
            val storedAccountName = context.dataStore[OAuthAccountKey] ?: return@withContext
            val storedToken = context.dataStore[InnerTubeOAuthTokenKey] ?: return@withContext
            val account = deviceAccounts(context).firstOrNull { it.name == storedAccountName }
            if (account == null) {
                // The account was removed from the device; its token can never refresh.
                context.dataStore.edit { settings ->
                    settings.remove(InnerTubeOAuthTokenKey)
                    settings.remove(OAuthAccountKey)
                }
                return@withContext
            }
            val freshToken = runCatching { getAuthToken(context, account, null) }.getOrNull()
                ?: return@withContext
            if (freshToken != storedToken) {
                context.dataStore.edit { it[InnerTubeOAuthTokenKey] = freshToken }
            }
        }
    }

    private suspend fun getAuthToken(
        context: Context,
        account: Account,
        activity: Activity?,
    ): String = suspendCancellableCoroutine { continuation ->
        AccountManager.get(context).getAuthToken(
            account,
            YOUTUBE_SCOPE,
            null,
            activity,
            { future ->
                try {
                    val token = future.result.getString(AccountManager.KEY_AUTHTOKEN)
                    continuation.resume(
                        token ?: throw IllegalStateException("No auth token for ${account.name}")
                    )
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            null,
        )
    }
}
