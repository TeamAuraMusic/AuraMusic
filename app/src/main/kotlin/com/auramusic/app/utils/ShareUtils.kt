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
        X("X", "com.twitter.android"),
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

            // Extract vibrant colors from thumbnail for modern design
            val palette = albumArtBitmap?.let { Palette.from(it).generate() }
            val dominantColor = palette?.getDominantColor(Color.parseColor("#6366f1")) ?: Color.parseColor("#6366f1")
            val vibrantColor = palette?.getVibrantColor(Color.parseColor("#8b5cf6")) ?: Color.parseColor("#8b5cf6")
            val mutedColor = palette?.getMutedColor(Color.parseColor("#1e1b4b")) ?: Color.parseColor("#1e1b4b")

            // Create modern multi-layer gradient background
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
                    intArrayOf(
                        darkenColor(dominantColor, 0.6f),
                        darkenColor(mutedColor, 0.3f),
                        darkenColor(vibrantColor, 0.8f),
                        darkenColor(dominantColor, 0.9f)
                    ),
                    floatArrayOf(0f, 0.4f, 0.7f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)

            // Add subtle overlay pattern
            val overlayPaint = Paint().apply {
                color = Color.parseColor("#0A000000")
                alpha = 30
            }
            canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), overlayPaint)

            // Draw album art with modern styling
            val albumArtSize = (cardWidth * 0.65).toInt()
            if (albumArtBitmap != null) {
                val scaledArt = Bitmap.createScaledBitmap(albumArtBitmap, albumArtSize, albumArtSize, true)
                val albumLeft = ((cardWidth - albumArtSize) / 2).toFloat()
                val albumTop = (cardHeight * 0.18).toFloat()

                // Draw shadow effect
                val shadowPaint = Paint().apply {
                    color = Color.parseColor("#40000000")
                    setShadowLayer(20f, 0f, 10f, Color.parseColor("#40000000"))
                    isAntiAlias = true
                }
                val shadowRect = RectF(albumLeft + 8, albumTop + 8, albumLeft + albumArtSize + 8, albumTop + albumArtSize + 8)
                canvas.drawRoundRect(shadowRect, 32f, 32f, shadowPaint)

                // Draw album art with rounded corners
                val artPaint = Paint().apply { isAntiAlias = true }
                val artRect = RectF(albumLeft, albumTop, albumLeft + albumArtSize, albumTop + albumArtSize)
                val path = android.graphics.Path().apply {
                    addRoundRect(artRect, 32f, 32f, android.graphics.Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(path)
                canvas.drawBitmap(scaledArt, albumLeft, albumTop, artPaint)
                canvas.restore()

                // Add subtle inner glow
                val glowPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    color = Color.WHITE
                    alpha = 100
                    isAntiAlias = true
                }
                canvas.drawRoundRect(artRect, 32f, 32f, glowPaint)
            }

            // Modern app branding at top
            val brandingY = cardHeight * 0.08f

            // Draw app icon with modern styling
            val appIconSize = 64
            try {
                val iconDrawable = context.packageManager.getApplicationIcon(context.packageName)
                val iconBitmap = Bitmap.createBitmap(appIconSize, appIconSize, Bitmap.Config.ARGB_8888)
                val iconCanvas = Canvas(iconBitmap)
                iconDrawable.setBounds(0, 0, appIconSize, appIconSize)
                iconDrawable.draw(iconCanvas)

                // Add icon background
                val iconBgPaint = Paint().apply {
                    color = Color.WHITE
                    alpha = 200
                    isAntiAlias = true
                }
                canvas.drawCircle(60f + appIconSize/2, brandingY - 10, appIconSize/2 + 8f, iconBgPaint)
                canvas.drawBitmap(iconBitmap, 60f, brandingY - appIconSize/2 - 10, null)
            } catch (_: Exception) {}

            // Modern AuraMusic logo with gradient
            val logoPaint = Paint().apply {
                textSize = 52f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                isAntiAlias = true
                shader = LinearGradient(
                    140f, brandingY - 20, 420f, brandingY - 20,
                    intArrayOf(
                        Color.parseColor("#F59E0B"),
                        Color.parseColor("#F97316"),
                        Color.parseColor("#EC4899"),
                        Color.parseColor("#8B5CF6")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawText("AuraMusic", 140f, brandingY + 8, logoPaint)

            // Modern "Now Playing" badge
            val badgeWidth = 280f
            val badgeHeight = 60f
            val badgeX = 60f
            val badgeY = cardHeight * 0.72f

            // Badge background with gradient
            val badgeBgPaint = Paint().apply {
                shader = LinearGradient(
                    badgeX, badgeY, badgeX + badgeWidth, badgeY,
                    intArrayOf(
                        Color.parseColor("#FFFFFF"),
                        Color.parseColor("#F8FAFC")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                alpha = 220
                isAntiAlias = true
            }
            val badgeRect = RectF(badgeX, badgeY - badgeHeight, badgeX + badgeWidth, badgeY)
            canvas.drawRoundRect(badgeRect, badgeHeight/2, badgeHeight/2, badgeBgPaint)

            // Badge border
            val badgeBorderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.parseColor("#E2E8F0")
                alpha = 150
                isAntiAlias = true
            }
            canvas.drawRoundRect(badgeRect, badgeHeight/2, badgeHeight/2, badgeBorderPaint)

            // "Now Playing" text in badge
            val badgeTextPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 24f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                isAntiAlias = true
            }
            val badgeText = "♪ Now Playing"
            val badgeTextWidth = badgeTextPaint.measureText(badgeText)
            canvas.drawText(badgeText, badgeX + (badgeWidth - badgeTextWidth)/2, badgeY - badgeHeight/2 + 8, badgeTextPaint)

            // Song information with modern typography
            val contentY = cardHeight * 0.82f
            val maxTextWidth = cardWidth - 120

            // Song title with drop shadow effect
            val titleShadowPaint = Paint().apply {
                color = Color.parseColor("#000000")
                textSize = 76f
                typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
                alpha = 100
                isAntiAlias = true
            }
            var titleText = songData.title
            while (titleShadowPaint.measureText(titleText) > maxTextWidth && titleText.isNotEmpty()) {
                titleText = titleText.dropLast(1)
            }
            if (titleText != songData.title) titleText += "…"

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 76f
                typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(8f, 0f, 4f, Color.parseColor("#40000000"))
            }
            canvas.drawText(titleText, 60f + 2, contentY + 2, titleShadowPaint)
            canvas.drawText(titleText, 60f, contentY, titlePaint)

            // Artist name with modern styling
            val artistY = contentY + 90
            val artistPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                textSize = 48f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                alpha = 240
                isAntiAlias = true
                setShadowLayer(4f, 0f, 2f, Color.parseColor("#20000000"))
            }
            var artistText = songData.artist
            while (artistPaint.measureText(artistText) > maxTextWidth && artistText.isNotEmpty()) {
                artistText = artistText.dropLast(1)
            }
            if (artistText != songData.artist) artistText += "…"
            canvas.drawText(artistText, 60f, artistY, artistPaint)

            // Modern footer with deep link
            val footerY = cardHeight - 80
            val deepLink = "auramusic.site/play/${songData.id}"

            // Footer background bar
            val footerBgPaint = Paint().apply {
                color = Color.parseColor("#FFFFFF")
                alpha = 120
            }
            canvas.drawRect(0f, footerY.toFloat() - 20f, cardWidth.toFloat(), cardHeight.toFloat(), footerBgPaint)

            // Footer text
            val footerPaint = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 32f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            val footerText = "Tap to listen on AuraMusic"
            val footerTextWidth = footerPaint.measureText(footerText)
            canvas.drawText(footerText, ((cardWidth.toFloat() - footerTextWidth)/2), footerY.toFloat() + 15f, footerPaint)

            // QR-like link (simplified)
            val linkPaint = Paint().apply {
                color = Color.parseColor("#6B7280")
                textSize = 28f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            val linkTextWidth = linkPaint.measureText(deepLink)
            canvas.drawText(deepLink, ((cardWidth.toFloat() - linkTextWidth)/2), footerY.toFloat() + 50f, linkPaint)

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
