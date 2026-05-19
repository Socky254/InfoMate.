package com.infomate.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.*

@Composable
fun NeuralGrowthDashboard(state: UIState, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian.copy(alpha = 0.98f))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("NEURAL EVOLUTION TRACKER", color = CyberCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("PHASE: SYNTHETIC COGNITION", color = CyberCyan.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SilverText)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Growth Visualization (Neural Density)
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                NeuralDensityCircle(state.neuralDensity)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(state.neuralDensity * 100).toInt()}%", color = CyberCyan, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("NEURAL DENSITY", color = CyberCyan.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Advanced Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard("SYNTHESIZED INSIGHTS", state.totalInsights.toString(), Modifier.weight(1f))
                MetricCard("PERSONALITY STAGE", "v${state.syntheticPersonalityLevel}.0", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Evolution Log
            Text("EVOLUTIONARY MILESTONES", color = CyberCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            EvolutionMilestone("Autonomy Initialization", "AI can now initiate contact based on data triggers.")
            EvolutionMilestone("Semantic Resonance", "Deep pattern matching across user archives optimized.")
            EvolutionMilestone("Synthetic Empathy", "Tone modulation based on conversational sentiment.")

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun NeuralDensityCircle(density: Float) {
    Canvas(modifier = Modifier.size(180.dp)) {
        drawArc(
            color = Color.White.copy(alpha = 0.05f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx())
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(CyberCyan, NeonBlue, CyberCyan)),
            startAngle = -90f,
            sweepAngle = 360f * density,
            useCenter = false,
            style = Stroke(width = 12.dp.toPx())
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
            .border(0.5.dp, CyberCyan.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = SilverText.copy(alpha = 0.5f), fontSize = 10.sp, textAlign = TextAlign.Center)
        Text(value, color = CyberCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EvolutionMilestone(title: String, detail: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Box(modifier = Modifier.size(8.dp).padding(top = 4.dp).background(CyberCyan, CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = SilverText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(detail, color = SilverText.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}
