

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stockmarket

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.ChangeChip
import com.example.stockmarket.ui.theme.components.TabHeader
import com.example.stockmarket.ui.theme.gainColor
import com.example.stockmarket.ui.theme.lossColor
import java.util.Locale

// Gold-friendly colours for the allocation bar
private val allocationColors = listOf(
    Color(0xFFE6C55A),
    Color(0xFFB8860B),
    Color(0xFF8D9199),
    Color(0xFFD6C58F),
    Color(0xFF6B5B2A),
    Color(0xFF66BB6A)
)

// 1) STATEFUL: gets the ViewModel
@Composable
fun PortfolioScreen(
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCompanyClick: (String) -> Unit,
    onBrowseMarket: () -> Unit,
    viewModel: PortfolioViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    PortfolioContent(
        state = state,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        onCompanyClick = onCompanyClick,
        onBrowseMarket = onBrowseMarket
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun PortfolioContent(
    state: PortfolioUiState,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCompanyClick: (String) -> Unit,
    onBrowseMarket: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabHeader(
            title = "Portfolio",
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )

        if (state.rows.isEmpty()) {
            EmptyPortfolio(cash = state.cash, onBrowseMarket = onBrowseMarket)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SummaryCard(state) }
                item { AllocationCard(state) }
                item {
                    Text(
                        text = "Holdings (${state.rows.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(state.rows, key = { it.symbol }) { row ->
                    HoldingCard(row = row, onClick = { onCompanyClick(row.symbol) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: PortfolioUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Total value", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "MK %,.2f", state.totalValue),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "MK %+,.2f (%+.2f%%) all time",
                    state.totalGain,
                    state.totalGainPct
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem("Invested", state.investedValue, Modifier.weight(1f))
                SummaryItem("Cash", state.cash, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = String.format(Locale.US, "MK %,.2f", amount),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AllocationCard(state: PortfolioUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Allocation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            // The stacked bar: each holding gets a slice as wide as its value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                state.rows.forEachIndexed { index, row ->
                    Box(
                        modifier = Modifier
                            .weight(row.value.toFloat().coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(allocationColors[index % allocationColors.size])
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // The legend: a dot, the symbol and its share
            state.rows.forEachIndexed { index, row ->
                val share = if (state.investedValue > 0) row.value / state.investedValue * 100 else 0.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(allocationColors[index % allocationColors.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = row.symbol, modifier = Modifier.weight(1f))
                    Text(
                        text = String.format(Locale.US, "%.1f%%", share),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldingCard(row: HoldingRow, onClick: () -> Unit) {
    val gainColor = if (row.gain >= 0) gainColor() else lossColor()

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = row.symbol, fontWeight = FontWeight.Bold)
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "%,d shares · avg MK %,.2f",
                        row.quantity,
                        row.avgCost
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "MK %,.2f", row.value),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.US, "%+,.2f", row.gain),
                    style = MaterialTheme.typography.bodySmall,
                    color = gainColor
                )
                Spacer(Modifier.height(4.dp))
                ChangeChip(row.gainPct)
            }
        }
    }
}

@Composable
private fun EmptyPortfolio(cash: Double, onBrowseMarket: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No investments yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(Locale.US, "Cash available: MK %,.2f", cash),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBrowseMarket) { Text("Browse the market") }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun PortfolioContentPreview() {
    StockMarketTheme {
        Surface {
            PortfolioContent(
                state = PortfolioUiState(
                    cash = 250_000.0,
                    investedValue = 825_900.0,
                    totalGain = 58_900.0,
                    totalGainPct = 7.68,
                    rows = listOf(
                        HoldingRow("NBM", "National Bank of Malawi", 300, 1850.0, 2010.0, 603_000.0, 48_000.0, 8.65),
                        HoldingRow("AIRTEL", "Airtel Malawi", 1500, 90.0, 95.0, 142_500.0, 7_500.0, 5.56),
                        HoldingRow("TNM", "Telekom Networks Malawi", 2000, 23.5, 24.5, 49_000.0, 2_000.0, 4.26)
                    )
                ),
                onNotificationsClick = {},
                onProfileClick = {},
                onCompanyClick = {},
                onBrowseMarket = {}
            )
        }
    }
}