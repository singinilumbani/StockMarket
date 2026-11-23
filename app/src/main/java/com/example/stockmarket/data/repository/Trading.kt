package com.example.stockmarket.data.repository

import com.example.stockmarket.data.local.AccountCache
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
import java.io.IOException
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

    // When the numbers on screen were last confirmed by the server, or loaded from the saved copy.
    // null means we have nothing yet.
    private val _lastSynced = MutableStateFlow<Long?>(null)
    val lastSynced: StateFlow<Long?> = _lastSynced.asStateFlow()

    // Set when the last refresh failed, so the numbers on screen may be out of date.
    // Cleared by the next successful answer.
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private var pollJob: Job? = null
    private var currentUserId: Int? = null
    private var generation = 0          // goes up on every logout, so late answers are ignored

    // ---------- Instant checks while the user types ----------

    fun validate(symbol: String, side: OrderSide, quantity: Int, price: Double): String? {
        val owned = _holdings.value.find { it.symbol == symbol }?.quantity ?: 0
        return OrderRules.validateOrder(side, quantity, price, _cash.value, owned)
    }

    // ---------- Talking to the server ----------

    // Called right after a login
    fun onLoggedIn(userId: Int) {
        if (currentUserId != null && currentUserId != userId) clear()
        currentUserId = userId
        val gen = generation
        scope.launch {
            // 1) Show what we saved last time, so the screens are truthful even when offline
            val cached = AccountCache.readAccount(userId)
            if (cached != null && gen == generation && _lastSynced.value == null) {
                applySnapshot(cached.account, fromCache = true)
                _lastSynced.value = cached.savedAt
            }
            // 2) Then ask the server. If it answers first, the saved copy was never used.
            refresh()
        }
    }

    // Returns an error message, or null when it worked
    suspend fun refresh(): String? {
        val gen = generation
        val error = attempt {
            val account = Api.account.account()
            if (gen == generation) applySnapshot(account)      // ignore an answer that arrives after a logout
        }
        // Any failed refresh means the numbers on screen may be old
        if (error != null && gen == generation) _syncError.value = error
        return error
    }

    // For the Retry button
    fun retry() {
        scope.launch { retryNow() }
    }

    suspend fun retryNow() {
        refresh()
        RegistrationStore.refresh()
    }

    suspend fun placeOrder(symbol: String, side: OrderSide, quantity: Int): PlaceOrderResult {
        return try {
            val response = Api.account.placeOrder(PlaceOrderRequest(symbol, side.name, quantity))
            applySnapshot(response.account)
            PlaceOrderResult(response.orderId, null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is IOException) _syncError.value = errorMessageOf(e)
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
        generation++
        currentUserId = null
        pollJob?.cancel()
        _cash.value = 0.0
        _holdings.value = emptyList()
        _transactions.value = emptyList()
        _orders.value = emptyList()
        _lastSynced.value = null
        _syncError.value = null
        scope.launch {
            try {
                AccountCache.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // ignored on purpose
            }
        }
    }

    // ---------- Internals ----------

    private suspend fun attempt(block: suspend () -> Unit): String? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // A network problem means we are offline. A rule such as "Not enough cash" means the
        // server DID answer, so we are not.
        if (e is IOException) _syncError.value = errorMessageOf(e)
        errorMessageOf(e)
    }

    // Replaces the whole copy with what the server says (or with the saved copy, when fromCache is true)
    private fun applySnapshot(account: AccountDto, fromCache: Boolean = false) {
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

        if (!fromCache) {
            // The server just confirmed these numbers
            val now = System.currentTimeMillis()
            _lastSynced.value = now
            _syncError.value = null
            saveToCache(account, now)
            if (newOrders.any { it.status == OrderStatus.SUBMITTED }) pollWhileWaiting()
        }
    }

    private fun saveToCache(account: AccountDto, savedAt: Long) {
        val userId = currentUserId ?: return
        val gen = generation
        scope.launch {
            if (gen != generation) return@launch                // the user logged out meanwhile
            try {
                AccountCache.saveAccount(userId, savedAt, account)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // a failed save must never break the app
            }
        }
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