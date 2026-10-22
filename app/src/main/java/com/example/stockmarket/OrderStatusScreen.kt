package com.example.stockmarket



import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.Order
import com.example.stockmarket.data.OrderSide
import com.example.stockmarket.data.OrderStatus
import com.example.stockmarket.data.TradingStore
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.gainColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

class OrderStatusViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // Route arguments arrive as text, so convert to a number
    private val orderId: Int = savedStateHandle.get<String>("orderId")?.toIntOrNull() ?: -1

    val order: StateFlow<Order?> = TradingStore.orders
        .map { list -> list.find { it.id == orderId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun OrderStatusScreen(
    onDone: () -> Unit,
    viewModel: OrderStatusViewModel = viewModel()
) {
    val order by viewModel.order.collectAsState()
    OrderStatusContent(order = order, onDone = onDone)
}

@Composable
fun OrderStatusContent(order: Order?, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (order == null) {
            Text("Order not found")
        } else {
            when (order.status) {
                OrderStatus.SUBMITTED -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Order submitted",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Waiting for the broker to fill your order...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                OrderStatus.FILLED -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = gainColor(),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Order filled",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your portfolio has been updated.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OrderStatus.REJECTED -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Order rejected",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "The broker rejected this order. Your money or shares have been returned.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val side = if (order.side == OrderSide.BUY) "Buy" else "Sell"
                    StatusRow("Order", "$side ${order.quantity} ${order.symbol}")
                    StatusRow("Price", String.format(Locale.US, "MK %,.2f", order.price))
                    StatusRow("Fees", String.format(Locale.US, "MK %,.2f", order.fees))
                    StatusRow("Date", order.createdAt)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) { Text("Done") }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun OrderStatusPreview() {
    StockMarketTheme {
        Surface {
            OrderStatusContent(
                order = Order(1, "NBM", OrderSide.BUY, 10, 2010.0, 402.0, OrderStatus.FILLED, "19 Oct 2026"),
                onDone = {}
            )
        }
    }
}