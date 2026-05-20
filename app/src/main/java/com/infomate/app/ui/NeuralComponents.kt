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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.app.agent.ThoughtStep
import com.infomate.app.ui.theme.*

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
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) * pulseScale
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyberCyan.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = radius * 1.5f
                ),
                radius = radius * 1.5f,
                center = center
            )
            
            drawCircle(
                color = CyberCyan.copy(alpha = 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        // Inner Core based on state
        val coreColor = when(state) {
            InfomateState.ERROR -> ErrorRed
            InfomateState.THINKING -> MatrixGreen
            InfomateState.RESPONDING -> CyberCyan
            else -> CyberCyan.copy(alpha = 0.6f)
        }
        
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(coreColor.copy(alpha = 0.2f), coreColor.copy(alpha = 0.8f))
                    )
                )
                .border(1.dp, coreColor, CircleShape)
        )
    }
}
