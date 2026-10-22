package com.example.stockmarket.data



import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

// SAMPLE fee rate for the simulation. These are not real MSE or broker fees.
const val FEE_RATE = 0.02

// ONE shared copy for the whole app. In the real app this lives on the server.
object TradingStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _cash = MutableStateFlow(250_000.0)
    val cash: StateFlow<Double> = _cash.asStateFlow()

    private val _holdings = MutableStateFlow(sampleHoldings)
    val holdings: StateFlow<List<Holding>> = _holdings.asStateFlow()

    private val _transactions = MutableStateFlow(sampleTransactions)
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private var nextOrderId = 1
    private var nextTransactionId = 100

    // Returns an error message, or null if the order is allowed
    fun validate(symbol: String, side: OrderSide, quantity: Int, price: Double): String? {
        if (quantity <= 0) return "Enter the number of shares"
        return when (side) {
            OrderSide.BUY -> {
                val total = quantity * price * (1 + FEE_RATE)
                if (total > _cash.value) {
                    String.format(
                        Locale.US,
                        "Not enough cash. You need MK %,.2f but have MK %,.2f",
                        total, _cash.value
                    )
                } else null
            }
            OrderSide.SELL -> {
                val owned = _holdings.value.find { it.symbol == symbol }?.quantity ?: 0
                if (quantity > owned) "You only own $owned shares" else null
            }
        }
    }


    // Returns the new order's id, or null if the order was not allowed
    fun placeOrder(symbol: String, side: OrderSide, quantity: Int, price: Double): Int? {
        if (validate(symbol, side, quantity, price) != null) return null

        val subtotal = quantity * price
        val fees = subtotal * FEE_RATE
        val avgCost = _holdings.value.find { it.symbol == symbol }?.avgCost ?: price
        val id = nextOrderId++
        val sideText = if (side == OrderSide.BUY) "Buy" else "Sell"

        // 1) RESERVE the money or the shares so they cannot be used twice
        if (side == OrderSide.BUY) {
            _cash.update { it - (subtotal + fees) }
        } else {
            removeShares(symbol, quantity)
        }

        _orders.update {
            listOf(Order(id, symbol, side, quantity, price, fees, OrderStatus.SUBMITTED, today())) + it
        }

        // 2) The simulated broker answers after 4 seconds
        scope.launch {
            delay(4_000)
            if (quantity == 13) {
                // Test trick: an order for exactly 13 shares is always rejected
                if (side == OrderSide.BUY) {
                    _cash.update { it + subtotal + fees }
                } else {
                    addShares(symbol, quantity, avgCost)
                }
                setStatus(id, OrderStatus.REJECTED)
                NotificationStore.add(
                    NotificationType.ORDER,
                    "Order rejected",
                    "$sideText $quantity $symbol was rejected. Your money or shares were returned."
                )
            } else {
                if (side == OrderSide.BUY) {
                    addShares(symbol, quantity, price)
                } else {
                    _cash.update { it + subtotal - fees }
                }
                val type = if (side == OrderSide.BUY) TxType.BUY else TxType.SELL
                _transactions.update {
                    it + Transaction(nextTransactionId++, type, symbol, quantity, price, today())
                }
                setStatus(id, OrderStatus.FILLED)
                NotificationStore.add(
                    NotificationType.ORDER,
                    "Order filled",
                    "$sideText $quantity $symbol at MK " + String.format(Locale.US, "%,.2f", price) + "."
                )
            }
        }
        return id
    }

    private fun setStatus(id: Int, status: OrderStatus) {
        _orders.update { list -> list.map { if (it.id == id) it.copy(status = status) else it } }
    }

    private fun addShares(symbol: String, quantity: Int, price: Double) {
        _holdings.update { list ->
            val existing = list.find { it.symbol == symbol }
            if (existing == null) {
                list + Holding(symbol, quantity, price)
            } else {
                val total = existing.quantity + quantity
                val newAvg = (existing.quantity * existing.avgCost + quantity * price) / total
                list.map { if (it.symbol == symbol) Holding(symbol, total, newAvg) else it }
            }
        }
    }

    private fun removeShares(symbol: String, quantity: Int) {
        _holdings.update { list ->
            list.map { if (it.symbol == symbol) it.copy(quantity = it.quantity - quantity) else it }
                .filter { it.quantity > 0 }
        }
    }

    // ---------- Wallet (all amounts are in MK) ----------
    const val MIN_TRANSFER = 1_000.0
    const val MAX_DEPOSIT = 5_000_000.0

    fun validateDeposit(amount: Double): String? = when {
        amount < MIN_TRANSFER ->
            String.format(Locale.US, "The minimum is MK %,.0f", MIN_TRANSFER)
        amount > MAX_DEPOSIT ->
            String.format(Locale.US, "The maximum deposit is MK %,.0f", MAX_DEPOSIT)
        else -> null
    }

    fun validateWithdrawal(amount: Double): String? {
        if (amount < MIN_TRANSFER) {
            return String.format(Locale.US, "The minimum is MK %,.0f", MIN_TRANSFER)
        }
        return  if (amount > _cash.value) {
            String.format(Locale.US, "You only have MK %,.2f available", _cash.value)
        } else null
    }

    // Returns true if the money moved
    fun deposit(amount: Double): Boolean {
        if (validateDeposit(amount) != null) return false
        _cash.update { it + amount }
        _transactions.update {
            it + Transaction(nextTransactionId++, TxType.DEPOSIT, null, 0, amount, today())
        }
        NotificationStore.add(
            NotificationType.ACCOUNT,
            "Deposit received",
            String.format(Locale.US, "MK %,.2f was added to your wallet.", amount)
        )
        return true
    }

    fun withdraw(amount: Double): Boolean {
        if (validateWithdrawal(amount) != null) return false
        _cash.update { it - amount }
        _transactions.update {
            it + Transaction(nextTransactionId++, TxType.WITHDRAWAL, null, 0, amount, today())
        }
        NotificationStore.add(
            NotificationType.ACCOUNT,
            "Withdrawal sent",
            String.format(Locale.US, "MK %,.2f is on its way to your account.", amount)
        )
        return true
    }

    private fun today(): String = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
}