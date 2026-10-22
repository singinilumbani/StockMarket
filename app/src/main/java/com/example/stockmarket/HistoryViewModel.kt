package com.example.stockmarket


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.Order
import com.example.stockmarket.data.OrderStatus
import com.example.stockmarket.data.TradingStore
import com.example.stockmarket.data.Transaction
import com.example.stockmarket.data.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class HistorySegment(val label: String) {
    TRANSACTIONS("Transactions"),
    ORDERS("Orders")
}

enum class TxFilter(val label: String) {
    ALL("All"),
    BUY("Buys"),
    SELL("Sells"),
    DEPOSIT("Deposits")
}

enum class OrderFilter(val label: String) {
    ALL("All"),
    WAITING("Waiting"),
    FILLED("Filled"),
    REJECTED("Rejected")
}

data class MonthGroup(val month: String, val items: List<Transaction>)

data class HistoryUiState(
    val segment: HistorySegment = HistorySegment.TRANSACTIONS,
    val txFilter: TxFilter = TxFilter.ALL,
    val orderFilter: OrderFilter = OrderFilter.ALL,
    val txGroups: List<MonthGroup> = emptyList(),
    val orders: List<Order> = emptyList()
)

class HistoryViewModel : ViewModel() {

    // The three things the user can change
    private data class Inputs(
        val segment: HistorySegment = HistorySegment.TRANSACTIONS,
        val txFilter: TxFilter = TxFilter.ALL,
        val orderFilter: OrderFilter = OrderFilter.ALL
    )

    private val inputs = MutableStateFlow(Inputs())

    // Recalculated whenever the transactions, the orders or the inputs change
    val uiState: StateFlow<HistoryUiState> = combine(
        TradingStore.transactions,
        TradingStore.orders,
        inputs
    ) { transactions, orders, input ->
        build(transactions, orders, input)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = build(TradingStore.transactions.value, TradingStore.orders.value, inputs.value)
    )

    // ---- Events from the screen ----
    fun onSegmentSelected(segment: HistorySegment) = inputs.update { it.copy(segment = segment) }
    fun onTxFilterSelected(filter: TxFilter) = inputs.update { it.copy(txFilter = filter) }
    fun onOrderFilterSelected(filter: OrderFilter) = inputs.update { it.copy(orderFilter = filter) }

    // ---- Working out what to show ----
    private fun build(
        transactions: List<Transaction>,
        orders: List<Order>,
        input: Inputs
    ): HistoryUiState {

        // Transactions: newest first, then filtered
        val filteredTx = transactions
            .sortedByDescending { it.id }
            .filter { tx ->
                when (input.txFilter) {
                    TxFilter.ALL -> true
                    TxFilter.BUY -> tx.type == TxType.BUY
                    TxFilter.SELL -> tx.type == TxType.SELL
                    TxFilter.DEPOSIT -> tx.type == TxType.DEPOSIT
                }
            }

        // Group them under month headings
        val groups = filteredTx
            .groupBy { tx -> tx.date.drop(3) }
            .map { (month, items) -> MonthGroup(month, items) }

        // Orders: newest first, then filtered
        val filteredOrders = orders
            .sortedByDescending { it.id }
            .filter { order ->
                when (input.orderFilter) {
                    OrderFilter.ALL -> true
                    OrderFilter.WAITING -> order.status == OrderStatus.SUBMITTED
                    OrderFilter.FILLED -> order.status == OrderStatus.FILLED
                    OrderFilter.REJECTED -> order.status == OrderStatus.REJECTED
                }
            }

        return HistoryUiState(
            segment = input.segment,
            txFilter = input.txFilter,
            orderFilter = input.orderFilter,
            txGroups = groups,
            orders = filteredOrders
        )
    }
}