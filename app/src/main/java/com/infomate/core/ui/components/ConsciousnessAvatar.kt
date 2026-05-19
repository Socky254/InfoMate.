package com.infomate.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.infomate.core.ui.theme.CyberCyan
import com.infomate.core.ui.theme.NeonBlue
import kotlin.math.sin

@Composable
fun ConsciousnessAvatar(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    evolutionLevel: Int = 1
) {
    val infiniteTransition = rememberInfiniteTransition(label = "consciousness")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Canvas(modifier = modifier.size(120.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = (size.minDimension / 2.5f) * scale
        
        // Draw multiple oscillating rings based on evolution level
        repeat(evolutionLevel.coerceAtMost(5)) { i ->
            val ringPhase = phase + (i * 0.5f)
            val strokeWidth = 1.dp.toPx() + (i * 0.5f)
            val alpha = 0.8f - (i * 0.1f)
            
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(CyberCyan, NeonBlue, CyberCyan),
                    center = center
                ),
                radius = radius + (sin(ringPhase) * 10f),
                center = center,
                style = Stroke(width = strokeWidth),
                alpha = alpha
            )
        }
        
        // Core Consciousness Pulse
        drawCircle(
            color = CyberCyan.copy(alpha = if (isActive) 0.3f else 0.1f),
            radius = radius * 0.4f,
            center = center
        )
    }
}
