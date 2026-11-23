package com.example.stockmarket

import com.example.stockmarket.background.findChanges
import com.example.stockmarket.data.remote.AccountDto
import com.example.stockmarket.data.remote.OrderDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// What the background worker decides to tell the user. Traces to NFR-02 (reliability) and FR-08 (notifications).
class BackgroundChangesTest {

    private fun order(id: Int, status: String, side: String = "BUY", quantity: Int = 10) =
        OrderDto(id, "NBM", side, quantity, 2010.0, 402.0, status, "05 Nov 2026")

    private fun account(vararg orders: OrderDto) =
        AccountDto(cash = 0.0, holdings = emptyList(), transactions = emptyList(), orders = orders.toList())

    @Test
    fun waitingOrder_nowFilled_isAnnouncedOnce() {
        val changes = findChanges(account(order(5, "SUBMITTED")), account(order(5, "FILLED")), null, "NOT_STARTED", null)

        assertEquals(1, changes.size)
        assertEquals("Order filled", changes[0].title)
        assertEquals("Buy 10 NBM at MK 2,010.00.", changes[0].text)
    }

    @Test
    fun waitingOrder_nowRejected_isAnnounced() {
        val changes = findChanges(account(order(6, "SUBMITTED", quantity = 13)), account(order(6, "REJECTED", quantity = 13)), null, "NOT_STARTED", null)

        assertEquals("Order rejected", changes.single().title)
        assertEquals("Buy 13 NBM was rejected. Your money or shares were returned.", changes.single().text)
    }

    @Test
    fun orderAlreadyFilledLastTime_isNotAnnouncedAgain() {
        val changes = findChanges(account(order(5, "FILLED")), account(order(5, "FILLED")), null, "NOT_STARTED", null)

        assertTrue(changes.isEmpty())
    }

    @Test
    fun noSavedCopy_announcesNoOrders() {
        val changes = findChanges(null, account(order(5, "FILLED")), null, "NOT_STARTED", null)

        assertTrue(changes.isEmpty())
    }

    @Test
    fun orderStillWaiting_isNotAnnounced() {
        val changes = findChanges(account(order(5, "SUBMITTED")), account(order(5, "SUBMITTED")), null, "NOT_STARTED", null)

        assertTrue(changes.isEmpty())
    }

    @Test
    fun twoOrdersAnsweredTogether_giveTwoSeparateNotifications() {
        val before = account(order(1, "SUBMITTED"), order(2, "SUBMITTED", side = "SELL", quantity = 5))
        val after = account(order(1, "FILLED"), order(2, "FILLED", side = "SELL", quantity = 5))

        val changes = findChanges(before, after, null, "NOT_STARTED", null)

        assertEquals(2, changes.size)
        assertEquals(2, changes.map { it.id }.toSet().size)        // different ids, so neither replaces the other
        assertTrue(changes.any { it.text == "Sell 5 NBM at MK 2,010.00." })
    }

    @Test
    fun registration_pendingToApproved_isAnnounced() {
        val changes = findChanges(account(), account(), "PENDING", "APPROVED", null)

        assertEquals("Registration approved", changes.single().title)
        assertEquals("You can now place buy and sell orders.", changes.single().text)
    }

    @Test
    fun registration_pendingToRejected_usesTheReviewersReason() {
        val changes = findChanges(account(), account(), "PENDING", "REJECTED", "Photo of the ID is unclear")

        assertEquals("Registration needs changes", changes.single().title)
        assertEquals("Photo of the ID is unclear", changes.single().text)
    }

    @Test
    fun registration_noDecisionYet_orNotUnderReview_saysNothing() {
        assertTrue(findChanges(account(), account(), "PENDING", "PENDING", null).isEmpty())
        assertTrue(findChanges(account(), account(), "IN_PROGRESS", "PENDING", null).isEmpty())
        assertTrue(findChanges(account(), account(), "APPROVED", "APPROVED", null).isEmpty())
    }
}