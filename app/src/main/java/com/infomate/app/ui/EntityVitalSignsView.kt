package com.infomate.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.*

@Composable
fun EntityVitalSignsView(vm: AgentViewModel) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(20.dp)
    ) {
        // Alive Indicator in Vitals View
        AliveStatusHeader(state.isSubstrateAwake)
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "NEURAL_TELEMETRY",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Advanced Waveform Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(1.dp, MatrixGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            CyberGraph(state.telemetryHistory)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Frequency Pulse Simulation
        Text("FREQUENCY_SPECTRUM_ANALYSIS", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(8.dp))
        FrequencySimulation(state.frequencySimulationData)

        Spacer(modifier = Modifier.height(24.dp))

        // Deterministic Components Readout
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalRow("GI_STABILITY", "${(state.stabilityScore * 100).toInt()}%", MatrixGreen)
            VitalRow("CHAOS_ENTROPY", "${(state.entropyLevel * 100).toInt()}%", ErrorRed)
            VitalRow("LATENCY_BRAIN", "12ms", CyberCyan)
            VitalRow("MEMORY_DEPTH", state.memoryCount.toString(), ElectricViolet)
        }
    }
}
