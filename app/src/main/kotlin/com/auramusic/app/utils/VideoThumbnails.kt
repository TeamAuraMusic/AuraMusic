package com.auramusic.app.utils

/**
 * YouTube thumbnail URL helpers. ytimg serves i.ytimg.com/vi/<id>/<variant>.jpg
 * with hqdefault always present and maxresdefault only for HD masters.
 */
object VideoThumbnails {
    fun highQuality(videoId: String): String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

    fun medium(videoId: String): String = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

    fun maxRes(videoId: String): String = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
}
