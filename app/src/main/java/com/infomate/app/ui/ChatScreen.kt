package com.infomate.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.compose.AsyncImage
import com.infomate.core.ui.components.BrainVisualizer
import com.infomate.core.ui.components.LiveThinkingView
import com.infomate.core.ui.theme.CyberCyan
import com.infomate.core.ui.theme.Obsidian
import com.infomate.core.ui.theme.SilverText
import com.infomate.core.ui.theme.InfoMateTheme
import com.infomate.core.ui.theme.NeonBlue

import androidx.compose.ui.graphics.graphicsLayer
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
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            vm.addMediaMessage(it.toString(), MessageType.IMAGE)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            vm.addMediaMessage("camera_capture", MessageType.IMAGE)
        }
    }

    InfoMateTheme {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
        ) {
            TechGridBackground()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.0f to CyberCyan.copy(alpha = 0.15f),
                            0.5f to NeonBlue.copy(alpha = 0.05f),
                            1.0f to Color.Transparent,
                            center = Offset(0f, 0f),
                            radius = 1500f
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
                        vm = vm,
                        state = state // ADDED
                    )
                },
                bottomBar = {
                    InputSection(
                        input = state.input,
                        isListening = state.isListening,
                        onInputChange = vm::updateInput,
                        onSend = vm::send,
                        onMediaClick = { 
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onCameraClick = { cameraLauncher.launch(null) },
                        onMicToggle = {
                            if (state.isListening) vm.stopListening() else vm.startListening()
                        }
                    )
                }
            ) { paddingValues ->
                val listState = rememberLazyListState()
                
                // Keyboard dismissal on scroll
                LaunchedEffect(listState.isScrollInProgress) {
                    if (listState.isScrollInProgress) {
                        focusManager.clearFocus()
                    }
                }

                LaunchedEffect(state.messages.size, state.cognitiveSteps.size) {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            VisualHub(
                                brainState = state.brainState,
                                isActive = state.isSpeaking || state.isListening,
                                amplitudes = state.voiceAmplitudes
                            )
                        }
                    }

                    item {
                        NeuralProcessMonitor(steps = state.cognitiveSteps)
                    }

                    val filteredMessages = if (searchQuery.isBlank()) state.messages 
                                         else state.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
                    
                    items(filteredMessages) { message ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            MessageCard(message, vm)
                        }
                    }
                }
            }
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
                        // Pulsing Neural Dot
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

        // Vertical Lines
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

        // Horizontal Lines
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
        
        // Circular Depth Glow
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
    vm: AgentViewModel,
    state: UIState // ADDED
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
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = vm::toggleVoiceOutput, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (state.isVoiceOutputEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = "Toggle Speech Output",
                            tint = if (state.isVoiceOutputEnabled) CyberCyan else SilverText.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
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
                        trailingIcon = {
                            if (input.isNotEmpty() && !isListening) {
                                IconButton(onClick = { onInputChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = SilverText.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
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
            val icon = if (isSend) Icons.Default.Send else if (isListening) Icons.Filled.Stop else Icons.Filled.Mic
            
            // Pulse Animation for Action Button
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (input.isEmpty() && !isListening) 1.05f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            IconButton(
                onClick = {
                    if (isSend) onSend() else onMicToggle()
                },
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
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
fun MessageCard(message: ChatMessage, vm: AgentViewModel) {
    val isOperator = message.sender == "OPERATOR"
    val clipboardManager = LocalClipboardManager.current
    
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
                color = if (isOperator) SilverText.copy(alpha = 0.1f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOperator) Brush.linearGradient(listOf(SilverText.copy(alpha = 0.2f), Color.Transparent))
                    else Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.5f), NeonBlue.copy(alpha = 0.2f)))
                ),
                modifier = if (!isOperator) Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = 800f
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) else Modifier
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (message.type != MessageType.TEXT && message.mediaUri != null) {
                        AsyncImage(
                            model = message.mediaUri,
                            contentDescription = "Shared Media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else if (message.type != MessageType.TEXT) {
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
