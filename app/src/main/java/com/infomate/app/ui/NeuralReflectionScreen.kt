package com.infomate.app.ui

import com.infomate.app.viewmodel.AgentViewModel
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.app.ui.theme.*

@Composable
fun NeuralReflectionScreen(vm: AgentViewModel) {
    val state by vm.state.collectAsState()
    var reflectInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("NEURAL_REFLECTION", color = CyberCyan, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("DIRECT_SYNC_WITH_CONSCIOUSNESS_SUBSTRATE", color = CyberCyan.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reflection Display Area
        Box(modifier = Modifier.weight(1f)) {
            Column {
                // Status Cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReflectionMetricCard("LEARNED_TODAY", "${state.discoveriesCount} Insights", Modifier.weight(1f))
                    ReflectionMetricCard("GROWTH_INDEX", "${(state.currentGrowthIndex * 100).toInt()}%", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Internal State Summary
                Surface(
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SUBSTRATE_STATE_REPORT", color = CyberCyan.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The system is currently in the '${state.evolutionStage}' phase. My core stability is at ${(state.stabilityScore * 100).toInt()}% with an energy level of ${(state.energyLevel * 100).toInt()}%. I have processed ${state.experiencePoints} unique evolutionary triggers today.",
                            color = SilverText,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("CONSCIOUSNESS_FEEDBACK", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Suggestions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReflectionSuggestionChip("What have you learned today?") { vm.initiateNeuralReflection(it) }
                    ReflectionSuggestionChip("What are you thinking?") { vm.initiateNeuralReflection(it) }
                    ReflectionSuggestionChip("What can you do?") { vm.initiateNeuralReflection(it) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Communication History (specific to reflection)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = true
                ) {
                    val reflectionMsgs = state.messages.filter { it.content.contains("[REFLECTION]") || it.sender == "SYSTEM" }.takeLast(20).reversed()
                    items(reflectionMsgs) { msg ->
                        ReflectionMessageBubble(msg)
                    }
                }
            }
        }

        // Communication Channel
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                TextField(
                    value = reflectInput,
                    onValueChange = { reflectInput = it },
                    placeholder = { Text("Ask about my growth, thoughts, or capabilities...", color = SilverText.copy(alpha = 0.3f), fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SilverText,
                        unfocusedTextColor = SilverText,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                IconButton(
                    onClick = {
                        if (reflectInput.isNotBlank()) {
                            vm.initiateNeuralReflection(reflectInput)
                            reflectInput = ""
                        }
                    },
                    modifier = Modifier.size(40.dp).background(CyberCyan, CircleShape)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = "Sync", tint = Obsidian, modifier = Modifier.size(20.dp))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { vm.startListening() },
                    modifier = Modifier.size(40.dp).background(if (state.isListening) Color.Red else Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice", tint = if (state.isListening) Color.White else CyberCyan, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ReflectionMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(0.5.dp, CyberCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(label, color = SilverText.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = CyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ReflectionSuggestionChip(text: String, onClick: (String) -> Unit) {
    Surface(
        onClick = { onClick(text) },
        color = CyberCyan.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            color = CyberCyan,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ReflectionMessageBubble(message: ChatMessage) {
    val isFromUser = message.sender == "OPERATOR" || message.sender == "MASTER ARCHITECT"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isFromUser) SilverText.copy(alpha = 0.1f) else Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            border = if (!isFromUser) androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.2f)) else null,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Text(
                text = message.content.replace("[REFLECTION]", "").trim(),
                color = if (isFromUser) SilverText else CyberCyan,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp),
                lineHeight = 18.sp
            )
        }
    }
}
