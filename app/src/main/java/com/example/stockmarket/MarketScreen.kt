@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stockmarket

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.Company
import com.example.stockmarket.data.sampleCompanies
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.ChangeChip
import com.example.stockmarket.ui.theme.components.EmptyState
import com.example.stockmarket.ui.theme.components.TabHeader
import java.util.Locale

// 1) STATEFUL: gets the ViewModel
@Composable
fun MarketScreen(
    onCompanyClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: MarketViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    MarketContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onTabSelected = viewModel::onTabSelected,
        onSectorSelected = viewModel::onSectorSelected,
        onSortSelected = viewModel::onSortSelected,
        onClearFilters = viewModel::onClearFilters,
        onCompanyClick = onCompanyClick,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun MarketContent(
    state: MarketUiState,
    onQueryChange: (String) -> Unit,
    onTabSelected: (MarketTab) -> Unit,
    onSectorSelected: (String?) -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onCompanyClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val filtering = state.query.isNotBlank() ||
            state.selectedSector != null ||
            state.tab != MarketTab.ALL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        TabHeader(
            title = "Market",
            unreadCount = 3,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )

        Spacer(Modifier.height(8.dp))

        // Search box
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text("Search") },
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            shape = RoundedCornerShape(20.dp),
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )

        Spacer(Modifier.height(4.dp))

        // All / Gainers / Losers
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarketTab.entries.forEach { tab ->
                FilterChip(
                    selected = state.tab == tab,
                    onClick = { onTabSelected(tab) },
                    label = { Text(tab.label) }
                )
            }
        }

        // Sector chips (scroll sideways)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedSector == null,
                    onClick = { onSectorSelected(null) },
                    label = { Text("All sectors") }
                )
            }
            items(state.sectors) { sector ->
                FilterChip(
                    selected = state.selectedSector == sector,
                    onClick = { onSectorSelected(sector) },
                    label = { Text(sector) }
                )
            }
        }

        // Result count + sort menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    !filtering -> "${state.companies.size} companies"
                    state.companies.size == 1 -> "1 result"
                    else -> "${state.companies.size} results"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            SortMenu(current = state.sort, onSortSelected = onSortSelected)
        }

        // List or empty message
        if (state.companies.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                EmptyState(
                    message = "No companies match your filters"
                )

                TextButton(
                    onClick = onClearFilters
                ) {
                    Text("Clear filters")
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.companies, key = { it.symbol }) { company ->
                    CompanyRow(
                        company = company,
                        onClick = { onCompanyClick(company.symbol) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortMenu(current: SortOption, onSortSelected: (SortOption) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { menuOpen = true }) {
            Text("Sort: ${current.label}")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSortSelected(option)
                        menuOpen = false
                    }
                )
            }
        }
    }
}

@Composable
fun CompanyRow(company: Company, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = company.symbol, fontWeight = FontWeight.Bold)
                Text(
                    text = company.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = company.sector,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = String.format(Locale.US, "%,.2f", company.price))
                Spacer(Modifier.height(4.dp))
                ChangeChip(company.changePct)
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun MarketContentPreview() {
    StockMarketTheme {
        Surface {
            MarketContent(
                state = MarketUiState(
                    sectors = listOf("Banking", "Telecoms"),
                    companies = sampleCompanies
                ),
                onQueryChange = {},
                onTabSelected = {},
                onSectorSelected = {},
                onSortSelected = {},
                onCompanyClick = {},
                onNotificationsClick = {},
                onProfileClick = {},
                onClearFilters = {},
            )
        }
    }
}