package com.example.stockmarket.data



import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class NotificationType { ORDER, ACCOUNT, ANNOUNCEMENT, PRICE_ALERT }

data class NotificationItem(
    val id: Int,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,          // milliseconds since 1970, from System.currentTimeMillis()
    val read: Boolean = false
)

// ONE shared copy for the whole app. In the real app these come from the server.
object NotificationStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val hour = 60 * 60 * 1000L
    private val now = System.currentTimeMillis()
    private var nextId = 100

    // SAMPLE DATA: three unread and two read
    private val _items = MutableStateFlow(
        listOf(
            NotificationItem(
                1, NotificationType.ANNOUNCEMENT, "Market closed",
                "Today's closing prices are now available. (Sample)", now - 1 * hour
            ),
            NotificationItem(
                2, NotificationType.PRICE_ALERT, "NBM is up 1.25%",
                "National Bank of Malawi closed at MK 2,010.00. (Sample)", now - 3 * hour
            ),
            NotificationItem(
                3, NotificationType.ACCOUNT, "Welcome to MSE Invest",
                "Browse prices freely. Register when you are ready to trade.", now - 5 * hour
            ),
            NotificationItem(
                4, NotificationType.ANNOUNCEMENT, "Dividend announcement",
                "A listed company announced a dividend. (Sample)", now - 30 * hour, read = true
            ),
            NotificationItem(
                5, NotificationType.ORDER, "Order filled",
                "Sold 500 NBS at MK 41.00.", now - 72 * hour, read = true
            )
        )
    )
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    // Worked out from the list, so it can never disagree with it
    val unreadCount: StateFlow<Int> = _items
        .map { list -> list.count { !it.read } }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = _items.value.count { !it.read }
        )

    fun add(type: NotificationType, title: String, message: String) {
        val item = NotificationItem(nextId++, type, title, message, System.currentTimeMillis())
        _items.update { listOf(item) + it }
    }

    fun markRead(id: Int) {
        _items.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
    }

    fun markAllRead() {
        _items.update { list -> list.map { it.copy(read = true) } }
    }
}