package com.example.stockmarket

import com.example.stockmarket.data.model.Transaction
import com.example.stockmarket.data.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests the transaction amount rule. Traces to FR-06 and NFR-02.
class TransactionTest {

    @Test
    fun buy_amountIsQuantityTimesPrice() {
        val tx = Transaction(1, TxType.BUY, "NBM", 300, 1850.0, "04 Oct 2026")

        assertEquals(555_000.0, tx.amount, 0.001)
        assertFalse(tx.isCash)
    }

    @Test
    fun deposit_amountIsThePriceField() {
        val tx = Transaction(2, TxType.DEPOSIT, null, 0, 10_000.0, "05 Oct 2026")

        assertEquals(10_000.0, tx.amount, 0.001)
        assertTrue(tx.isCash)
    }

    @Test
    fun withdrawal_isCash() {
        val tx = Transaction(3, TxType.WITHDRAWAL, null, 0, 20_000.0, "06 Oct 2026")

        assertTrue(tx.isCash)
    }
}

