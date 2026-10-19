package com.example.stockmarket



import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stockmarket.ui.theme.components.DetailHeader
import com.example.stockmarket.ui.theme.components.TabHeader

@Composable
fun TabPlaceholder(
    title: String,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TabHeader(
            title = title,
            unreadCount = 3,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$title screen coming soon",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotificationsPlaceholder(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "Notifications", onBack = onBack)
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No notifications yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}