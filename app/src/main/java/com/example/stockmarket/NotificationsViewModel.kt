package com.example.stockmarket



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.NotificationItem
import com.example.stockmarket.data.NotificationStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class NotificationsUiState(
    val today: List<NotificationItem> = emptyList(),
    val earlier: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0
)

class NotificationsViewModel : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = NotificationStore.items
        .map { items -> build(items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = build(NotificationStore.items.value)
        )

    // ---- Events from the screen ----
    fun onNotificationClick(id: Int) = NotificationStore.markRead(id)
    fun onMarkAllRead() = NotificationStore.markAllRead()

    // ---- Working out what to show ----
    private fun build(items: List<NotificationItem>): NotificationsUiState {
        val now = System.currentTimeMillis()
        val sorted = items.sortedByDescending { it.timestamp }

        // partition splits the list in two: the items where the test is true, and the rest
        val (today, earlier) = sorted.partition { item ->
            now - item.timestamp < 24 * 60 * 60 * 1000L
        }

        return NotificationsUiState(
            today = today,
            earlier = earlier,
            unreadCount = items.count { !it.read }
        )
    }
}