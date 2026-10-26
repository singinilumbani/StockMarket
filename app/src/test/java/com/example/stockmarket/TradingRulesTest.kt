package com.example.stockmarket


import com.example.stockmarket.data.repository.OrderSide
import com.example.stockmarket.data.repository.TradingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests TradingStore.validate with the starting data: cash MK 250,000 and 300 NBM shares.
// Traces to FR-03, FR-04 and NFR-02.
@OptIn(ExperimentalCoroutinesApi::class)
class TradingRulesTest {

    // The stand-in for the main thread. It is installed before each test and removed after.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun buy_withEnoughCash_isAllowed() {
        assertNull(TradingStore.validate("NBM", OrderSide.BUY, 10, 2010.0))
    }

    @Test
    fun buy_moreThanTheCash_saysNotEnoughCash() {
        val message = TradingStore.validate("NBM", OrderSide.BUY, 1000, 2010.0)

        assertNotNull(message)
        assertTrue(message!!.startsWith("Not enough cash"))
    }

    @Test
    fun sell_ownedShares_isAllowed() {
        assertNull(TradingStore.validate("NBM", OrderSide.SELL, 5, 2010.0))
    }

    @Test
    fun sell_moreThanOwned_saysHowManyAreOwned() {
        assertEquals("You only own 300 shares", TradingStore.validate("NBM", OrderSide.SELL, 301, 2010.0))
    }

    @Test
    fun sell_companyNotOwned_saysZeroOwned() {
        assertEquals("You only own 0 shares", TradingStore.validate("OMU", OrderSide.SELL, 1, 3040.0))
    }

    @Test
    fun zeroQuantity_asksForShares() {
        assertEquals("Enter the number of shares", TradingStore.validate("NBM", OrderSide.BUY, 0, 2010.0))
    }
}