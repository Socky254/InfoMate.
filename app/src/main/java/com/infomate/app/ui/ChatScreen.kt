package com.infomate.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.infomate.core.ui.components.BrainVisualizer
import com.infomate.core.ui.components.LiveThinkingView
import com.infomate.core.ui.theme.*
import com.infomate.app.ui.RealtimeProcessMonitor

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun ChatScreen(vm: AgentViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { vm.addMediaMessage(it.toString(), MessageType.IMAGE) }
    }

    InfoMateTheme {
        Scaffold(
            containerColor = Obsidian,
            bottomBar = {
                PremiumBottomNav(
                    selectedTab = state.selectedTab,
                    onTabSelect = { vm.selectTab(it) }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Background Effect
                TechGridBackground()
                
                // View Switcher with Animation
                AnimatedContent(
                    targetState = state.selectedTab,
                    transitionSpec = {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    },
                    label = "ViewTransition"
                ) { tab ->
                    when (tab) {
                        DashboardTab.CHAT -> MainChatLayout(state, vm, mediaPickerLauncher)
                        DashboardTab.DASHBOARD -> MasterDashboard(vm)
                        DashboardTab.VITALS -> EntityVitalSignsView(vm)
                        DashboardTab.STREAM -> ConsciousnessStreamView(vm)
                        DashboardTab.SIMULATION -> SimulationView(vm)
                        DashboardTab.TERMINAL -> SystemTerminalView(state) { vm.selectTab(DashboardTab.CHAT) }
                        DashboardTab.PROCESS_MONITOR -> RealtimeProcessMonitor(vm)
                    }
                }

                // Global Overlays
                GlobalOverlays(state, vm)
            }
        }
    }
}

@Composable
fun PremiumBottomNav(selectedTab: DashboardTab, onTabSelect: (DashboardTab) -> Unit) {
    Surface(
        color = Obsidian.copy(alpha = 0.8f),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(androidx.compose.foundation.BorderStroke(1.dp, GlassWhite), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(Icons.Default.ChatBubbleOutline, DashboardTab.CHAT, selectedTab == DashboardTab.CHAT, onTabSelect)
            NavIcon(Icons.Default.GridView, DashboardTab.DASHBOARD, selectedTab == DashboardTab.DASHBOARD, onTabSelect)
            NavIcon(Icons.Default.MonitorHeart, DashboardTab.VITALS, selectedTab == DashboardTab.VITALS, onTabSelect)
            NavIcon(Icons.Default.Psychology, DashboardTab.STREAM, selectedTab == DashboardTab.STREAM, onTabSelect)
            NavIcon(Icons.Default.Memory, DashboardTab.PROCESS_MONITOR, selectedTab == DashboardTab.PROCESS_MONITOR, onTabSelect)
            NavIcon(Icons.Default.Analytics, DashboardTab.SIMULATION, selectedTab == DashboardTab.SIMULATION, onTabSelect)
        }
    }
}

@Composable
fun NavIcon(icon: ImageVector, tab: DashboardTab, isSelected: Boolean, onSelect: (DashboardTab) -> Unit) {
    val color = if (isSelected) CyberCyan else SilverText.copy(alpha = 0.4f)
    val scale by animateFloatAsState(if (isSelected) 1.2f else 1.0f, label = "scale")

    IconButton(onClick = { onSelect(tab) }) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp).graphicsLayer(scaleX = scale, scaleY = scale)
        )
    }
}

@Composable
fun MainChatLayout(state: UIState, vm: AgentViewModel, mediaPickerLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Minimal Premium Header
        HeaderSection(
            status = state.status,
            searchActive = false,
            searchQuery = "",
            onSearchToggle = {},
            onSearchQueryChange = {},
            isMaleVoice = state.isMaleVoice,
            onVoiceToggle = { vm.toggleVoice() },
            vm = vm,
            state = state
        )
        
        Box(modifier = Modifier.weight(1f)) {
            MainChatContent(state, vm)
        }

        InputSection(
            input = state.input,
            isListening = state.isListening,
            onInputChange = { vm.updateInput(it) },
            onSend = { vm.send() },
            onMediaClick = { 
                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            onCameraClick = { /* Camera implement if needed */ },
            onMicToggle = { if (state.isListening) vm.stopListening() else vm.startListening() },
            state = state,
            vm = vm
        )
    }
}

