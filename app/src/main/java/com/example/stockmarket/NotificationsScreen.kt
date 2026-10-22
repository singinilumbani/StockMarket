@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stockmarket

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.NotificationItem
import com.example.stockmarket.data.NotificationType
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.DetailHeader
import com.example.stockmarket.ui.theme.components.EmptyState

// 1) STATEFUL: gets the ViewModel
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    NotificationsContent(
        state = state,
        onBack = onBack,
        onNotificationClick = viewModel::onNotificationClick,
        onMarkAllRead = viewModel::onMarkAllRead
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onNotificationClick: (Int) -> Unit,
    onMarkAllRead: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // The "actions" slot from Lesson 9: we decide what goes on the right
        DetailHeader(
            title = "Notifications",
            onBack = onBack,
            actions = {
                TextButton(onClick = onMarkAllRead, enabled = state.unreadCount > 0) {
                    Text("Mark all read")
                }
            }
        )

        if (state.today.isEmpty() && state.earlier.isEmpty()) {
            EmptyState(message = "No notifications yet")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.today.isNotEmpty()) {
                    item(key = "header_today") { SectionTitle("Today") }
                    items(state.today, key = { "n_${it.id}" }) { n ->
                        NotificationCard(n = n, onClick = { onNotificationClick(n.id) })
                    }
                }
                if (state.earlier.isNotEmpty()) {
                    item(key = "header_earlier") { SectionTitle("Earlier") }
                    items(state.earlier, key = { "n_${it.id}" }) { n ->
                        NotificationCard(n = n, onClick = { onNotificationClick(n.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )
}

@Composable
private fun NotificationCard(n: NotificationItem, onClick: () -> Unit) {
    // Unread cards get a tinted background; read cards use the normal card colour
    val colors = if (n.read) {
        CardDefaults.cardColors()
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    }

    Card(onClick = onClick, colors = colors, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFor(n.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = n.title,
                    fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = n.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeAgo(n.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!n.read) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private fun iconFor(type: NotificationType): ImageVector = when (type) {
    NotificationType.ORDER -> Icons.Default.History
    NotificationType.ACCOUNT -> Icons.Default.Person
    NotificationType.ANNOUNCEMENT -> Icons.Default.Info
    NotificationType.PRICE_ALERT -> Icons.Default.Notifications
}

private fun timeAgo(timestamp: Long): String {
    val minutes = (System.currentTimeMillis() - timestamp) / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        minutes < 48 * 60 -> "Yesterday"
        else -> "${minutes / (24 * 60)} days ago"
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun NotificationsContentPreview() {
    val now = System.currentTimeMillis()
    val hour = 60 * 60 * 1000L

    StockMarketTheme {
        Surface {
            NotificationsContent(
                state = NotificationsUiState(
                    today = listOf(
                        NotificationItem(1, NotificationType.ORDER, "Order filled", "Buy 10 NBM at MK 2,010.00.", now - 5 * 60_000),
                        NotificationItem(2, NotificationType.ANNOUNCEMENT, "Market closed", "Closing prices are available.", now - 2 * hour, read = true)
                    ),
                    earlier = listOf(
                        NotificationItem(3, NotificationType.ACCOUNT, "Registration approved", "You can now place orders.", now - 30 * hour, read = true)
                    ),
                    unreadCount = 1
                ),
                onBack = {},
                onNotificationClick = {},
                onMarkAllRead = {}
            )
        }
    }
}
