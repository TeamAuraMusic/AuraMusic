package com.auramusic.desktop.mpv

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal JNA binding for the small part of libmpv the desktop spike needs.
 *
 * The DLL/shared library can be selected with -Dauramusic.mpv.library=<absolute path>
 * or MPV_LIBRARY. With no override, the platform loader looks for "mpv" on the
 * library path. Windows packaging docs live in docs/implementation/DESKTOP_SPIKE.md.
 */
private interface MpvLib : Library {
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_set_option_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_command(ctx: Pointer, args: Array<String>): Int
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    fun mpv_free(ptr: Pointer)
    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_wait_event(ctx: Pointer, timeout: Double): Pointer?
    fun mpv_destroy(ctx: Pointer)
    fun mpv_error_string(error: Int): String?

    companion object {
        val INSTANCE: MpvLib by lazy {
            val explicit = System.getProperty("auramusic.mpv.library") ?: System.getenv("MPV_LIBRARY")
            if (!explicit.isNullOrBlank()) {
                Native.load(explicit, MpvLib::class.java)
            } else {
                // Compose Desktop points this at the packaged app resources dir.
                val resources = System.getProperty("compose.application.resources.dir")
                val candidates = listOfNotNull(
                    resources?.let { "$it${File.separator}mpv-2.dll" },
                    resources?.let { "$it${File.separator}libmpv-2.dll" },
                    resources?.let { "$it${File.separator}libmpv.so" },
                    resources?.let { "$it${File.separator}libmpv.dylib" },
                )
                candidates.firstOrNull { File(it).exists() }
                    ?.let { Native.load(it, MpvLib::class.java) }
                    ?: Native.load("mpv", MpvLib::class.java)
            }
        }
    }
}

/** A single libmpv instance for audio playback. No Media3/ExoPlayer on this path. */
class MpvPlayer {
    private val running = AtomicBoolean(true)
    private var pollThread: Thread? = null

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0.0)
    val position: StateFlow<Double> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0.0)
    val duration: StateFlow<Double> = _duration.asStateFlow()

    private val ctx: Pointer? = createContext()

    val available: Boolean get() = ctx != null

    private fun createContext(): Pointer? {
        var handle: Pointer? = null
        val created = try {
            handle = MpvLib.INSTANCE.mpv_create()
            if (handle == null) {
                _error.value = "libmpv could not create a player handle"
                null
            } else {
                option(handle, "idle", "yes")
                option(handle, "force-window", "no")
                // Audio-only spike: no video surface to host yet.
                option(handle, "vo", "null")
                option(handle, "msg-level", "all=error")
                val rc = MpvLib.INSTANCE.mpv_initialize(handle)
                if (rc < 0) throw IllegalStateException(describe(rc))
                handle
            }
        } catch (t: Throwable) {
            runCatching { handle?.let { MpvLib.INSTANCE.mpv_destroy(it) } }
            _error.value = "libmpv unavailable (${t.message}). Set MPV_LIBRARY to mpv-2.dll / libmpv.so."
            null
        }
        if (created != null) startPolling(created)
        return created
    }

    private fun startPolling(handle: Pointer) {
        Thread({
            while (running.get()) {
                runCatching { MpvLib.INSTANCE.mpv_wait_event(handle, 0.1) }
                _isPlaying.value = property(handle, "pause") == "no"
                _position.value = property(handle, "time-pos")?.toDoubleOrNull() ?: 0.0
                _duration.value = property(handle, "duration")?.toDoubleOrNull() ?: 0.0
                Thread.sleep(200)
            }
        }, "auramusic-mpv-events").apply {
            isDaemon = true
            pollThread = this
            start()
        }
    }

    private fun option(handle: Pointer, name: String, value: String) {
        val rc = MpvLib.INSTANCE.mpv_set_option_string(handle, name, value)
        if (rc < 0) throw IllegalStateException("$name: ${describe(rc)}")
    }

    private fun property(handle: Pointer, name: String): String? {
        val ptr = MpvLib.INSTANCE.mpv_get_property_string(handle, name) ?: return null
        return try {
            ptr.getString(0)
        } finally {
            MpvLib.INSTANCE.mpv_free(ptr)
        }
    }

    private fun setProperty(handle: Pointer, name: String, value: String) {
        val rc = MpvLib.INSTANCE.mpv_set_property_string(handle, name, value)
        if (rc < 0) throw IllegalStateException("$name: ${describe(rc)}")
    }

    private fun command(handle: Pointer, vararg args: String) {
        val rc = MpvLib.INSTANCE.mpv_command(handle, arrayOf(*args))
        if (rc < 0) throw IllegalStateException("${args.firstOrNull()}: ${describe(rc)}")
    }

    private fun describe(rc: Int): String =
        runCatching { MpvLib.INSTANCE.mpv_error_string(rc) ?: "error $rc" }.getOrDefault("error $rc")

    fun play(url: String, title: String) {
        val handle = ctx ?: return
        runCatching {
            setProperty(handle, "media-title", title)
            command(handle, "loadfile", url, "replace")
            setProperty(handle, "pause", "no")
            _title.value = title
            _error.value = null
        }.onFailure { _error.value = "Playback failed: ${it.message}" }
    }

    fun togglePause() {
        val handle = ctx ?: return
        runCatching {
            setProperty(handle, "pause", if (_isPlaying.value) "yes" else "no")
        }.onFailure { _error.value = it.message }
    }

    fun stop() {
        val handle = ctx ?: return
        runCatching {
            command(handle, "stop")
            _title.value = null
            _isPlaying.value = false
            _position.value = 0.0
            _duration.value = 0.0
        }.onFailure { _error.value = it.message }
    }

    fun seekBy(seconds: Int) {
        val handle = ctx ?: return
        runCatching { command(handle, "seek", seconds.toString(), "relative") }
            .onFailure { _error.value = it.message }
    }

    fun setVolume(percent: Int) {
        val handle = ctx ?: return
        runCatching { setProperty(handle, "volume", percent.coerceIn(0, 100).toString()) }
            .onFailure { _error.value = it.message }
    }

    fun close() {
        running.set(false)
        pollThread?.join(500)
        ctx?.let { runCatching { MpvLib.INSTANCE.mpv_destroy(it) } }
    }
}
