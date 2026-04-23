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
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
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
        TWITTER("X", "com.twitter.android"),
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

            // Load album art
            val albumArtBitmap = songData.thumbnailUrl?.let { url ->
                try {
                    val inputStream = URL(url).openStream()
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) { null }
            }

            // Extract dominant color from thumbnail, fallback to dark
            val dominantColor = if (albumArtBitmap != null) {
                val palette = Palette.from(albumArtBitmap).generate()
                palette.getDarkMutedColor(
                    palette.getMutedColor(
                        palette.getDominantColor(Color.parseColor("#1a1a2e"))
                    )
                )
            } else {
                Color.parseColor("#1a1a2e")
            }

            // Draw gradient background using thumbnail colors
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, cardHeight.toFloat(),
                    intArrayOf(
                        darkenColor(dominantColor, 0.4f),
                        darkenColor(dominantColor, 0.15f)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)

            // Draw app icon
            val appIconSize = 56
            try {
                val iconDrawable = context.packageManager.getApplicationIcon(context.packageName)
                val iconBitmap = Bitmap.createBitmap(appIconSize, appIconSize, Bitmap.Config.ARGB_8888)
                val iconCanvas = Canvas(iconBitmap)
                iconDrawable.setBounds(0, 0, appIconSize, appIconSize)
                iconDrawable.draw(iconCanvas)
                canvas.drawBitmap(iconBitmap, 60f, 50f, null)
            } catch (_: Exception) {}

            // Draw "AuraMusic" text with brand gradient (orange to pink)
            val logoPaint = Paint().apply {
                textSize = 48f
                typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
                isAntiAlias = true
                shader = LinearGradient(
                    130f, 0f, 400f, 0f,
                    Color.parseColor("#F97316"),
                    Color.parseColor("#EC4899"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawText("AuraMusic", 130f, 90f, logoPaint)

            // Draw album art centered with rounded corners
            val albumArtSize = (cardWidth * 0.55).toInt()
            if (albumArtBitmap != null) {
                val scaledArt = Bitmap.createScaledBitmap(albumArtBitmap, albumArtSize, albumArtSize, true)
                val albumLeft = ((cardWidth - albumArtSize) / 2).toFloat()
                val albumTop = (cardHeight * 0.14).toFloat()

                // Draw with rounded corners
                val artPaint = Paint().apply { isAntiAlias = true }
                val artRect = RectF(albumLeft, albumTop, albumLeft + albumArtSize, albumTop + albumArtSize)
                val path = android.graphics.Path().apply {
                    addRoundRect(artRect, 24f, 24f, android.graphics.Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(path)
                canvas.drawBitmap(scaledArt, albumLeft, albumTop, artPaint)
                canvas.restore()
            }

            // "Now Playing" label
            val nowPlayingPaint = Paint().apply {
                color = Color.parseColor("#80FFFFFF")
                textSize = 40f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("Now Playing", 60f, (cardHeight * 0.72).toFloat(), nowPlayingPaint)

            // Song title
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 72f
                typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
                isAntiAlias = true
            }
            val maxTitleWidth = cardWidth - 120
            var titleText = songData.title
            while (titlePaint.measureText(titleText) > maxTitleWidth && titleText.isNotEmpty()) {
                titleText = titleText.dropLast(1)
            }
            if (titleText != songData.title) titleText += "…"
            canvas.drawText(titleText, 60f, (cardHeight * 0.78).toFloat(), titlePaint)

            // Artist name
            val artistPaint = Paint().apply {
                color = Color.parseColor("#B3FFFFFF")
                textSize = 52f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            var artistText = songData.artist
            while (artistPaint.measureText(artistText) > maxTitleWidth && artistText.isNotEmpty()) {
                artistText = artistText.dropLast(1)
            }
            if (artistText != songData.artist) artistText += "…"
            canvas.drawText(artistText, 60f, (cardHeight * 0.85).toFloat(), artistPaint)

            // Deep link URL at bottom
            val deepLink = "auramusic.site/play/${songData.id}"
            val linkPaint = Paint().apply {
                color = Color.parseColor("#99FFFFFF")
                textSize = 36f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText(deepLink, 60f, (cardHeight * 0.93).toFloat(), linkPaint)

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
                    "${context.packageName}.FileProvider",
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
        val shareLink = "https://www.auramusic.site/play/${songData.id}"
        val shareText = "🎵 ${songData.title} - ${songData.artist}\nTap to play in AuraMusic: $shareLink"

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = if (cardFile != null) "image/jpeg" else "text/plain"

            if (cardFile != null) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.FileProvider",
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
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
