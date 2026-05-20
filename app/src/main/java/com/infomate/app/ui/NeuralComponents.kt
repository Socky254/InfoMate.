package com.infomate.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.app.agent.ThoughtStep
import com.infomate.app.ui.theme.*

@Composable
fun AliveStatusHeader(isAwake: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "alive")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isAwake) MatrixGreen.copy(alpha = alpha) else Color.Red)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isAwake) "SUBSTRATE_ALIVE" else "SUBSTRATE_STASIS",
            color = if (isAwake) MatrixGreen else Color.Red,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun LiveThinkingView(steps: List<ThoughtStep>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .padding(top = 6.dp)
                        .clip(CircleShape)
                        .background(if (index == steps.size - 1) CyberCyan else SilverText.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = step.title.uppercase(),
                        color = if (index == steps.size - 1) CyberCyan else SilverText.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = step.description,
                        color = if (index == steps.size - 1) SilverText else SilverText.copy(alpha = 0.3f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConsciousnessFace(
    state: InfomateState,
    isActive: Boolean,
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "face")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 1. Atmospheric Ambient Glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) * pulseScale
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyberCyan.copy(alpha = 0.1f), Color.Transparent),
                    center = center,
                    radius = radius * 1.8f
                ),
                radius = radius * 1.8f,
                center = center
            )
        }

        // 2. Rotating Orbital Rings
        repeat(3) { i ->
            val ringRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = if (i % 2 == 0) 360f else -360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000 + (i * 2000), easing = LinearEasing)
                ),
                label = "ring$i"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f - (i * 0.1f))
                    .graphicsLayer { rotationZ = ringRotation }
                    .border(
                        width = 0.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, CyberCyan.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        // 3. Inner Core based on state
        val coreColor = when(state) {
            InfomateState.ERROR -> ErrorRed
            InfomateState.THINKING -> MatrixGreen
            InfomateState.RESPONDING -> CyberCyan
            else -> if (isActive) CyberCyan else SilverText.copy(alpha = 0.2f)
        }
        
        val coreScale by animateFloatAsState(
            targetValue = if (state == InfomateState.THINKING) 1.2f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "coreScale"
        )
        
        Box(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer { 
                    scaleX = coreScale * pulseScale
                    scaleY = coreScale * pulseScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(coreColor.copy(alpha = 0.8f), coreColor.copy(alpha = 0.1f))
                    )
                )
                .border(1.dp, coreColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // "Iris" Detail
            Box(
                modifier = Modifier
                    .size(if (state == InfomateState.THINKING) 30.dp else 20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(0.5.dp, coreColor, CircleShape)
            )
        }
    }
}
