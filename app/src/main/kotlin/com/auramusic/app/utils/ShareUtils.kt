/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object ShareUtils {

    data class SongShareData(
        val id: String,
        val title: String,
        val artist: String,
        val album: String? = null,
        val thumbnailUrl: String? = null,
        val duration: Long? = null
    )

    enum class SharePlatform(val displayName: String, val packageName: String?) {
        INSTAGRAM("Instagram", "com.instagram.android"),
        FACEBOOK("Facebook", "com.facebook.katana"),
        WHATSAPP("WhatsApp", "com.whatsapp"),
        TWITTER("X (Twitter)", "com.twitter.android"),
        TELEGRAM("Telegram", "org.telegram.messenger"),
        SNAPCHAT("Snapchat", "com.snapchat.android"),
        TIKTOK("TikTok", "com.zhiliaoapp.musically"),
        GENERIC("More Apps", null)
    }

    suspend fun generateShareCard(
        context: Context,
        songData: SongShareData,
        cardWidth: Int = 1080,
        cardHeight: Int = 1080
    ): File? = withContext(Dispatchers.IO) {
        try {
            val bitmap = createBitmap(cardWidth, cardHeight)
            val canvas = Canvas(bitmap)

            val dominantColor = Color.parseColor("#1DB954")

            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, cardHeight.toFloat(),
                    intArrayOf(
                        darkenColor(dominantColor, 0.3f),
                        darkenColor(dominantColor, 0.7f)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), paint)

            val albumArtSize = (cardWidth * 0.55).toInt()
            val albumArtBitmap = songData.thumbnailUrl?.let { url ->
                try {
                    // Load image from URL directly
                    val inputStream = URL(url).openStream()
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) { null }
            }

            if (albumArtBitmap != null) {
                val albumLeft = (cardWidth - albumArtSize) / 2
                val albumTop = (cardHeight * 0.12).toInt()
                canvas.drawBitmap(albumArtBitmap, albumLeft.toFloat(), albumTop.toFloat(), null)
            }

            val logoPaint = Paint().apply {
                color = Color.WHITE
                textSize = 48f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("AuraMusic", 60f, 90f, logoPaint)

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 72f
                typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
                isAntiAlias = true
            }

            val maxTitleWidth = cardWidth - 120
            var titleText = songData.title
            while (titlePaint.measureText("$titleText...") > maxTitleWidth && titleText.isNotEmpty()) {
                titleText = titleText.dropLast(1)
            }
            if (!songData.title.startsWith(titleText)) titleText += "..."
            canvas.drawText(titleText, 60f, (cardHeight * 0.78).toFloat(), titlePaint)

            val artistPaint = Paint().apply {
                color = Color.parseColor("#B3FFFFFF")
                textSize = 52f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            var artistText = songData.artist
            while (artistPaint.measureText("$artistText...") > maxTitleWidth && artistText.isNotEmpty()) {
                artistText = artistText.dropLast(1)
            }
            if (!songData.artist.startsWith(artistText)) artistText += "..."
            canvas.drawText(artistText, 60f, (cardHeight * 0.85).toFloat(), artistPaint)

            val nowPlayingPaint = Paint().apply {
                color = Color.parseColor("#80FFFFFF")
                textSize = 40f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("Now Playing", 60f, (cardHeight * 0.72).toFloat(), nowPlayingPaint)

            // Add "Tap to play" text at bottom
            val tapPaint = Paint().apply {
                color = Color.parseColor("#1DB954")
                textSize = 44f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("Tap to play in AuraMusic", 60f, (cardHeight * 0.93).toFloat(), tapPaint)

            val cacheDir = File(context.cacheDir, "share_cards")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "song_card_${songData.id}.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareToSocialMedia(
        context: Context,
        songData: SongShareData,
        platform: SharePlatform,
        cardFile: File? = null
    ) {
        // Use AuraMusic deep link that opens the app directly
        val deepLink = "https://www.auramusic.site/play/${songData.id}"
        
        val shareText = buildString {
            append("🎵 ${songData.title} - ${songData.artist}\n")
            append("Tap to play in AuraMusic: $deepLink")
        }

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = if (cardFile != null) "image/jpeg" else "text/plain"

            if (cardFile != null) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    cardFile
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
            } else {
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            if (platform.packageName != null) {
                setPackage(platform.packageName)
            }

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            if (platform == SharePlatform.GENERIC) {
                context.startActivity(Intent.createChooser(intent, "Share via"))
            } else {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            if (platform != SharePlatform.GENERIC) {
                shareToSocialMedia(context, songData, SharePlatform.GENERIC, cardFile)
            }
        }
    }

    fun shareToAllPlatforms(
        context: Context,
        songData: SongShareData,
        cardFile: File? = null
    ) {
        val shareLink = "https://music.youtube.com/watch?v=${songData.id}"
        val shareText = "🎵 ${songData.title} - ${songData.artist}\nListen on AuraMusic: $shareLink"

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = if (cardFile != null) "image/jpeg" else "text/plain"

            if (cardFile != null) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    cardFile
                )
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share song via"))
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (android.graphics.Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (android.graphics.Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (android.graphics.Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
