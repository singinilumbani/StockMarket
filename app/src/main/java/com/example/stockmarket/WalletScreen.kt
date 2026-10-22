

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.Transaction
import com.example.stockmarket.data.TxType
import com.example.stockmarket.data.sampleTransactions
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.DetailHeader
import com.example.stockmarket.ui.theme.gainColor
import com.example.stockmarket.ui.theme.lossColor
import java.util.Locale

// 1) STATEFUL: gets the ViewModel
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    WalletContent(
        state = state,
        onBack = onBack,
        validate = viewModel::validate,
        onConfirm = viewModel::confirm
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun WalletContent(
    state: WalletUiState,
    onBack: () -> Unit,
    validate: (CashAction, Double) -> String?,
    onConfirm: (CashAction, Double) -> Boolean
) {
    // Which sheet is open (null = none), what was typed, and the message after it worked
    var action by rememberSaveable { mutableStateOf<CashAction?>(null) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var resultMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        DetailHeader(title = "Wallet", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { BalanceCard(state.cash) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { action = CashAction.DEPOSIT },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Deposit") }
                    OutlinedButton(
                        onClick = { action = CashAction.WITHDRAW },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Withdraw") }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Payment account",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = state.accountLabel, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Simulated. No real money moves in this project.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Recent cash activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.cashHistory.isEmpty()) {
                item {
                    Text(
                        text = "No deposits or withdrawals yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.cashHistory, key = { it.id }) { tx -> CashRow(tx) }
            }
        }
    }

    // ----- The deposit / withdraw sheet -----
    val currentAction = action
    if (currentAction != null) {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val error = if (amount > 0) validate(currentAction, amount) else null
        val close = {
            action = null
            amountText = ""
        }

        ModalBottomSheet(onDismissRequest = close) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = currentAction.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(9) },
                    label = { Text("Amount (MK)") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        Text(
                            error ?: if (currentAction == CashAction.WITHDRAW) {
                                String.format(Locale.US, "Available: MK %,.2f", state.cash)
                            } else {
                                "Minimum MK 1,000"
                            }
                        )
                    },
                    textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5_000, 10_000, 50_000).forEach { quick ->
                        FilterChip(
                            selected = false,
                            onClick = { amountText = quick.toString() },
                            label = { Text(String.format(Locale.US, "%,d", quick)) }
                        )
                    }
                }

                Text(
                    text = (if (currentAction == CashAction.DEPOSIT) "From: " else "To: ") + state.accountLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        if (onConfirm(currentAction, amount)) {
                            resultMessage = String.format(
                                Locale.US,
                                "%s of MK %,.2f is complete.",
                                currentAction.title,
                                amount
                            )
                            close()
                        }
                    },
                    enabled = amount > 0 && error == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) { Text("Confirm ${currentAction.title.lowercase()}") }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ----- The "it worked" message -----
    val message = resultMessage
    if (message != null) {
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text("Done") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { resultMessage = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun BalanceCard(cash: Double) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Wallet balance", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "MK %,.2f", cash),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Available to trade", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CashRow(tx: Transaction) {
    val incoming = tx.type == TxType.DEPOSIT
    val color = if (incoming) gainColor() else lossColor()

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (incoming) "Deposit" else "Withdrawal",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = tx.date,
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
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun WalletContentPreview() {
    StockMarketTheme {
        Surface {
            WalletContent(
                state = WalletUiState(
                    cash = 250_000.0,
                    accountLabel = "Mobile money · ••••••••00",
                    cashHistory = sampleTransactions.filter { it.isCash }
                ),
                onBack = {},
                validate = { _, _ -> null },
                onConfirm = { _, _ -> true }
            )
        }
    }
}