package com.infomate.core

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infomate.core.brain.InfomateBrain
import com.infomate.core.domain.agent.InfomateAgent
import com.infomate.core.memory.CognitiveArchive
import com.infomate.core.device.ContextSensors
import com.infomate.core.tools.ToolRouter
import com.infomate.core.security.ExecutionFirewall
import com.infomate.core.ui.components.*
import com.infomate.core.ui.theme.*
import com.infomate.core.tools.MediaType

import com.infomate.core.infrastructure.NeuralVoiceEngine
import com.infomate.core.infrastructure.InfomateDownloadManager
import com.infomate.core.presentation.viewmodel.InfomateViewModel

class MainActivity : ComponentActivity() {
    private lateinit var voiceEngine: NeuralVoiceEngine
    private lateinit var sensors: ContextSensors

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, re-init sensors if needed or just log
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        
        val archive = CognitiveArchive(this)
        val brain = InfomateBrain(archive)
        sensors = ContextSensors(this)
        val tools = ToolRouter()
        val firewall = ExecutionFirewall()
        val agent = InfomateAgent(brain, archive, sensors, tools, firewall)
        val downloadManager = InfomateDownloadManager(this)
        voiceEngine = NeuralVoiceEngine(this)

        setContent {
            InfoMateTheme {
                var showAuthScreen by remember { 
                    mutableStateOf(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) 
                }

                Surface(modifier = Modifier.fillMaxSize(), color = Obsidian) {
                    if (showAuthScreen) {
                        NeuralAuthorizationScreen(onAuthorize = {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            showAuthScreen = false
                        })
                    } else {
                        val viewModel: InfomateViewModel = viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return InfomateViewModel(agent, sensors, voiceEngine) as T
                                }
                            }
                        )
                        InfomateDashboard(viewModel, downloadManager)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.shutdown()
        sensors.shutdown()
    }
}

@Composable
fun InfomateDashboard(viewModel: InfomateViewModel, downloadManager: InfomateDownloadManager) {
    val uiState by viewModel.uiState.collectAsState()
    val logs = viewModel.logs
    val suggestions = viewModel.suggestions
    val notifications = viewModel.notifications
    var userInput by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize()) {
        GridBackground()

        if (!uiState.isSystemReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 60.dp, bottom = 40.dp)
            ) {
                item {
                    HeaderSection(userName = uiState.currentUser?.name)
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        BrainVisualizer(state = uiState.state)
                        StateStatusHUD(uiState.state)
                    }
                }

                item {
                    SecurityStatusHUD()
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    NotificationHub(notifications = notifications)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    SuggestionChips(suggestions = suggestions, onChipClick = { viewModel.onQuerySubmit(it) })
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    InputSection(
                        userInput = userInput,
                        onValueChange = { userInput = it },
                        onSend = {
                            if (userInput.isNotBlank()) {
                                viewModel.onQuerySubmit(userInput)
                                userInput = ""
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                uiState.lastResponse?.let { response ->
                    item {
                        ResponseContainer(response)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                item {
                    MemoryStreamPanel(logs = logs)
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun GridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        for (x in 0..(size.width / step).toInt()) {
            drawLine(CyberCyan.copy(alpha = 0.03f), Offset(x * step, 0f), Offset(x * step, size.height))
        }
        for (y in 0..(size.height / step).toInt()) {
            drawLine(CyberCyan.copy(alpha = 0.03f), Offset(0f, y * step), Offset(size.width, y * step))
        }
    }
}

@Composable
fun StateStatusHUD(state: InfomateState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 220.dp)
    ) {
        Text(
            text = "INFOMATE CORE",
            style = MaterialTheme.typography.labelSmall.copy(color = SilverText.copy(alpha = 0.4f), letterSpacing = 2.sp)
        )
        Text(
            text = state.name,
            style = MaterialTheme.typography.labelLarge.copy(
                color = when(state) {
                    InfomateState.ERROR -> ErrorRed
                    InfomateState.AWAKENED -> ElectricViolet
                    InfomateState.COMPANION -> CompanionGold
                    InfomateState.DELEGATING -> NeonBlue
                    else -> CyberCyan
                },
                fontWeight = FontWeight.Bold, letterSpacing = 4.sp
            )
        )
    }
}

@Composable
fun ResponseContainer(response: com.infomate.core.domain.model.AgentResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = DeepSpace.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            LiveThinkingView(steps = response.steps)
            HorizontalDivider(color = CyberCyan.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 20.dp))
            ResponseSection(response = response)
        }
    }
}

