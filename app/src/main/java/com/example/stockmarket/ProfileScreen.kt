

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stockmarket

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.RegistrationStatus
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.TabHeader

enum class ProfileDialog { HELP, ABOUT, TERMS, LOGOUT }

// 1) STATEFUL: gets the ViewModel
@Composable
fun ProfileScreen(
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    ProfileContent(
        state = state,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onRegisterClick = onRegisterClick,
        onLogout = onLogout
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLogout: () -> Unit
) {
    // Which pop-up is open, if any. rememberSaveable keeps it open when the phone rotates.
    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabHeader(
            title = "Profile",
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { UserCard(state) }
            item { AccountCard(state.status, onRegisterClick) }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MenuRow("Settings", "Appearance, alerts and security", onSettingsClick)
                        HorizontalDivider()
                        MenuRow("Help and support", null) { dialog = ProfileDialog.HELP }
                        HorizontalDivider()
                        MenuRow("About MSE Invest", null) { dialog = ProfileDialog.ABOUT }
                        HorizontalDivider()
                        MenuRow("Terms and risk warning", null) { dialog = ProfileDialog.TERMS }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { dialog = ProfileDialog.LOGOUT },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) { Text("Log out") }
            }

            item {
                Text(
                    text = "MSE Invest · version 0.1 · university project",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    when (dialog) {
        ProfileDialog.HELP -> InfoDialog(
            title = "Help and support",
            text = "A help centre and contact options will be added in a later version.",
            onDismiss = { dialog = null }
        )
        ProfileDialog.ABOUT -> InfoDialog(
            title = "About MSE Invest",
            text = "MSE Invest is a university project that shows how an app for the Malawi Stock Exchange could work. " +
                    "It is not connected to the exchange or to a broker. Prices and accounts are sample data.",
            onDismiss = { dialog = null }
        )
        ProfileDialog.TERMS -> InfoDialog(
            title = "Terms and risk warning",
            text = "Investing in shares involves risk. The value of your investments can go down as well as up, " +
                    "and you may lose money. Nothing in this app is financial advice.",
            onDismiss = { dialog = null }
        )
        ProfileDialog.LOGOUT -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Log out?") },
            text = { Text("You will need to log in again to see your account.") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    onLogout()
                }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
        null -> {}
    }
}

@Composable
private fun UserCard(state: ProfileUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.initial,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (state.status) {
                        RegistrationStatus.APPROVED -> "Approved investor"
                        RegistrationStatus.PENDING -> "Registration under review"
                        else -> "Not yet registered"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class AccountInfo(val title: String, val text: String, val button: String?)

@Composable
private fun AccountCard(status: RegistrationStatus, onRegisterClick: () -> Unit) {
    val info = when (status) {
        RegistrationStatus.NOT_STARTED -> AccountInfo(
            "Investment account: not registered",
            "Register to place buy and sell orders.",
            "Start registration"
        )
        RegistrationStatus.IN_PROGRESS -> AccountInfo(
            "Registration in progress",
            "Your details are saved. Pick up where you left off.",
            "Continue registration"
        )
        RegistrationStatus.PENDING -> AccountInfo(
            "Registration under review",
            "We will notify you as soon as it is approved.",
            null
        )
        RegistrationStatus.REJECTED -> AccountInfo(
            "Registration needs changes",
            "Please fix your details and submit again.",
            "Fix details"
        )
        RegistrationStatus.APPROVED -> AccountInfo(
            "Investment account: approved",
            "You can place buy and sell orders.",
            null
        )
    }

    val colors = if (status == RegistrationStatus.REJECTED) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    } else {
        CardDefaults.cardColors()
    }

    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(info.title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(info.text)
            if (info.button != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRegisterClick) { Text(info.button) }
            }
        }
    }
}

@Composable
private fun MenuRow(title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else null,
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun ProfileContentPreview() {
    StockMarketTheme {
        Surface {
            ProfileContent(
                state = ProfileUiState(name = "Test User", status = RegistrationStatus.APPROVED),
                onNotificationsClick = {},
                onProfileClick = {},
                onSettingsClick = {},
                onRegisterClick = {},
                onLogout = {}
            )
        }
    }
}