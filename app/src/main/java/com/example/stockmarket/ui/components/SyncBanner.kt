package com.example.stockmarket.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.repository.TradingStore
import com.example.stockmarket.ui.theme.StockMarketTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// 1730000000000 -> "10:42" (today) or "09 Nov 22:15" (another day)
fun formatSyncTime(
    timeMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val time = Instant.ofEpochMilli(timeMillis).atZone(zone)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)


    val pattern = if (time.toLocalDate() == now.toLocalDate()) "HH:mm" else "dd MMM HH:mm"

    return DateTimeFormatter.ofPattern(pattern, Locale.US).format(time)
}

data class SyncUiState(
    val error: String? = null,          // not null = the last refresh failed
    val lastSynced: Long? = null,       // when the numbers on screen were last confirmed
    val isRetrying: Boolean = false
)

class SyncViewModel : ViewModel() {

    private val retrying = MutableStateFlow(false)

    val uiState: StateFlow<SyncUiState> = combine(
        TradingStore.syncError, TradingStore.lastSynced, retrying
    ) { error, lastSynced, isRetrying ->
        SyncUiState(error, lastSynced, isRetrying)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncUiState())

    fun retry() {
        if (retrying.value) return
        retrying.value = true
        viewModelScope.launch {
            TradingStore.retryNow()
            retrying.value = false
        }
    }
}

// 1) STATEFUL: finds the ViewModel
@Composable
fun SyncBanner(viewModel: SyncViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    SyncBannerContent(state = state, onRetry = viewModel::retry)
}

// 2) STATELESS: draws nothing at all when everything is fine
@Composable
fun SyncBannerContent(state: SyncUiState, onRetry: () -> Unit) {
    if (state.error == null) return

    val message = if (state.lastSynced != null) {
        "Can't reach the server. Showing your data from ${formatSyncTime(state.lastSynced)}."
    } else {
        "Can't reach the server. Your balances can't be shown right now."
    }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        // statusBarsPadding keeps the text clear of the clock; the colour still fills behind it
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onRetry,
                enabled = !state.isRetrying,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) { Text(if (state.isRetrying) "Trying..." else "Retry") }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun SyncBannerPreview() {
    StockMarketTheme {
        SyncBannerContent(
            state = SyncUiState(error = "Can't reach the server", lastSynced = System.currentTimeMillis() - 3_600_000),
            onRetry = {}
        )
    }
}
