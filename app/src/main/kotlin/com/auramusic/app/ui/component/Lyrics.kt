/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0. See LICENSE file for details.
 */

package com.auramusic.app.ui.component


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.text.Layout
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.auramusic.app.LocalListenTogetherManager
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.DarkModeKey
import com.auramusic.app.constants.LyricsAnimationStyle
import com.auramusic.app.constants.LyricsAnimationStyleKey
import com.auramusic.app.constants.LyricsClickKey
import com.auramusic.app.constants.LyricsConnectedLinesKey
import com.auramusic.app.constants.LyricsFont
import com.auramusic.app.constants.LyricsFontKey
import com.auramusic.app.constants.LyricsGlowEffectKey
import com.auramusic.app.constants.EnhancedLyricsKey
import com.auramusic.app.constants.LyricsInstrumentalGapMsKey
import com.auramusic.app.constants.LyricsLineSpacingKey
import com.auramusic.app.constants.LyricsRomanizeBelarusianKey
import com.auramusic.app.constants.LyricsRomanizeBulgarianKey
import com.auramusic.app.constants.LyricsRomanizeChineseKey
import com.auramusic.app.constants.LyricsRomanizeCyrillicByLineKey
import com.auramusic.app.constants.LyricsRomanizeJapaneseKey
import com.auramusic.app.constants.LyricsRomanizeKoreanKey
import com.auramusic.app.constants.LyricsRomanizeKyrgyzKey
import com.auramusic.app.constants.LyricsRomanizeMacedonianKey
import com.auramusic.app.constants.LyricsRomanizeRussianKey
import com.auramusic.app.constants.LyricsRomanizeSerbianKey
import com.auramusic.app.constants.LyricsRomanizeUkrainianKey
import com.auramusic.app.constants.LyricsScrollKey
import com.auramusic.app.constants.LyricsTextPositionKey
import com.auramusic.app.constants.LyricsTextSizeKey
import com.auramusic.app.constants.PlayerBackgroundStyle
import com.auramusic.app.constants.OpenRouterApiKey
import com.auramusic.app.constants.OpenRouterBaseUrlKey
import com.auramusic.app.constants.OpenRouterModelKey
import com.auramusic.app.constants.AutoTranslateLyricsKey
import com.auramusic.app.constants.AutoTranslateLyricsMismatchKey
import com.auramusic.app.constants.TranslateLanguageKey
import com.auramusic.app.constants.TranslateModeKey
import com.auramusic.app.constants.PlayerBackgroundStyleKey
import com.auramusic.app.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.auramusic.app.lyrics.LyricsEntry
import com.auramusic.app.lyrics.LyricsUtils.findCurrentLineIndex
import com.auramusic.app.lyrics.LyricsUtils.isBelarusian
import com.auramusic.app.lyrics.LyricsUtils.isBulgarian
import com.auramusic.app.lyrics.LyricsUtils.isChinese
import com.auramusic.app.lyrics.LyricsUtils.isJapanese
import com.auramusic.app.lyrics.LyricsUtils.isKorean
import com.auramusic.app.lyrics.LyricsUtils.isKyrgyz
import com.auramusic.app.lyrics.LyricsUtils.isMacedonian
import com.auramusic.app.lyrics.LyricsUtils.isRussian
import com.auramusic.app.lyrics.LyricsUtils.isSerbian
import com.auramusic.app.lyrics.LyricsUtils.isUkrainian
import com.auramusic.app.lyrics.LyricsUtils.parseLyrics
import com.auramusic.app.lyrics.LyricsUtils.romanizeChinese
import com.auramusic.app.lyrics.LyricsUtils.romanizeCyrillic
import com.auramusic.app.lyrics.LyricsUtils.romanizeJapanese
import com.auramusic.app.lyrics.LyricsUtils.romanizeKorean
import com.auramusic.app.lyrics.LyricsTranslationHelper
import com.auramusic.app.ui.component.shimmer.ShimmerHost
import com.auramusic.app.ui.component.shimmer.TextPlaceholder
import com.auramusic.app.ui.screens.settings.DarkMode
import com.auramusic.app.ui.screens.settings.LyricsPosition
import com.auramusic.app.ui.theme.Manrope
import com.auramusic.app.ui.theme.Outfit
import com.auramusic.app.ui.theme.SpaceGrotesk
import com.auramusic.app.ui.utils.fadingEdge
import com.auramusic.app.utils.ComposeToImage
import com.auramusic.app.ui.component.LyricsBackgroundStyle
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/**
 * Top-level Lyrics composable that dispatches between the Enhanced Lyrics
 * renderer (word-by-word, liquid glow, springs) and the original renderer
 * based on the [EnhancedLyricsKey] user preference. The public signature
 * stays unchanged so existing callers don't need to be updated.
 */
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    disableInteractiveFeatures: Boolean = false
) {
    val enhancedLyrics by rememberPreference(EnhancedLyricsKey, false)
    if (enhancedLyrics) {
        EnhancedLyricsView(
            sliderPositionProvider = sliderPositionProvider,
            modifier = modifier,
            showLyrics = showLyrics,
        )
    } else {
        OriginalLyrics(
            sliderPositionProvider = sliderPositionProvider,
            modifier = modifier,
            showLyrics = showLyrics,
            disableInteractiveFeatures = disableInteractiveFeatures,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun OriginalLyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    disableInteractiveFeatures: Boolean = false
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalWindowInfo.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)

    // When interactive features are disabled (e.g. on TV), suppress
    // click-to-seek behaviour. Auto-scrolling stays enabled so the synced
    // lyrics actually follow the song.
    val effectiveChangeLyrics = if (!disableInteractiveFeatures) changeLyrics else false
    val effectiveScrollLyrics = scrollLyrics
    val romanizeJapaneseLyrics by rememberPreference(LyricsRomanizeJapaneseKey, true)
    val romanizeKoreanLyrics by rememberPreference(LyricsRomanizeKoreanKey, true)
    val romanizeRussianLyrics by rememberPreference(LyricsRomanizeRussianKey, true)
    val romanizeUkrainianLyrics by rememberPreference(LyricsRomanizeUkrainianKey, true)
    val romanizeSerbianLyrics by rememberPreference(LyricsRomanizeSerbianKey, true)
    val romanizeBulgarianLyrics by rememberPreference(LyricsRomanizeBulgarianKey, true)
    val romanizeBelarusianLyrics by rememberPreference(LyricsRomanizeBelarusianKey, true)
    val romanizeKyrgyzLyrics by rememberPreference(LyricsRomanizeKyrgyzKey, true)
    val romanizeMacedonianLyrics by rememberPreference(LyricsRomanizeMacedonianKey, true)
    val romanizeCyrillicByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    val romanizeChineseLyrics by rememberPreference(LyricsRomanizeChineseKey, true)
    val lyricsGlowEffect by rememberPreference(LyricsGlowEffectKey, false)
    val enhancedLyrics by rememberPreference(EnhancedLyricsKey, false)
    val lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.NONE)
    val baseLyricsTextSize by rememberPreference(LyricsTextSizeKey, 24f)
    val lyricsTextSize = baseLyricsTextSize
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
    val instrumentalGapMs by rememberPreference(LyricsInstrumentalGapMsKey, 5000)
    val connectedLines by rememberPreference(LyricsConnectedLinesKey, true)
    val lyricsFont by rememberEnumPreference(LyricsFontKey, LyricsFont.SYSTEM_DEFAULT)
    val lyricsFontFamily = when (lyricsFont) {
        LyricsFont.SYSTEM_DEFAULT -> FontFamily.Default
        LyricsFont.OUTFIT -> Outfit
        LyricsFont.MANROPE -> Manrope
        LyricsFont.SPACE_GROTESK -> SpaceGrotesk
    }
    
    val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
    val openRouterModel by rememberPreference(OpenRouterModelKey, "mistralai/mistral-small-3.1-24b-instruct:free")
    val autoTranslateLyrics by rememberPreference(AutoTranslateLyricsKey, false)
    val autoTranslateLyricsMismatch by rememberPreference(AutoTranslateLyricsMismatchKey, false)
    val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(TranslateModeKey, "Literal")
    
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val lines = remember(lyrics, scope) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else if (lyrics.startsWith("[")) {
            val parsedLines = parseLyrics(lyrics)

            val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
            val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
            val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
            val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
            val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
            val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
            val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)

            parsedLines.map { entry ->
                val newEntry = LyricsEntry(entry.time, entry.text, entry.words, agent = entry.agent, isBackground = entry.isBackground)
                
                if (romanizeJapaneseLyrics && isJapanese(entry.text) && !isChinese(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeJapanese(entry.text)
                    }
                }

                if (romanizeKoreanLyrics && isKorean(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeKorean(entry.text)
                    }
                }

                if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(entry.text) else isRussianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(entry.text) else isUkrainianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(entry.text) else isSerbianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(entry.text) else isBulgarianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(entry.text) else isBelarusianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(entry.text) else isKyrgyzLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(entry.text) else isMacedonianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeChineseLyrics && isChinese(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeChinese(entry.text)
                    }
                }

                newEntry
            }.let {
                listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
            }
        } else {
            val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
            val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
            val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
            val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
            val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
            val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
            val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)

            lyrics.lines().mapIndexed { index, line ->
                val newEntry = LyricsEntry(index * 100L, line)

                if (romanizeJapaneseLyrics && isJapanese(line) && !isChinese(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeJapanese(line)
                    }
                }

                if (romanizeKoreanLyrics && isKorean(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeKorean(line)
                    }
                }

                if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(line) else isRussianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(line) else isUkrainianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(line) else isSerbianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(line) else isBulgarianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(line) else isBelarusianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(line) else isKyrgyzLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(line) else isMacedonianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeChineseLyrics && isChinese(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeChinese(line)
                    }
                }

                newEntry
            }
        }
    }
    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    // Insert pseudo "instrumental" entries between lines whose gap is large
    // enough. Disabled when [instrumentalGapMs] <= 0 or for unsynced lyrics.
    val displayLines = remember(lines, instrumentalGapMs, isSynced, enhancedLyrics) {
        if (enhancedLyrics) {
            // Enhanced lyrics: show the intro interval indicator before the
            // first vocal line, and (when allowed) also keep the regular
            // instrumental indicators between lyric lines so the user gets
            // both behaviors at once.
            buildList {
                val allLines = if (lines.isNotEmpty()) {
                    listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + lines
                } else {
                    emptyList()
                }

                for (i in allLines.indices) {
                    val entry = allLines[i]
                    if (entry.text.isNotBlank()) {
                        add(entry)
                    }
                    val nextEntry = allLines.getOrNull(i + 1) ?: continue
                    val gap = nextEntry.time - entry.time
                    if (entry === LyricsEntry.HEAD_LYRICS_ENTRY) {
                        // Intro circular wavy indicator before the first
                        // vocal line.
                        if (gap > 1000L) {
                            add(
                                LyricsEntry(
                                    time = entry.time,
                                    text = "",
                                    isIntervalIndicator = true,
                                    intervalGapStart = entry.time,
                                    intervalGapEnd = nextEntry.time
                                )
                            )
                        }
                    } else if (isSynced && instrumentalGapMs > 0 && gap >= instrumentalGapMs) {
                        // Regular instrumental indicator (music notes +
                        // progress) between two real lyric lines.
                        add(
                            LyricsEntry(
                                time = entry.time + gap / 2,
                                text = "",
                                isInstrumental = true,
                                endTime = nextEntry.time
                            )
                        )
                    }
                }
            }
        } else if (!isSynced || instrumentalGapMs <= 0 || lines.size < 2) {
            lines
        } else {
            buildList {
                for (i in lines.indices) {
                    val current = lines[i]
                    add(current)
                    val next = lines.getOrNull(i + 1) ?: continue
                    // For instrumental indicator, use raw gap between line timestamps
                    val gap = next.time - current.time
                    // Only show indicator after a real lyric line (skip the
                    // synthetic HEAD entry to avoid an indicator before song
                    // starts when the first line has a normal lead-in).
                    val isHead = current === LyricsEntry.HEAD_LYRICS_ENTRY
                    timber.log.Timber.d("Lyrics: gap between lines $i and ${i+1}: ${gap}ms (threshold: ${instrumentalGapMs}ms)")
                    if (gap >= instrumentalGapMs && !isHead) {
                        timber.log.Timber.d("Lyrics: Adding instrumental indicator at ${current.time + gap / 2}ms")
                        add(
                            LyricsEntry(
                                time = current.time + gap / 2, // Place in middle of gap
                                text = "",
                                isInstrumental = true,
                                endTime = next.time
                            )
                        )
                    }
                }
            }
        }
    }

    // State for translation status
    val translationStatus by LyricsTranslationHelper.status.collectAsState()
    
    // Track composition lifecycle
    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose {
            LyricsTranslationHelper.setCompositionActive(false)
            LyricsTranslationHelper.cancelTranslation()
        }
    }
    
    // Listen for manual trigger
    LaunchedEffect(showLyrics, lines.size, openRouterApiKey) {
        LyricsTranslationHelper.manualTrigger.collect {
             if (showLyrics && lines.isNotEmpty() && openRouterApiKey.isNotBlank()) {
                 LyricsTranslationHelper.translateLyrics(
                     lyrics = lines,
                     targetLanguage = translateLanguage,
                     apiKey = openRouterApiKey,
                     baseUrl = openRouterBaseUrl,
                     model = openRouterModel,
                     mode = translateMode,
                     scope = scope,
                     context = context
                 )
             } else if (openRouterApiKey.isBlank()) {
                 Toast.makeText(context, context.getString(R.string.ai_api_key_required), Toast.LENGTH_SHORT).show()
             }
        }
    }

    LaunchedEffect(lines, autoTranslateLyrics, autoTranslateLyricsMismatch, openRouterApiKey, translateMode, translateLanguage) {
        if (lines.isNotEmpty()) {
            // Reset status if auto-translate is disabled
            if (!autoTranslateLyrics) {
                LyricsTranslationHelper.resetStatus()
                return@LaunchedEffect
            }
            
            // First, try to apply cached translations
            val targetLang = if (autoTranslateLyricsMismatch) java.util.Locale.getDefault().language else translateLanguage
            val hasCached = LyricsTranslationHelper.applyCachedTranslations(lines, translateMode, targetLang)
            
            // If no cache and auto-translate is enabled, translate
            if (!hasCached && autoTranslateLyrics && openRouterApiKey.isNotBlank()) {
                val needsTranslation = lines.any { it.translatedTextFlow.value == null && it.text.isNotBlank() }
                if (needsTranslation) {
                    var shouldTranslate = true
                    if (autoTranslateLyricsMismatch) {
                        try {
                            val combinedText = lines.take(5).joinToString(" ") { it.text }
                            // Skip language detection if ML Kit not available (FOSS builds)
                            // Default to translating if detection fails
                            var detectedLang: String? = null
                            try {
                                val langHelper = Class.forName("com.auramusic.app.lyrics.LanguageDetectionHelper")
                                val method = langHelper.getMethod("identifyLanguage", String::class.java)
                                detectedLang = method.invoke(null, combinedText) as? String
                            } catch (e: Exception) {
                                // ML Kit not available in FOSS build, proceed with translation
                            }
                            val systemLang = java.util.Locale.getDefault().language
                            
                            if (detectedLang != null && detectedLang == systemLang) {
                                shouldTranslate = false
                            }
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "Language detection failed, proceeding with translation")
                        }
                    }

                    if (shouldTranslate) {
                        LyricsTranslationHelper.translateLyrics(
                            lyrics = lines,
                            targetLanguage = targetLang,
                            apiKey = openRouterApiKey,
                            baseUrl = openRouterBaseUrl,
                            model = openRouterModel,
                            mode = translateMode,
                            scope = scope,
                            context = context
                        )
                    }
                }
            }
        }
    }

    // Use Material 3 expressive accents and keep glow/text colors unified
    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        PlayerBackgroundStyle.BLUR,
        PlayerBackgroundStyle.GRADIENT,
        PlayerBackgroundStyle.ANIMATED_GRADIENT -> {
            // For blur/gradient backgrounds, always use light colors regardless of theme
            Color.White
        }
    }
    val textColor = expressiveAccent

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }
    var currentPlaybackPosition by remember {
        mutableLongStateOf(0L)
    }
    // Because LaunchedEffect has delay, which leads to inconsistent with current line color and scroll animation,
    // we use deferredCurrentLineIndex when user is scrolling
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var previousLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var isSeeking by remember {
        mutableStateOf(false)
    }

    var initialScrollDone by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldScrollToFirstLine by rememberSaveable {
        mutableStateOf(true)
    }

    var isAppMinimized by rememberSaveable {
        mutableStateOf(false)
    }

    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    // State for multi-selection
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) } // State for showing max selection toast

    val isLyricsProviderShown = lyricsEntity?.provider != null && lyricsEntity?.provider != "Unknown" && !isSelectionModeActive

    val lazyListState = rememberLazyListState()
    
    // Professional animation states for smooth transitions
    var isAnimating by remember { mutableStateOf(false) }
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

    // Handle back button press - close selection mode instead of exiting screen
    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    // Define max selection limit
    val maxSelectionLimit = 5

    // Show toast when max selection is reached
    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep screen on while lyrics are visible
    DisposableEffect(showLyrics) {
        val activity = context as? Activity
        if (showLyrics) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if(event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset selection mode if lyrics change
    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(8) // Faster update for word-by-word animation
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            val position = sliderPosition ?: playerConnection.player.currentPosition
            currentPlaybackPosition = position
            val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
            currentLineIndex = findCurrentLineIndex(displayLines, position - lyricsOffset)
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(2.seconds)
            lastPreviewTime = 0L
        }
    }

    suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 1500) {
        if (isAnimating) return // Prevent multiple animations
        isAnimating = true
        try {
            val lookUpIndex = if (isLyricsProviderShown) targetIndex + 1 else targetIndex
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lookUpIndex }
            if (itemInfo != null) {
                // Item is visible, animate directly to center without sudden jumps
                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val offset = itemCenter - center
                if (kotlin.math.abs(offset) > 10) {
                    lazyListState.animateScrollBy(
                        value = offset.toFloat(),
                        animationSpec = tween(durationMillis = duration)
                    )
                }
            } else {
                // Item is not visible, scroll to it first without animation, then it will be handled in next cycle
                lazyListState.scrollToItem(targetIndex)
            }
        } finally {
            isAnimating = false
        }
    }
    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled) {
        if (!isSynced) return@LaunchedEffect
        if (isAutoScrollEnabled) {
        if((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            // Initial scroll to center the first line with medium animation (600ms)
            val initialCenterIndex = kotlin.math.max(0, currentLineIndex)
            performSmoothPageScroll(initialCenterIndex, 800) // Initial scroll duration
            if(!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                // Fast scroll for seeking to center the target line (300ms)
                val seekCenterIndex = kotlin.math.max(0, currentLineIndex)
                performSmoothPageScroll(seekCenterIndex, 500) // Fast seek duration
            } else if ((lastPreviewTime == 0L || currentLineIndex != previousLineIndex) && effectiveScrollLyrics) {
                // Auto-scroll when lyrics settings allow it
                if (currentLineIndex != previousLineIndex) {
                    // Calculate which line should be at the top to center the active group
                    val centerTargetIndex = currentLineIndex
                    performSmoothPageScroll(centerTargetIndex, 1500) // Auto scroll duration
                }
            }
        if(currentLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousLineIndex = currentLineIndex
        }
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        // Status UI for translation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(top = 56.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val status = translationStatus) {
                is LyricsTranslationHelper.TranslationStatus.Translating -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.ai_translating_lyrics),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                is LyricsTranslationHelper.TranslationStatus.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.error),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                is LyricsTranslationHelper.TranslationStatus.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.ai_lyrics_translated),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                is LyricsTranslationHelper.TranslationStatus.Idle -> {
                    // No status display
                }
            }
        }

        if (lyrics == LYRICS_NOT_FOUND) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        } else {
            val baseLyricsStyle = LocalTextStyle.current
            val effectiveLyricsStyle = baseLyricsStyle.copy(fontFamily = lyricsFontFamily)
            ProvideTextStyle(value = effectiveLyricsStyle) {
            LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars
                .only(WindowInsetsSides.Top)
                .add(WindowInsets(top = maxHeight / 3, bottom = maxHeight / 2))
                .asPaddingValues(),
            userScrollEnabled = !disableInteractiveFeatures,
            modifier = Modifier
                .fadingEdge(vertical = 64.dp)
                .then(
                    if (disableInteractiveFeatures) {
                        // On TV we don't want the lyrics container to swallow
                        // D-pad focus – the lyrics are read-only and should
                        // never become a focus target.
                        Modifier.focusProperties { canFocus = false }
                    } else {
                        Modifier
                    }
                )
                .nestedScroll(remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (source == NestedScrollSource.UserInput) {
                                isAutoScrollEnabled = false
                            }
                            if (!isSelectionModeActive) { // Only update preview time if not selecting
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity
                        ): Velocity {
                            isAutoScrollEnabled = false
                            if (!isSelectionModeActive) { // Only update preview time if not selecting
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostFling(consumed, available)
                        }
                    }
                })
        ) {
            val lyricsOffset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L
            val effectivePlaybackPosition = currentPlaybackPosition - lyricsOffset

            val displayedCurrentLineIndex = if (!isAutoScrollEnabled) {
                currentLineIndex
            } else {
                if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex
            }

            // Show lyrics provider at the top, scrolling with content
            if (isLyricsProviderShown) {
                item {
                    Text(
                        text = "Lyrics from ${lyricsEntity?.provider}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

                itemsIndexed(
                    items = displayLines,
                    key = { index: Int, item: LyricsEntry ->
                        when {
                            enhancedLyrics && item.isIntervalIndicator -> "interval-$index-${item.intervalGapStart}-${item.intervalGapEnd}"
                            item.isInstrumental -> "instrumental-$index-${item.time}"
                            else -> "line-$index-${item.time}"
                        }
                    }
                ) { index: Int, item: LyricsEntry ->
                    val isSelected = selectedIndices.contains(index)
                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)) // Clip for background
                        .combinedClickable(
                            enabled = !disableInteractiveFeatures,
                            onClick = {
                                if (isSelectionModeActive) {
                                    // Toggle selection
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive =
                                                false // Exit mode if last item deselected
                                        }
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (isSynced && effectiveChangeLyrics && !isGuest) {
                                    // Professional seek action with smooth animation
                                    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                                    playerConnection.seekTo((item.time - lyricsOffset).coerceAtLeast(0))
                                    // Smooth slow scroll when clicking on lyrics (3 seconds)
                                    scope.launch {
                                        // First scroll to the clicked item without animation
                                        lazyListState.scrollToItem(index = index)

                                        // Then animate it to center position slowly
                                        val itemInfo =
                                            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportHeight =
                                                lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            val center =
                                                lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val offset = itemCenter - center

                                            if (kotlin.math.abs(offset) > 10) { // Only animate if not already centered
                                                lazyListState.animateScrollBy(
                                                    value = offset.toFloat(),
                                                    animationSpec = tween(durationMillis = 1500) // Reduced to half speed
                                                )
                                            }
                                        }
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    // If already at limit, show toast
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    // If already at limit, show toast
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        .background(
                            if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f
                            )
                            else Color.Transparent
                        )
                        .padding(horizontal = 24.dp, vertical = 8.dp)

                    // Handle interval indicators for enhanced lyrics
                    if (enhancedLyrics && item.isIntervalIndicator) {
                        val visible = effectivePlaybackPosition >= item.intervalGapStart &&
                            effectivePlaybackPosition <= item.intervalGapEnd - 650L
                        IntervalIndicator(
                            gapStartMs = item.intervalGapStart,
                            gapEndMs = item.intervalGapEnd - 650L,
                            currentPositionMs = effectivePlaybackPosition,
                            visible = visible,
                            color = expressiveAccent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {

                    // Check if this line shares the same time as the currently active line
                    // This enables synchronized word-by-word animation for both main and background vocals
                    val currentLineTime = if (displayedCurrentLineIndex >= 0 && displayedCurrentLineIndex < displayLines.size) {
                        displayLines[displayedCurrentLineIndex].time
                    } else -1L
                    val isLineAtSameTime = item.time == currentLineTime
                    val isActiveByIndex = index == displayedCurrentLineIndex
                    val isActiveByTime = isLineAtSameTime && displayedCurrentLineIndex >= 0
                    // Monochrome/Experimental animation styles have been removed; the
                    // Enhanced Lyrics renderer now provides the advanced word-level
                    // animations these legacy modes used to approximate.
                    val isMonochromeStyle = false
                    val isExperimentalStyle = false
                    val isThisLineActive = isActiveByIndex || isActiveByTime

                    // Monochrome-style per-line state (past / inactive / upcoming / active)
                    // Matches Apple Music–like motion popularised by Monochrome:
                    //   active   → opacity 1.00, scale 1.00, blur 0
                    //   upcoming → opacity 0.70, scale 0.98, blur 0.8dp
                    //   inactive → opacity 0.50, scale 0.95, blur 1.5dp
                    //   past     → opacity 0.30, scale 0.93, blur 2.0dp
                    val monochromeRelative = when {
                        !isMonochromeStyle || !isSynced || displayedCurrentLineIndex < 0 -> 0
                        isThisLineActive -> 0
                        index == displayedCurrentLineIndex + 1 -> 1   // upcoming
                        index < displayedCurrentLineIndex -> -1       // past
                        else -> 2                                     // future / inactive
                    }

                    // Experimental: distance-based alpha falloff so far-away lines
                    // dim out gradually, creating a focus tunnel toward the active line.
                    val experimentalDistance = when {
                        !isExperimentalStyle || !isSynced || displayedCurrentLineIndex < 0 -> 0
                        else -> kotlin.math.abs(index - displayedCurrentLineIndex)
                    }
                    val experimentalAlpha = when (experimentalDistance) {
                        0 -> 1f
                        1 -> 0.50f
                        2 -> 0.30f
                        3 -> 0.20f
                        4 -> 0.15f
                        else -> 0.10f
                    }

                    val alpha by animateFloatAsState(
                        targetValue = when {
                            !isSynced || (isSelectionModeActive && isSelected) -> 1f
                            isThisLineActive -> 1f
                            isExperimentalStyle -> experimentalAlpha
                            isMonochromeStyle -> when (monochromeRelative) {
                                1 -> 0.70f
                                -1 -> 0.30f
                                else -> 0.50f
                            }
                            else -> 0.5f
                        },
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                    )
                    val scale by animateFloatAsState(
                        targetValue = when {
                            isThisLineActive && isExperimentalStyle -> 1.06f
                            isThisLineActive -> 1.05f
                            isMonochromeStyle -> when (monochromeRelative) {
                                1 -> 0.98f
                                -1 -> 0.93f
                                else -> 0.95f
                            }
                            else -> 1f
                        },
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                    )
                    val monochromeBlur by animateDpAsState(
                        targetValue = when {
                            !isMonochromeStyle || isThisLineActive -> 0.dp
                            monochromeRelative == 1 -> 0.8.dp
                            monochromeRelative == -1 -> 2.0.dp
                            else -> 1.5.dp
                        },
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "monochromeBlur"
                    )

                    // Use user's alignment setting, but center background vocals
                    val agentAlignment = when {
                        item.isBackground -> Alignment.CenterHorizontally
                        else -> when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> Alignment.Start
                            LyricsPosition.CENTER -> Alignment.CenterHorizontally
                            LyricsPosition.RIGHT -> Alignment.End
                        }
                    }
                    
                    val agentTextAlign = when {
                        item.isBackground -> TextAlign.Center
                        else -> when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> TextAlign.Left
                            LyricsPosition.CENTER -> TextAlign.Center
                            LyricsPosition.RIGHT -> TextAlign.Right
                        }
                    }
                    
                    // Smaller scale for background vocals
                    val bgScale = if (item.isBackground) 0.85f else 1f

                    if (item.isInstrumental) {
                        val instrumentalEnd = item.endTime.coerceAtLeast(item.time + 1L)
                        val instrumentalDuration = (instrumentalEnd - item.time).coerceAtLeast(1L)
                        val rawProgress = ((effectivePlaybackPosition - item.time).toFloat() / instrumentalDuration).coerceIn(0f, 1f)
                        val isInstrumentalActive = effectivePlaybackPosition in item.time..instrumentalEnd
                        InstrumentalIndicator(
                            modifier = itemModifier,
                            color = expressiveAccent,
                            progress = if (isInstrumentalActive) rawProgress else if (effectivePlaybackPosition > instrumentalEnd) 1f else 0f,
                            active = isInstrumentalActive,
                            alignment = when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> Alignment.Start
                                LyricsPosition.CENTER -> Alignment.CenterHorizontally
                                LyricsPosition.RIGHT -> Alignment.End
                            }
                        )
                        return@itemsIndexed
                    }

                    Column(
                        modifier = itemModifier
                            .then(if (isMonochromeStyle) Modifier.blur(monochromeBlur) else Modifier)
                            .graphicsLayer {
                                // Anchor the scale pivot to the text alignment edge so
                                // the active (scaled-up) line never overflows the column
                                // bounds on the left / right (fixes letter clipping).
                                val originX = when {
                                    item.isBackground -> 0.5f
                                    else -> when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> 0f
                                        LyricsPosition.CENTER -> 0.5f
                                        LyricsPosition.RIGHT -> 1f
                                    }
                                }
                                this.transformOrigin = TransformOrigin(originX, 0.5f)
                                this.alpha = if (item.isBackground) alpha * 0.8f else alpha
                                this.scaleX = scale * bgScale
                                this.scaleY = scale * bgScale
                            },
                        horizontalAlignment = agentAlignment
                    ) {
                        // Use time-based active check to sync both main and background lines with same timestamp.
                        // When the "connected lines" option is on, an item also stays active while the
                        // playback position is still inside its word-timing range, so word-by-word
                        // animations are not cut short when the next line starts (e.g. with {bg}).
                        val nextLine = displayLines.getOrNull(index + 1)
                        val estimatedDurationMs = when {
                            item.words?.isNotEmpty() == true -> 0L // Use actual word end times
                            nextLine != null -> {
                                // For standard LRC, line stays active until next line starts minus small buffer
                                val rawGap = nextLine.time - item.time
                                (rawGap - 100L).coerceAtLeast(1000L) // At least 1 second, minus 100ms buffer
                            }
                            else -> 3000L
                        }
                        val coversByTime = connectedLines && isSynced &&
                            effectivePlaybackPosition in item.time..item.effectiveEndTime(estimatedDurationMs)
                        if (connectedLines && index == displayedCurrentLineIndex) {
                            timber.log.Timber.d("Lyrics: connectedLines active for line $index, coversByTime=$coversByTime, effectiveEnd=${item.effectiveEndTime(estimatedDurationMs)}ms, playback=${effectivePlaybackPosition}ms")
                        }
                        val isActiveLine = ((isActiveByIndex || isActiveByTime) && isSynced) || coversByTime
                        val lineColor = if (isActiveLine) {
                            if (item.isBackground) expressiveAccent.copy(alpha = 0.85f) else expressiveAccent
                        } else {
                            expressiveAccent.copy(alpha = if (item.isBackground) 0.5f else 0.7f)
                        }
                        val alignment = agentTextAlign
                        
                        val hasWordTimings = item.words?.isNotEmpty() == true
                        
                        // Word-by-word animation styles
                        if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.NONE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && effectivePlaybackPosition > wordEndMs

                                    val transitionProgress = when {
                                        !isActiveLine -> 0f
                                        hasWordPassed -> 1f
                                        isWordActive && wordDuration > 0 -> {
                                            val elapsed = effectivePlaybackPosition - wordStartMs
                                            val linear = (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                            linear * linear * (3f - 2f * linear)
                                        }
                                        else -> 0f
                                    }

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.7f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.5f + (0.5f * transitionProgress)
                                        else -> 0.35f
                                    }

                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Medium
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.FADE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && effectivePlaybackPosition > wordEndMs

                                    val fadeProgress = if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = effectivePlaybackPosition - wordStartMs
                                        val linear = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        // Smooth cubic easing
                                        linear * linear * (3f - 2f * linear)
                                    } else if (hasWordPassed) 1f else 0f

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.55f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.4f + (0.6f * fadeProgress)
                                        else -> 0.4f
                                    }
                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Medium
                                    }
                                    // Enhanced shadow for active words
                                    val wordShadow = when {
                                        isWordActive && fadeProgress > 0.2f -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.35f * fadeProgress),
                                            offset = Offset.Zero,
                                            blurRadius = 10f * fadeProgress
                                        )
                                        hasWordPassed -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.15f),
                                            offset = Offset.Zero,
                                            blurRadius = 6f
                                        )
                                        else -> null
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.GLOW) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition in wordStartMs..wordEndMs
                                    val hasWordPassed = isActiveLine && effectivePlaybackPosition > wordEndMs

                                    val fillProgress = if (isWordActive && wordDuration > 0) {
                                        val linear = ((effectivePlaybackPosition - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
                                        linear * linear * (3f - 2f * linear)
                                    } else if (hasWordPassed) 1f else 0f

                                    val glowIntensity = fillProgress * fillProgress
                                    val brightness = 0.45f + (0.55f * fillProgress)

                                    val wordColor = when {
                                        !isActiveLine -> expressiveAccent.copy(alpha = 0.5f)
                                        isWordActive || hasWordPassed -> expressiveAccent.copy(alpha = brightness)
                                        else -> expressiveAccent.copy(alpha = 0.35f)
                                    }
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        hasWordPassed -> FontWeight.Bold
                                        else -> FontWeight.Medium
                                    }
                                    val wordShadow = if (isWordActive && glowIntensity > 0.05f) {
                                        Shadow(color = expressiveAccent.copy(alpha = 0.5f + (0.3f * glowIntensity)), offset = Offset.Zero, blurRadius = 16f + (12f * glowIntensity))
                                    } else if (hasWordPassed) {
                                        Shadow(color = expressiveAccent.copy(alpha = 0.25f), offset = Offset.Zero, blurRadius = 8f)
                                    } else null

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.SLIDE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && effectivePlaybackPosition >= wordEndMs) || (!isActiveLine && item.time < currentLineTime)

                                    if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = effectivePlaybackPosition - wordStartMs
                                        val fillProgress = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        val breatheValue = (timeElapsed % 3000) / 3000f
                                        val breatheEffect = (kotlin.math.sin(breatheValue * Math.PI.toFloat() * 2f) * 0.03f).coerceIn(0f, 0.03f)
                                        val glowIntensity = (0.3f + fillProgress * 0.7f + breatheEffect).coerceIn(0f, 1.1f)

                                        val slideBrush = Brush.horizontalGradient(
                                            0.0f to expressiveAccent,
                                            (fillProgress * 0.95f).coerceIn(0f, 1f) to expressiveAccent,
                                            fillProgress to expressiveAccent.copy(alpha = 0.9f),
                                            (fillProgress + 0.02f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.5f),
                                            (fillProgress + 0.08f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.35f),
                                            1.0f to expressiveAccent.copy(alpha = 0.35f)
                                        )

                                        withStyle(style = SpanStyle(
                                            brush = slideBrush,
                                            fontWeight = FontWeight.ExtraBold,
                                            shadow = Shadow(color = expressiveAccent.copy(alpha = 0.4f * glowIntensity), offset = Offset(0f, 0f), blurRadius = 14f + (4f * fillProgress))
                                        )) {
                                            append(word.text)
                                        }
                                    } else if (hasWordPassed && isActiveLine) {
                                        withStyle(style = SpanStyle(
                                            color = expressiveAccent,
                                            fontWeight = FontWeight.Bold,
                                            shadow = Shadow(color = expressiveAccent.copy(alpha = 0.4f), offset = Offset(0f, 0f), blurRadius = 12f)
                                        )) {
                                            append(word.text)
                                        }
                                    } else {
                                        val wordColor = if (!isActiveLine) lineColor else expressiveAccent.copy(alpha = 0.35f)
                                        withStyle(style = SpanStyle(color = wordColor, fontWeight = FontWeight.Medium)) {
                                            append(word.text)
                                        }
                                    }
                                    if (wordIndex < item.words.size - 1) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.KARAOKE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && effectivePlaybackPosition >= wordEndMs) || (!isActiveLine && item.time < currentLineTime)

                                    if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = effectivePlaybackPosition - wordStartMs
                                        val linearProgress = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        // Smoother easing curve for more natural fill animation
                                        val fillProgress = linearProgress * linearProgress * (3f - 2f * linearProgress)
                                        
                                        // Enhanced glow intensity calculation
                                        val glowIntensity = fillProgress * fillProgress

                                        val wordBrush = Brush.horizontalGradient(
                                            0.0f to expressiveAccent.copy(alpha = 0.4f),
                                            (fillProgress * 0.6f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.75f),
                                            (fillProgress * 0.85f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.95f),
                                            fillProgress to expressiveAccent,
                                            (fillProgress + 0.03f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.85f),
                                            (fillProgress + 0.1f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.5f),
                                            1.0f to expressiveAccent.copy(alpha = if (fillProgress >= 0.9f) 0.95f else 0.4f)
                                        )

                                        // Improved shadow with better glow effect
                                        val wordShadow = Shadow(
                                            color = expressiveAccent.copy(alpha = 0.5f + (0.3f * glowIntensity)),
                                            offset = Offset.Zero,
                                            blurRadius = 16f + (12f * glowIntensity)
                                        )

                                        withStyle(style = SpanStyle(
                                            brush = wordBrush,
                                            fontWeight = FontWeight.ExtraBold,
                                            shadow = wordShadow
                                        )) {
                                            append(word.text)
                                        }
                                    } else if (hasWordPassed && isActiveLine) {
                                        // Completed words with subtle glow
                                        withStyle(style = SpanStyle(
                                            color = expressiveAccent,
                                            fontWeight = FontWeight.Bold,
                                            shadow = Shadow(
                                                color = expressiveAccent.copy(alpha = 0.25f),
                                                offset = Offset.Zero,
                                                blurRadius = 8f
                                            )
                                        )) {
                                            append(word.text)
                                        }
                                    } else {
                                        // Inactive words
                                        val wordColor = if (!isActiveLine) lineColor else expressiveAccent.copy(alpha = 0.4f)
                                        withStyle(style = SpanStyle(color = wordColor, fontWeight = FontWeight.Medium)) {
                                            append(word.text)
                                        }
                                    }
                                    if (wordIndex < item.words.size - 1) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)
                        } else if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.APPLE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && effectivePlaybackPosition >= wordEndMs) || (!isActiveLine && item.time < currentLineTime)

                                    val rawProgress = if (isWordActive && wordDuration > 0) {
                                        val elapsed = effectivePlaybackPosition - wordStartMs
                                        (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                    } else if (hasWordPassed) 1f else 0f

                                    // Smooth cubic easing for natural animation
                                    val smoothProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.55f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.55f + (0.45f * smoothProgress)
                                        else -> 0.4f
                                    }
                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.SemiBold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Normal
                                    }
                                    // Enhanced shadow with better glow intensity
                                    val glowIntensity = smoothProgress * smoothProgress
                                    val wordShadow = when {
                                        isWordActive -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.2f + (0.4f * glowIntensity)),
                                            offset = Offset.Zero,
                                            blurRadius = 10f + (12f * glowIntensity)
                                        )
                                        hasWordPassed && isActiveLine -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.2f),
                                            offset = Offset.Zero,
                                            blurRadius = 8f
                                        )
                                        else -> null
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight, shadow = wordShadow)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1) append(" ")
                                }
                            }
                            Text(text = styledText, fontSize = lyricsTextSize.sp, textAlign = alignment, lineHeight = (lyricsTextSize * lyricsLineSpacing).sp)
                        } else if (hasWordTimings && item.words != null) {
                            // MONOCHROME animation style removed; Enhanced Lyrics now provides this.
                            // Monochrome word-by-word liquid karaoke wipe.
                            // Each word smoothly "fills" from a dim resting state to
                            // a bright, glowing peak as its timing window passes,
                            // then settles into a held bright state — giving the
                            // fluid Apple Music–style flow popularised by Monochrome.
                            val dimAlpha = 0.22f
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition in wordStartMs until wordEndMs
                                    val hasWordPassed = (isActiveLine && effectivePlaybackPosition >= wordEndMs) ||
                                        (!isActiveLine && item.time < currentLineTime)

                                    val rawProgress = if (isWordActive && wordDuration > 0) {
                                        val elapsed = effectivePlaybackPosition - wordStartMs
                                        (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                    } else if (hasWordPassed) 1f else 0f

                                    // Smoothstep for liquid easing.
                                    val eased = rawProgress * rawProgress * (3f - 2f * rawProgress)
                                    // Cubic again for a stronger leading-edge "swell".
                                    val swell = eased * eased

                                    val wordAlpha = when {
                                        !isActiveLine -> dimAlpha
                                        hasWordPassed -> 1f
                                        isWordActive -> dimAlpha + ((1f - dimAlpha) * eased)
                                        else -> dimAlpha
                                    }

                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.SemiBold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> if (eased > 0.5f) FontWeight.ExtraBold else FontWeight.Bold
                                        else -> FontWeight.SemiBold
                                    }

                                    // Liquid glow: a soft halo that swells during the
                                    // word and gently relaxes after it has passed.
                                    val wordShadow = when {
                                        isWordActive -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.25f + 0.45f * swell),
                                            offset = Offset.Zero,
                                            blurRadius = 14f + 22f * swell
                                        )
                                        hasWordPassed && isActiveLine -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.22f),
                                            offset = Offset.Zero,
                                            blurRadius = 12f
                                        )
                                        else -> null
                                    }

                                    withStyle(
                                        style = SpanStyle(
                                            color = wordColor,
                                            fontWeight = wordWeight,
                                            shadow = wordShadow,
                                            letterSpacing = (-0.01f).sp
                                        )
                                    ) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                            } else if (hasWordTimings && item.words != null) {
                            // EXPERIMENTAL animation style removed; Enhanced Lyrics now provides this.
                            // Experimental — adds per-word "wobble" pulses, swelling glow,
                            // a soft multi-stop wipe across the active word, and a sustained
                            // afterglow on already-sung words. Inspired by experimental
                            // animated lyric renderers.
                            val dimAlpha = 0.18f
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)

                                    val isWordActive = isActiveLine &&
                                        effectivePlaybackPosition in wordStartMs until wordEndMs
                                    val hasWordPassed = (isActiveLine && effectivePlaybackPosition >= wordEndMs) ||
                                        (!isActiveLine && item.time < currentLineTime)

                                    val rawProgress = if (isWordActive) {
                                        ((effectivePlaybackPosition - wordStartMs).toFloat() / wordDuration)
                                            .coerceIn(0f, 1f)
                                    } else if (hasWordPassed) 1f else 0f

                                    // Smoothstep for liquid easing.
                                    val eased = rawProgress * rawProgress * (3f - 2f * rawProgress)

                                    // Per-word "wobble": sharp 125ms rise then 625ms decay.
                                    val timeSinceWord = (effectivePlaybackPosition - wordStartMs).toFloat()
                                    val wobble = when {
                                        !isActiveLine -> 0f
                                        timeSinceWord < 0f -> 0f
                                        timeSinceWord < 125f -> timeSinceWord / 125f
                                        timeSinceWord < 750f -> (1f - (timeSinceWord - 125f) / 625f).coerceAtLeast(0f)
                                        else -> 0f
                                    }

                                    val wordAlpha = when {
                                        !isActiveLine -> dimAlpha
                                        hasWordPassed -> 1f
                                        isWordActive -> dimAlpha + ((1f - dimAlpha) * eased)
                                        else -> dimAlpha
                                    }

                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.SemiBold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive && eased > 0.4f -> FontWeight.ExtraBold
                                        isWordActive -> FontWeight.Bold
                                        else -> FontWeight.SemiBold
                                    }

                                    // Glow that swells with the wobble on word entry,
                                    // settles into a soft afterglow on sung words.
                                    val swell = (eased * eased) + wobble * 0.85f
                                    val wordShadow = when {
                                        isWordActive -> Shadow(
                                            color = expressiveAccent.copy(
                                                alpha = (0.30f + 0.55f * swell).coerceAtMost(0.85f)
                                            ),
                                            offset = Offset.Zero,
                                            blurRadius = 16f + 28f * swell
                                        )
                                        hasWordPassed && isActiveLine -> Shadow(
                                            color = expressiveAccent.copy(alpha = 0.22f),
                                            offset = Offset.Zero,
                                            blurRadius = 14f
                                        )
                                        else -> null
                                    }

                                    withStyle(
                                        style = SpanStyle(
                                            color = wordColor,
                                            fontWeight = wordWeight,
                                            shadow = wordShadow,
                                            letterSpacing = (-0.02f - wobble * 0.06f).sp
                                        )
                                    ) {
                                        append(word.text)
                                    }
                                    if (wordIndex < item.words.size - 1) append(" ")
                                }
                            }

                            // Subtle line-wide pulse on word entry — driven by the
                            // largest wobble across the words for a "breathing" beat.
                            val maxWobble = if (isActiveLine) {
                                var m = 0f
                                item.words.forEach { word ->
                                    val ws = (word.startTime * 1000).toLong()
                                    val ts = (effectivePlaybackPosition - ws).toFloat()
                                    val w = when {
                                        ts < 0f -> 0f
                                        ts < 125f -> ts / 125f
                                        ts < 750f -> (1f - (ts - 125f) / 625f).coerceAtLeast(0f)
                                        else -> 0f
                                    }
                                    if (w > m) m = w
                                }
                                m
                            } else 0f
                            val pulseScale = 1f + 0.015f * maxWobble

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                            )
                        } else if (isActiveLine && lyricsGlowEffect) {
                            // Initial animation for glow fill from left to right
                            val fillProgress = remember { Animatable(0f) }
                            // Continuous pulsing animation for the glow
                            val pulseProgress = remember { Animatable(0f) }
                            
                            LaunchedEffect(index) {
                                fillProgress.snapTo(0f)
                                fillProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 1200,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                            
                            // Continuous slow pulsing animation
                            LaunchedEffect(Unit) {
                                while (true) {
                                    pulseProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 3000,
                                            easing = LinearEasing
                                        )
                                    )
                                    pulseProgress.snapTo(0f)
                                }
                            }
                            
                            val fill = fillProgress.value
                            val pulse = pulseProgress.value
                            
                            // Combine fill animation with subtle pulse
                            val pulseEffect = (kotlin.math.sin(pulse * Math.PI.toFloat()) * 0.15f).coerceIn(0f, 0.15f)
                            val glowIntensity = (fill + pulseEffect).coerceIn(0f, 1.2f)
                            
                            // Create left-to-right gradient fill with glow
                            val glowBrush = Brush.horizontalGradient(
                                0.0f to expressiveAccent.copy(alpha = 0.3f),
                                (fill * 0.7f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.9f),
                                fill to expressiveAccent,
                                (fill + 0.1f).coerceIn(0f, 1f) to expressiveAccent.copy(alpha = 0.7f),
                                1.0f to expressiveAccent.copy(alpha = if (fill >= 1f) 1f else 0.3f)
                            )
                            
                            val styledText = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        shadow = Shadow(
                                            color = expressiveAccent.copy(alpha = 0.8f * glowIntensity),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 28f * (1f + pulseEffect)
                                        ),
                                        brush = glowBrush
                                    )
                                ) {
                                    append(item.text)
                                }
                            }
                            
                            // Single smooth bounce animation
                            val bounceScale = if (fill < 0.3f) {
                                // Gentler rise during fill
                                1f + (kotlin.math.sin(fill * 3.33f * Math.PI.toFloat()) * 0.03f)
                            } else {
                                // Hold at normal scale
                                1f
                            }
                            
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = bounceScale
                                        scaleY = bounceScale
                                    }
                            )
                        } else if (isActiveLine && !lyricsGlowEffect) {
                            // Active line without glow effect - just bold text
                            Text(
                                text = item.text,
                                fontSize = lyricsTextSize.sp,
                                color = expressiveAccent,
                                textAlign = alignment,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else {
                            // Inactive line
                            Text(
                                text = item.text,
                                fontSize = lyricsTextSize.sp,
                                color = lineColor,
                                textAlign = alignment,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        }
                        if (currentSong?.romanizeLyrics == true
                            && (romanizeJapaneseLyrics ||
                                    romanizeKoreanLyrics ||
                                    romanizeRussianLyrics ||
                                    romanizeUkrainianLyrics ||
                                    romanizeSerbianLyrics ||
                                    romanizeBulgarianLyrics ||
                                    romanizeBelarusianLyrics ||
                                    romanizeKyrgyzLyrics ||
                                    romanizeMacedonianLyrics ||
                                    romanizeChineseLyrics)) {
                            // Show romanized text if available
                            val romanizedText by item.romanizedTextFlow.collectAsState()
                            romanizedText?.let { romanized ->
                                Text(
                                    text = romanized,
                                    fontSize = 18.sp,
                                    color = expressiveAccent.copy(alpha = 0.6f),
                                    textAlign = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> TextAlign.Left
                                        LyricsPosition.CENTER -> TextAlign.Center
                                        LyricsPosition.RIGHT -> TextAlign.Right
                                    },
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        
                        // Show translated text if available
                        if (autoTranslateLyrics || openRouterApiKey.isNotBlank()) {
                            val translatedText by item.translatedTextFlow.collectAsState()
                            translatedText?.let { translated ->
                                Text(
                                    text = translated,
                                    fontSize = 16.sp,
                                    color = expressiveAccent.copy(alpha = 0.5f),
                                    textAlign = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> TextAlign.Left
                                        LyricsPosition.CENTER -> TextAlign.Center
                                        LyricsPosition.RIGHT -> TextAlign.Right
                                    },
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        // Action buttons are now in the bottom bar
        // Removed the more button from bottom - it's now in the top header
    }

    Box(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
    ) {
        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FilledTonalButton(onClick = {
                scope.launch {
                    performSmoothPageScroll(currentLineIndex, 1500)
                }
                isAutoScrollEnabled = true
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.sync),
                    contentDescription = stringResource(R.string.auto_scroll),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.auto_scroll))
            }
        }

        AnimatedVisibility(
            visible = isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        isSelectionModeActive = false
                        selectedIndices.clear()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(R.string.cancel),
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalButton(
                    onClick = {
                        if (selectedIndices.isNotEmpty()) {
                            val sortedIndices = selectedIndices.sorted()
                            val selectedLyricsText = sortedIndices
                                .mapNotNull { displayLines.getOrNull(it)?.takeIf { entry -> !entry.isInstrumental }?.text }
                                .joinToString("\n")

                            if (selectedLyricsText.isNotBlank()) {
                                shareDialogData = Triple(
                                    selectedLyricsText,
                                    mediaMetadata?.title ?: "",
                                    mediaMetadata?.artists?.joinToString { it.name } ?: ""
                                )
                                showShareDialog = true
                            }
                            isSelectionModeActive = false
                            selectedIndices.clear()
                        }
                    },
                    enabled = selectedIndices.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(R.string.share_selected),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.share))
                }
            }
        }
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = { /* Don't dismiss */ }) {
            Card( // Use Card for better styling
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!! // Renamed 'lyrics' to 'lyricsText' for clarity
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.share_lyrics),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Share as Text Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    val songLink =
                                        "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                    // Use the potentially multi-line lyricsText here
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.share_lyrics)
                                    )
                                )
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), // Use new share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_text),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Share as Image Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Pass the potentially multi-line lyrics to the color picker
                                shareDialogData = Triple(lyricsText, songTitle, artists)
                                showColorPickerDialog = true
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), // Use new share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_image),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Cancel Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showShareDialog = false }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        val coverUrl = mediaMetadata?.thumbnailUrl
        val paletteColors = remember { mutableStateListOf<Color>() }
        val backgroundStyle = remember { mutableStateOf(LyricsBackgroundStyle.SOLID) }

        val previewCardWidth = configuration.containerDpSize.width * 0.90f
        val previewPadding = 20.dp * 2
        val previewBoxPadding = 28.dp * 2
        val previewAvailableWidth = previewCardWidth - previewPadding - previewBoxPadding
        val previewBoxHeight = 340.dp
        val headerFooterEstimate = (48.dp + 14.dp + 16.dp + 20.dp + 8.dp + 28.dp * 2)
        val previewAvailableHeight = previewBoxHeight - headerFooterEstimate

        val lyricsTextAlign = when (lyricsTextPosition) {
            LyricsPosition.LEFT -> TextAlign.Left
            LyricsPosition.CENTER -> TextAlign.Center
            LyricsPosition.RIGHT -> TextAlign.Right
        }

        val textStyleForMeasurement = TextStyle(
            color = previewTextColor,
            fontWeight = FontWeight.Bold,
            textAlign = lyricsTextAlign
        )
        val textMeasurer = rememberTextMeasurer()

        rememberAdjustedFontSize(
            text = lyricsText,
            maxWidth = previewAvailableWidth,
            maxHeight = previewAvailableHeight,
            density = density,
            initialFontSize = 50.sp,
            minFontSize = 22.sp,
            style = textStyleForMeasurement,
            textMeasurer = textMeasurer
        )

        LaunchedEffect(coverUrl) {
            if (coverUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                        val result = loader.execute(req)
                        val bmp = result.image?.toBitmap()
                        if (bmp != null) {
                            val palette = Palette.from(bmp).generate()
                            val swatches = palette.swatches.sortedByDescending { it.population }
                            val colors = swatches.map { Color(it.rgb) }
                                .filter { color ->
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                    hsv[1] > 0.2f
                                }
                            paletteColors.clear()
                            paletteColors.addAll(colors.take(5))
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.customize_colors),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .padding(8.dp)
                    ) {
                        LyricsImageCard(
                            lyricText = lyricsText,
                            mediaMetadata = mediaMetadata ?: return@Box,
                            backgroundColor = previewBackgroundColor,
                            backgroundStyle = backgroundStyle.value,
                            textColor = previewTextColor,
                                secondaryTextColor = previewSecondaryTextColor,
                                textAlign = lyricsTextAlign
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(text = stringResource(id = R.string.background_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color(0xFF242424), Color(0xFF121212), Color.White, Color.Black, Color(0xFFF5F5F5))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewBackgroundColor = color }
                                    .border(
                                        2.dp,
                                        if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color.White, Color.Black, Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewTextColor = color }
                                    .border(
                                        2.dp,
                                        if (previewTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.secondary_text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors.map { it.copy(alpha = 0.7f) } + listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f), Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewSecondaryTextColor = color }
                                    .border(
                                        2.dp,
                                        if (previewSecondaryTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = stringResource(id = R.string.player_background_style), style = MaterialTheme.typography.titleMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            LyricsBackgroundStyle.entries.forEach { style ->
                                val label =
                                    when (style) {
                                        LyricsBackgroundStyle.SOLID -> stringResource(R.string.player_background_solid)
                                        LyricsBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                        LyricsBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                    }
                                val selected = backgroundStyle.value == style

                                androidx.compose.material3.FilterChip(
                                    selected = selected,
                                    onClick = { backgroundStyle.value = style },
                                    label = { Text(label) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                        onClick = {
                            showColorPickerDialog = false
                            showProgressDialog = true
                            scope.launch {
                                try {
                                    val screenWidth = configuration.containerSize.width
                                    val screenHeight = configuration.containerSize.height

                                    val image = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = coverUrl,
                                        songTitle = songTitle,
                                        artistName = artists,
                                        lyrics = lyricsText,
                                        width = (screenWidth * density.density).toInt(),
                                        height = (screenHeight * density.density).toInt(),
                                         backgroundColor = previewBackgroundColor.toArgb(),
                                         backgroundStyle = backgroundStyle.value,
                                         textColor = previewTextColor.toArgb(),
                                         secondaryTextColor = previewSecondaryTextColor.toArgb(),
                                         lyricsAlignment = when (lyricsTextPosition) {
                                             LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL
                                             LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER
                                             LyricsPosition.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                                         }
                                    )
                                    val timestamp = System.currentTimeMillis()
                                    val filename = "lyrics_$timestamp"
                                    val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.share_lyrics)
                                        )
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.failed_to_create_image, e.message), Toast.LENGTH_SHORT).show()
                                } finally {
                                    showProgressDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.share))
                    }
                }
            }
        }
    }
}
}
}

