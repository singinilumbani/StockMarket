package com.example.stockmarket



import com.example.stockmarket.data.model.Company
import com.example.stockmarket.data.repository.OrderSide
import com.example.stockmarket.feature.trade.OrderUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for the order ticket calculations. Traces to FR-03, FR-04 and NFR-02.
class OrderUiStateTest {

    private val nbm = Company(
        symbol = "NBM",
        name = "National Bank of Malawi",
        sector = "Banking",
        price = 2010.0,
        changePct = 1.25
    )

    @Test
    fun buyTenShares_calculatesSubtotalFeesAndTotal() {
        val state = OrderUiState(company = nbm, side = OrderSide.BUY, quantityText = "10")

        assertEquals(20_100.0, state.subtotal, 0.001)
        assertEquals(402.0, state.fees, 0.001)
        assertEquals(20_502.0, state.total, 0.001)
    }

    @Test
    fun sellFiveShares_totalIsSubtotalMinusFees() {
        val state = OrderUiState(company = nbm, side = OrderSide.SELL, quantityText = "5")
        assertEquals(10_050.0, state.subtotal, 0.001)
        assertEquals(201.0, state.fees, 0.001)
        assertEquals(9_849.0, state.total, 0.001)
    }

    @Test
    fun emptyQuantity_isZero_andCannotPlace() {
        val state = OrderUiState(company = nbm, quantityText = "")

        assertEquals(0, state.quantity)
        assertFalse(state.canPlace)
    }

    @Test
    fun validQuantityWithoutError_canPlace() {
        val state = OrderUiState(company = nbm, quantityText = "10", error = null)

        assertTrue(state.canPlace)
    }

    @Test
    fun validQuantityWithError_cannotPlace() {
        val state = OrderUiState(company = nbm, quantityText = "10", error = "Not enough cash")

        assertFalse(state.canPlace)
    }
}