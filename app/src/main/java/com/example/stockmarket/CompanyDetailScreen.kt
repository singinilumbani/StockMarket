package com.example.stockmarket

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stockmarket.data.CompanyRepository
import com.example.stockmarket.data.OrderSide
import com.example.stockmarket.data.RegistrationStatus

import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.ChangeChip
import com.example.stockmarket.ui.theme.components.DetailHeader
import com.example.stockmarket.ui.theme.gainColor
import com.example.stockmarket.ui.theme.lossColor
import java.util.Locale

@Composable
fun CompanyDetailScreen(
    symbol: String,
    registrationStatus: RegistrationStatus,
    isWatched: Boolean,
    onBack: () -> Unit,
    onToggleWatch: () -> Unit,
    onTrade: (OrderSide) -> Unit,
    onRegisterClick: () -> Unit
) {
    val companies by CompanyRepository.companies.collectAsState()
    val company = companies.find { it.symbol == symbol }
    var gateOpen by remember { mutableStateOf(false) }

    // THE GATE: only approved users reach the order ticket
    val handleTrade: (OrderSide) -> Unit = { side ->
        if (registrationStatus == RegistrationStatus.APPROVED) {
            onTrade(side)
        } else {
            gateOpen = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        DetailHeader(
            title = symbol,
            onBack = onBack,
            actions = {
                IconButton(onClick = onToggleWatch) {
                    Icon(
                        imageVector = if (isWatched) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isWatched) "Remove from watchlist" else "Add to watchlist",
                        tint = if (isWatched) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        if (company == null) {
            Text(
                text = "Company not found",
                modifier = Modifier.padding(16.dp)
            )
        } else {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = company.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "MK %,.2f", company.price),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    ChangeChip(company.changePct)
                }

                Spacer(Modifier.height(24.dp))

                if (company.history.size >= 2) {
                    val lineColor =
                        if (company.history.last() >= company.history.first()) gainColor() else lossColor()

                    PriceChart(prices = company.history, lineColor = lineColor)

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Last ${company.history.size} days (sample data)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { handleTrade(OrderSide.SELL) },
                    modifier = Modifier.weight(1f)
                ) { Text("Sell") }
                Button(
                    onClick = { handleTrade(OrderSide.BUY) },
                    modifier = Modifier.weight(1f)
                ) { Text("Buy") }
            }
        }
    }

    if (gateOpen) {
        TradeGateDialog(
            status = registrationStatus,
            onDismiss = { gateOpen = false },
            onRegisterClick = onRegisterClick
        )
    }
}

@Composable
private fun TradeGateDialog(
    status: RegistrationStatus,
    onDismiss: () -> Unit,
    onRegisterClick: () -> Unit
) {
    if (status == RegistrationStatus.PENDING) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Registration under review") },
            text = {
                Text("We will notify you as soon as your account is approved. You can keep browsing prices in the meantime.")
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Register to trade") },
            text = { Text("You need an approved investment account before you can place orders.") },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    onRegisterClick()
                }) {
                    Text(
                        if (status == RegistrationStatus.IN_PROGRESS) "Continue registration"
                        else "Start registration"
                    )
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun CompanyDetailPreview() {
    StockMarketTheme {
        Surface {
            CompanyDetailScreen(
                symbol = "NBM",
                registrationStatus = RegistrationStatus.NOT_STARTED,
                isWatched = true,
                onBack = {},
                onToggleWatch = {},
                onTrade = {},
                onRegisterClick = {}
            )
        }
    }
}