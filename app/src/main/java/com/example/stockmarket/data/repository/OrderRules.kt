package com.example.stockmarket.data.repository

import java.util.Locale

// Quick checks on the phone for instant feedback. The server repeats every one of them.
// These are plain functions with no state, so they are easy to test.
object OrderRules {

    const val MIN_TRANSFER = 1_000.0
    const val MAX_DEPOSIT = 5_000_000.0

    fun validateOrder(side: OrderSide, quantity: Int, price: Double, cash: Double, owned: Int): String? {
        if (quantity <= 0) return "Enter the number of shares"
        return when (side) {
            OrderSide.BUY -> {
                val total = quantity * price * (1 + FEE_RATE)
                if (total > cash) {
                    String.format(Locale.US, "Not enough cash. You need MK %,.2f but have MK %,.2f", total, cash)
                } else null
            }
            OrderSide.SELL -> if (quantity > owned) "You only own $owned shares" else null
        }
    }

    fun validateDeposit(amount: Double): String? = when {
        amount < MIN_TRANSFER -> String.format(Locale.US, "The minimum is MK %,.0f", MIN_TRANSFER)
        amount > MAX_DEPOSIT -> String.format(Locale.US, "The maximum deposit is MK %,.0f", MAX_DEPOSIT)
        else -> null
    }

    fun validateWithdrawal(amount: Double, cash: Double): String? = when {
        amount < MIN_TRANSFER -> String.format(Locale.US, "The minimum is MK %,.0f", MIN_TRANSFER)
        amount > cash -> String.format(Locale.US, "You only have MK %,.2f available", cash)
        else -> null
    }
}

