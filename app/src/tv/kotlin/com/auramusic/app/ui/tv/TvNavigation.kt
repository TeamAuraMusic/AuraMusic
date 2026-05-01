/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Lightweight in-memory navigation for the TV variant. Avoids pulling in
 * androidx.navigation:navigation-compose just for a 1-activity TV shell.
 */
 sealed class TvDestination {
     data object Home : TvDestination()
     data object Library : TvDestination()
     data object Search : TvDestination()
     data object Settings : TvDestination()
     data object AppearanceSettings : TvDestination()
     data object AccountSettings : TvDestination()
     data object PlaybackSettings : TvDestination()
     data object ContentSettings : TvDestination()
     data object AboutScreen : TvDestination()
     data object UpdaterScreen : TvDestination()
     data object Login : TvDestination()
     data object Player : TvDestination()
     data class Album(val id: String) : TvDestination()
     data class Artist(val id: String) : TvDestination()
     data class Playlist(val id: String) : TvDestination()
 }

class TvNavigator(private val stack: MutableState<List<TvDestination>>) {
    val current: TvDestination get() = stack.value.last()

    fun navigate(destination: TvDestination) {
        stack.value = stack.value + destination
    }

    fun selectTopLevel(destination: TvDestination) {
        // Reset back-stack to a single top-level destination (Home/Library/Search/Settings).
        stack.value = listOf(destination)
    }

    fun popBack(): Boolean {
        return if (stack.value.size > 1) {
            stack.value = stack.value.dropLast(1)
            true
        } else {
            false
        }
    }
}

val LocalTvNavigator = compositionLocalOf<TvNavigator> {
    error("No TvNavigator provided")
}

@Composable
fun rememberTvNavigator(): TvNavigator {
    val stack = remember {
        mutableStateOf<List<TvDestination>>(listOf(TvDestination.Home))
    }
    return remember(stack) { TvNavigator(stack) }
}
