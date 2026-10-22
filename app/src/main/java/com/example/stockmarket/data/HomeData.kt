package com.example.stockmarket.data


enum class RegistrationStatus { NOT_STARTED, IN_PROGRESS, PENDING, REJECTED, APPROVED }

data class Holding(
    val symbol: String,
    val quantity: Int,
    val avgCost: Double
)


enum class TxType { BUY, SELL, DEPOSIT, WITHDRAWAL }

data class Transaction(
    val id: Int,
    val type: TxType,
    val symbol: String?,
    val quantity: Int,
    val price: Double,
    val date: String
) {
    // A deposit or withdrawal moves cash only, with no shares
    val isCash: Boolean
        get() = type == TxType.DEPOSIT || type == TxType.WITHDRAWAL

    // For cash, "price" holds the amount of money
    val amount: Double
        get() = if (isCash) price else quantity * price
}


// SAMPLE DATA ONLY
val sampleHoldings = listOf(
    Holding("NBM", 300, 1850.0),
    Holding("TNM", 2000, 23.5),
    Holding("AIRTEL", 1500, 90.0),
    Holding("FDHB", 400, 75.0)
)

val sampleTransactions = listOf(
    Transaction(1, TxType.DEPOSIT, null, 0, 1_000_000.0, "02 Oct 2026"),
    Transaction(2, TxType.BUY, "NBM", 300, 1850.0, "05 Oct 2026"),
    Transaction(3, TxType.BUY, "TNM", 2000, 23.5, "08 Oct 2026"),
    Transaction(4, TxType.BUY, "AIRTEL", 1500, 90.0, "12 Oct 2026"),
    Transaction(5, TxType.BUY, "FDHB", 400, 75.0, "15 Oct 2026"),
    Transaction(6, TxType.SELL, "NBS", 500, 41.0, "16 Oct 2026")
)