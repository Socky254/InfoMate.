package com.infomate.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.infomate.core.ui.theme.CyberCyan
import com.infomate.core.ui.theme.NeonBlue
import com.infomate.core.ui.theme.ElectricViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ConsciousnessFace(
    state: InfomateState,
    isActive: Boolean,
    amplitudes: List<Float> = emptyList(),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "face")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.size(200.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 3
        
        // 1. Background Aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyberCyan.copy(alpha = 0.1f * pulse), Color.Transparent),
                center = center,
                radius = baseRadius * 2
            ),
            radius = baseRadius * 1.5f,
            center = center
        )

        // 2. Rotating Outer Geometric Rings
        rotate(rotation) {
            repeat(3) { i ->
                val r = baseRadius * (1.2f + i * 0.1f)
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.05f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 0.5.dp.toPx())
                )
                
                // Dynamic geometric points on rings
                val points = 4 + i
                repeat(points) { p ->
                    val angle = (p * (2 * PI) / points).toFloat()
                    val px = center.x + r * cos(angle)
                    val py = center.y + r * sin(angle)
                    drawCircle(
                        color = if (isActive) CyberCyan else CyberCyan.copy(alpha = 0.3f),
                        radius = 2.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        // 3. Central Core: Waveform or Spectrum
        when (state) {
            InfomateState.THINKING -> {
                // Circular Oscillating Waves
                repeat(4) { i ->
                    val path = Path()
                    val segments = 60
                    val ringRadius = baseRadius * (0.6f + i * 0.1f)
                    
                    for (s in 0..segments) {
                        val angle = (s * (2 * PI) / segments).toFloat()
                        val offset = sin(angle * 5 + rotation / 10 + i) * 5f * pulse
                        val x = center.x + (ringRadius + offset) * cos(angle)
                        val y = center.y + (ringRadius + offset) * sin(angle)
                        
                        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(
                        path = path,
                        color = CyberCyan.copy(alpha = 0.6f - i * 0.1f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            InfomateState.RESPONDING, InfomateState.STREAMING -> {
                // Voice Spectrum in a circular formation
                val points = if (amplitudes.isNotEmpty()) amplitudes else List(20) { 0.1f }
                val slice = (2 * PI / points.size).toFloat()
                
                points.forEachIndexed { i, amp ->
                    val angle = i * slice
                    val barHeight = 20.dp.toPx() * amp * pulse
                    val start = Offset(
                        center.x + baseRadius * 0.7f * cos(angle),
                        center.y + baseRadius * 0.7f * sin(angle)
                    )
                    val end = Offset(
                        center.x + (baseRadius * 0.7f + barHeight) * cos(angle),
                        center.y + (baseRadius * 0.7f + barHeight) * sin(angle)
                    )
                    
                    drawLine(
                        brush = Brush.linearGradient(listOf(CyberCyan, ElectricViolet)),
                        start = start,
                        end = end,
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            InfomateState.ERROR -> {
                // Fragmented Glitch Circle
                drawCircle(
                    color = Color.Red.copy(alpha = 0.2f),
                    radius = baseRadius * pulse,
                    center = center,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                )
            }
            else -> {
                // Steady idle pulse
                drawCircle(
                    brush = Brush.sweepGradient(listOf(CyberCyan.copy(alpha = 0.2f), NeonBlue.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.2f))),
                    radius = baseRadius * 0.8f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
