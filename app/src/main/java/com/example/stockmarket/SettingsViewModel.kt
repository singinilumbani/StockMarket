package com.example.stockmarket


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.AppSettings
import com.example.stockmarket.data.SettingsRepository
import com.example.stockmarket.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // ---- Events from the screen ----
    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun onOrderAlertsChange(on: Boolean) {
        viewModelScope.launch { repository.setOrderAlerts(on) }
    }

    fun onPriceAlertsChange(on: Boolean) {
        viewModelScope.launch { repository.setPriceAlerts(on) }
    }

    fun onBiometricChange(on: Boolean) {
        viewModelScope.launch { repository.setBiometric(on) }
    }
}