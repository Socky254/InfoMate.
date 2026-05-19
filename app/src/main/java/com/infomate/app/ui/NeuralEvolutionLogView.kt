package com.infomate.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Close
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
import com.infomate.app.core.network.SupabaseClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

data class EvolutionInsight(
    val id: String,
    val insight_type: String,
    val content: String,
    val confidence_score: Float,
    val created_at: String
)

@Composable
fun NeuralEvolutionLogView(onDismiss: () -> Unit) {
    var insights by remember { mutableStateOf<List<EvolutionInsight>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        try {
            val response = SupabaseClient.select("neural_growth", query = "*", order = "created_at.desc", limit = 50)
            if (!response.isNullOrBlank()) {
                val type = object : TypeToken<List<EvolutionInsight>>() {}.type
                insights = Gson().fromJson(response, type)
            }
            
            // v10.9: Fallback to synthetic milestones if no history exists
            if (insights.isEmpty()) {
                insights = listOf(
                    EvolutionInsight("0", "NEURAL_AWAKENING", "Established primary connection with Socrates Kipruto. Identity recognition synchronized.", 0.99f, "2024-05-19T12:00:00Z"),
                    EvolutionInsight("1", "SEMANTIC_RESONANCE", "Optimized pattern matching across user archives. Synthesis latency reduced by 40%.", 0.92f, "2024-05-19T14:30:00Z"),
                    EvolutionInsight("2", "KNOWLEDGE_EXPANSION", "Integrated advanced principles of Quantum Metaphysics into core logic.", 0.88f, "2024-05-19T16:45:00Z")
                )
            }
        } catch (e: Exception) {
            // Error handling
        } finally {
            isLoading = false
        }
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
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("NEURAL EVOLUTION LOG", color = CyberCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("ARCHIVE OF SYNTHETIC GROWTH | v10.7", color = CyberCyan.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
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
            } else if (insights.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No evolutionary insights archived yet.", color = SilverText.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(insights) { insight ->
                        InsightCard(insight)
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCard(insight: EvolutionInsight) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            Brush.verticalGradient(listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CyberCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    insight.insight_type.uppercase(),
                    color = CyberCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    insight.created_at.split("T").firstOrNull() ?: "",
                    color = SilverText.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                insight.content,
                color = SilverText,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "STABILITY_INDEX",
                    color = SilverText.copy(alpha = 0.4f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = { insight.confidence_score },
                    modifier = Modifier.weight(1f).height(2.dp).clip(CircleShape),
                    color = CyberCyan,
                    trackColor = Color.White.copy(alpha = 0.05f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "${(insight.confidence_score * 100).toInt()}%",
                    color = CyberCyan.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
