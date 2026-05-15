/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.ui.utils

import androidx.navigation.NavController
import com.auramusic.app.ui.screens.Screens

fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.mapNotNull { it?.route }

    while (previousBackStackEntry != null &&
        currentBackStackEntry?.destination?.route !in mainRoutes
    ) {
        popBackStack()
    }
}
