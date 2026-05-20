package com.infomate.app.ui

import com.infomate.app.viewmodel.AgentViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.app.ui.theme.Obsidian
import com.infomate.app.ui.theme.MatrixGreen
import com.infomate.app.ui.theme.SilverText

@Composable
fun SimulationView(vm: AgentViewModel) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Science, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "ECOSYSTEM_LEDGER",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        // Live Simulation Status Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(MatrixGreen.copy(alpha = 0.1f), Color.Transparent)))
                .border(1.dp, MatrixGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("CURRENT_SIMULATION_STATE", color = MatrixGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    state.ecosystemStatus.uppercase(),
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // v11.9: Ongoing Process Monitor
        NeuralHUD(state.activeProcesses)

        Spacer(modifier = Modifier.height(24.dp))

        // Ledger Feed
        Text("TRANSACTION_HISTORY // REAL-TIME", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(12.dp))
        
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.activeSimulationLogs.reversed()) { log ->
                    Row {
                        Text(">", color = MatrixGreen, modifier = Modifier.padding(end = 12.dp), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            log,
                            color = SilverText.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
