package com.infomate.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.infomate.core.ui.theme.CyberCyan

@Composable
fun NeuralWaveformChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = CyberCyan
) {
    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        if (data.size < 2) return@Canvas
        
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)
        
        val path = Path().apply {
            moveTo(0f, height * (1 - data[0]))
            data.forEachIndexed { index, value ->
                if (index != 0) {
                    lineTo(index * stepX, height * (1 - value))
                }
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx()),
            alpha = 0.8f
        )
        
        // Fill Area
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.2f), Color.Transparent)
            )
        )
    }
}
