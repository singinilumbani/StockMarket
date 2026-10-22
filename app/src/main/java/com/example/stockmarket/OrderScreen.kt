package com.example.stockmarket



import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.FEE_RATE
import com.example.stockmarket.data.OrderSide
import com.example.stockmarket.data.builtInCompanies
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.DetailHeader
import java.util.Locale

// 1) STATEFUL: gets the ViewModel
@Composable
fun OrderScreen(
    onBack: () -> Unit,
    onOrderPlaced: (Int) -> Unit,
    viewModel: OrderViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    OrderContent(
        state = state,
        onQuantityChange = viewModel::onQuantityChange,
        onBack = onBack,
        onPlaceOrder = { viewModel.placeOrder()?.let(onOrderPlaced) }
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun OrderContent(
    state: OrderUiState,
    onQuantityChange: (String) -> Unit,
    onBack: () -> Unit,
    onPlaceOrder: () -> Unit
) {
    val company = state.company
    val isBuy = state.side == OrderSide.BUY
    var confirmOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {

        DetailHeader(
            title = (if (isBuy) "Buy " else "Sell ") + (company?.symbol ?: ""),
            onBack = onBack
        )

        if (company == null) {
            Text("Company not found", modifier = Modifier.padding(16.dp))
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(company.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = String.format(Locale.US, "Market price: MK %,.2f", company.price),
                fontWeight = FontWeight.SemiBold
            )

            val helper = if (isBuy) {
                String.format(Locale.US, "Available cash: MK %,.2f", state.cash)
            } else {
                "You own ${state.owned} shares"
            }

            OutlinedTextField(
                value = state.quantityText,
                onValueChange = onQuantityChange,
                label = { Text("Number of shares") },
                singleLine = true,
                isError = state.error != null,
                supportingText = { Text(state.error ?: helper) },
                textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TotalsRow("Subtotal", state.subtotal)
                    TotalsRow("Fees (sample ${(FEE_RATE * 100).toInt()}%)", state.fees)
                    HorizontalDivider()
                    TotalsRow(if (isBuy) "Total cost" else "You receive", state.total, bold = true)
                }
            }

            Text(
                text = "Orders are sent to a simulated broker in this project.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = { confirmOpen = true },
            enabled = state.canPlace,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp)
        ) { Text(if (isBuy) "Review buy order" else "Review sell order") }
    }

    if (confirmOpen && company != null) {
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            title = { Text("Confirm your order") },
            text = {
                Text(
                    String.format(
                        Locale.US,
                        "%s %,d %s shares at MK %,.2f.\n%s: MK %,.2f",
                        if (isBuy) "Buy" else "Sell",
                        state.quantity,
                        company.symbol,
                        company.price,
                        if (isBuy) "Total cost" else "You receive",
                        state.total
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmOpen = false
                    onPlaceOrder()
                }) { Text("Place order") }
            },
            dismissButton = {
                TextButton(onClick = { confirmOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TotalsRow(label: String, amount: Double, bold: Boolean = false) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = weight, modifier = Modifier.weight(1f))
        Text(String.format(Locale.US, "MK %,.2f", amount), fontWeight = weight)
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun OrderContentPreview() {
    StockMarketTheme {
        Surface {
            OrderContent(
                state = OrderUiState(
                    company = builtInCompanies.first(),
                    quantityText = "10",
                    cash = 250_000.0
                ),
                onQuantityChange = {},
                onBack = {},
                onPlaceOrder = {}
            )
        }
    }
}