/**
 * Compact "instrumental" indicator inserted between two lyric lines that are
 * separated by a long silence. Shows three musical-note dots with an animated
 * progress bar that fills as the silence elapses.
 */
@Composable
fun InstrumentalIndicator(
    modifier: Modifier,
    color: Color,
    progress: Float,
    active: Boolean,
    alignment: Alignment.Horizontal
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "instrumentalProgress"
    )
    val containerAlpha = if (active) 1f else 0.45f
    Column(
        modifier = modifier.alpha(containerAlpha),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            // Three notes; for active indicator, the "current" one pulses.
            for (i in 0 until 3) {
                val dotAlpha = if (!active) 0.6f else {
                    val phase = (animatedProgress * 3f) - i
                    when {
                        phase < 0f -> 0.4f
                        phase > 1f -> 1f
                        else -> 0.4f + 0.6f * phase
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = color.copy(alpha = dotAlpha),
                    modifier = Modifier.size(if (active) 22.dp else 18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            color = color,
            trackColor = color.copy(alpha = 0.25f),
            modifier = Modifier
                .width(if (active) 96.dp else 72.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}

/**
 * Interval indicator for enhanced lyrics - shows wavy circular progress indicator
 * between lyrics lines when there's a gap > 4 seconds.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntervalIndicator(
    gapStartMs: Long,
    gapEndMs: Long,
    currentPositionMs: Long,
    visible: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val rowHeightPx = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            rowHeightPx.animateTo(1f, tween(200))
            alpha.animateTo(1f, tween(200))
        } else {
            alpha.animateTo(0f, tween(200))
            rowHeightPx.animateTo(0f, tween(200))
        }
    }

    val density = LocalDensity.current
    val targetHeightDp = 72.dp

    val progress = if (gapEndMs > gapStartMs) {
        ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "intervalProgress"
    )

    Box(
        modifier = modifier
            .height(targetHeightDp * rowHeightPx.value)
            .padding(top = 16.dp * rowHeightPx.value)
            .graphicsLayer {
                this.alpha = alpha.value
                this.clip = true
            },
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .size(36.dp)
                .alpha(alpha.value),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

// Professional page animation constants - slower for smoothness
const val AUTO_SCROLL_DURATION = 1500L // Much slower auto-scroll for smooth transitions
const val INITIAL_SCROLL_DURATION = 1000L // Slower initial positioning
const val SEEK_DURATION = 800L // Slower user interaction
const val FAST_SEEK_DURATION = 600L // Less aggressive seeking

// Lyrics constants
// val LyricsPreviewTime = 2.seconds // removed, used inline
