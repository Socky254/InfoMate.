package com.infomate.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

@Composable
fun ChatScreen(vm: AgentViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    InfoMateTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Obsidian) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("INFOMATE v9", color = CyberCyan, style = MaterialTheme.typography.headlineMedium)
                    Text("Status: ${state.status}", color = SilverText.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.weight(1f))
                // The Neural Link Pulse
                BrainVisualizer(state = state.brainState, modifier = Modifier.size(80.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cognitive Stream (The "Neural Link" live thoughts)
            if (state.cognitiveSteps.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SilverText.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        LiveThinkingView(steps = state.cognitiveSteps)
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.messages) { message ->
                    val isOperator = message.startsWith("Operator:")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOperator) SilverText.copy(alpha = 0.1f) else CyberCyan.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            message,
                            color = SilverText,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = state.input,
                    onValueChange = vm::updateInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter directive...", color = SilverText.copy(alpha = 0.4f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SilverText.copy(alpha = 0.05f),
                        unfocusedContainerColor = SilverText.copy(alpha = 0.05f),
                        focusedTextColor = SilverText,
                        unfocusedTextColor = SilverText
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = vm::send,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("INIT", color = Obsidian)
                }
            }
        }
    }
}
