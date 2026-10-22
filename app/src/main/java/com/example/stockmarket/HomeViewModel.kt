package com.example.stockmarket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.Company
import com.example.stockmarket.data.CompanyRepository
import com.example.stockmarket.data.Holding
import com.example.stockmarket.data.RegistrationStatus
import com.example.stockmarket.data.RegistrationStore
import com.example.stockmarket.data.TradingStore
import com.example.stockmarket.data.Transaction
import com.example.stockmarket.data.WatchlistStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val registrationStatus: RegistrationStatus = RegistrationStatus.NOT_STARTED,
    val portfolioValue: Double = 0.0,
    val todayChange: Double = 0.0,
    val todayChangePct: Double = 0.0,
    val topGainers: List<Company> = emptyList(),
    val topLosers: List<Company> = emptyList(),
    val watchlist: List<Company> = emptyList(),
    val recentActivity: List<Transaction> = emptyList()
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        buildState(
            RegistrationStore.status.value,
            TradingStore.holdings.value,
            TradingStore.transactions.value,
            WatchlistStore.symbols.value,
            CompanyRepository.companies.value
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Rebuild the screen whenever ANY of these five things changes
        viewModelScope.launch {
            combine(
                RegistrationStore.status,
                TradingStore.holdings,
                TradingStore.transactions,
                WatchlistStore.symbols,
                CompanyRepository.companies
            ) { status, holdings, transactions, watched, companies ->
                buildState(status, holdings, transactions, watched, companies)
            }.collect { _uiState.value = it }
        }
    }

    private fun buildState(
        status: RegistrationStatus,
        holdings: List<Holding>,
        transactions: List<Transaction>,
        watched: List<String>,
        companies: List<Company>
    ): HomeUiState {
        var value = 0.0
        var previousValue = 0.0

        holdings.forEach { holding ->
            val company = companies.find { it.symbol == holding.symbol } ?: return@forEach
            val current = holding.quantity * company.price
            value += current
            previousValue += current / (1 + company.changePct / 100)
        }

        val change = value - previousValue
        val changePct = if (previousValue > 0) change / previousValue * 100 else 0.0
        val byChange = companies.sortedByDescending { it.changePct }

        return HomeUiState(
            registrationStatus = status,
            portfolioValue = value,
            todayChange = change,
            todayChangePct = changePct,
            topGainers = byChange.take(3),
            topLosers = byChange.takeLast(3).reversed(),
            watchlist = watched.mapNotNull { symbol -> companies.find { it.symbol == symbol } },
            recentActivity = transactions.sortedByDescending { it.id }.take(3)
        )
    }
}