@Composable
fun SignInScreen(onSignIn: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .background(DeepSpace.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("INFOMATE", style = MaterialTheme.typography.headlineLarge, color = CyberCyan, fontWeight = FontWeight.ExtraBold, letterSpacing = 12.sp)
            Text("IDENTITY VERIFICATION", style = MaterialTheme.typography.labelSmall, color = SilverText.copy(alpha = 0.5f), letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(48.dp))
            TextField(value = name, onValueChange = { name = it }, placeholder = { Text("Agent ID", color = GhostGray) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Obsidian, unfocusedContainerColor = Obsidian, focusedTextColor = SilverText, cursorColor = CyberCyan), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            TextField(value = email, onValueChange = { email = it }, placeholder = { Text("Secure Access Key", color = GhostGray) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Obsidian, unfocusedContainerColor = Obsidian, focusedTextColor = SilverText, cursorColor = CyberCyan), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(40.dp))
            Button(onClick = { if(name.isNotBlank() && email.isNotBlank()) onSignIn(name, email) }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = CyberCyan), shape = RoundedCornerShape(12.dp)) {
                Text("AUTHENTICATE", color = Obsidian, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
fun SuggestionChips(suggestions: List<String>, onChipClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        suggestions.forEach { suggestion ->
            AssistChip(onClick = { onChipClick(suggestion) }, label = { Text(suggestion, fontSize = 10.sp) }, colors = AssistChipDefaults.assistChipColors(labelColor = CyberCyan, containerColor = DeepSpace), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun HeaderSection(userName: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "INFOMATE", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp, color = CyberCyan))
        Text(text = "AGENT: ${userName?.uppercase() ?: "RELIABLE COGNITIVE AGENT"}", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, color = SilverText.copy(alpha = 0.6f)))
    }
}

@Composable
fun SecurityStatusHUD() {
    Row(modifier = Modifier.fillMaxWidth().background(DeepSpace.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).border(1.dp, CyberCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(MatrixGreen, androidx.compose.foundation.shape.CircleShape).blur(4.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("FIREWALL ACTIVE", style = MaterialTheme.typography.labelSmall.copy(color = MatrixGreen, fontSize = 9.sp, letterSpacing = 1.sp))
                Text("Protocol: AES-256-QUANTUM", style = MaterialTheme.typography.labelSmall.copy(color = SilverText.copy(alpha = 0.4f), fontSize = 7.sp))
            }
        }
        Text("DETERMINISTIC MODE", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan.copy(alpha = 0.6f), fontSize = 9.sp, letterSpacing = 1.sp))
    }
}

@Composable
fun InputSection(userInput: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Column {
        TextField(value = userInput, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter directive or query...", color = GhostGray, fontSize = 14.sp) }, colors = TextFieldDefaults.colors(focusedContainerColor = DeepSpace.copy(alpha = 0.5f), unfocusedContainerColor = DeepSpace.copy(alpha = 0.3f), focusedTextColor = SilverText, unfocusedTextColor = SilverText, cursorColor = CyberCyan, focusedIndicatorColor = CyberCyan, unfocusedIndicatorColor = CyberCyan.copy(alpha = 0.2f)), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSend, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Brush.linearGradient(listOf(CyberCyan, NeonBlue))), contentPadding = PaddingValues(0.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.1f), NeonBlue.copy(alpha = 0.2f)))), contentAlignment = Alignment.Center) {
                Text(text = "INITIALIZE COGNITIVE LOOP", color = CyberCyan, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ResponseSection(response: com.infomate.core.domain.model.AgentResponse) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp, 16.dp).background(CyberCyan))
            Spacer(modifier = Modifier.width(12.dp))
            Text("INTELLIGENCE OUTPUT", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, letterSpacing = 2.sp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = response.output, color = SilverText, lineHeight = 26.sp, style = MaterialTheme.typography.bodyLarge)
        if (response.media.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            response.media.forEach { media ->
                MediaItem(media)
            }
        }
        response.recommendation?.let {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(color = MatrixGreen.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MatrixGreen.copy(alpha = 0.2f))) {
                Text(text = "NEXT ACTION: $it", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MatrixGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun MediaItem(media: com.infomate.core.tools.MediaOutput) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Obsidian), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(32.dp).background(DeepSpace, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text(text = media.type.name.take(1), color = CyberCyan, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(media.type.name, style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, letterSpacing = 1.sp))
                    Text(media.description, color = SilverText.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                }
            }
            Button(onClick = { /* Implement real download logic here */ }, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = DeepSpace), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))) {
                Text(text = if (media.type == MediaType.LINK) "OPEN" else "FETCH", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}
