package com.example.stockmarket


import androidx.lifecycle.ViewModel
import com.example.stockmarket.data.Company
import com.example.stockmarket.data.RegistrationStatus
import com.example.stockmarket.data.Transaction
import com.example.stockmarket.data.sampleCompanies
import com.example.stockmarket.data.sampleHoldings
import com.example.stockmarket.data.sampleTransactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val registrationStatus: RegistrationStatus = RegistrationStatus.NOT_STARTED,
    val portfolioValue: Double = 0.0,
    val todayChange: Double = 0.0,
    val todayChangePct: Double = 0.0,
    val topGainers: List<Company> = emptyList(),
    val topLosers: List<Company> = emptyList(),
    val recentActivity: List<Transaction> = emptyList()
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private fun buildState(): HomeUiState {
        var value = 0.0
        var previousValue = 0.0

        sampleHoldings.forEach { holding ->
            val company = sampleCompanies.find { it.symbol == holding.symbol } ?: return@forEach
            val current = holding.quantity * company.price
            value += current
            previousValue += current / (1 + company.changePct / 100)

        }

        val change = value - previousValue
        val changePct = if (previousValue > 0) change / previousValue * 100 else 0.0
        val byChange = sampleCompanies.sortedByDescending { it.changePct }

        return HomeUiState(
            registrationStatus = RegistrationStatus.NOT_STARTED,
            portfolioValue = value,
            todayChange = change,
            todayChangePct = changePct,
            topGainers = byChange.take(3),
            topLosers = byChange.takeLast(3).reversed(),
            recentActivity = sampleTransactions.sortedByDescending { it.id }.take(3)
        )
    }
}