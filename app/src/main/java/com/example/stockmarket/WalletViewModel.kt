package com.example.stockmarket


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.RegistrationDraft
import com.example.stockmarket.data.RegistrationStore
import com.example.stockmarket.data.TradingStore
import com.example.stockmarket.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class CashAction(val title: String) {
    DEPOSIT("Deposit"),
    WITHDRAW("Withdraw")
}

data class WalletUiState(
    val cash: Double = 0.0,
    val accountLabel: String = "No payment account saved yet",
    val cashHistory: List<Transaction> = emptyList()
)

class WalletViewModel : ViewModel() {

    val uiState: StateFlow<WalletUiState> = combine(
        TradingStore.cash,
        TradingStore.transactions,
        RegistrationStore.draft
    ) { cash, transactions, draft ->
        build(cash, transactions, draft)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = build(
            TradingStore.cash.value,
            TradingStore.transactions.value,
            RegistrationStore.draft.value
        )
    )

    // ---- Called by the screen ----
    fun validate(action: CashAction, amount: Double): String? = when (action) {
        CashAction.DEPOSIT -> TradingStore.validateDeposit(amount)
        CashAction.WITHDRAW -> TradingStore.validateWithdrawal(amount)
    }

    fun confirm(action: CashAction, amount: Double): Boolean = when (action) {
        CashAction.DEPOSIT -> TradingStore.deposit(amount)
        CashAction.WITHDRAW -> TradingStore.withdraw(amount)
    }

    // ---- Working out what to show ----
    private fun build(
        cash: Double,
        transactions: List<Transaction>,
        draft: RegistrationDraft
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
                .take(10)
        )
    }

    // Hides all but the last 2 characters: 0999000000 -> ••••••••00
    private fun mask(text: String): String =
        if (text.length <= 2) text else "•".repeat(text.length - 2) + text.takeLast(2)
}