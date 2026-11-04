/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auramusic.app.utils.ReleaseInfo
import com.auramusic.app.utils.TvUpdateInstallController
import com.auramusic.app.utils.TvUpdatePhase
import com.auramusic.app.utils.Updater
import kotlinx.coroutines.delay

/**
 * The TV update prompt: shown on launch when a newer release exists, and again
 * from the updater screen before an install. "Update now" downloads the TV APK
 * in place and hands it to the system installer, because a TV usually has no
 * browser to send the user to.
 */
@Composable
fun TvUpdateDialog(
    info: ReleaseInfo,
    installer: TvUpdateInstallController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val phase = installer.phase
    val busy = phase is TvUpdatePhase.Downloading || phase is TvUpdatePhase.Installing
    val confirmFocus = remember { FocusRequester() }
    val dismissFocus = remember { FocusRequester() }

    // Re-seed D-pad focus whenever the visible button set changes.
    LaunchedEffect(phase) {
        val target = when (phase) {
            TvUpdatePhase.Installing -> null
            TvUpdatePhase.NeedsPermission, TvUpdatePhase.Launched -> dismissFocus
            else -> confirmFocus
        } ?: return@LaunchedEffect
        repeat(6) { attempt ->
            if (attempt > 0) delay(60L * attempt)
            if (runCatching { target.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(
                text = "Update to v${info.versionName}",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (phase) {
                    TvUpdatePhase.Idle -> {
                        Text("A new version of AuraMusic Tv is available. Would you like to download it?")
                        if (info.description.isNotBlank()) {
                            Text(
                                "What's new",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    text = info.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    is TvUpdatePhase.Downloading -> {
                        Text("Downloading update…")
                        TvUpdateProgressBar(phase.progress)
                    }

                    TvUpdatePhase.Installing -> {
                        Text("Starting the installer…")
                        TvUpdateProgressBar(-1f)
                    }

                    TvUpdatePhase.NeedsPermission -> {
                        Text(
                            "Allow AuraMusic to install unknown apps, then choose Install again to finish updating.",
                        )
                    }

                    TvUpdatePhase.Launched -> {
                        Text("Follow the system prompts to finish updating.")
                    }

                    is TvUpdatePhase.Failed -> {
                        Text(
                            text = "The update could not be installed.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = phase.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (phase) {
                TvUpdatePhase.Idle -> TvDialogButton(
                    label = "Update now",
                    primary = true,
                    focusRequester = confirmFocus,
                    onClick = {
                        installer.start(
                            context,
                            Updater.getDownloadUrlForCurrentVariant(
                                info,
                                preferredVariant = "tv",
                                preferredArchitecture = "automatic",
                            ),
                        )
                    },
                )

                is TvUpdatePhase.Downloading -> TvDialogButton(
                    label = "Cancel",
                    focusRequester = confirmFocus,
                    onClick = { installer.reset() },
                )

                TvUpdatePhase.NeedsPermission -> TvDialogButton(
                    label = "Install again",
                    primary = true,
                    focusRequester = confirmFocus,
                    onClick = { installer.retryInstall(context) },
                )

                is TvUpdatePhase.Failed -> TvDialogButton(
                    label = "Try again",
                    primary = true,
                    focusRequester = confirmFocus,
                    onClick = {
                        installer.start(
                            context,
                            Updater.getDownloadUrlForCurrentVariant(
                                info,
                                preferredVariant = "tv",
                                preferredArchitecture = "automatic",
                            ),
                        )
                    },
                )

                else -> Unit
            }
        },
        dismissButton = {
            when (phase) {
                TvUpdatePhase.Idle -> TvDialogButton(
                    label = "Later",
                    focusRequester = dismissFocus,
                    onClick = onDismiss,
                )

                TvUpdatePhase.NeedsPermission -> TvDialogButton(
                    label = "Allow permission",
                    focusRequester = dismissFocus,
                    onClick = { installer.allowPermission(context) },
                )

                TvUpdatePhase.Launched, is TvUpdatePhase.Failed -> TvDialogButton(
                    label = "Close",
                    focusRequester = dismissFocus,
                    onClick = onDismiss,
                )

                else -> Unit
            }
        },
    )
}

@Composable
private fun TvDialogButton(
    label: String,
    focusRequester: FocusRequester,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Button(
        onClick = onClick,
        colors = if (primary) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            ),
    ) {
        Text(label, fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium)
    }
}

/** Shared progress/status block for the dialog and the updater screen. */
@Composable
fun TvUpdateInstallStatus(
    phase: TvUpdatePhase,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (phase) {
            TvUpdatePhase.Idle -> Unit

            is TvUpdatePhase.Downloading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Downloading update…",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (phase.progress >= 0f) {
                        Text(
                            text = "${(phase.progress.coerceAtMost(1f) * 100f).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                TvUpdateProgressBar(phase.progress)
            }

            TvUpdatePhase.Installing -> {
                Text(
                    text = "Starting the installer…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TvUpdateProgressBar(-1f)
            }

            TvUpdatePhase.NeedsPermission -> Text(
                text = "Allow AuraMusic to install unknown apps, then choose Install again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )

            TvUpdatePhase.Launched -> Text(
                text = "Follow the system prompts to finish updating.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            is TvUpdatePhase.Failed -> Text(
                text = phase.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TvUpdateProgressBar(progress: Float, modifier: Modifier = Modifier) {
    if (progress < 0f) {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier.fillMaxWidth(),
        )
    }
}
