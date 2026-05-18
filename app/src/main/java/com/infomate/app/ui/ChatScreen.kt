package com.infomate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infomate.core.ui.components.BrainVisualizer
import com.infomate.core.ui.components.InfomateState
import com.infomate.core.ui.components.LiveThinkingView
import com.infomate.core.ui.theme.CyberCyan
import com.infomate.core.ui.theme.Obsidian
import com.infomate.core.ui.theme.SilverText
import com.infomate.core.ui.theme.InfoMateTheme
import com.infomate.core.ui.theme.NeonBlue

@Composable
fun ChatScreen(vm: AgentViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    InfoMateTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Obsidian) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Futuristic Header with Search Provision
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!searchActive) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("INFOMATE v9", color = CyberCyan, style = MaterialTheme.typography.headlineMedium)
                            Text("CORE: ACTIVE", color = SilverText.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search neural logs...", color = SilverText.copy(alpha = 0.4f)) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Obsidian,
                                unfocusedContainerColor = Obsidian,
                                focusedTextColor = SilverText
                            ),
                            singleLine = true
                        )
                    }
                    
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    BrainVisualizer(state = state.brainState, modifier = Modifier.size(60.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cognitive Thought Stream (Premium Glass Card)
                if (state.cognitiveSteps.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SilverText.copy(alpha = 0.03f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            LiveThinkingView(steps = state.cognitiveSteps)
                        }
                    }
                }

                // Chat Messages Area
                LazyColumn(modifier = Modifier.weight(1f)) {
                    val filteredMessages = if (searchQuery.isBlank()) state.messages 
                                         else state.messages.filter { it.contains(searchQuery, ignoreCase = true) }
                    
                    items(filteredMessages) { message ->
                        val isOperator = message.startsWith("Operator:")
                        MessageCard(message, isOperator)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium Input Section
                Surface(
                    color = SilverText.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        TextField(
                            value = state.input,
                            onValueChange = vm::updateInput,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Input directive...", color = SilverText.copy(alpha = 0.4f), fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedTextColor = SilverText,
                                unfocusedTextColor = SilverText,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                        
                        IconButton(
                            onClick = vm::send,
                            modifier = Modifier.background(
                                brush = Brush.linearGradient(listOf(CyberCyan, NeonBlue)),
                                shape = RoundedCornerShape(12.dp)
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Execute", tint = Obsidian)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageCard(text: String, isOperator: Boolean) {
    val displayContent = if (text.startsWith("INFOMATE:")) text.removePrefix("INFOMATE:") else text.removePrefix("Operator:")
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = if (isOperator) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, 
                topEnd = 16.dp, 
                bottomStart = if (isOperator) 16.dp else 0.dp,
                bottomEnd = if (isOperator) 0.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isOperator) SilverText.copy(alpha = 0.1f) else CyberCyan.copy(alpha = 0.1f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isOperator) "OPERATOR" else "INFOMATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOperator) SilverText.copy(alpha = 0.5f) else CyberCyan,
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayContent.trim(),
                    color = SilverText,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
