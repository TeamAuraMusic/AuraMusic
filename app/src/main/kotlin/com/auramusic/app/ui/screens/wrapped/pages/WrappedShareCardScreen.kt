/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.screens.wrapped.pages

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.R
import com.auramusic.app.ui.screens.wrapped.WrappedConstants
import com.auramusic.app.ui.screens.wrapped.WrappedState
import com.auramusic.app.ui.screens.wrapped.components.AnimatedBackground
import com.auramusic.app.ui.screens.wrapped.components.ShapeType
import com.auramusic.app.ui.theme.bbh_bartle
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

@Composable
fun WrappedShareCardScreen(
    state: WrappedState,
    isVisible: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(200)
            visible = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle, ShapeType.Line))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 200)) + slideInVertically(animationSpec = tween(1000, delayMillis = 200))
            ) {
                Text(
                    text = "Share Your Stats",
                    style = TextStyle(
                        fontFamily = bbh_bartle,
                        fontSize = 40.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 400)) + slideInVertically(animationSpec = tween(1000, delayMillis = 400))
            ) {
                Text(
                    text = "Show the world your music taste",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 600)) + slideInVertically(animationSpec = tween(1000, delayMillis = 600))
            ) {
                Button(
                    onClick = {
                        val bitmap = generateShareCardBitmap(context, state)
                        saveAndShareBitmap(context, bitmap)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notification_icon),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        text = "Generate & Share",
                        style = TextStyle(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}

private fun generateShareCardBitmap(context: Context, state: WrappedState): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background gradient
    val gradient = LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        intArrayOf(
            android.graphics.Color.parseColor("#1a1a2e"),
            android.graphics.Color.parseColor("#16213e"),
            android.graphics.Color.parseColor("#0f3460")
        ),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
        shader = gradient
    })

    // Title
    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 72f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("AuraMusic", width / 2f, 200f, titlePaint)

    val subtitlePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#B0B0B0")
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(WrappedConstants.displayMonthYear, width / 2f, 280f, subtitlePaint)

    // Stats
    val statValuePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 96f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val statLabelPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#B0B0B0")
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    var yPos = 480f

    // Total minutes
    canvas.drawText("${state.totalMinutes}", width / 2f, yPos, statValuePaint)
    canvas.drawText("minutes listened", width / 2f, yPos + 50f, statLabelPaint)
    yPos += 200f

    // Unique songs
    canvas.drawText("${state.uniqueSongCount}", width / 2f, yPos, statValuePaint)
    canvas.drawText("unique songs", width / 2f, yPos + 50f, statLabelPaint)
    yPos += 200f

    // Top song
    state.topSongs.firstOrNull()?.let { topSong ->
        canvas.drawText("TOP SONG", width / 2f, yPos, statLabelPaint)
        yPos += 50f
        val songTitlePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val songTitle = if (topSong.title.length > 30) topSong.title.take(27) + "..." else topSong.title
        canvas.drawText(songTitle, width / 2f, yPos + 40f, songTitlePaint)
        yPos += 100f
        canvas.drawText(topSong.artistName ?: "", width / 2f, yPos, statLabelPaint)
        yPos += 120f
    }

    // Top artist
    state.topArtists.firstOrNull()?.let { topArtist ->
        canvas.drawText("TOP ARTIST", width / 2f, yPos, statLabelPaint)
        yPos += 50f
        val artistNamePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val artistName = topArtist.artist?.name ?: "Unknown"
        canvas.drawText(artistName, width / 2f, yPos + 40f, artistNamePaint)
    }

    // Footer
    val footerPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#666666")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Made with AuraMusic", width / 2f, height - 100f, footerPaint)

    return bitmap
}

private fun saveAndShareBitmap(context: Context, bitmap: Bitmap) {
    val file = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "AuraMusic_Wrapped_${System.currentTimeMillis()}.png"
    )
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.FileProvider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_TEXT, "Check out my ${WrappedConstants.displayMonthYear} music stats on AuraMusic!")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Wrapped Stats"))
}