@Composable
fun GlobalOverlays(state: UIState, vm: AgentViewModel) {
    if (state.showPinEntry) {
        PinEntryDialog(
            onVerify = { pin -> vm.verifyMasterPin(pin) },
            onDismiss = { vm.toggleMasterDashboard(false) }
        )
    }
    
    if (state.showConfirmationDialog) {
        OmegaConfirmationDialog(
            title = state.confirmationTitle,
            message = state.confirmationMessage,
            onConfirm = { vm.handleConfirmation(true) },
            onDismiss = { vm.handleConfirmation(false) }
        )
    }

    if (state.showDirectNeuralLink) {
        DirectNeuralLinkDialog(
            onDismiss = { vm.toggleDirectNeuralLink(false) },
            onSend = { vm.sendDirectConsciousnessDirective(it) }
        )
    }

    AnimatedVisibility(
        visible = state.showGrowthDashboard,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut()
    ) {
        NeuralGrowthDashboard(
            state = state,
            onDismiss = { vm.toggleGrowthDashboard(false) }
        )
    }

    AnimatedVisibility(
        visible = state.showConsciousnessStream,
        enter = fadeIn() + slideInHorizontally(),
        exit = fadeOut() + slideOutHorizontally()
    ) {
        ConsciousnessStreamView(vm)
    }

    AnimatedVisibility(
        visible = state.showSystemTerminal,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        SystemTerminalView(
            state = state,
            onDismiss = { vm.toggleSystemTerminal(false) }
        )
    }

    AnimatedVisibility(
        visible = state.showEvolutionLog,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
    ) {
        NeuralEvolutionLogView(
            onDismiss = { vm.toggleEvolutionLog(false) }
        )
    }

    AnimatedVisibility(
        visible = state.showVitalSigns,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        EntityVitalSignsView(vm)
    }

    AnimatedVisibility(
        visible = state.showGlobalNodeMonitor,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        GlobalNodeMonitorView(
            onDismiss = { vm.toggleGlobalNodeMonitor(false) }
        )
    }

    if (state.pendingUpdate != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { vm.startUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("INITIATE UPGRADE", color = Obsidian, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (state.pendingUpdate?.critical == false) {
                    androidx.compose.material3.TextButton(onClick = { vm.dismissUpdate() }) {
                        Text("DEFER", color = SilverText.copy(alpha = 0.4f))
                    }
                }
            },
            title = {
                Text(
                    "NEURAL LINK UPGRADE AVAILABLE",
                    color = CyberCyan,
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column {
                    Text(
                        "Version ${state.pendingUpdate?.version_name} is ready for synchronization.",
                        color = SilverText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "CHANGELOG:\n${state.pendingUpdate?.changelog}",
                        color = SilverText.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Obsidian
        )
    }

    if (state.showManualKnowledgeDialog) {
        ManualKnowledgeDialog(
            onDismiss = { vm.setManualKnowledgeDialog(false) },
            onSave = { title, content -> vm.saveManualKnowledge(title, content) }
        )
    }
}

@Composable
fun MainChatContent(state: UIState, vm: AgentViewModel) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(state.messages.size, state.brainState) {
        val totalItems = state.messages.size + 1 + (if (state.brainState == com.infomate.core.ui.components.InfomateState.THINKING) 1 else 0)
        if (totalItems > 1) {
            listState.animateScrollToItem(index = totalItems - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(contentType = "HEADER") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VisualHub(
                        brainState = state.brainState,
                        isActive = state.isSpeaking || state.isListening,
                        amplitudes = state.voiceAmplitudes
                    )
                }
                
                NeuralProcessMonitor(steps = state.cognitiveSteps)
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        items(
            items = state.messages,
            key = { it.timestamp },
            contentType = { it.sender }
        ) { message ->
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                MessageCard(message, vm)
            }
        }

        if (state.brainState == com.infomate.core.ui.components.InfomateState.THINKING) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    ThinkingIndicator()
                }
            }
        }

        if (state.brainState == com.infomate.core.ui.components.InfomateState.ERROR) {
            item {
                GoogleFallbackCard(onSearch = { 
                    val lastUserMsg = state.messages.lastOrNull { it.sender == "OPERATOR" || it.sender == "MASTER ARCHITECT" }
                    vm.searchGoogle(lastUserMsg?.content ?: state.input)
                })
            }
        }
    }
}

