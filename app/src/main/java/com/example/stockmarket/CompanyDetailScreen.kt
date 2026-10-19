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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stockmarket.data.sampleCompanies
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.ChangeChip
import com.example.stockmarket.ui.theme.components.DetailHeader
import com.example.stockmarket.ui.theme.gainColor
import com.example.stockmarket.ui.theme.lossColor
import java.util.Locale

@Composable
fun CompanyDetailScreen(symbol: String, onBack: () -> Unit) {
    val company = sampleCompanies.find { it.symbol == symbol }

    Column(modifier = Modifier.fillMaxSize()) {

        DetailHeader(title = symbol, onBack = onBack)

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
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) { Text("Sell") }
                Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Buy") }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun CompanyDetailPreview() {
    StockMarketTheme {
        Surface {
            CompanyDetailScreen(symbol = "NBM", onBack = {})
        }
    }
}