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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.Order
import com.example.stockmarket.data.OrderSide
import com.example.stockmarket.data.OrderStatus
import com.example.stockmarket.data.Transaction
import com.example.stockmarket.data.TxType
import com.example.stockmarket.data.sampleTransactions
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.EmptyState
import com.example.stockmarket.ui.theme.components.TabHeader
import com.example.stockmarket.ui.theme.gainColor
import com.example.stockmarket.ui.theme.lossColor
import java.util.Locale

// 1) STATEFUL: gets the ViewModel
@Composable
fun HistoryScreen(
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    HistoryContent(
        state = state,
        onSegmentSelected = viewModel::onSegmentSelected,
        onTxFilterSelected = viewModel::onTxFilterSelected,
        onOrderFilterSelected = viewModel::onOrderFilterSelected,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun HistoryContent(
    state: HistoryUiState,
    onSegmentSelected: (HistorySegment) -> Unit,
    onTxFilterSelected: (TxFilter) -> Unit,
    onOrderFilterSelected: (OrderFilter) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // We remember only the ID of the tapped row. The row itself is looked up from the
    // current state, so the sheet stays correct if an order changes status while it is open.
    var selectedTxId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedOrderId by rememberSaveable { mutableStateOf<Int?>(null) }

    val selectedTx = selectedTxId?.let { id ->
        state.txGroups.flatMap { it.items }.find { it.id == id }
    }
    val selectedOrder = selectedOrderId?.let { id ->
        state.orders.find { it.id == id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabHeader(
            title = "History",
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )

        // Segments: Transactions | Orders
        TabRow(selectedTabIndex = HistorySegment.entries.indexOf(state.segment)) {
            HistorySegment.entries.forEach { segment ->
                Tab(
                    selected = state.segment == segment,
                    onClick = { onSegmentSelected(segment) },
                    text = { Text(segment.label) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Filter chips for the current segment
        if (state.segment == HistorySegment.TRANSACTIONS) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TxFilter.entries) { filter ->
                    FilterChip(
                        selected = state.txFilter == filter,
                        onClick = { onTxFilterSelected(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(OrderFilter.entries) { filter ->
                    FilterChip(
                        selected = state.orderFilter == filter,
                        onClick = { onOrderFilterSelected(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }
        }

        // The list for the current segment
        if (state.segment == HistorySegment.TRANSACTIONS) {
            if (state.txGroups.isEmpty()) {
                EmptyState(message = "No transactions to show")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.txGroups.forEach { group ->
                        item(key = "header_${group.month}") {
                            Text(
                                text = group.month,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                            )
                        }
                        items(group.items, key = { "tx_${it.id}" }) { tx ->
                            TransactionRow(tx = tx, onClick = { selectedTxId = tx.id })
                        }
                    }
                }
            }
        } else {
            if (state.orders.isEmpty()) {
                EmptyState(message = "No orders to show")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.orders, key = { "order_${it.id}" }) { order ->
                        OrderRow(order = order, onClick = { selectedOrderId = order.id })
                    }
                }
            }
        }
    }

    // Detail sheets
    if (selectedTx != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTxId = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            TransactionDetail(selectedTx)
        }
    }
    if (selectedOrder != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedOrderId = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            OrderDetail(selectedOrder)
        }
    }
}

// ---------- Helpers ----------

private fun txTitle(tx: Transaction): String = when (tx.type) {
    TxType.BUY -> "Bought ${tx.symbol}"
    TxType.SELL -> "Sold ${tx.symbol}"
    TxType.DEPOSIT -> "Deposit"
    TxType.WITHDRAWAL -> "Withdrawal"
}

private fun orderTotal(order: Order): Double {
    val subtotal = order.quantity * order.price
    return if (order.side == OrderSide.BUY) subtotal + order.fees else subtotal - order.fees
}

@Composable
private fun StatusText(status: OrderStatus) {
    val (label, color) = when (status) {
        OrderStatus.SUBMITTED -> "Waiting" to MaterialTheme.colorScheme.onSurfaceVariant
        OrderStatus.FILLED -> "Filled" to gainColor()
        OrderStatus.REJECTED -> "Rejected" to MaterialTheme.colorScheme.error
    }
    Text(text = label, color = color, fontWeight = FontWeight.SemiBold)
}

// ---------- Rows ----------

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val incoming = tx.type == TxType.SELL || tx.type == TxType.DEPOSIT
    val color = if (incoming) gainColor() else lossColor()

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = txTitle(tx), fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (tx.type == TxType.DEPOSIT) tx.date
                    else "${tx.quantity} shares · ${tx.date}",
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

@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    val side = if (order.side == OrderSide.BUY) "Buy" else "Sell"

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "$side ${order.quantity} ${order.symbol}", fontWeight = FontWeight.SemiBold)
                Text(
                    text = order.createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusText(order.status)
                Text(
                    text = String.format(Locale.US, "MK %,.2f", orderTotal(order)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- Detail sheets ----------

@Composable
private fun TransactionDetail(tx: Transaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = txTitle(tx),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider()
        DetailLine("Reference", "TX-%05d".format(tx.id))
        DetailLine("Date", tx.date)
        if (tx.type != TxType.DEPOSIT) {
            DetailLine("Shares", String.format(Locale.US, "%,d", tx.quantity))
            DetailLine("Price per share", String.format(Locale.US, "MK %,.2f", tx.price))
        }
        DetailLine("Amount", String.format(Locale.US, "MK %,.2f", tx.amount), bold = true)
    }
}

@Composable
private fun OrderDetail(order: Order) {
    val side = if (order.side == OrderSide.BUY) "Buy" else "Sell"
    val subtotal = order.quantity * order.price

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "$side ${order.symbol}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        StatusText(order.status)
        HorizontalDivider()
        DetailLine("Reference", "ORD-%05d".format(order.id))
        DetailLine("Date", order.createdAt)
        DetailLine("Shares", String.format(Locale.US, "%,d", order.quantity))
        DetailLine("Price per share", String.format(Locale.US, "MK %,.2f", order.price))
        DetailLine("Subtotal", String.format(Locale.US, "MK %,.2f", subtotal))
        DetailLine("Fees (sample)", String.format(Locale.US, "MK %,.2f", order.fees))
        DetailLine(
            if (order.side == OrderSide.BUY) "Total cost" else "You receive",
            String.format(Locale.US, "MK %,.2f", orderTotal(order)),
            bold = true
        )
        Text(
            text = when (order.status) {
                OrderStatus.SUBMITTED -> "The broker has not filled this order yet."
                OrderStatus.FILLED -> "This order was filled and is in your portfolio."
                OrderStatus.REJECTED -> "The broker rejected this order. Your money or shares were returned."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun HistoryContentPreview() {
    StockMarketTheme {
        Surface {
            HistoryContent(
                state = HistoryUiState(
                    txGroups = listOf(MonthGroup("Oct 2026", sampleTransactions.takeLast(4))),
                    orders = listOf(
                        Order(1, "NBM", OrderSide.BUY, 10, 2010.0, 402.0, OrderStatus.FILLED, "19 Oct 2026"),
                        Order(2, "TNM", OrderSide.SELL, 500, 24.5, 245.0, OrderStatus.SUBMITTED, "20 Oct 2026")
                    )
                ),
                onSegmentSelected = {},
                onTxFilterSelected = {},
                onOrderFilterSelected = {},
                onNotificationsClick = {},
                onProfileClick = {}
            )
        }
    }
}

