/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Download-and-install state machine for TV updates.
 *
 * TV devices usually have no browser to speak of, so handing the release page to
 * an external app is not a usable update path there. The APK is streamed into
 * the cache and passed straight to the system installer instead - the same way
 * the store-less phone builds update themselves.
 */
sealed interface TvUpdatePhase {
    data object Idle : TvUpdatePhase

    /** [progress] is 0f..1f, or -1f while the total size is unknown. */
    data class Downloading(val progress: Float) : TvUpdatePhase

    data object Installing : TvUpdatePhase
    data object NeedsPermission : TvUpdatePhase
    data object Launched : TvUpdatePhase
    data class Failed(val message: String) : TvUpdatePhase
}

class TvUpdateInstallController {
    var phase: TvUpdatePhase by mutableStateOf(TvUpdatePhase.Idle)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var apkFile: File? = null

    val isBusy: Boolean
        get() = phase is TvUpdatePhase.Downloading ||
            phase is TvUpdatePhase.Installing ||
            phase is TvUpdatePhase.Launched

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Downloads [downloadUrl] and hands the APK to the installer. A null URL
     * means the release carried no TV package, which is reported instead of
     * doing nothing.
     */
    fun start(context: Context, downloadUrl: String?) {
        if (isBusy) return
        if (downloadUrl.isNullOrBlank()) {
            phase = TvUpdatePhase.Failed("This release does not include a TV build")
            return
        }
        val appContext = context.applicationContext
        phase = TvUpdatePhase.Downloading(0f)
        job = scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { download(appContext, downloadUrl) }
                apkFile = file
                install(appContext, file)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                phase = TvUpdatePhase.Failed(t.message ?: "Could not download the update")
            }
        }
    }

    /** Retries the installer after the user granted unknown-source permission. */
    fun retryInstall(context: Context) {
        val file = apkFile
        if (file == null) {
            phase = TvUpdatePhase.Failed("The update file is gone - download it again")
            return
        }
        job = scope.launch { install(context.applicationContext, file) }
    }

    fun allowPermission(context: Context) {
        openInstallPermissionSettings(context.applicationContext)
    }

    fun reset() {
        job?.cancel()
        phase = TvUpdatePhase.Idle
    }

    fun dispose() {
        job?.cancel()
        scope.cancel()
    }

    private suspend fun download(context: Context, url: String): File {
        val target = File(context.cacheDir, APK_FILE_NAME)
        runCatching { if (target.exists()) target.delete() }
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Server returned HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty download response")
            val total = body.contentLength()
            var received = 0L
            var lastReported = 0L
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        if (received - lastReported >= 128 * 1024) {
                            lastReported = received
                            phase = TvUpdatePhase.Downloading(
                                if (total > 0) received.toFloat() / total.toFloat() else -1f,
                            )
                        }
                    }
                }
            }
            phase = TvUpdatePhase.Downloading(1f)
        }
        if (target.length() < MIN_APK_BYTES) {
            target.delete()
            throw IOException("Downloaded file is too small - likely an error page")
        }
        return target
    }

    private suspend fun install(context: Context, file: File) {
        phase = TvUpdatePhase.Installing
        phase = when {
            !file.exists() || file.length() < MIN_APK_BYTES ->
                TvUpdatePhase.Failed("The downloaded update is incomplete")
            !canInstallPackages(context) ->
                TvUpdatePhase.NeedsPermission
            else -> runCatching {
                launchInstaller(context, file)
                TvUpdatePhase.Launched
            }.getOrElse { TvUpdatePhase.Failed(it.message ?: "Could not start the installer") }
        }
    }

    private fun launchInstaller(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.FileProvider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun canInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    private fun openInstallPermissionSettings(context: Context) {
        runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    companion object {
        private const val APK_FILE_NAME = "auramusic-tv-update.apk"
        private const val MIN_APK_BYTES = 1_000L
    }
}

@Composable
fun rememberTvUpdateInstallController(): TvUpdateInstallController {
    val controller = remember { TvUpdateInstallController() }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}