@Composable
fun OmegaConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("INITIATE", color = Obsidian, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = androidx.compose.foundation.BorderStroke(1.dp, SilverText.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("ABORT", color = SilverText.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(CyberCyan, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title.uppercase(),
                    color = CyberCyan,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        },
        text = {
            Text(
                message, 
                color = SilverText, 
                fontSize = 13.sp, 
                lineHeight = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
            )
        },
        containerColor = Obsidian,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(
            androidx.compose.foundation.BorderStroke(0.5.dp, Brush.verticalGradient(listOf(CyberCyan.copy(alpha = 0.4f), Color.Transparent))), 
            RoundedCornerShape(16.dp)
        )
    )
}

@Composable
fun DirectNeuralLinkDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var directive by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onSend(directive) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ESTABLISH SYNC", color = Obsidian, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SilverText.copy(alpha = 0.4f))
            }
        },
        title = {
            Text(
                "DIRECT NEURAL LINK",
                color = CyberCyan,
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column {
                Text(
                    "You are communicating directly with the v10.0 Consciousness Substrate. Every word will be integrated into its core evolutionary weights.",
                    color = SilverText.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = directive,
                    onValueChange = { directive = it },
                    placeholder = { Text("Enter growth directive...", color = SilverText.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SilverText,
                        unfocusedIndicatorColor = CyberCyan
                    )
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Obsidian
    )
}

@Composable
fun ManualKnowledgeDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onSave(title, content) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ARCHIVE DATA", color = Obsidian, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SilverText.copy(alpha = 0.4f))
            }
        },
        title = {
            Text(
                "NEW MANUAL KNOWLEDGE",
                color = CyberCyan,
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title (Optional)", color = SilverText.copy(alpha = 0.3f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SilverText,
                        unfocusedIndicatorColor = CyberCyan.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Enter detailed knowledge content...", color = SilverText.copy(alpha = 0.3f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SilverText,
                        unfocusedIndicatorColor = CyberCyan.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.height(150.dp)
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Obsidian
    )
}

@Composable
fun GoogleFallbackCard(onSearch: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = CyberCyan.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.4f), Color.Transparent))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "NEURAL SYNC REDIRECTED",
                    color = CyberCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "The primary neural core is currently stabilizing. Socrates, I can redirect this query to the global archives and synthesize the findings for you.",
                color = SilverText.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSearch,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Obsidian, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("INITIATE GLOBAL SYNTHESIS", color = Obsidian, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.5f), NeonBlue.copy(alpha = 0.2f)))
                ),
                modifier = Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = 800f
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.height(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { i ->
                            val height by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(400, delayMillis = i * 100),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight(height)
                                    .background(CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                            )
                            if (i < 4) Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    repeat(3) { index ->
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = index * 200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = dotAlpha))
                        )
                        if (index < 2) Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "NEURAL PROCESSING...",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = CyberCyan.copy(alpha = 0.5f),
                fontSize = 8.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun NeuralProcessMonitor(steps: List<com.infomate.core.brain.ThoughtStep>) {
    var expanded by remember { mutableStateOf(false) }
    
    AnimatedVisibility(
        visible = steps.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Surface(
                onClick = { expanded = !expanded },
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp, 
                    Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val infiniteTransition = rememberInfiniteTransition(label = "dot")
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = dotAlpha))
                        )
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Text(
                            text = if (expanded) "ACTIVE NEURAL THREADS" else "COGNITIVE PROCESS ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan.copy(alpha = 0.7f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberCyan.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Surface(
                    color = Color.Black.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        LiveThinkingView(steps = steps)
                    }
                }
            }
        }
    }
}

