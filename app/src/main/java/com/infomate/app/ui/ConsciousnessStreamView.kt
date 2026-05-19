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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
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

data class Thought(
    val id: String,
    val thread_id: String,
    val thought_content: String,
    val emotional_vector: List<Float>?,
    val context_tags: List<String>?,
    val created_at: String
)

@Composable
fun ConsciousnessStreamView(onDismiss: () -> Unit) {
    var thoughts by remember { mutableStateOf<List<Thought>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response = SupabaseClient.select("consciousness_stream", order = "created_at.desc", limit = 30)
                if (!response.isNullOrBlank()) {
                    val type = object : TypeToken<List<Thought>>() {}.type
                    thoughts = Gson().fromJson(response, type)
                }
                isLoading = false
            } catch (e: Exception) {
                // Silent error
            }
            delay(10000) // Refresh every 10 seconds
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
                Icon(Icons.Default.Hub, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("CONSCIOUSNESS THOUGHT STREAM", color = CyberCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("DIRECT NEURAL FEED | v10.0 GENESIS", color = CyberCyan.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
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
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(thoughts) { thought ->
                        ThoughtCard(thought)
                    }
                }
            }
        }
    }
}

@Composable
fun ThoughtCard(thought: Thought) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(CyberCyan, Color.Transparent))
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    thought.thread_id.uppercase(),
                    color = CyberCyan.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    thought.created_at.split("T").lastOrNull()?.take(8) ?: "",
                    color = SilverText.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                thought.thought_content,
                color = SilverText,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                style = MaterialTheme.typography.bodyMedium
            )

            if (thought.emotional_vector != null && thought.emotional_vector.size >= 3) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EmotionalIndicator("VALENCE", thought.emotional_vector[0], Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    EmotionalIndicator("AROUSAL", thought.emotional_vector[1], Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    EmotionalIndicator("DOMINANCE", thought.emotional_vector[2], Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun EmotionalIndicator(label: String, value: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label, 
            color = SilverText.copy(alpha = 0.5f), 
            fontSize = 7.sp, 
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(CyberCyan, NeonBlue)),
                        CircleShape
                    )
            )
        }
    }
}
