package com.example.stockmarket.background

import com.example.stockmarket.data.remote.AccountDto
import java.util.Locale

// One thing to tell the user. The id makes a repeat REPLACE the old message instead of piling up.
data class Change(val id: Int, val title: String, val text: String)

private const val REGISTRATION_ID = 1
private const val ORDER_ID_BASE = 1_000

// Compares what the phone knew last time with what the server says now.
// It has NO Android code in it, so it can be tested on the computer.
fun findChanges(
    before: AccountDto?,             // the saved copy (null = nothing saved yet)
    after: AccountDto,               // what the server says now
    beforeRegistration: String?,
    afterRegistration: String,
    rejectionReason: String?
): List<Change> {
    val changes = mutableListOf<Change>()

    // ---- Orders: only those that were WAITING last time and have been answered since ----
    // With no saved copy we cannot know what is new, so we announce nothing (no spam of old orders).
    if (before != null) {
        val waiting = before.orders.filter { it.status == "SUBMITTED" }.map { it.id }.toSet()

        after.orders.filter { it.id in waiting }.forEach { order ->
            val side = if (order.side == "BUY") "Buy" else "Sell"
            when (order.status) {
                "FILLED" -> changes.add(
                    Change(
                        ORDER_ID_BASE + order.id,
                        "Order filled",
                        String.format(Locale.US, "%s %d %s at MK %,.2f.", side, order.quantity, order.symbol, order.price)
                    )
                )
                "REJECTED" -> changes.add(
                    Change(
                        ORDER_ID_BASE + order.id,
                        "Order rejected",
                        "$side ${order.quantity} ${order.symbol} was rejected. Your money or shares were returned."
                    )
                )
            }
        }
    }

    // ---- Registration: only a decision on one that was under review ----
    if (beforeRegistration == "PENDING") {
        when (afterRegistration) {
            "APPROVED" -> {
                changes.add(Change(REGISTRATION_ID, "Registration approved", "You can now place buy and sell orders."))
            }
            "REJECTED" -> changes.add(
                Change(REGISTRATION_ID, "Registration needs changes", rejectionReason ?: "Please fix your details and submit again.")
            )
        }
    }

    return changes
}
