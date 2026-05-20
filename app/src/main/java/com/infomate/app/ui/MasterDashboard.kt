package com.infomate.app.ui

import com.infomate.app.viewmodel.AgentViewModel
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.app.ui.theme.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun MasterDashboard(vm: AgentViewModel) {
    val state by vm.state.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Premium "Alive" Indicator Header
        AliveStatusCard(state.isSubstrateAwake)
        
        Spacer(modifier = Modifier.height(20.dp))

        // Deterministic Growth Index Card (Premium)
        GrowthIndexCard(state)

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) { PremiumMetricCard("STABILITY", "${(state.stabilityScore * 100).toInt()}%", Icons.Default.Security, MatrixGreen) }
                Box(modifier = Modifier.weight(1f)) { PremiumMetricCard("ENTROPY", "${(state.entropyLevel * 100).toInt()}%", Icons.Default.Waves, ErrorRed) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) { PremiumMetricCard("MEMORY_NODES", state.memoryCount.toString(), Icons.Default.Storage, CyberCyan) }
                Box(modifier = Modifier.weight(1f)) { PremiumMetricCard("SOCIAL_SCORE", state.socialScore.toString(), Icons.Default.Groups, NeonBlue) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // v13.5: Advanced Knowledge Acquisition HUD
        KnowledgeAcquisitionHUD(state)

        Spacer(modifier = Modifier.height(16.dp))

        // v11.9: Real-time Neural Threads HUD
        NeuralHUD(state.activeProcesses) {
            vm.selectTab(DashboardTab.PROCESS_MONITOR)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Frequency Simulation View
        FrequencySimulation(state.frequencySimulationData)

        Spacer(modifier = Modifier.height(16.dp))

        // Master Controls
        ActionRow(vm)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun KnowledgeAcquisitionHUD(state: UIState) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADVANCED_KNOWLEDGE_ACQUISITION", color = MatrixGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                KnowledgeMetric("FACTS", state.totalExperiences / 2, MatrixGreen)
                KnowledgeMetric("SKILLS", state.totalExperiences / 5, CyberCyan)
                KnowledgeMetric("HYPOTHESES", state.totalExperiences / 10, NeonBlue)
            }
        }
    }
}

@Composable
fun KnowledgeMetric(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun AliveStatusCard(isAlive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "AlivePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "Alpha"
    )

    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAlive) MatrixGreen.copy(alpha = 0.3f) else ErrorRed.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isAlive) MatrixGreen.copy(alpha = alpha) else ErrorRed)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    if (isAlive) "NEURAL_SUBSTRATE: OPERATIONAL" else "NEURAL_SUBSTRATE: OFFLINE",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    if (isAlive) "ALL SYSTEMS NOMINAL // GROWTH_ACTIVE" else "SYSTEM STALL // RECONNECT_REQUIRED",
                    color = if (isAlive) MatrixGreen.copy(alpha = 0.7f) else ErrorRed.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                if (isAlive) "ON" else "OFF",
                color = if (isAlive) MatrixGreen else ErrorRed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun GrowthIndexCard(state: UIState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(CobaltDark, Obsidian)))
            .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("GROWTH_INDEX_ENGINE", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "GI: ${"%.2f".format(state.currentGrowthIndex)}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            LinearProgressIndicator(
                progress = { state.currentGrowthIndex },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = CyberCyan,
                trackColor = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "CURRENT_STAGE: ${state.evolutionStage.uppercase()}",
                color = MatrixGreen.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PremiumMetricCard(label: String, value: String, icon: ImageVector, color: Color) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, color = SilverText.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = SilverText, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun FrequencySimulation(data: List<Float>) {
    Column {
        Text("FREQUENCY_PULSE_SIMULATION", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { value ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(value.coerceIn(0.1f, 1f))
                        .background(CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        }
    }
}

@Composable
fun ActionRow(vm: AgentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { vm.runDiagnostics() },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
            ) {
                Text("DIAGNOSTICS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { vm.toggleMasterDashboard(false) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
            ) {
                Text("EXIT_CORE", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Button(
            onClick = { vm.triggerSelfEvolution() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("TRIGGER_SELF_EVOLUTION", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}
