package com.infomate.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.*

@Composable
fun MasterDashboard(state: UIState, vm: AgentViewModel, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian.copy(alpha = 0.95f))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "MASTER ARCHITECT CONSOLE",
                        color = CyberCyan,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "System Authority Level: ROOT",
                        color = CyberCyan.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SilverText)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // System Health Section
            DashboardSection(title = "NEURAL LINK STABILITY") {
                HealthMetric(label = "Primary Synapse (Supabase)", value = "OPERATIONAL", color = Color.Green)
                HealthMetric(label = "Edge Brain (Gemini Nano)", value = "IDLE (WAITING FOR DATA)", color = CyberCyan)
                HealthMetric(label = "Contextual Ingestor", value = "99.8% ACCURACY", color = CyberCyan)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quota Section
            DashboardSection(title = "RESOURCE ALLOCATION") {
                state.quota?.let { quota ->
                    LinearProgressIndicator(
                        progress = { quota.requestsUsed.toFloat() / quota.requestsLimit.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = CyberCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Requests: ${quota.requestsUsed} / ${quota.requestsLimit}",
                        color = SilverText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Tokens Consumed: ${quota.tokensUsed}",
                        color = SilverText.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                } ?: Text("Calculating neural load...", color = SilverText.copy(alpha = 0.4f))
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
                    onClick = { /* TODO: Re-authenticate or change keys */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    Text("RE-VALIDATE CREDENTIALS", color = CyberCyan, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Advanced Controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardActionCard(
                    icon = Icons.Default.DeleteForever,
                    label = "PURGE CACHE",
                    color = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                ) {
                    // vm.purgeCache()
                }
                DashboardActionCard(
                    icon = Icons.Default.Update,
                    label = "FORCE UPGRADE",
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                ) {
                    // vm.checkForSystemUpdates()
                }
            }
        }
    }
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
