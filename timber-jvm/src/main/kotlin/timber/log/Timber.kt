package timber.log

/** Minimal JVM Timber replacement for the shared InnerTube sources. */
object Timber {
    private fun log(level: String, message: String, args: Array<out Any?>) {
        if (System.getProperty("auramusic.desktop.verbose") != "true") return
        val rendered = if (args.isEmpty()) message else runCatching {
            String.format(message, *args)
        }.getOrDefault(message)
        println("[InnerTube/$level] $rendered")
    }

    fun v(message: String, vararg args: Any?) = log("V", message, args)
    fun d(message: String, vararg args: Any?) = log("D", message, args)
    fun i(message: String, vararg args: Any?) = log("I", message, args)
    fun w(message: String, vararg args: Any?) = log("W", message, args)
    fun e(message: String, vararg args: Any?) = log("E", message, args)
    fun e(t: Throwable, message: String, vararg args: Any?) {
        log("E", message, args)
        if (System.getProperty("auramusic.desktop.verbose") == "true") t.printStackTrace()
    }
}
