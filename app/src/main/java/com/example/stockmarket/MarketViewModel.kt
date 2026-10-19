package com.example.stockmarket

import androidx.lifecycle.ViewModel
import com.example.stockmarket.data.Company
import com.example.stockmarket.data.sampleCompanies
import com.example.stockmarket.data.sector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MarketViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    // ---- Events from the screen ----
    fun onQueryChange(query: String) = change { it.copy(query = query) }
    fun onTabSelected(tab: MarketTab) = change { it.copy(tab = tab) }
    fun onSectorSelected(sector: String?) = change { it.copy(selectedSector = sector) }
    fun onSortSelected(sort: SortOption) = change { it.copy(sort = sort) }

    // ---- Internals ----
    private fun initialState(): MarketUiState {
        val base = MarketUiState(
            sectors = sampleCompanies.map { it.sector }.distinct().sorted()
        )
        return base.copy(companies = applyFilters(base))
    }

    // Changes one input, then recalculates the list
    private fun change(edit: (MarketUiState) -> MarketUiState) {
        _uiState.update { current ->
            val edited = edit(current)
            edited.copy(companies = applyFilters(edited))
        }
    }

    private fun applyFilters(state: MarketUiState): List<Company> {
        // 1) search text and sector
        val searched = sampleCompanies.filter { company ->
            val matchesQuery =
                company.name.contains(state.query, ignoreCase = true) ||
                        company.symbol.contains(state.query, ignoreCase = true)
            val matchesSector =
                state.selectedSector == null || company.sector == state.selectedSector
            matchesQuery && matchesSector
        }

        // 2) All / Gainers / Losers chips
        val byTab = when (state.tab) {
            MarketTab.ALL -> searched
            MarketTab.GAINERS -> searched.filter { it.changePct > 0 }
            MarketTab.LOSERS -> searched.filter { it.changePct < 0 }
        }

        // 3) sorting
        return when (state.sort) {
            SortOption.NAME -> byTab.sortedBy { it.name }

            SortOption.PRICE -> byTab.sortedByDescending { it.price }

            SortOption.CHANGE -> {
                if (state.tab == MarketTab.LOSERS) {
                    byTab.sortedBy { it.changePct }
                } else {
                    byTab.sortedByDescending { it.changePct }
                }
            }
        }
    }

    fun onClearFilters() {
        onTabSelected(MarketTab.ALL)
        onSectorSelected(null)
        onQueryChange("")
    }
}