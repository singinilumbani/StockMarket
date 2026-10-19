@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stockmarket

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.Company
import com.example.stockmarket.data.RegistrationStatus
import com.example.stockmarket.data.Transaction
import com.example.stockmarket.data.TxType
import com.example.stockmarket.data.sampleCompanies
import com.example.stockmarket.data.sampleTransactions
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.ChangeChip
import com.example.stockmarket.ui.theme.components.TabHeader
import com.example.stockmarket.ui.theme.gainColor
import com.example.stockmarket.ui.theme.lossColor
import java.util.Locale

// 1) STATEFUL: gets the ViewModel
@Composable
fun HomeScreen(
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onTradeClick: () -> Unit,
    onCompanyClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    HomeContent(
        state = state,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        onRegisterClick = onRegisterClick,
        onTradeClick = onTradeClick,
        onCompanyClick = onCompanyClick,
        onSeeAllClick = onSeeAllClick
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun HomeContent(
    state: HomeUiState,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onTradeClick: () -> Unit,
    onCompanyClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabHeader(
            title = "Home",
            unreadCount = 3,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.registrationStatus != RegistrationStatus.APPROVED) {
                item { StatusBanner(state.registrationStatus, onRegisterClick) }
            }

            item {
                PortfolioCard(
                    value = state.portfolioValue,
                    change = state.todayChange,
                    changePct = state.todayChangePct
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(onClick = onTradeClick, modifier = Modifier.weight(1f)) { Text("Buy") }
                    FilledTonalButton(onClick = onTradeClick, modifier = Modifier.weight(1f)) { Text("Sell") }
                    FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) { Text("Deposit") }
                }
            }

            item { MoversRow("Top gainers", state.topGainers, onCompanyClick) }
            item { MoversRow("Top losers", state.topLosers, onCompanyClick) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onSeeAllClick) { Text("See all") }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.recentActivity.forEach { ActivityRow(it) }
                    }
                }
            }
        }
    }
}

private data class BannerContent(val title: String, val subtitle: String, val button: String?)

@Composable
private fun StatusBanner(status: RegistrationStatus, onRegisterClick: () -> Unit) {
    val content = when (status) {
        RegistrationStatus.NOT_STARTED -> BannerContent(
            "Register to start trading",
            "Open your investment account in a few minutes.",
            "Start registration"
        )
        RegistrationStatus.IN_PROGRESS -> BannerContent(
            "Continue your registration",
            "Your details are saved. Pick up where you left off.",
            "Continue"
        )
        RegistrationStatus.PENDING -> BannerContent(
            "Registration under review",
            "We will notify you as soon as it is approved.",
            null
        )
        RegistrationStatus.REJECTED -> BannerContent(
            "Registration needs changes",
            "Please fix your details and submit again.",
            "Fix details"
        )
        RegistrationStatus.APPROVED -> return
    }

    val colors = MaterialTheme.colorScheme
    val container = when (status) {
        RegistrationStatus.REJECTED -> colors.errorContainer
        RegistrationStatus.PENDING -> colors.secondaryContainer
        else -> colors.primaryContainer
    }
    val onContainer = when (status) {
        RegistrationStatus.REJECTED -> colors.onErrorContainer
        RegistrationStatus.PENDING -> colors.onSecondaryContainer
        else -> colors.onPrimaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(text = content.subtitle, style = MaterialTheme.typography.bodyMedium)
            if (content.button != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRegisterClick) { Text(content.button) }
            }
        }
    }
}

@Composable
private fun PortfolioCard(value: Double, change: Double, changePct: Double) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Portfolio value", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "MK %,.2f", value),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(Locale.US, "MK %+,.2f (%+.2f%%) today", change, changePct),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MoversRow(title: String, companies: List<Company>, onCompanyClick: (String) -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(companies, key = { it.symbol }) { company ->
                MoverCard(company = company, onClick = { onCompanyClick(company.symbol) })
            }
        }
    }
}

@Composable
private fun MoverCard(company: Company, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(140.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = company.symbol, fontWeight = FontWeight.Bold)
            Text(
                text = String.format(Locale.US, "%,.2f", company.price),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
            ChangeChip(company.changePct)
        }
    }
}

@Composable
private fun ActivityRow(tx: Transaction) {
    val incoming = tx.type == TxType.SELL || tx.type == TxType.DEPOSIT
    val color = if (incoming) gainColor() else lossColor()

    val title = when (tx.type) {
        TxType.BUY -> "Bought ${tx.symbol}"
        TxType.SELL -> "Sold ${tx.symbol}"
        TxType.DEPOSIT -> "Deposit"
    }
    val detail = if (tx.type == TxType.DEPOSIT) tx.date else "${tx.quantity} shares · ${tx.date}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = (if (incoming) "+" else "-") + String.format(Locale.US, "%,.2f", tx.amount),
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun HomeContentPreview() {
    StockMarketTheme {
        Surface {
            HomeContent(
                state = HomeUiState(
                    portfolioValue = 825_900.0,
                    todayChange = 10_488.0,
                    todayChangePct = 1.29,
                    topGainers = sampleCompanies.take(3),
                    topLosers = sampleCompanies.takeLast(3),
                    recentActivity = sampleTransactions.takeLast(3)
                ),
                onNotificationsClick = {},
                onProfileClick = {},
                onRegisterClick = {},
                onTradeClick = {},
                onCompanyClick = {},
                onSeeAllClick = {}
            )
        }
    }
}