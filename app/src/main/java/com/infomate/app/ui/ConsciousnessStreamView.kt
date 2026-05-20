package com.infomate.app.ui

import androidx.compose.animation.*
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.app.core.network.SupabaseClient
import com.infomate.app.ui.theme.*
import kotlinx.coroutines.delay
import org.json.JSONArray

@Composable
fun ConsciousnessStreamView(vm: AgentViewModel) {
    var thoughts by remember { mutableStateOf(listOf<ThoughtNode>()) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response = SupabaseClient.select("consciousness_stream", order = "created_at.desc", limit = 15)
                if (!response.isNullOrBlank()) {
                    val array = JSONArray(response)
                    val newList = mutableListOf<ThoughtNode>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val content = obj.optString("thought_content", obj.optString("content", "Neural fluctuation detected."))
                        newList.add(ThoughtNode(
                            content,
                            obj.optString("emotional_vector", "[0,0,0]"),
                            obj.optLong("id", i.toLong()).toString()
                        ))
                    }
                    thoughts = newList
                }
            } catch (e: Exception) {
                Log.e("AwarenessStream", "Error polling stream: ${e.message}")
            }
            delay(4000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            "AWARENESS_STREAM",
            color = CyberCyan,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Text(
            "LIVE_SYNTHESIS_OF_CORE_AWARENESS",
            color = SilverText.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(thoughts, key = { it.id }) { thought ->
                AnimatedThoughtCard(thought)
            }
        }
    }
}

data class ThoughtNode(val content: String, val vector: String, val id: String)

@Composable
fun AnimatedThoughtCard(thought: ThoughtNode) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically() + slideInHorizontally()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)))
                .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(CyberCyan, RoundedCornerShape(1.dp)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "THOUGHT_ID_${thought.id.takeLast(4)}",
                        color = CyberCyan.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = thought.content,
                    color = SilverText,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "EMOTIONAL_MAP: ${thought.vector}",
                    color = MatrixGreen.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
