package com.infomate.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infomate.core.ui.components.BrainVisualizer
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
        Box(modifier = Modifier.fillMaxSize().background(Obsidian)) {
            // Background Ambient Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = 1000f
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    HeaderSection(
                        status = state.status,
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                        onSearchToggle = { searchActive = !searchActive },
                        onSearchQueryChange = { searchQuery = it },
                        isMaleVoice = state.isMaleVoice,
                        onVoiceToggle = vm::toggleVoice,
                        vm = vm
                    )
                },
                bottomBar = {
                    InputSection(
                        input = state.input,
                        isListening = state.isListening,
                        onInputChange = vm::updateInput,
                        onSend = vm::send,
                        onMediaClick = { /* Handle media picking */ },
                        onCameraClick = { /* Handle camera */ },
                        onMicToggle = {
                            if (state.isListening) vm.stopListening() else vm.startListening()
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Central Visual Hub
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.35f),
                        contentAlignment = Alignment.Center
                    ) {
                        VisualHub(
                            brainState = state.brainState,
                            isActive = state.isSpeaking || state.isListening,
                            amplitudes = state.voiceAmplitudes
                        )
                    }

                    // Content Area
                    Column(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxWidth()
                    ) {
                        // Thinking steps overlay
                        AnimatedVisibility(
                            visible = state.cognitiveSteps.isNotEmpty(),
                            enter = slideInVertically { -it } + fadeIn(),
                            exit = slideOutVertically { -it } + fadeOut()
                        ) {
                            Surface(
                                color = SilverText.copy(alpha = 0.03f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    CyberCyan.copy(alpha = 0.1f)
                                )
                            ) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    LiveThinkingView(steps = state.cognitiveSteps)
                                }
                            }
                        }

                        // Chat Stream
                        val listState = rememberLazyListState()
                        LaunchedEffect(state.messages.size) {
                            if (state.messages.isNotEmpty()) {
                                listState.animateScrollToItem(state.messages.size - 1)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val filteredMessages = if (searchQuery.isBlank()) state.messages 
                                                 else state.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
                            
                            items(filteredMessages) { message ->
                                MessageCard(message)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisualHub(
    brainState: com.infomate.core.ui.components.InfomateState,
    isActive: Boolean,
    amplitudes: List<Float>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Neon Halo for the Iris
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(160.dp),
                color = Color.Transparent,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    Brush.sweepGradient(listOf(CyberCyan.copy(alpha = 0.2f), NeonBlue.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.2f)))
                )
            ) {}
            
            BrainVisualizer(
                state = brainState,
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                VoiceSpectrum(amplitudes = amplitudes)
            } else {
                // Futuristic inactive line with glow
                Surface(
                    modifier = Modifier
                        .width(60.dp)
                        .height(2.dp)
                        .clip(CircleShape),
                    color = CyberCyan.copy(alpha = 0.3f)
                ) {}
            }
        }
    }
}

@Composable
fun VoiceSpectrum(amplitudes: List<Float>) {
    Row(
        modifier = Modifier.height(50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        amplitudes.forEach { amplitude ->
            val animatedHeight by animateFloatAsState(
                targetValue = amplitude * 45f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                label = "amp"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight.dp.coerceAtLeast(4.dp))
                    .padding(horizontal = 1.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(CyberCyan, NeonBlue, CyberCyan)
                        )
                    )
            )
        }
    }
}

@Composable
fun HeaderSection(
    status: String,
    searchActive: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    isMaleVoice: Boolean,
    onVoiceToggle: () -> Unit,
    vm: AgentViewModel
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .statusBarsPadding()
    ) {
        if (!searchActive) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "INFOMATE v9",
                        color = CyberCyan,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "by Socrates Kipruto",
                        color = CyberCyan.copy(alpha = 0.3f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = status,
                        color = SilverText.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onVoiceToggle, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isMaleVoice) Icons.Filled.Male else Icons.Filled.Female,
                            contentDescription = "Toggle Voice",
                            tint = CyberCyan.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search neural logs...", color = SilverText.copy(alpha = 0.3f)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = SilverText,
                    unfocusedIndicatorColor = CyberCyan.copy(alpha = 0.3f)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { 
                        vm.performSearch(searchQuery)
                        onSearchToggle() // Close bar after search
                    }
                )
            )
        }
        
        IconButton(onClick = {
            if (searchActive && searchQuery.isNotBlank()) {
                vm.performSearch(searchQuery)
                onSearchToggle()
            } else {
                onSearchToggle()
            }
        }) {
            Icon(
                imageVector = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                contentDescription = "Search",
                tint = CyberCyan.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun InputSection(
    input: String,
    isListening: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMediaClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMicToggle: () -> Unit
) {
    Surface(
        color = Obsidian,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Surface(
                color = if (isListening) CyberCyan.copy(alpha = 0.1f) else SilverText.copy(alpha = 0.08f),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    if (!isListening) {
                        IconButton(onClick = onMediaClick) {
                            Icon(Icons.Filled.Add, contentDescription = "Attach", tint = SilverText.copy(alpha = 0.6f))
                        }
                    }
                    
                    TextField(
                        value = if (isListening) "Listening to neural input..." else input,
                        onValueChange = { if (!isListening) onInputChange(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Directive...", color = SilverText.copy(alpha = 0.4f), fontSize = 16.sp) },
                        readOnly = isListening,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = if (isListening) CyberCyan else SilverText,
                            unfocusedTextColor = if (isListening) CyberCyan else SilverText,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    if (input.isEmpty() && !isListening) {
                        IconButton(onClick = onCameraClick) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = SilverText.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val isSend = input.isNotBlank() && !isListening
            val icon = if (isSend) Icons.AutoMirrored.Filled.Send else if (isListening) Icons.Filled.Stop else Icons.Filled.Mic
            
            IconButton(
                onClick = {
                    if (isSend) onSend() else onMicToggle()
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            if (isListening) listOf(Color.Red, Color(0xFFFF5252))
                            else listOf(CyberCyan, NeonBlue)
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(icon, contentDescription = "Action", tint = Obsidian)
            }
        }
    }
}

@Composable
fun MessageCard(message: ChatMessage) {
    val isOperator = message.sender == "OPERATOR"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOperator) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOperator) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 24.dp, 
                    topEnd = 24.dp, 
                    bottomStart = if (isOperator) 24.dp else 4.dp,
                    bottomEnd = if (isOperator) 4.dp else 24.dp
                ),
                color = if (isOperator) SilverText.copy(alpha = 0.08f) else CyberCyan.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    if (isOperator) SilverText.copy(alpha = 0.1f) else CyberCyan.copy(alpha = 0.2f)
                ),
                shadowElevation = if (isOperator) 0.dp else 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (message.type != MessageType.TEXT) {
                        MediaPlaceholder(message.type)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Text(
                        text = message.content,
                        color = SilverText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 24.sp,
                            letterSpacing = 0.2.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = if (isOperator) "OPERATOR" else "INFOMATE SYSTEM",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    shadow = Shadow(color = Color.Black, blurRadius = 2f)
                ),
                color = if (isOperator) SilverText.copy(alpha = 0.3f) else CyberCyan.copy(alpha = 0.5f),
                fontSize = 8.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun MediaPlaceholder(type: MessageType) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when(type) {
                MessageType.IMAGE -> Icons.Filled.Image
                MessageType.VIDEO -> Icons.Filled.PlayArrow
                else -> Icons.Filled.Description
            },
            contentDescription = null,
            tint = SilverText.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        )
    }
}
