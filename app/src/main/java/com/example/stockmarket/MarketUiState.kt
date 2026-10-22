package com.example.stockmarket

import com.example.stockmarket.data.Company

enum class MarketTab(val label: String) {
    ALL("All"),
    GAINERS("Gainers"),
    LOSERS("Losers")
}

enum class SortOption(val label: String) {
    NAME("Name"),
    PRICE("Price"),
    CHANGE("Change")
}

data class MarketUiState(
    val query: String = "",
    val tab: MarketTab = MarketTab.ALL,
    val selectedSector: String? = null,   // null means "all sectors"
    val sort: SortOption = SortOption.NAME,
    val sectors: List<String> = emptyList(),
    val companies: List<Company> = emptyList(),
    val isLoading: Boolean = true,        // the very first load: show the skeleton
    val isRefreshing: Boolean = false,    // a later reload: show the pull-to-refresh spinner
    val error: String? = null,
    val lastUpdated: String? = null
)