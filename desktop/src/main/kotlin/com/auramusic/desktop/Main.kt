package com.auramusic.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(width = 1120.dp, height = 800.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "AuraMusic Desktop",
        state = windowState,
    ) {
        AuraMusicApp()
    }
}
