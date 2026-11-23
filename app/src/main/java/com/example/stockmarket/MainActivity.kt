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
import com.example.stockmarket.data.local.ThemeMode
import com.example.stockmarket.data.repository.CompanyRepository
import com.example.stockmarket.navigation.AppNavigation
import com.example.stockmarket.ui.theme.StockMarketTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.stockmarket.data.repository.SessionManager
import com.example.stockmarket.data.repository.SessionState
import com.example.stockmarket.data.repository.TradingStore
import com.example.stockmarket.data.repository.RegistrationStore
import com.example.stockmarket.data.local.AccountCache
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.stockmarket.background.AppVisibility
import com.example.stockmarket.background.BackgroundSync
import com.example.stockmarket.background.Notifier
import kotlinx.coroutines.flow.first
import com.example.stockmarket.BuildConfig

private const val REFRESH_EVERY_MS = 5 * 60 * 1000L
// After a failed refresh, try again sooner, so the banner clears soon after the connection returns
private const val RETRY_EVERY_MS = 30_000L

class MainActivity : ComponentActivity() {

    private val askForNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private var askedThisTime = false
    override fun onCreate(savedInstanceState: Bundle?) {
        // MUST be first. It switches from the splash theme to Theme.StockMarket.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        CompanyRepository.init(applicationContext)
        AccountCache.init(applicationContext)
        Notifier.createChannel(applicationContext)

        // Once somebody is logged in, ask (Android 13 and above) for permission to show notifications
        lifecycleScope.launch {
            SessionManager.state.first { it is SessionState.LoggedIn }
            if (!askedThisTime &&
                Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                askedThisTime = true
                askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        SessionManager.init(applicationContext)

        // Keep the splash screen until we know whether a saved login exists
        splash.setKeepOnScreenCondition { SessionManager.state.value is SessionState.Loading }

        // Runs while the app is visible. It stops when the app goes to the background,
        // and starts again, with an immediate refresh, when the app comes back.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    CompanyRepository.refresh()
                    SessionManager.verify()
                    if (SessionManager.currentToken() != null) TradingStore.refresh()
                    if (SessionManager.currentToken() != null) RegistrationStore.refresh()
                    delay(if (TradingStore.syncError.value != null) RETRY_EVERY_MS else REFRESH_EVERY_MS)
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
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkTheme }
                )
                onDispose { }
            }

            StockMarketTheme(darkTheme = darkTheme) {
                AppNavigation()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        AppVisibility.isVisible = true
    }

    override fun onStop() {
        super.onStop()
        AppVisibility.isVisible = false
        // TESTING ONLY, in the debug build: 20 seconds after you leave the app, run one background
        // check, so you do not have to wait 15 minutes. The demo and release builds do not do this.
        if (BuildConfig.BUILD_TYPE == "debug") BackgroundSync.runSoon(applicationContext)
    }
}