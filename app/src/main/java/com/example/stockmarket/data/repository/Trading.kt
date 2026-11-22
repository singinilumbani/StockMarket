package com.example.stockmarket.data.repository

import com.example.stockmarket.data.model.Holding
import com.example.stockmarket.data.model.Transaction
import com.example.stockmarket.data.model.TxType
import com.example.stockmarket.data.remote.AccountDto
import com.example.stockmarket.data.remote.AmountRequest
import com.example.stockmarket.data.remote.Api
import com.example.stockmarket.data.remote.OrderDto
import com.example.stockmarket.data.remote.PlaceOrderRequest
import com.example.stockmarket.data.remote.errorMessageOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

enum class OrderSide { BUY, SELL }
enum class OrderStatus { SUBMITTED, FILLED, REJECTED }

data class Order(
    val id: Int,
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val price: Double,
    val fees: Double,
    val status: OrderStatus,
    val createdAt: String
)

// Used only to SHOW an estimate while typing. The server works out the real fees.
const val FEE_RATE = 0.02

data class PlaceOrderResult(val orderId: Int?, val error: String?)

// A COPY of the logged-in user's account. The server holds the real one.
object TradingStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _cash = MutableStateFlow(0.0)
    val cash: StateFlow<Double> = _cash.asStateFlow()

    private val _holdings = MutableStateFlow<List<Holding>>(emptyList())
    val holdings: StateFlow<List<Holding>> = _holdings.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private var pollJob: Job? = null

    // ---------- Instant checks while the user types ----------

    fun validate(symbol: String, side: OrderSide, quantity: Int, price: Double): String? {
        val owned = _holdings.value.find { it.symbol == symbol }?.quantity ?: 0
        return OrderRules.validateOrder(side, quantity, price, _cash.value, owned)
    }

    // ---------- Talking to the server ----------

    // Called right after a login. Fetches the user's account in the background.
    fun onLoggedIn() {
        scope.launch { refresh() }
    }

    // Returns an error message, or null when it worked
    suspend fun refresh(): String? = attempt { applySnapshot(Api.account.account()) }

    suspend fun placeOrder(symbol: String, side: OrderSide, quantity: Int): PlaceOrderResult {
        return try {
            val response = Api.account.placeOrder(PlaceOrderRequest(symbol, side.name, quantity))
            applySnapshot(response.account)
            PlaceOrderResult(response.orderId, null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PlaceOrderResult(null, errorMessageOf(e))
        }
    }

    suspend fun deposit(amount: Double): String? {
        val error = attempt { applySnapshot(Api.account.deposit(AmountRequest(amount))) }
        if (error == null) {
            NotificationStore.add(
                NotificationType.ACCOUNT, "Deposit received",
                String.format(Locale.US, "MK %,.2f was added to your wallet.", amount)
            )
        }
        return error
    }

    suspend fun withdraw(amount: Double): String? {
        val error = attempt { applySnapshot(Api.account.withdraw(AmountRequest(amount))) }
        if (error == null) {
            NotificationStore.add(
                NotificationType.ACCOUNT, "Withdrawal sent",
                String.format(Locale.US, "MK %,.2f is on its way to your account.", amount)
            )
        }
        return error
    }

    // Called when the user logs out, so the next user never sees this data
    fun clear() {
        pollJob?.cancel()
        _cash.value = 0.0
        _holdings.value = emptyList()
        _transactions.value = emptyList()
        _orders.value = emptyList()
    }

    // ---------- Internals ----------

    private suspend fun attempt(block: suspend () -> Unit): String? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        errorMessageOf(e)
    }

    // Replaces the whole copy with what the server says
    private fun applySnapshot(account: AccountDto) {
        val before = _orders.value.associateBy { it.id }
        val newOrders = account.orders.map { it.toOrder() }

        // An order that was waiting and has now been answered: tell the user
        newOrders.forEach { order ->
            if (before[order.id]?.status == OrderStatus.SUBMITTED) {
                val side = if (order.side == OrderSide.BUY) "Buy" else "Sell"
                when (order.status) {
                    OrderStatus.FILLED -> NotificationStore.add(
                        NotificationType.ORDER, "Order filled",
                        String.format(Locale.US, "%s %d %s at MK %,.2f.", side, order.quantity, order.symbol, order.price)
                    )
                    OrderStatus.REJECTED -> NotificationStore.add(
                        NotificationType.ORDER, "Order rejected",
                        "$side ${order.quantity} ${order.symbol} was rejected. Your money or shares were returned."
                    )
                    OrderStatus.SUBMITTED -> {}
                }
            }
        }

        _cash.value = account.cash
        _holdings.value = account.holdings.map { Holding(it.symbol, it.quantity, it.avgCost) }
        _transactions.value = account.transactions.map {
            Transaction(it.id, TxType.valueOf(it.type), it.symbol, it.quantity, it.price, it.date)
        }
        _orders.value = newOrders

        if (newOrders.any { it.status == OrderStatus.SUBMITTED }) pollWhileWaiting()
    }

    // The broker answers after a few seconds, so ask again every second until no order is waiting
    private fun pollWhileWaiting() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            var tries = 0
            while (tries < 30 && _orders.value.any { it.status == OrderStatus.SUBMITTED }) {
                delay(1_000)
                refresh()
                tries++
            }
        }
    }

    private fun OrderDto.toOrder() = Order(
        id, symbol, OrderSide.valueOf(side), quantity, price, fees, OrderStatus.valueOf(status), createdAt
    )
}