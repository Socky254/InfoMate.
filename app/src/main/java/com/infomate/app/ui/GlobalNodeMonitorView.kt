package com.infomate.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.WifiTethering
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
import com.infomate.app.ui.theme.*
import com.infomate.app.agent.GlobalSearchAgent

@Composable
fun GlobalNodeMonitorView(onDismiss: () -> Unit) {
    var nodes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        nodes = GlobalSearchAgent.fetchNodePerformance()
        isLoading = false
    }

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
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WifiTethering, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("GLOBAL NODE MONITOR", color = CyberCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("NEURAL NETWORK TOPOLOGY | v10.8", color = CyberCyan.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SilverText)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyberCyan)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(nodes) { node ->
                        NodeCard(node)
                    }
                }
            }
        }
    }
}

@Composable
fun NodeCard(node: Map<String, Any>) {
    val name = node["node_name"] as? String ?: "Unknown Node"
    val rating = (node["reliability_rating"] as? Double ?: 1.0).toFloat()
    val lastPing = node["last_ping"] as? String ?: "N/A"

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    name.uppercase(), 
                    color = SilverText, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    if (rating > 0.8) "SYNC_ACTIVE" else "SYNC_DEGRADED",
                    color = if (rating > 0.8) MatrixGreen else ErrorRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "RELIABILITY_INDEX", 
                color = SilverText.copy(alpha = 0.4f), 
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { rating },
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                color = if (rating > 0.8) CyberCyan else ErrorRed,
                trackColor = Color.White.copy(alpha = 0.05f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "LAST_BRIDGE_SYNC: $lastPing", 
                color = SilverText.copy(alpha = 0.3f), 
                fontSize = 9.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
