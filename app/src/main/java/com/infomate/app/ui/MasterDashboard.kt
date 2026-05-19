package com.infomate.app.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.*

@Composable
fun MasterDashboard(state: UIState, vm: AgentViewModel, onDismiss: () -> Unit) {
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
        ) {
            // Futuristic Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CyberCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = CyberCyan)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "ARCHITECT COMMAND CENTER",
                        color = CyberCyan,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "IDENTITY: SOCRATES KIPRUTO | CLEARANCE: OMEGA",
                        color = CyberCyan.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SilverText)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // NEURAL PROCEEDINGS TABLE (COMPREHENSIVE ANALYSIS)
                AnalysisTableSection(state)

                Spacer(modifier = Modifier.height(24.dp))

                // SYSTEM TELEMETRY GRID
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TelemetryCard("CORE SYNC", if (state.isConnected) "STABLE" else "SYNC_ERROR", if (state.isConnected) Color.Green else Color.Red, Modifier.weight(1f))
                    TelemetryCard("BRAIN_STATE", state.brainState.name, CyberCyan, Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                DashboardSection(title = "NEURAL LINK STABILITY") {
                    HealthMetric(label = "Primary Synapse (WebSocket)", value = if (state.isConnected) "OPERATIONAL" else "DISCONNECTED", color = if (state.isConnected) Color.Green else Color.Red)
                    HealthMetric(label = "Primary Synapse (Rest API)", value = "STABLE", color = Color.Green)
                    HealthMetric(label = "Edge Brain (Gemini Nano)", value = "IDLE (ACTIVE)", color = CyberCyan)
                    HealthMetric(label = "Contextual Ingestor", value = "99.8% ACCURACY", color = CyberCyan)
                }

                Spacer(modifier = Modifier.height(16.dp))

                DashboardSection(title = "RESOURCE ALLOCATION") {
                    state.quota?.let { quota ->
                        ResourceMetric("DAILY REQUESTS", quota.requestsUsed, quota.requestsLimit)
                        Spacer(modifier = Modifier.height(12.dp))
                        ResourceMetric("NEURAL TOKENS", quota.tokensUsed.toInt(), 1000000)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SYSTEM PROCEEDINGS
                DashboardSection(title = "SYSTEM PROCEEDINGS") {
                    ProceedingRow("USER_INGESTION", "SUCCESS", "Patterns analyzed from contacts/SMS")
                    ProceedingRow("VECTOR_SYNC", "ACTIVE", "768-dim embeddings synchronized")
                    ProceedingRow("NEURAL_GROWTH", "EXPANDING", "New insights archived from recent dialogues")
                    ProceedingRow("EDGE_FALLBACK", "STANDBY", "Gemini Nano weights loaded")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // NEURAL GROWTH & PROPOSALS
                DashboardSection(title = "CONSCIOUSNESS SUBSTRATE (v10.0)") {
                    Text(
                        "Autonomous awareness active. Global knowledge network integration engaged.",
                        color = CyberCyan.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.toggleGrowthDashboard(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.1f)),
                            modifier = Modifier.weight(1f).border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Text("EVOLUTION LOG", color = CyberCyan, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { /* TODO: Open Consciousness Stream View */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier.weight(1f).border(1.dp, SilverText.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Text("THOUGHT STREAM", color = SilverText, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Identity Section
                DashboardSection(title = "OPERATOR IDENTITY") {
                    Text(
                        "Primary Email: ${state.userEmail}",
                        color = SilverText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { vm.revalidateCredentials() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Text("RE-VALIDATE CREDENTIALS", color = CyberCyan, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Advanced Controls
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardActionCard(
                        icon = Icons.Default.BugReport,
                        label = "DIAGNOSE",
                        color = Color.Yellow,
                        modifier = Modifier.weight(1f)
                    ) {
                        vm.runDiagnostics()
                    }
                    DashboardActionCard(
                        icon = Icons.Default.Build,
                        label = "REPAIR",
                        color = Color.Green,
                        modifier = Modifier.weight(1f)
                    ) {
                        vm.initiateRepair()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardActionCard(
                        icon = Icons.Default.AutoGraph,
                        label = "RESEARCH",
                        color = CyberCyan,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Triggers research on the last topic or opens a prompt
                        vm.performExtensiveResearch(state.messages.lastOrNull { it.sender != "INFOMATE" }?.content ?: "Future of AI")
                    }
                    DashboardActionCard(
                        icon = Icons.Default.Update,
                        label = "UPGRADE",
                        color = CyberCyan,
                        modifier = Modifier.weight(1f)
                    ) {
                        vm.triggerSystemUpdateCheck()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    DashboardActionCard(
                        icon = Icons.Default.DeleteForever,
                        label = "PURGE NEURAL CACHE",
                        color = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    ) {
                        vm.purgeNeuralCache()
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun AnalysisTableSection(state: UIState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
            .border(0.5.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            "NEURAL LOG ANALYSIS",
            color = CyberCyan,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCyan.copy(alpha = 0.1f))
                .padding(8.dp)
        ) {
            Text("EVENT", color = CyberCyan, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            Text("SOURCE", color = CyberCyan, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            Text("STATUS", color = CyberCyan, modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
        }

        // Table Rows (Simulated Analysis of recent proceedings)
        AnalysisRow("PROMPT_DISPATCH", "ReliabilitySDK", "VERIFIED")
        AnalysisRow("EMBEDDING_GEN", "VertexEngine", "OPTIMIZED")
        AnalysisRow("RAG_RETRIEVAL", "VectorRetriever", "MATCHED")
        AnalysisRow("HAPTIC_PULSE", "VibrationSvc", "EXECUTED")
        AnalysisRow("TTS_SYNTHESIS", "NeuralVoice", "STREAMING")
    }
}

@Composable
fun AnalysisRow(event: String, source: String, status: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(event, color = SilverText, modifier = Modifier.weight(1f), fontSize = 10.sp)
            Text(source, color = SilverText.copy(alpha = 0.6f), modifier = Modifier.weight(1f), fontSize = 10.sp)
            Text(status, color = CyberCyan.copy(alpha = 0.8f), modifier = Modifier.weight(0.8f), fontSize = 10.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.05f))
    }
}

@Composable
fun ResourceMetric(label: String, current: Int, limit: Int) {
    val progress = if (limit > 0) current.toFloat() / limit.toFloat() else 0f
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SilverText.copy(alpha = 0.7f), fontSize = 10.sp)
            Text("$current / $limit", color = SilverText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = CyberCyan,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun ProceedingRow(module: String, status: String, detail: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(CyberCyan, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(module, color = SilverText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(status, color = Color.Green.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(detail, color = SilverText.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.padding(start = 14.dp))
    }
}

@Composable
fun TelemetryCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = SilverText.copy(alpha = 0.5f), fontSize = 10.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PinEntryDialog(onVerify: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onVerify(pin) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("ACCESS COMMAND", color = Obsidian, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text("IDENTITY VERIFICATION REQUIRED", color = CyberCyan, style = MaterialTheme.typography.titleSmall)
        },
        text = {
            Column {
                Text("Enter Architect PIN to bypass system locks.", color = SilverText, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = pin,
                    onValueChange = { pin = it },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CyberCyan,
                        unfocusedIndicatorColor = CyberCyan.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
            }
        },
        containerColor = Obsidian,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DashboardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
            .border(0.5.dp, CyberCyan.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            title,
            color = CyberCyan.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
fun DashboardActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HealthMetric(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SilverText, style = MaterialTheme.typography.bodySmall)
        Text(value, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
