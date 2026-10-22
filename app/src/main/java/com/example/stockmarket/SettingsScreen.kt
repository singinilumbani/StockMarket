

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stockmarket

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.AppSettings
import com.example.stockmarket.data.ThemeMode
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.DetailHeader

// 1) STATEFUL: gets the ViewModel
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    SettingsContent(
        settings = settings,
        onBack = onBack,
        onThemeSelected = viewModel::onThemeSelected,
        onOrderAlertsChange = viewModel::onOrderAlertsChange,
        onPriceAlertsChange = viewModel::onPriceAlertsChange,
        onBiometricChange = viewModel::onBiometricChange
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun SettingsContent(
    settings: AppSettings,
    onBack: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onOrderAlertsChange: (Boolean) -> Unit,
    onPriceAlertsChange: (Boolean) -> Unit,
    onBiometricChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        DetailHeader(title = "Settings", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SectionCard(title = "Appearance") {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeSelected(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Text(
                    text = "System follows your phone's setting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }

            SectionCard(title = "Notifications") {
                SwitchRow(
                    title = "Order updates",
                    subtitle = "When an order is filled or rejected",
                    checked = settings.orderAlerts,
                    onCheckedChange = onOrderAlertsChange
                )
                HorizontalDivider()
                SwitchRow(
                    title = "Price alerts",
                    subtitle = "When a company you follow moves a lot",
                    checked = settings.priceAlerts,
                    onCheckedChange = onPriceAlertsChange
                )
            }

            SectionCard(title = "Security") {
                SwitchRow(
                    title = "Biometric login",
                    subtitle = "Use your fingerprint or face to log in (simulated for now)",
                    checked = settings.biometricLogin,
                    onCheckedChange = onBiometricChange
                )
            }
        }
    }
}

// A titled card. The "content" parameter is a slot, like "actions" in DetailHeader.
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun SettingsContentPreview() {
    StockMarketTheme {
        Surface {
            SettingsContent(
                settings = AppSettings(themeMode = ThemeMode.DARK, biometricLogin = true),
                onBack = {},
                onThemeSelected = {},
                onOrderAlertsChange = {},
                onPriceAlertsChange = {},
                onBiometricChange = {}
            )
        }
    }
}