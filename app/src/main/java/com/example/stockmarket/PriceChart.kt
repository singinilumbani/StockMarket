package com.example.stockmarket

import android.R.attr.y

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp



@Composable
fun PriceChart(prices: List<Double>, lineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        if (prices.size < 2) return@Canvas

        val min = prices.min()
        val max = prices.max()
        val range = if (max - min == 0.0) 1.0 else max - min
        val stepX = size.width / (prices.size - 1)

        val path = Path()
        prices.forEachIndexed { index, price ->
            val x = index * stepX
            val y = (
                    size.height -
                            ((price - min) / range) * size.height
                    ).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PriceChartPreview() {
    PriceChart(
        prices = listOf(10.0, 20.0, 15.0, 30.0),
        lineColor = Color(0xFFD4A017)
    )
}