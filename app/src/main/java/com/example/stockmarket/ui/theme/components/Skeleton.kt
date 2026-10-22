package com.example.stockmarket.ui.theme.components



import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stockmarket.ui.theme.StockMarketTheme

// A moving gradient. Every grey box on the screen shares one, so they shimmer together.
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val position by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPosition"
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(position - 300f, 0f),
        end = Offset(position, 0f)
    )
}

@Composable
fun SkeletonBox(brush: Brush, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(brush = brush, shape = RoundedCornerShape(8.dp)))
}

// Looks like six company rows from the Market list
@Composable
fun MarketSkeleton(rows: Int = 6) {
    val brush = shimmerBrush()

    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics { contentDescription = "Loading companies" },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(rows) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(brush, Modifier.width(70.dp).height(16.dp))
                        Spacer(Modifier.height(8.dp))
                        SkeletonBox(brush, Modifier.width(160.dp).height(14.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SkeletonBox(brush, Modifier.width(80.dp).height(16.dp))
                        Spacer(Modifier.height(8.dp))
                        SkeletonBox(brush, Modifier.width(56.dp).height(22.dp))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MarketSkeletonPreview() {
    StockMarketTheme {
        Surface {
            MarketSkeleton()
        }
    }
}