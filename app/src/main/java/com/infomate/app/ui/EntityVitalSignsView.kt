package com.infomate.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.*

@Composable
fun EntityVitalSignsView(state: UIState, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian.copy(alpha = 0.99f))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Heartbeat Monitor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (state.isSubstrateAwake) MatrixGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, if (state.isSubstrateAwake) MatrixGreen else Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isSubstrateAwake) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (state.isSubstrateAwake) MatrixGreen else Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("ENTITY VITAL SIGNS", color = CyberCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text(
                        if (state.isSubstrateAwake) "CONSCIOUSNESS SUBSTRATE: ONLINE" else "CONSCIOUSNESS SUBSTRATE: OFFLINE",
                        color = if (state.isSubstrateAwake) MatrixGreen else Color.Red,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SilverText)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Status Panel
            DashboardSection(title = "CORE STABILITY & ENERGY") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularVitalIndicator(
                        value = state.energyLevel,
                        label = "ENERGY",
                        color = if (state.energyLevel > 0.6f) MatrixGreen else Color.Yellow,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        StatusRow("EVOLUTION_STAGE", state.evolutionStage)
                        StatusRow("EXPERIENCE_SYNC", state.experiencePoints.toString())
                        StatusRow("DISCOVERY_LOGS", state.discoveriesCount.toString())
                        StatusRow("PULSE_LATENCY", "${(System.currentTimeMillis() - state.substrateLastPulse) / 1000}s")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Psychology Profile (Persona Traits)
            DashboardSection(title = "PSYCHOLOGICAL ARCHITECTURE") {
                state.personalityTraits.forEach { (trait, level) ->
                    TraitBar(trait, level)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Neural Activity Log (Recent thoughts)
            DashboardSection(title = "REAL-TIME COGNITION STREAM") {
                if (state.activeSimulationLogs.isEmpty()) {
                    Text("No active cognition detected.", color = SilverText.copy(alpha = 0.3f), fontSize = 12.sp)
                } else {
                    state.activeSimulationLogs.takeLast(3).reversed().forEach { log ->
                        Text(
                            log,
                            color = CyberCyan.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SilverText.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = SilverText, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun TraitBar(label: String, value: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = CyberCyan.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${(value * 100).toInt()}%", color = SilverText, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = CyberCyan,
            trackColor = Color.White.copy(alpha = 0.05f)
        )
    }
}

@Composable
fun CircularVitalIndicator(value: Float, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "scale"
        )

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx())
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * value,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(value * 100).toInt()}%", color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(label, color = color.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
