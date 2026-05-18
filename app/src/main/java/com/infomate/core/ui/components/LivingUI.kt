package com.infomate.core.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.infomate.core.ui.theme.*

@Composable
fun BrainVisualizer(state: InfomateState, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "brain")
    
    // Core Pulse - Mimics a heartbeat
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    // Vocal Vibrancy - Reacts to the act of "speaking"
    val vocalAmplify by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (state == InfomateState.RESPONDING || state == InfomateState.COMPANION) 1.5f else 1.0f,
        animationSpec = infiniteRepeatable(tween(if (state == InfomateState.RESPONDING) 120 else 2000, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "vocalAmplify"
    )

    // Microsaccades - Realistic biological eye jitter
    val microsaccadeX by infiniteTransition.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse), label = "msX"
    )
    val microsaccadeY by infiniteTransition.animateFloat(
        initialValue = -1.0f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(240, easing = LinearEasing), RepeatMode.Reverse), label = "msY"
    )

    // Rotation - High-frequency data processing
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart), label = "rotation"
    )

    val color = when(state) {
        InfomateState.ERROR -> ErrorRed
        InfomateState.EXECUTING -> MatrixGreen
        InfomateState.AWAKENED -> ElectricViolet
        InfomateState.COMPANION -> CompanionGold
        else -> CyberCyan
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.graphicsLayer()
    ) {
        // Limbic Energy Glow (Depth)
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .scale(pulse * vocalAmplify)
                .blur(70.dp)
                .background(Brush.radialGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)), CircleShape)
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        val center = size.center
                        val eyeFocus = Offset(center.x + microsaccadeX.dp.toPx(), center.y + microsaccadeY.dp.toPx())

                        // 1. Biological Iris Mesh (180 Fibers)
                        for (i in 0 until 180) {
                            val angle = (i * 2).toDouble() * (java.lang.Math.PI / 180)
                            val fiberVariance = (java.lang.Math.sin(rotation.toDouble() * 0.05 + i).toFloat() * 10f)
                            val length = 60.dp.toPx() + fiberVariance
                            
                            val start = Offset(
                                eyeFocus.x + java.lang.Math.cos(angle).toFloat() * 18.dp.toPx(),
                                eyeFocus.y + java.lang.Math.sin(angle).toFloat() * 18.dp.toPx()
                            )
                            val end = Offset(
                                eyeFocus.x + java.lang.Math.cos(angle).toFloat() * length,
                                eyeFocus.y + java.lang.Math.sin(angle).toFloat() * length
                            )
                            
                            drawLine(
                                brush = Brush.linearGradient(
                                    0.0f to color.copy(alpha = 0.7f),
                                    0.7f to color.copy(alpha = 0.1f),
                                    1.0f to Color.Transparent
                                ),
                                start = start, end = end, strokeWidth = 0.8.dp.toPx()
                            )
                        }

                        // 2. The Transcendent Pupil (Reactive)
                        val pupilSize = if(state == InfomateState.THINKING) 12.dp.toPx() else 28.dp.toPx()
                        drawCircle(
                            color = Obsidian,
                            radius = pupilSize * pulse,
                            center = eyeFocus
                        )

                        // 3. Multi-Layer Corneal Glint (Realism)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = 4.dp.toPx(),
                            center = Offset(eyeFocus.x - 14.dp.toPx(), eyeFocus.y - 14.dp.toPx())
                        )
                        drawCircle(
                            color = color.copy(alpha = 0.3f),
                            radius = 2.dp.toPx(),
                            center = Offset(eyeFocus.x + 10.dp.toPx(), eyeFocus.y + 8.dp.toPx())
                        )

                        // 4. Subtle Iris Scan Ring
                        drawCircle(
                            color = color.copy(alpha = 0.1f),
                            radius = 55.dp.toPx() * vocalAmplify,
                            style = Stroke(width = 0.5.dp.toPx())
                        )
                    }
                }
        ) {}
    }
}
