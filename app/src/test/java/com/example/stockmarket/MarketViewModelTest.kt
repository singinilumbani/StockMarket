package com.example.stockmarket

import com.example.stockmarket.feature.market.MarketTab
import com.example.stockmarket.feature.market.MarketViewModel
import com.example.stockmarket.feature.market.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Automated versions of TC-MKT-02, 03, 04 and 06. Traces to FR-02.
@OptIn(ExperimentalCoroutinesApi::class)
class MarketViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun search_noMatch_returnsNoCompanies() {
        val viewModel = MarketViewModel()

        viewModel.onQueryChange("zzz")

        assertTrue(viewModel.uiState.value.companies.isEmpty())
    }

    @Test
    fun search_nb_returnsOnlyMatchingCompanies() {
        val viewModel = MarketViewModel()

        viewModel.onQueryChange("nb")
        val companies = viewModel.uiState.value.companies

        assertTrue(companies.isNotEmpty())
        assertTrue(companies.all {
            it.symbol.contains("nb", ignoreCase = true) || it.name.contains("nb", ignoreCase = true)
        })
    }

    @Test
    fun gainersTab_showsOnlyRisingCompanies() {
        val viewModel = MarketViewModel()

        viewModel.onTabSelected(MarketTab.GAINERS)

        assertTrue(viewModel.uiState.value.companies.all { it.changePct > 0 })
    }

    @Test
    fun losersTab_showsOnlyFallingCompanies() {
        val viewModel = MarketViewModel()
        viewModel.onTabSelected(MarketTab.LOSERS)
        assertTrue(viewModel.uiState.value.companies.all { it.changePct < 0 })
    }

    @Test
    fun sortByPrice_putsTheHighestPriceFirst() {
        val viewModel = MarketViewModel()

        viewModel.onSortSelected(SortOption.PRICE)
        val prices = viewModel.uiState.value.companies.map { it.price }

        assertEquals(prices.sortedDescending(), prices)
    }
}
