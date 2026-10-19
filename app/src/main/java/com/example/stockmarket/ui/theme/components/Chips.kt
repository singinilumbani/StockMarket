package com.example.stockmarket.ui.theme.components


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.example.stockmarket.ui.theme.gainColor
import com.example.stockmarket.ui.theme.lossColor
import java.util.Locale

@Composable
fun ChangeChip(pct: Double, modifier: Modifier = Modifier) {
    val color = if (pct >= 0) gainColor() else lossColor()

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Text(
            text = String.format(Locale.US, "%+.2f%%", pct),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}