package com.example.stockmarket.data


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val orderAlerts: Boolean = true,
    val priceAlerts: Boolean = true,
    val biometricLogin: Boolean = false
)

// One DataStore file called "settings". The "by" makes a single shared copy.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    // applicationContext lives as long as the app, so we never hold on to a screen
    private val store = context.applicationContext.dataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ORDER_ALERTS = booleanPreferencesKey("order_alerts")
        val PRICE_ALERTS = booleanPreferencesKey("price_alerts")
        val BIOMETRIC = booleanPreferencesKey("biometric_login")
    }

    // Emits a fresh AppSettings every time anything in the file changes
    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.entries.find { it.name == prefs[Keys.THEME] } ?: ThemeMode.SYSTEM,
            orderAlerts = prefs[Keys.ORDER_ALERTS] ?: true,
            priceAlerts = prefs[Keys.PRICE_ALERTS] ?: true,
            biometricLogin = prefs[Keys.BIOMETRIC] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setOrderAlerts(on: Boolean) {
        store.edit { it[Keys.ORDER_ALERTS] = on }
    }

    suspend fun setPriceAlerts(on: Boolean) {
        store.edit { it[Keys.PRICE_ALERTS] = on }
    }

    suspend fun setBiometric(on: Boolean) {
        store.edit { it[Keys.BIOMETRIC] = on }
    }
}