package com.example.stockmarket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.Company
import com.example.stockmarket.data.CompanyRepository
import com.example.stockmarket.data.sector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarketViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    init {
        // Whenever the repository has a new list or a new loading state, rebuild the screen state
        viewModelScope.launch {
            combine(CompanyRepository.companies, CompanyRepository.loadState) { companies, load ->
                companies to load
            }.collect { (companies, load) ->
                _uiState.update { current ->
                    val next = current.copy(
                        sectors = companies.map { it.sector }.distinct().sorted(),
                        // The skeleton shows only for the very first load
                        isLoading = load.isLoading && load.lastUpdated == null,
                        isRefreshing = load.isLoading && load.lastUpdated != null,
                        error = load.error,
                        lastUpdated = load.lastUpdated
                    )
                    next.copy(companies = applyFilters(next, companies))
                }
            }
        }
    }

    // ---- Events from the screen ----
    fun onQueryChange(query: String) = change { it.copy(query = query) }
    fun onTabSelected(tab: MarketTab) = change { it.copy(tab = tab) }
    fun onSectorSelected(sector: String?) = change { it.copy(selectedSector = sector) }
    fun onSortSelected(sort: SortOption) = change { it.copy(sort = sort) }
    fun onRetry() = CompanyRepository.refresh()

    // ---- Internals ----
    // Changes one input, then recalculates the list
    private fun change(edit: (MarketUiState) -> MarketUiState) {
        _uiState.update { current ->
            val edited = edit(current)
            edited.copy(companies = applyFilters(edited, CompanyRepository.companies.value))
        }
    }

    private fun applyFilters(state: MarketUiState, source: List<Company>): List<Company> {
        // 1) search text and sector
        val searched = source.filter { company ->
            val matchesQuery =
                company.name.contains(state.query, ignoreCase = true) ||
                        company.symbol.contains(state.query, ignoreCase = true)
            val matchesSector =
                state.selectedSector == null || company.sector == state.selectedSector
            matchesQuery && matchesSector
        }

        // 2) the All / Gainers / Losers chips
        val byTab = when (state.tab) {
            MarketTab.ALL -> searched
            MarketTab.GAINERS -> searched.filter { it.changePct > 0 }
            MarketTab.LOSERS -> searched.filter { it.changePct < 0 }
        }

        // 3) sorting
        return when (state.sort) {
            SortOption.NAME -> byTab.sortedBy { it.name }
            SortOption.PRICE -> byTab.sortedByDescending { it.price }
            SortOption.CHANGE -> byTab.sortedByDescending { it.changePct }
        }
    }
}