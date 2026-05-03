/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
 import com.auramusic.app.LocalDatabase
 import com.auramusic.app.LocalPlayerConnection
 import com.auramusic.app.constants.DarkModeKey
 import com.auramusic.app.constants.DynamicThemeKey
 import com.auramusic.app.constants.PureBlackKey
 import com.auramusic.app.constants.SelectedThemeColorKey
 import com.auramusic.app.db.MusicDatabase
 import com.auramusic.app.listentogether.ListenTogetherManager
 import com.auramusic.app.playback.MusicService
 import com.auramusic.app.playback.MusicService.MusicBinder
 import com.auramusic.app.playback.PlayerConnection
 import com.auramusic.app.ui.component.LocalMenuState
 import com.auramusic.app.ui.theme.AuraMusicTheme
import com.auramusic.app.ui.theme.DefaultThemeColor
import com.auramusic.app.ui.screens.settings.DarkMode
import com.auramusic.app.ui.tv.TvApp
import com.auramusic.app.utils.SyncUtils
import com.auramusic.app.utils.rememberEnumPreference
import com.auramusic.app.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity that hosts the Android TV (Compose) shell for AuraMusic.
 *
 * It owns the [PlayerConnection] lifecycle and exposes it as a flow that the
 * Compose UI observes. This avoids the race where fragments/screens were
 * being created with a `null` player connection because the [MusicService]
 * binds asynchronously after `onCreate`.
 */
@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var listenTogetherManager: ListenTogetherManager

    @Inject
    lateinit var syncUtils: SyncUtils

    private val playerConnectionFlow = MutableStateFlow<PlayerConnection?>(null)
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicBinder ?: return
            try {
                val connection = PlayerConnection(
                    this@TvMainActivity,
                    binder,
                    database,
                    lifecycleScope,
                )
                listenTogetherManager.setPlayerConnection(connection)
                playerConnectionFlow.value = connection
                Timber.tag("TvMainActivity").d("PlayerConnection created successfully")
            } catch (e: Exception) {
                Timber.tag("TvMainActivity").e(e, "Failed to create PlayerConnection")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            disposePlayerConnection()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize sync on TV launch
        lifecycleScope.launch {
            syncUtils.tryAutoSync()
        }
        
          setContent {
              val playerConnection by playerConnectionFlow.collectAsState()

              // Theme preferences
              val darkMode by rememberEnumPreference(DarkModeKey, com.auramusic.app.ui.screens.settings.DarkMode.AUTO)
              val isSystemInDarkTheme = isSystemInDarkTheme()
              val useDarkTheme = when (darkMode) {
                  com.auramusic.app.ui.screens.settings.DarkMode.AUTO -> isSystemInDarkTheme
                  com.auramusic.app.ui.screens.settings.DarkMode.ON -> true
                  com.auramusic.app.ui.screens.settings.DarkMode.OFF -> false
              }

              val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
              val dynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
              val selectedThemeColorInt by rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
              val selectedThemeColor = Color(selectedThemeColorInt)
              val themeColor = if (dynamicTheme) DefaultThemeColor else selectedThemeColor

              AuraMusicTheme(
                  darkTheme = useDarkTheme,
                  pureBlack = pureBlack,
                  themeColor = themeColor
              ) {
                  CompositionLocalProvider(
                      LocalDatabase provides database,
                      LocalPlayerConnection provides playerConnection
                  ) {
                      TvApp(playerConnection = playerConnection)
                  }
              }
          }
    }

    override fun onStart() {
        super.onStart()
        serviceBound = bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE,
        )
    }

    override fun onStop() {
        if (serviceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Timber.tag("TvMainActivity").w(e, "Service was not bound")
            }
            serviceBound = false
        }
        disposePlayerConnection()
        super.onStop()
    }

    private fun disposePlayerConnection() {
        listenTogetherManager.setPlayerConnection(null)
        playerConnectionFlow.value?.dispose()
        playerConnectionFlow.value = null
    }
}
