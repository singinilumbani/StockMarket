package com.example.stockmarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.stockmarket.ui.theme.StockMarketTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var keepsSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keeps the system splash screen visible until keepsSplashOnScreen turns false
        splashScreen.setKeepOnScreenCondition { keepsSplashOnScreen }

        // Perform initialization (e.g., auth check, fetching initial stock data)
        lifecycleScope.launch {
            delay(1500) // Simulate data loading or auth state check
            keepsSplashOnScreen = false // Dismisses the system splash screen
        }

        enableEdgeToEdge()

        setContent {
            StockMarketTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}