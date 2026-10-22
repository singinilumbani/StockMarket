package com.example.stockmarket

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.CompanyRepository
import com.example.stockmarket.data.ThemeMode
import com.example.stockmarket.ui.theme.StockMarketTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// How often to ask the server for new prices. Use 20_000L (20 seconds) while testing.
private const val REFRESH_EVERY_MS = 5 * 60 * 1000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // MUST be first. It switches from the splash theme to Theme.StockMarket.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Runs while the app is visible. It stops when the app goes to the background,
        // and starts again, with an immediate refresh, when the app comes back.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    CompanyRepository.refresh()
                    delay(REFRESH_EVERY_MS)
                }
            }
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keeps the clock and battery icons readable in either theme
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                )
                onDispose { }
            }

            StockMarketTheme(darkTheme = darkTheme) {
                AppNavigation()
            }
        }
    }
}