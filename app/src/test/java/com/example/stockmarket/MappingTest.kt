package com.example.stockmarket



import com.example.stockmarket.data.local.CompanyEntity
import com.example.stockmarket.data.local.toCompany
import com.example.stockmarket.data.local.toEntity
import com.example.stockmarket.data.remote.CompanyDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests the conversions between the server shape, the database row and the app model.
// Traces to FR-02 and NFR-02.
class MappingTest {

    private val dto = CompanyDto(
        symbol = "NBM",
        name = "National Bank of Malawi",
        sector = "Banking",
        price = 2010.0,
        changePct = 1.25,
        updatedAt = "2026-10-21",
        history = listOf(1980.5, 2009.78, 2010.0)
    )

    @Test
    fun toEntity_joinsHistoryWithCommas() {
        assertEquals("1980.5,2009.78,2010.0", dto.toEntity().historyCsv)
    }

    @Test
    fun serverToDatabaseAndBack_keepsEveryValue() {
        val company = dto.toEntity().toCompany()

        assertEquals("NBM", company.symbol)
        assertEquals("National Bank of Malawi", company.name)
        assertEquals(2010.0, company.price, 0.0)
        assertEquals(1.25, company.changePct, 0.0)
        assertEquals(dto.history, company.history)
    }

    @Test
    fun emptyHistoryText_givesEmptyHistoryList() {
        val entity = CompanyEntity("NBM", "National Bank of Malawi", "Banking", 2010.0, 1.25, "2026-10-21", "")

        assertTrue(entity.toCompany().history.isEmpty())
    }
}