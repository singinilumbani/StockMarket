package com.example.stockmarket

import com.example.stockmarket.ui.components.formatSyncTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

// The time shown in the offline banner.
class SyncTimeFormatTest {

    private val utc = ZoneOffset.UTC

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, utc).toInstant().toEpochMilli()

    @Test
    fun sameDay_showsOnlyTheTime() {
        val saved = millis(2026, 11, 10, 10, 42)
        val now = millis(2026, 11, 10, 18, 5)

        assertEquals("10:42", formatSyncTime(saved, now, utc))
    }

    @Test
    fun earlierDay_showsTheDateAndTheTime() {
        val saved = millis(2026, 11, 9, 22, 15)
        val now = millis(2026, 11, 10, 8, 0)

        assertEquals("09 Nov 22:15", formatSyncTime(saved, now, utc))
    }

    @Test
    fun justAfterMidnight_isStillTheSameDay() {
        val saved = millis(2026, 11, 10, 0, 1)
        val now = millis(2026, 11, 10, 23, 59)

        assertEquals("00:01", formatSyncTime(saved, now, utc))
    }

    @Test
    fun acrossTheNewYear_showsTheDate() {
        val saved = millis(2026, 12, 31, 23, 50)
        val now = millis(2027, 1, 1, 9, 0)

        assertEquals("31 Dec 23:50", formatSyncTime(saved, now, utc))
    }
}