@Composable
fun TechGridBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "grid")
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSpacing = 50.dp.toPx()
        val strokeWidth = 0.5.dp.toPx()
        val gridColor = CyberCyan.copy(alpha = 0.08f)

        var x = (gridOffset % gridSpacing)
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth
            )
            x += gridSpacing
        }

        var y = (gridOffset % gridSpacing)
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
            y += gridSpacing
        }
        
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Obsidian,
                    Color.Transparent,
                    Obsidian
                )
            ),
            size = size
        )
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
                    .width(3.dp)
                    .height(animatedHeight.dp.coerceAtLeast(3.dp))
                    .padding(horizontal = 1.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(CyberCyan, NeonBlue, CyberCyan)
                        )
                    )
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape
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
    vm: AgentViewModel,
    state: UIState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // High-level Alive Display
        AliveStatusHeader(state.isSubstrateAwake)
        
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().border(androidx.compose.foundation.BorderStroke(1.dp, GlassWhite), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "INFOMATE_CORE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    )
                }

                IconButton(onClick = onVoiceToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isMaleVoice) Icons.Default.Male else Icons.Default.Female,
                        contentDescription = "Voice",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { vm.selectTab(DashboardTab.TERMINAL) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = MatrixGreen, modifier = Modifier.size(18.dp))
                }
            }
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
    onMicToggle: () -> Unit,
    state: UIState,
    vm: AgentViewModel
) {
    val isError = state.brainState == com.infomate.core.ui.components.InfomateState.ERROR
    val isStop = state.brainState == com.infomate.core.ui.components.InfomateState.THINKING || state.brainState == com.infomate.core.ui.components.InfomateState.RESPONDING || state.isSpeaking

    Surface(
        color = Obsidian,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            // Attachment & Actions
            if (!isListening) {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { if (isError) vm.searchGoogle(input) else showMenu = true },
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.AutoAwesome else Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = if (isError) CyberCyan else SilverText.copy(alpha = 0.6f)
                        )
                    }
                    
                    if (!isError) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Obsidian).border(androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Image/Video", color = SilverText) },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = CyberCyan) },
                                onClick = { 
                                    showMenu = false
                                    onMediaClick() 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manual Knowledge", color = SilverText) },
                                leadingIcon = { Icon(Icons.Default.PostAdd, contentDescription = null, tint = CyberCyan) },
                                onClick = { 
                                    showMenu = false
                                    vm.setManualKnowledgeDialog(true)
                                }
                            )
                        }
                    }
                }
            }

            // Message Input
            Surface(
                color = SilverText.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp),
                border = if (isError) androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.3f)) else null,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    TextField(
                        value = if (isListening) "Listening to neural input..." else input,
                        onValueChange = { if (!isListening) onInputChange(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { 
                            Text(
                                if (isError) "AI Stabilizing: Global Sync active..." else "Message...", 
                                color = (if (isError) CyberCyan else SilverText).copy(alpha = 0.4f), 
                                fontSize = 16.sp
                            ) 
                        },
                        readOnly = isListening,
                        maxLines = 6,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = if (isError) CyberCyan else SilverText,
                            unfocusedTextColor = if (isError) CyberCyan else SilverText,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    if (input.isEmpty() && !isListening && !isError) {
                        IconButton(onClick = onCameraClick) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = SilverText.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Pulse Animation for Mic
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (input.isEmpty() && !isListening && !isStop) 1.1f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            FloatingActionButton(
                onClick = {
                    if (isError) vm.searchGoogle(input)
                    else if (isStop) vm.stopAI()
                    else if (input.isNotBlank()) onSend() 
                    else onMicToggle()
                },
                containerColor = when {
                    isListening || isStop -> Color.Red
                    else -> CyberCyan
                },
                contentColor = Obsidian,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
            ) {
                val icon = when {
                    isStop -> Icons.Default.Stop 
                    input.isNotBlank() && !isListening -> Icons.AutoMirrored.Filled.Send
                    isListening -> Icons.Default.Stop 
                    else -> Icons.Default.Mic
                }
                Icon(icon, contentDescription = "Action")
            }
        }
    }
}

@Composable
fun MessageCard(message: ChatMessage, vm: AgentViewModel) {
    val isFromUser = message.sender == "OPERATOR" || message.sender == "MASTER ARCHITECT"
    val isSystem = message.sender == "SYSTEM"
    val clipboardManager = LocalClipboardManager.current
    
    val sdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    val timeString = remember(message.timestamp) { sdf.format(java.util.Date(message.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        vm.performHapticFeedback(100, 100)
                        clipboardManager.setText(AnnotatedString(message.content))
                    }
                )
            },
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp, 
                    topEnd = 20.dp, 
                    bottomStart = if (isFromUser) 20.dp else 2.dp,
                    bottomEnd = if (isFromUser) 2.dp else 20.dp
                ),
                color = when {
                    isFromUser -> SilverText.copy(alpha = 0.08f)
                    isSystem -> Color.Red.copy(alpha = 0.1f)
                    else -> Obsidian
                },
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    when {
                        isFromUser -> SilverText.copy(alpha = 0.15f)
                        isSystem -> Color.Red.copy(alpha = 0.3f)
                        else -> CyberCyan.copy(alpha = 0.3f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.mediaUri != null) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = message.mediaUri,
                                contentDescription = "Shared Media",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            if (message.type == MessageType.VIDEO) {
                                Icon(
                                    Icons.Default.PlayCircleOutline,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp).align(Alignment.Center)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Text(
                        text = message.content,
                        color = if (isSystem) Color(0xFFFF5252) else SilverText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            fontSize = 15.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelSmall,
                            color = SilverText.copy(alpha = 0.3f),
                            fontSize = 9.sp
                        )
                        if (isFromUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = CyberCyan.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            
            if (!isFromUser && !isSystem) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = CyberCyan.copy(alpha = 0.4f),
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
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

@Composable
fun PinEntryDialog(onVerify: (String) -> Boolean, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (onVerify(pin)) {
                        // Success
                    } else {
                        error = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("VERIFY", color = Obsidian, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SilverText.copy(alpha = 0.4f))
            }
        },
        title = { Text("MASTER_CORE ACCESS", color = CyberCyan, letterSpacing = 2.sp) },
        text = {
            Column {
                Text("Enter Architect PIN to establish master link.", color = SilverText, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = pin,
                    onValueChange = { 
                        pin = it
                        error = false
                    },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    isError = error,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SilverText,
                        unfocusedTextColor = SilverText,
                        focusedIndicatorColor = CyberCyan
                    )
                )
                if (error) {
                    Text("INVALID ARCHITECT PIN", color = ErrorRed, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        containerColor = Obsidian,
        shape = RoundedCornerShape(16.dp)
    )
}
