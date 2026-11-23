package com.example.stockmarket.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.model.Transaction
import com.example.stockmarket.data.repository.OrderRules
import com.example.stockmarket.data.repository.RegistrationDraft
import com.example.stockmarket.data.repository.RegistrationStore
import com.example.stockmarket.data.repository.TradingStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CashAction(val title: String) {
    DEPOSIT("Deposit"),
    WITHDRAW("Withdraw")
}

data class WalletUiState(
    val cash: Double = 0.0,
    val accountLabel: String = "No payment account saved yet",
    val cashHistory: List<Transaction> = emptyList(),
    val isOffline: Boolean = false      // true while the server cannot be reached
)

class WalletViewModel : ViewModel() {

    val uiState: StateFlow<WalletUiState> = combine(
        TradingStore.cash,
        TradingStore.transactions,
        RegistrationStore.draft,
        TradingStore.syncError
    ) { cash, transactions, draft, syncError ->
        build(cash, transactions, draft, syncError != null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = build(
            TradingStore.cash.value,
            TradingStore.transactions.value,
            RegistrationStore.draft.value,
            TradingStore.syncError.value != null
        )
    )

    // Instant check while typing. The server checks again.
    fun validate(action: CashAction, amount: Double): String? = when (action) {
        CashAction.DEPOSIT -> OrderRules.validateDeposit(amount)
        CashAction.WITHDRAW -> OrderRules.validateWithdrawal(amount, TradingStore.cash.value)
    }

    // Asks the server. onResult gets null on success, or the message to show.
    fun confirm(action: CashAction, amount: Double, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = when (action) {
                CashAction.DEPOSIT -> TradingStore.deposit(amount)
                CashAction.WITHDRAW -> TradingStore.withdraw(amount)
            }
            onResult(error)
        }
    }

    private fun build(
        cash: Double,
        transactions: List<Transaction>,
        draft: RegistrationDraft,
        offline: Boolean
    ): WalletUiState {
        val label = if (draft.accountNumber.isBlank()) {
            "No payment account saved yet"
        } else {
            "${draft.payoutMethod} · ${mask(draft.accountNumber)}"
        }
        return WalletUiState(
            cash = cash,
            accountLabel = label,
            cashHistory = transactions
                .filter { it.isCash }
                .sortedByDescending { it.id }
                .take(10),
            isOffline = offline
        )
    }

    // Hides all but the last 2 characters
    private fun mask(text: String): String =
        if (text.length <= 2) text else "•".repeat(text.length - 2) + text.takeLast(2)
}