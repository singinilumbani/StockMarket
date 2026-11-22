package com.example.stockmarket

import com.example.stockmarket.data.repository.OrderRules
import com.example.stockmarket.data.repository.OrderSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class OrderRulesTest {

    @Test
    fun buy_withEnoughCash_isAllowed() {
        assertNull(OrderRules.validateOrder(OrderSide.BUY, 10, 2010.0, cash = 250_000.0, owned = 300))
    }

    @Test
    fun buy_121Shares_isAllowed() {
        assertNull(OrderRules.validateOrder(OrderSide.BUY, 121, 2010.0, cash = 250_000.0, owned = 300))
    }

    @Test
    fun buy_122Shares_saysNotEnoughCash() {
        assertEquals(
            "Not enough cash. You need MK 250,124.40 but have MK 250,000.00",
            OrderRules.validateOrder(OrderSide.BUY, 122, 2010.0, cash = 250_000.0, owned = 300)
        )
    }

    @Test
    fun sell_ownedShares_isAllowed() {
        assertNull(OrderRules.validateOrder(OrderSide.SELL, 5, 2010.0, cash = 250_000.0, owned = 300))
    }

    @Test
    fun sell_301When300Owned_saysHowManyAreOwned() {
        assertEquals(
            "You only own 300 shares",
            OrderRules.validateOrder(OrderSide.SELL, 301, 2010.0, cash = 250_000.0, owned = 300)
        )
    }

    @Test
    fun sell_companyNotOwned_saysZeroOwned() {
        assertEquals(
            "You only own 0 shares",
            OrderRules.validateOrder(OrderSide.SELL, 1, 3040.0, cash = 250_000.0, owned = 0)
        )
    }

    @Test
    fun zeroQuantity_asksForShares() {
        assertEquals(
            "Enter the number of shares",
            OrderRules.validateOrder(OrderSide.BUY, 0, 2010.0, cash = 250_000.0, owned = 300)
        )
    }

    @Test
    fun deposit_limits() {
        assertEquals("The minimum is MK 1,000", OrderRules.validateDeposit(500.0))
        assertNull(OrderRules.validateDeposit(1_000.0))
        assertEquals("The maximum deposit is MK 5,000,000", OrderRules.validateDeposit(5_000_001.0))
    }

    @Test
    fun withdrawal_limits() {
        assertEquals("You only have MK 250,000.00 available", OrderRules.validateWithdrawal(250_001.0, cash = 250_000.0))
        assertNull(OrderRules.validateWithdrawal(250_000.0, cash = 250_000.0))
    }
}

