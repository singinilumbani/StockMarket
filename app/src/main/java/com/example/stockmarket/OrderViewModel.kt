package com.example.stockmarket



import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.Company
import com.example.stockmarket.data.CompanyRepository
import com.example.stockmarket.data.FEE_RATE
import com.example.stockmarket.data.OrderSide
import com.example.stockmarket.data.TradingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderUiState(
    val company: Company? = null,
    val side: OrderSide = OrderSide.BUY,
    val quantityText: String = "",
    val cash: Double = 0.0,
    val owned: Int = 0,
    val error: String? = null
) {
    val quantity: Int get() = quantityText.toIntOrNull() ?: 0
    val subtotal: Double get() = (company?.price ?: 0.0) * quantity
    val fees: Double get() = subtotal * FEE_RATE
    val total: Double get() = if (side == OrderSide.BUY) subtotal + fees else subtotal - fees

    val canPlace: Boolean
        get() = false // TODO: true when quantity is more than 0 AND error is null
}

class OrderViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // The route "order/NBM/BUY" hands us its parts through savedStateHandle
    private val symbol: String = savedStateHandle["symbol"] ?: ""
    private val side: OrderSide =
        if (savedStateHandle.get<String>("side") == "SELL") OrderSide.SELL else OrderSide.BUY

    private val company: Company? = CompanyRepository.find(symbol)

    private val _uiState = MutableStateFlow(OrderUiState(company = company, side = side))
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    init {
        // Keep "available cash" and "shares owned" up to date
        viewModelScope.launch {
            combine(TradingStore.cash, TradingStore.holdings) { cash, holdings ->
                cash to (holdings.find { it.symbol == symbol }?.quantity ?: 0)
            }.collect { (cash, owned) ->
                change { it.copy(cash = cash, owned = owned) }
            }
        }
    }

    fun onQuantityChange(text: String) {
        change { it.copy(quantityText = text.filter { c -> c.isDigit() }.take(7)) }
    }

    fun placeOrder(): Int? {
        val state = _uiState.value
        val company = state.company ?: return null
        return TradingStore.placeOrder(company.symbol, state.side, state.quantity, company.price)
    }

    // Applies an edit, then re-checks the order
    private fun change(edit: (OrderUiState) -> OrderUiState) {
        _uiState.update { current ->
            val edited = edit(current)
            edited.copy(error = errorFor(edited))
        }
    }

    private fun errorFor(state: OrderUiState): String? {
        val company = state.company ?: return "Company not found"
        if (state.quantity == 0) return null
        return TradingStore.validate(company.symbol, state.side, state.quantity, company.price)
    }
}