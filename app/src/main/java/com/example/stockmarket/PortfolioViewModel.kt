package com.example.stockmarket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.Company
import com.example.stockmarket.data.CompanyRepository
import com.example.stockmarket.data.Holding
import com.example.stockmarket.data.TradingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HoldingRow(
    val symbol: String,
    val name: String,
    val quantity: Int,
    val avgCost: Double,
    val price: Double,
    val value: Double,
    val gain: Double,
    val gainPct: Double
)

data class PortfolioUiState(
    val cash: Double = 0.0,
    val investedValue: Double = 0.0,
    val totalGain: Double = 0.0,
    val totalGainPct: Double = 0.0,
    val rows: List<HoldingRow> = emptyList()
) {
    val totalValue: Double get() = cash + investedValue
}

class PortfolioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        build(TradingStore.cash.value, TradingStore.holdings.value, CompanyRepository.companies.value)
    )
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                TradingStore.cash,
                TradingStore.holdings,
                        CompanyRepository.companies
            ) { cash, holdings, companies ->
                build(cash, holdings, companies)
            }.collect { _uiState.value = it }
        }
    }

    private fun build(cash: Double, holdings: List<Holding>, companies: List<Company>): PortfolioUiState {
        val rows = holdings.mapNotNull { holding ->
            val company = companies.find { it.symbol == holding.symbol }
                ?: return@mapNotNull null

            val value = holding.quantity * company.price
            val cost = holding.quantity * holding.avgCost
            val gain = value - cost
            val gainPct = if (cost > 0) gain / cost * 100 else 0.0

            HoldingRow(
                symbol = holding.symbol,
                name = company.name,
                quantity = holding.quantity,
                avgCost = holding.avgCost,
                price = company.price,
                value = value,
                gain = gain,
                gainPct = gainPct
            )
        }.sortedByDescending { it.value }

        val invested = rows.sumOf { it.value }
        val totalGain = rows.sumOf { it.gain }
        val totalCost = invested - totalGain
        val totalGainPct = if (totalCost > 0) totalGain / totalCost * 100 else 0.0

        return PortfolioUiState(
            cash = cash,
            investedValue = invested,
            totalGain = totalGain,
            totalGainPct = totalGainPct,
            rows = rows
        )
    }
}