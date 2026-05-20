package com.infomate.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun NeuralWaveformChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)

        val path = Path().apply {
            val startY = height - (data[0] * height)
            moveTo(0f, startY)
            data.forEachIndexed { index, value ->
                if (index > 0) {
                    val x = index * stepX
                    val y = height - (value * height)
                    lineTo(x, y)
                }
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
