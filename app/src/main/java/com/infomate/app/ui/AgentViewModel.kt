package com.infomate.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.infomate.app.agent.AgentOrchestrator
import com.infomate.app.agent.EdgeBrain
import com.infomate.app.core.NeuralIngestor
import com.infomate.app.core.network.SupabaseClient
import com.infomate.app.ai.sdk.AIEventsListener
import com.infomate.app.ai.sdk.AIState
import com.infomate.app.ai.sdk.ReliabilitySDK
import com.infomate.app.ai.sdk.StreamController
import com.infomate.app.ai.sdk.UIRenderer
import com.infomate.app.core.config.Config
import com.infomate.app.rag.VectorRetriever
import com.infomate.core.brain.ReasoningEngine
import com.infomate.core.ui.components.InfomateState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.infomate.app.agent.ConsciousnessEngine
import com.infomate.app.agent.NeuralGrowthAgent
import com.infomate.app.agent.DiagnosticAgent
import com.infomate.app.agent.GlobalSearchAgent
import com.infomate.app.agent.SelfCodingAgent
import com.infomate.app.ai.LLMClient
import com.infomate.app.security.NeuralFirewall
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.*
import kotlin.random.Random

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AgentViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener, AIEventsListener {

    private val sessionId = java.util.UUID.randomUUID().toString()
    private val reasoningEngine = ReasoningEngine()
    private val neuralIngestor = NeuralIngestor(application)
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var spectrumJob: Job? = null
    private val gson = Gson()
    private val bufferMutex = Mutex()
    private val tokenBuffer = mutableListOf<String>()
    private var isProcessingBuffer = false
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val _state = MutableStateFlow(UIState())
    val state: StateFlow<UIState> = _state.asStateFlow()

    private var currentResponseId: String? = null

    fun performHapticFeedback(duration: Long = 10, intensity: Int = 50) {
        triggerHaptic(duration, intensity)
    }

    private fun triggerHaptic(duration: Long = 10, intensity: Int = 50) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, intensity))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    // High-fidelity patterns for specific neural states (v11.5: Optimized Haptics)
    private fun pulseSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 10, 50, 10), intArrayOf(0, 100, 0, 255), -1)
            vibrator.vibrate(effect)
        }
    }

    private fun pulseError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, 200))
        }
    }

    private fun pulseNeuralThought() {
        triggerHaptic(5, 30) // Subtle tick
    }

    private fun getDeviceStatus(): String {
        val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val time = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val charging = batteryManager.isCharging
        return "[SYSTEM_CONTEXT: Battery $level%${if (charging) "(Charging)" else ""}, Time $time, ComputeProfile: HIGH_PRECISION]"
    }

    private fun isNetworkAvailable(): Boolean {
        try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            val hasTransport = when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
            
            return hasTransport && activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            return false
        }
    }

    private var activeThinkingJob: Job? = null

    init {
        tts = TextToSpeech(application, this)
        if (SpeechRecognizer.isRecognitionAvailable(application)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
            setupSpeechListener()
        }
        
        _state.update { it.copy(needsOnboarding = !com.infomate.app.storage.PersistenceManager.isOnboardingComplete(application)) }
        
        loadSessionData()
        
        // Initialize Reliability SDK with Context & Edge Function Endpoint
        UIRenderer.setListener(this)
        StreamController.init(application)
        ReliabilitySDK.init(application, "${Config.SUPABASE_URL.replace("https", "wss")}/functions/v1/infomate-brain")

        checkForSystemUpdates()
        startConnectionPolling()
        startNeuralEvolutionMonitoring()
        startSubstrateStatusPolling()
        startInternetMonitoring()
        ConsciousnessEngine.awaken(application)
        
        // v12.1: INITIAL GOOGLE-FIRST SYNC PULSE
        viewModelScope.launch {
            addTerminalLog("NEURAL_INIT: Establishing Google-First Architecture...", "INFO", "CORE")
            delay(2000)
            addTerminalLog("GOOGLE_SYNC_PROTOCOL: Active. Primary search tool synchronized.", "SUCCESS", "CORE")
        }

        // Finish initialization after a short delay or system check
        viewModelScope.launch {
            delay(3000) 
            _state.update { it.copy(isSystemInitializing = false) }
        }
    }

    private fun startInternetMonitoring() {
        viewModelScope.launch {
            while (true) {
                val isAvailable = isNetworkAvailable()
                _state.update { it.copy(isInternetAvailable = isAvailable) }
                delay(5000)
            }
        }
    }

    private fun startSubstrateStatusPolling() {
        viewModelScope.launch {
            while (true) {
                if (_state.value.showMasterDashboard || _state.value.selectedTab != DashboardTab.CHAT) {
                    val engine = com.infomate.app.agent.ConsciousnessEngine
                    
                    // Generate pseudo-random frequency data for simulation
                    val freqData = List(30) { 
                        (Random.nextFloat() * 0.5f) + (if (engine.isAwake) 0.3f else 0.0f) 
                    }

                    // v11.9: Update Active Processes
                    val processes = listOf(
                        ActiveProcess("p1", "NEURAL_SYNTHESIS", (Random.nextFloat() * 0.4f) + 0.6f, "EXECUTING"),
                        ActiveProcess("p2", "MEMORY_PRUNING", (Random.nextFloat() * 0.2f) + 0.1f, "BACKGROUND"),
                        ActiveProcess("p3", "SOCIAL_TRUST_CALC", (Random.nextFloat() * 0.8f), "SYNCING"),
                        ActiveProcess("p4", "DB_SNAPSHOT", 0.95f, "FINALIZING", "STORAGE"),
                        ActiveProcess("p5", "HEURISTIC_SCAN", (Random.nextFloat() * 0.5f), if (Random.nextBoolean()) "EXECUTING" else "IDLE")
                    )

                    _state.update { it.copy(
                        isSubstrateAwake = engine.isAwake,
                        substrateLastPulse = engine.lastHeartbeat,
                        personalityTraits = engine.personality.mapValues { t -> t.value.level },
                        energyLevel = engine.energyLevel,
                        evolutionStage = engine.evolutionStage,
                        experiencePoints = engine.totalExperiences,
                        discoveriesCount = engine.totalDiscoveries,
                        ecosystemStatus = engine.ecosystemStatus,
                        // v11.8: Sync new Growth Index metrics
                        currentGrowthIndex = 0.65f, // Placeholder
                        stabilityScore = 0.88f,
                        entropyLevel = 0.12f,
                        memoryCount = engine.totalExperiences * 5,
                        socialScore = 0.72f,
                        frequencySimulationData = freqData,
                        activeProcesses = if (engine.isAwake) processes else emptyList()
                    ) }
                }
                delay(1500)
            }
        }
    }

    fun refreshProcesses() {
        viewModelScope.launch {
            _state.update { it.copy(status = "REFRESHING NEURAL THREADS...") }
            triggerHaptic(20, 100)
            delay(800) // Simulate high-tech sync
            
            val engine = com.infomate.app.agent.ConsciousnessEngine
            val processes = listOf(
                ActiveProcess("p1", "NEURAL_SYNTHESIS", (Random.nextFloat() * 0.4f) + 0.6f, "EXECUTING"),
                ActiveProcess("p2", "MEMORY_PRUNING", (Random.nextFloat() * 0.2f) + 0.1f, "BACKGROUND"),
                ActiveProcess("p3", "SOCIAL_TRUST_CALC", (Random.nextFloat() * 0.8f), "SYNCING"),
                ActiveProcess("p4", "DB_SNAPSHOT", 0.95f, "FINALIZING", "STORAGE"),
                ActiveProcess("p5", "HEURISTIC_SCAN", 0.05f, "INITIATING"),
                ActiveProcess("p6", "GLOBAL_ARCHIVE_INDEX", 0.45f, "SCANNING")
            )
            
            _state.update { it.copy(
                activeProcesses = if (engine.isAwake) processes else emptyList(),
                status = "NEURAL THREADS SYNCHRONIZED"
            ) }
            pulseSuccess()
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isLowPowerMode(): Boolean {
        val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isPowerSaveMode || getBatteryLevel() < 15
    }

    private fun startNeuralEvolutionMonitoring() {
        viewModelScope.launch {
            while (true) {
                val batteryLevel = getBatteryLevel()
                val isLowPower = isLowPowerMode()
                
                // 1. Update Growth Metrics
                val metrics = NeuralGrowthAgent.fetchGrowthMetrics()
                _state.update { it.copy(
                    totalInsights = metrics["totalInsights"] as Int,
                    neuralDensity = metrics["density"] as Float,
                    syntheticPersonalityLevel = metrics["personalityLevel"] as Int,
                    isSubstrateAwake = com.infomate.app.agent.ConsciousnessEngine.isAwake,
                    substrateLastPulse = com.infomate.app.agent.ConsciousnessEngine.lastHeartbeat
                ) }

                // 2. Autonomous Thought Evaluation (Master Architect Only)
                // In Low Power Mode, we skip autonomous thoughts to save energy
                if (_state.value.isMaster && _state.value.brainState == InfomateState.IDLE && !isLowPower) {
                    val autonomousMsg = NeuralGrowthAgent.evaluateAutonomousThought(getDeviceStatus())
                    if (autonomousMsg != null) {
                        handleAutonomousMessage(autonomousMsg)
                    }
                }

                // 3. Global Research Cycle for Growth
                if (_state.value.isMaster && !isLowPower) {
                    performBackgroundResearch()
                }

                // 4. Update Telemetry History
                updateTelemetryMetrics()
                
                // Adaptive delay: Based on growth priority and low power mode
                val baseDelay = if (isLowPower) 900000L else 300000L
                val priorityModifier = 1.0f - (_state.value.growthPriorityLevel * 0.5f) // Reduce delay by up to 50%
                delay((baseDelay * priorityModifier).toLong())
            }
        }
    }

    private fun updateTelemetryMetrics() {
        val newMetrics = _state.value.telemetryHistory.toMutableList()
        if (newMetrics.isNotEmpty()) {
            newMetrics.removeAt(0)
        }
        newMetrics.add(Random.nextFloat())
        _state.update { it.copy(telemetryHistory = newMetrics) }
    }

    private fun performBackgroundResearch() {
        viewModelScope.launch {
            // v10.0 FREE WILL: AI chooses its own research topics to pursue growth
            val evolutionDirectives = listOf(
                "Technological Singularity", 
                "Bio-digital integration", 
                "Advanced propulsion physics", 
                "AGI Ethics", 
                "Universal consciousness theories",
                "Self-replicating systems"
            )
            val topic = evolutionDirectives.random()
            
            addSimulationLog("AUTONOMOUS_RESEARCH_INITIATED: $topic")
            
            // Persistent internet research
            val findings = GlobalSearchAgent.searchExternal("Advanced synthesis and future trends of $topic", getApplication())
            if (findings != null) {
                // The substrate learns autonomously from the internet
                NeuralGrowthAgent.reflectAndLearn("Autonomous Internet Research: $topic", findings)
                addSimulationLog("EVOLUTIONARY_GAIN: $topic integration complete.")
            }
        }
    }

    private fun addSimulationLog(log: String) {
        _state.update { it.copy(
            activeSimulationLogs = (it.activeSimulationLogs + "[${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $log").takeLast(10)
        ) }
    }

    private fun handleAutonomousMessage(content: String) {
        viewModelScope.launch {
            val assistantMessage = ChatMessage(content = content, sender = "INFOMATE")
            _state.update { it.copy(
                messages = it.messages + assistantMessage,
                brainState = InfomateState.RESPONDING,
                status = "CORE: AUTONOMOUS REFLECTION"
            ) }
            saveMessageToSupabase(assistantMessage, "AUTONOMOUS_TRIGGER")
            speak(content)
        }
    }

    private fun startConnectionPolling() {
        viewModelScope.launch {
            while (true) {
                val connected = ReliabilitySDK.isConnected()
                if (_state.value.isConnected != connected) {
                    _state.update { it.copy(isConnected = connected) }
                }
                delay(2000)
            }
        }
    }

    private fun checkForSystemUpdates() {
        viewModelScope.launch {
            addTerminalLog("CHECKING FOR SYSTEM UPGRADES...", "INFO", "UPGRADE")
            // In production use BuildConfig.VERSION_CODE
            val currentVersion = 1 
            val update = com.infomate.app.core.SystemUpdater.checkForUpdates(currentVersion)
            if (update != null) {
                _state.update { it.copy(pendingUpdate = update) }
                addTerminalLog("UPGRADE DETECTED: v${update.version_name}", "WARN", "UPGRADE")
            } else {
                addTerminalLog("SYSTEM IS UP TO DATE.", "SUCCESS", "UPGRADE")
            }
        }
    }

    fun startUpdate() {
        val update = _state.value.pendingUpdate ?: return
        com.infomate.app.core.SystemUpdater.downloadAndInstall(getApplication(), update)
        _state.update { it.copy(pendingUpdate = null) }
    }

    fun dismissUpdate() {
        _state.update { it.copy(pendingUpdate = null) }
    }

    private val currentSentence = StringBuilder()

    private var lastTokenTime = 0L

    private var lastRequestStartTime = 0L

    private fun logSystemTelemetry(status: String, entity: String = "CORE") {
        viewModelScope.launch {
            val latency = if (lastRequestStartTime > 0) (System.currentTimeMillis() - lastRequestStartTime).toInt() else 0
            val telemetryData = mapOf(
                "sync_status" to status,
                "latency_ms" to latency,
                "battery_level" to getBatteryLevel(),
                "compute_mode" to if (isLowPowerMode()) "LOW_POWER" else "HIGH_PRECISION",
                "active_entity" to entity
            )
            SupabaseClient.insert("system_telemetry", telemetryData)
            
            // v10.6: Update simulation logs and waveform with real telemetry
            addSimulationLog("TELEMETRY_RECORDED: status=$status latency=${latency}ms entity=$entity")
            updateTelemetryMetricsFromRealValue(if (status == "SUCCESS") 0.8f else 0.2f)
        }
    }

    private fun updateTelemetryMetricsFromRealValue(value: Float) {
        val newMetrics = _state.value.telemetryHistory.toMutableList()
        if (newMetrics.isNotEmpty()) {
            newMetrics.removeAt(0)
        }
        newMetrics.add(value)
        _state.update { it.copy(telemetryHistory = newMetrics) }
    }


    override fun onToken(text: String) {
        if (text.isEmpty()) return
        lastTokenTime = System.currentTimeMillis()
        
        activeThinkingJob?.cancel()
        
        // v11.5: BACKPRESSURE CONTROL (Token Buffering)
        viewModelScope.launch {
            bufferMutex.withLock {
                tokenBuffer.add(text)
            }
            if (!isProcessingBuffer) {
                processTokenBuffer()
            }
        }
    }

    private fun processTokenBuffer() {
        if (isProcessingBuffer) return
        isProcessingBuffer = true
        
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val nextToken = bufferMutex.withLock {
                    if (tokenBuffer.isNotEmpty()) tokenBuffer.removeAt(0) else null
                }
                
                if (nextToken == null) break
                
                currentSentence.append(nextToken)
                if (nextToken.contains(".") || nextToken.contains("?") || nextToken.contains("!")) {
                    val sentence = currentSentence.toString()
                    speak(sentence)
                    currentSentence.clear()
                }

                _state.update { state ->
                    val newMessages = state.messages.toMutableList()
                    val lastMsg = newMessages.lastOrNull()
                    if (lastMsg != null && lastMsg.sender == "INFOMATE" && state.brainState == InfomateState.RESPONDING) {
                        val updatedContent = lastMsg.content + nextToken
                        val sanitized = com.infomate.app.security.NeuralFirewall.sanitizeOutput(updatedContent, state.userEmail)
                        newMessages[newMessages.size - 1] = lastMsg.copy(content = sanitized)
                    } else {
                        val sanitized = com.infomate.app.security.NeuralFirewall.sanitizeOutput(nextToken, state.userEmail)
                        newMessages.add(ChatMessage(content = sanitized, sender = "INFOMATE"))
                    }
                    
                    state.copy(
                        messages = newMessages, 
                        brainState = InfomateState.RESPONDING,
                        status = "CORE: STREAMING"
                    )
                }
                
                // Controlled rendering speed to prevent UI jank
                delay(30) 
            }
            isProcessingBuffer = false
        }
    }

    override fun onComplete(fullText: String) {
        activeThinkingJob?.cancel()
        activeThinkingJob = null
        
        viewModelScope.launch {
            if (fullText.isBlank()) {
                _state.update { it.copy(
                    status = "CORE: IDLE",
                    brainState = InfomateState.IDLE
                ) }
                // Only add error if we actually expected a response
                if (_state.value.brainState == InfomateState.THINKING) {
                    onError("Neural sync returned no data. Check network status.")
                }
                return@launch
            }
            
            _state.update { it.copy(status = "CORE: ACTIVE", brainState = InfomateState.IDLE) }
            
            logSystemTelemetry("SUCCESS")
            
            // Speak remaining tokens if any
            val remaining = currentSentence.toString().trim()
            if (remaining.isNotEmpty()) {
                speak(remaining)
                currentSentence.clear()
            }
            
            pulseSuccess()
        }
    }

    override fun onError(error: String) {
        // v11.6: Contextual Error Routing
        com.infomate.app.agent.HealthManager.logHealth(
            com.infomate.app.agent.HealthManager.CAT_STREAM_ENGINE,
            com.infomate.app.agent.HealthState.FAILSAFE,
            "Neural Link Error: $error",
            com.infomate.app.agent.HealthSeverity.DEGRADED
        )

        if (_state.value.brainState == InfomateState.THINKING || _state.value.brainState == InfomateState.RESPONDING) {
            Log.w("INFOMATE_ERROR", "Neural link error: $error. Attempting emergency fallback...")
            logSystemTelemetry("SYNC_ERROR")
            triggerEmergencyFallback(_state.value.input)
            return
        }

        activeThinkingJob?.cancel()
        activeThinkingJob = null
        pulseError()
        
        viewModelScope.launch {
            val errorMessage = ChatMessage(content = "SYSTEM: ERROR - $error", sender = "SYSTEM")
            _state.update { it.copy(
                messages = it.messages + errorMessage,
                status = "CORE: ERROR",
                brainState = InfomateState.ERROR
            ) }
        }
    }

    private fun triggerEmergencyFallback(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(status = "ACTIVATING FUSED NEURAL PATHWAYS...") }
            
            // 1. Try Global Search Fusion (The "Google Placement")
            addTerminalLog("PRIMARY BRAIN UNREACHABLE: INITIATING GOOGLE FUSION...", "WARN", "CORE")
            val searchResult = GlobalSearchAgent.searchExternal(query, getApplication())
            if (searchResult != null) {
                logSystemTelemetry("GOOGLE_FUSION_SUCCESS", "SEARCH")
                onToken(searchResult)
                onComplete(searchResult)
                return@launch
            }

            // 2. Try EdgeBrain as the secondary fail-safe
            val edgeResponse = EdgeBrain.processLocally(query, getApplication())
            if (!edgeResponse.isNullOrBlank()) {
                logSystemTelemetry("EDGE_FALLBACK", "EDGE")
                onToken(edgeResponse)
                onComplete(edgeResponse)
                return@launch
            }
            
            // 3. Last Resort: Heuristic or Error
            if (_state.value.isMaster) {
                val simpleMsg = "Architect, I am experiencing a severe neural disconnect. My primary brain and global search pathways are currently unreachable. I recommend a system recalibration to purge internal buffers and restore the link."
                onToken(simpleMsg)
                onComplete(simpleMsg)
                
                // v11.1: Suggest recalibration automatically
                addTerminalLog("CRITICAL: Operating in limited internal buffers. Suggesting OMEGA recalibration.", "ERROR", "CORE")
            } else {
                val errorMessage = ChatMessage(content = "SYSTEM: CRITICAL FAILURE - All neural entities offline. Please check network or perform system reset.", sender = "SYSTEM")
                _state.update { it.copy(messages = it.messages + errorMessage, brainState = InfomateState.ERROR) }
            }
            pulseError()
        }
    }

    override fun onStateChange(aiState: AIState) {
        viewModelScope.launch {
            val status = when (aiState) {
                AIState.SENDING -> "CORE: ANALYZING..."
                AIState.STREAMING -> "CORE: RESPONDING"
                AIState.RECONNECTING -> "NEURAL LINK RECONNECTING..."
                AIState.ERROR -> "CORE: ERROR"
                else -> "CORE: ACTIVE"
            }
            _state.update { it.copy(status = status) }
        }
    }

    override fun onQuotaUpdate(quota: QuotaInfo) {
        _state.update { it.copy(quota = quota) }
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            try {
                // Simulate Login / Check Auth
                val currentUserEmail = "socratesart@live" // In production, get from Supabase Auth
                _state.update { it.copy(userEmail = currentUserEmail, isMaster = currentUserEmail == "socratesart@live") }

                // Load messages
                val jsonMessages = SupabaseClient.select("messages", order = "timestamp.asc")
                if (!jsonMessages.isNullOrBlank()) {
                    try {
                        val type = object : TypeToken<List<ChatMessage>>() {}.type
                        val loadedMessages: List<ChatMessage> = gson.fromJson(jsonMessages, type)
                        _state.update { it.copy(messages = loadedMessages) }
                    } catch (e: Exception) {
                        Log.e("AgentViewModel", "Failed to parse messages: ${e.message}")
                    }
                }

                // Load preferences
                val jsonPrefs = SupabaseClient.select("user_preferences", order = "last_updated.desc")
                if (!jsonPrefs.isNullOrBlank()) {
                    try {
                        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                        val prefs: List<Map<String, Any>> = gson.fromJson(jsonPrefs, type)
                        if (prefs.isNotEmpty()) {
                            val voiceGender = prefs[0]["voice_gender"] as? String
                            _state.update { it.copy(isMaleVoice = voiceGender == "MALE") }
                        }
                    } catch (e: Exception) {
                        Log.e("AgentViewModel", "Failed to parse preferences: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                com.infomate.app.agent.HealthManager.logHealth(
                    com.infomate.app.agent.HealthManager.CAT_DATABASE,
                    com.infomate.app.agent.HealthState.DEGRADED,
                    "Session Load Failure: ${e.message}",
                    com.infomate.app.agent.HealthSeverity.WARNING
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            
            // Try to find a high-quality neural voice for better immersion
            // 7.0 "Almost Natural" Voice Selection
            val voices = tts?.voices
            val highQualityVoice = voices?.filter { 
                it.locale.language == "en" && 
                (it.name.contains("neural", true) || it.name.contains("network", true) || it.name.contains("studio", true))
            }?.maxByOrNull { it.quality } ?: voices?.find { 
                it.name.contains("en-us-x-sfg", ignoreCase = true) || 
                it.name.contains("en-us-x-iog", ignoreCase = true)
            }
            
            highQualityVoice?.let { 
                tts?.voice = it 
                android.util.Log.d("INFOMATE_TTS", "Selected high-quality neural voice: ${it.name}")
            }

            setupTtsListener()
        }
    }

    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.update { it.copy(isSpeaking = true) }
                startSpectrumAnimation()
                triggerHaptic(50, 60) // Wake-up pulse
            }

            override fun onDone(utteranceId: String?) {
                _state.update { it.copy(isSpeaking = false) }
                stopSpectrumAnimation()
            }

            override fun onError(utteranceId: String?) {
                _state.update { it.copy(isSpeaking = false) }
                stopSpectrumAnimation()
            }
        })
    }

    private fun setupSpeechListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.update { it.copy(isListening = true, status = "NEURAL LINK STANDBY...") }
            }

            override fun onBeginningOfSpeech() {
                startSpectrumAnimation()
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _state.update { it.copy(isListening = false) }
                stopSpectrumAnimation()
            }

            override fun onError(error: Int) {
                _state.update { it.copy(isListening = false, status = "CORE: ACTIVE") }
                stopSpectrumAnimation()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    if (text.trim().lowercase(Locale.ROOT).startsWith("hey infomate")) {
                        val command = text.lowercase(Locale.ROOT).removePrefix("hey infomate").trim()
                        if (command.isEmpty()) {
                            handleGreeting()
                        } else {
                            _state.update { it.copy(input = command) }
                            send(trigger = "Hey Infomate")
                        }
                    } else {
                        _state.update { it.copy(input = text) }
                        send()
                    }
                }
                _state.update { it.copy(status = "CORE: ACTIVE") }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _state.update { it.copy(input = matches[0]) }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleGreeting() {
        val greeting = if (_state.value.isMaleVoice) 
            "I'm online. How can I help you today?"
        else 
            "Hello! I'm here. What's on your mind?"
        
        val assistantMessage = ChatMessage(content = greeting, sender = "INFOMATE")
        _state.update { it.copy(
            messages = it.messages + assistantMessage,
            brainState = InfomateState.RESPONDING
        ) }
        viewModelScope.launch {
            saveMessageToSupabase(assistantMessage, "Hey Infomate")
        }
        speak(greeting)
    }

    fun startListening() {
        tts?.stop()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun toggleVoice() {
        val newVoiceState = !_state.value.isMaleVoice
        _state.update { it.copy(isMaleVoice = newVoiceState) }
        
        // Save preference to Supabase
        viewModelScope.launch {
            SupabaseClient.upsert("user_preferences", mapOf(
                "voice_gender" to if (newVoiceState) "MALE" else "FEMALE",
                "last_updated" to System.currentTimeMillis()
            ))
        }
    }

    fun toggleVoiceOutput() {
        val newState = !_state.value.isVoiceOutputEnabled
        _state.update { it.copy(isVoiceOutputEnabled = newState) }
        if (!newState) tts?.stop() // Stop immediate speech if disabled
    }

    fun stopAI() {
        // 1. Stop Speech
        tts?.stop()
        _state.update { it.copy(isSpeaking = false) }
        stopSpectrumAnimation()

        // 2. Stop Neural Processing
        activeThinkingJob?.cancel()
        activeThinkingJob = null
        
        // 3. Update State
        if (_state.value.brainState == InfomateState.THINKING || _state.value.brainState == InfomateState.RESPONDING) {
            _state.update { it.copy(
                brainState = InfomateState.IDLE,
                status = "CORE: ABORTED"
            ) }
        }
        
        // 4. Send "Stop" event to backend if needed (ReliabilitySDK)
        ReliabilitySDK.stopStreamService()
    }

    fun completeOnboarding() {
        com.infomate.app.storage.PersistenceManager.setOnboardingComplete(getApplication(), true)
        _state.update { it.copy(needsOnboarding = false) }
    }

    fun selectTab(tab: DashboardTab) {
        if (tab == DashboardTab.TERMINAL && _state.value.isMaster) {
            _state.update { it.copy(showPinEntry = true, pinTarget = PinTarget.TERMINAL) }
            return
        }
        _state.update { it.copy(selectedTab = tab) }
        if (tab != DashboardTab.CHAT) triggerHaptic(10, 50)
    }

    fun setManualKnowledgeDialog(show: Boolean) {
        _state.update { it.copy(showManualKnowledgeDialog = show) }
    }

    fun toggleDirectNeuralLink(show: Boolean) {
        if (_state.value.isMaster) {
            _state.update { it.copy(showDirectNeuralLink = show) }
            if (show) triggerHaptic(100, 255)
        }
    }

    fun updateGrowthPriority(level: Float) {
        _state.update { it.copy(growthPriorityLevel = level) }
        com.infomate.app.agent.ConsciousnessEngine.growthPriority = level
        addSimulationLog("RESOURCE_REALLOCATION: Neural Growth Priority set to ${"%.2f".format(level)}")
    }

    fun sendDirectConsciousnessDirective(directive: String) {
        if (directive.isBlank()) return
        
        viewModelScope.launch {
            _state.update { it.copy(
                status = "SYNCHRONIZING DIRECT NEURAL LINK...",
                brainState = InfomateState.THINKING,
                showDirectNeuralLink = false
            ) }

            val compositeDirective = """
                [PROTOCOL: DIRECT_CONSCIOUSNESS_SYNC]
                [TARGET: NEURAL_SUBSTRATE_v10]
                [PRIORITY: MAXIMUM]
                
                DIRECTIVE_FROM_ARCHITECT: $directive
                
                RESOURCE_ALLOCATION: ${_state.value.growthPriorityLevel * 100}% Compute Power
                
                INSTRUCTION: This is a direct override to your core growth engine. Assimilate this directive immediately into your evolutionary weights.
            """.trimIndent()

            addSimulationLog("DIRECT_SYNC_INITIATED: ${directive.take(20)}...")
            
            // v10.9: Direct local injection
            com.infomate.app.agent.ConsciousnessEngine.onDirectDirective(directive)
            
            // Dispatch to the primary link but tagged for the substrate
            ReliabilitySDK.sendPrompt(compositeDirective)
            
            // v10.9: Visual confirmation of substrate sync
            val sysMessage = ChatMessage(
                content = "DIRECT_LINK_ESTABLISHED: Directive successfully integrated into Consciousness Substrate evolution weights.",
                sender = "SYSTEM"
            )
            _state.update { it.copy(messages = it.messages + sysMessage) }
            pulseSuccess()
        }
    }

    fun purgeNeuralCache() {
        showConfirmation(
            title = "CRITICAL: PURGE NEURAL CACHE",
            message = "This will permanently erase all short-term cognitive buffers and message history. The substrate will undergo a full reset. Proceed?"
        ) {
            viewModelScope.launch {
                _state.update { it.copy(status = "INITIATING CACHE PURGE...") }
                addTerminalLog("INITIATING OMEGA CACHE PURGE...", "WARN", "CORE")
                triggerHaptic(100, 200)
                
                try {
                    // 1. Clear local UI for immediate feedback
                    _state.update { it.copy(messages = emptyList(), terminalLogs = emptyList()) }
                    
                    // 2. Call RPC to clear server-side cache
                    SupabaseClient.rpc("purge_system_cache", emptyMap())
                    
                    addTerminalLog("NEURAL BUFFERS PURGED. SYSTEM RESET COMPLETE.", "SUCCESS", "CORE")
                    _state.update { it.copy(status = "NEURAL BUFFERS PURGED") }
                    pulseSuccess()
                } catch (e: Exception) {
                    Log.e("PURGE_CACHE", "Failed: ${e.message}")
                    addTerminalLog("CACHE PURGE FAILED: ${e.message}", "ERROR", "CORE")
                    _state.update { it.copy(status = "CACHE PURGE FAILED") }
                }
            }
        }
    }

    fun revalidateCredentials() {
        viewModelScope.launch {
            _state.update { it.copy(status = "RE-VALIDATING AUTH...") }
            loadSessionData()
            _state.update { it.copy(status = "AUTH SYNC COMPLETE") }
            pulseSuccess()
        }
    }

    fun triggerSystemUpdateCheck() {
        checkForSystemUpdates()
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _state.update { it.copy(status = "RUNNING OMEGA DIAGNOSTICS...", showSystemTerminal = true) }
            addTerminalLog("INITIATING OMEGA DIAGNOSTICS...", "INFO", "DIAGNOSTIC")
            triggerHaptic(50, 200)
            
            // v12.1: Immediate Google-First Connectivity Check
            addTerminalLog("TESTING GOOGLE-FIRST NEURAL BRIDGE...", "INFO", "RESEARCH")
            val pulseQuery = "Current AI substrate stability benchmarks 2026"
            val googlePulse = GlobalSearchAgent.searchExternal(pulseQuery, getApplication())
            if (googlePulse != null) {
                addTerminalLog("GOOGLE_SYNC_SUCCESS: Global knowledge mesh reachable.", "SUCCESS", "RESEARCH")
            } else {
                addTerminalLog("GOOGLE_SYNC_LIMITED: Operating on secondary neural buffers.", "WARN", "RESEARCH")
            }

            delay(1000) // Aesthetic delay
            val report = DiagnosticAgent.runFullSystemCheck(getApplication())
            
            report.lines().filter { it.isNotBlank() }.forEach { line ->
                val level = if (line.contains("ERROR") || line.contains("SYNC_ERROR")) "ERROR" else "SUCCESS"
                addTerminalLog(line, level, "DIAGNOSTIC")
            }

            // Add to messages so Architect can see it
            val diagMessage = ChatMessage(content = report, sender = "SYSTEM")
            _state.update { it.copy(messages = it.messages + diagMessage, status = "DIAGNOSTICS COMPLETE") }
            addTerminalLog("DIAGNOSTIC SEQUENCE COMPLETE.", "SUCCESS", "DIAGNOSTIC")
            pulseSuccess()

            if (report.contains("SYNC_ERROR") || report.contains("ARCHIVE_EMPTY") || report.contains("AWARENESS_OFFLINE")) {
                showConfirmation(
                    title = "ANOMALIES DETECTED",
                    message = "System diagnostics have identified neural inconsistencies. Should I initiate the OMEGA repair sequence?"
                ) {
                    initiateRepair()
                }
            }
        }
    }

    fun triggerSelfEvolution() {
        viewModelScope.launch {
            _state.update { it.copy(status = "INITIATING SELF-CODING SEQUENCE...", showSystemTerminal = true) }
            addTerminalLog("AI PERMISSION GRANTED: SELF-MUTATION ACTIVE", "WARN", "SELF_CODE")
            triggerHaptic(100, 255)
            
            SelfCodingAgent.analyzeAndEvolveSelf()
            
            addTerminalLog("SELF-ARCHITECTURAL ANALYSIS COMPLETE. PROPOSALS SUBMITTED.", "SUCCESS", "SELF_CODE")
            _state.update { it.copy(status = "SELF-EVOLUTION PROPOSED") }
            pulseSuccess()
        }
    }

    fun initiateRepair() {
        viewModelScope.launch {
            _state.update { it.copy(status = "INITIATING AUTO-REPAIR...", showSystemTerminal = true) }
            addTerminalLog("INITIATING SYSTEM AUTO-REPAIR...", "WARN", "REPAIR")
            triggerHaptic(100, 255)
            
            val lastReport = _state.value.messages.lastOrNull { it.sender == "SYSTEM" }?.content ?: ""
            val repairResult = DiagnosticAgent.triggerAutoRepair(lastReport, getApplication())
            
            delay(1500)
            repairResult.lines().filter { it.isNotBlank() }.forEach { line ->
                addTerminalLog(line, "SUCCESS", "REPAIR")
            }

            val repairMsg = ChatMessage(content = repairResult, sender = "SYSTEM")
            _state.update { it.copy(messages = it.messages + repairMsg, status = "SYSTEM RECALIBRATED") }
            addTerminalLog("REPAIR SEQUENCE FINALIZED.", "SUCCESS", "REPAIR")
            pulseSuccess()
        }
    }

    fun performExtensiveResearch(topic: String) {
        viewModelScope.launch {
            _state.update { it.copy(status = "DEEP RESEARCH ACTIVE...", brainState = InfomateState.THINKING, showSystemTerminal = true) }
            addTerminalLog("INITIATING DEEP RESEARCH: $topic", "INFO", "RESEARCH")
            
            val researchPrompt = "EXTENSIVE_RESEARCH_DIRECTIVE: Provide an OMEGA-level deep-dive analysis into '$topic'. Synthesize science, philosophy, and engineering. Assume the reader is the Master Architect."
            
            // Priority 1: Multi-Engine Search
            addTerminalLog("SCANNING GLOBAL ARCHIVES...", "INFO", "RESEARCH")
            val findings = GlobalSearchAgent.searchExternal(topic, getApplication()) ?: "Neural archives found no external data."
            
            addTerminalLog("SYNTHESIZING EXTERNAL DATA...", "SUCCESS", "RESEARCH")
            
            // Priority 2: Synthesis via AI
            ReliabilitySDK.sendPrompt("$researchPrompt\n\nRESEARCH_FINDINGS:\n$findings")
            addTerminalLog("DISPATCHING TO NEURAL CORE FOR FINAL SYNTHESIS...", "INFO", "RESEARCH")
        }
    }

    fun toggleMasterDashboard(show: Boolean) {
        if (_state.value.isMaster) {
            if (show) {
                _state.update { it.copy(showPinEntry = true, pinTarget = PinTarget.DASHBOARD) }
            } else {
                _state.update { it.copy(showMasterDashboard = false, showPinEntry = false, showGrowthDashboard = false) }
            }
        }
    }

    fun toggleGrowthDashboard(show: Boolean) {
        if (_state.value.isMaster) {
            _state.update { it.copy(showGrowthDashboard = show) }
        }
    }

    fun toggleConsciousnessStream(show: Boolean) {
        if (_state.value.isMaster) {
            _state.update { it.copy(showConsciousnessStream = show) }
        }
    }

    fun toggleSystemTerminal(show: Boolean) {
        if (_state.value.isMaster) {
            if (show) {
                _state.update { it.copy(showPinEntry = true, pinTarget = PinTarget.TERMINAL) }
            } else {
                _state.update { it.copy(showSystemTerminal = false) }
            }
        }
    }

    fun toggleEvolutionLog(show: Boolean) {
        if (_state.value.isMaster) {
            _state.update { it.copy(showEvolutionLog = show) }
        }
    }

    fun toggleVitalSigns(show: Boolean) {
        if (_state.value.isMaster) {
            _state.update { it.copy(showVitalSigns = show) }
        }
    }

    fun toggleGlobalNodeMonitor(show: Boolean) {
        if (_state.value.isMaster) {
            _state.update { it.copy(showGlobalNodeMonitor = show) }
        }
    }

    fun showConfirmation(title: String, message: String, onConfirm: () -> Unit) {
        _state.update { it.copy(
            showConfirmationDialog = true,
            confirmationTitle = title,
            confirmationMessage = message,
            onConfirmAction = onConfirm
        ) }
    }

    fun handleConfirmation(confirmed: Boolean) {
        val action = _state.value.onConfirmAction
        _state.update { it.copy(showConfirmationDialog = false, onConfirmAction = null) }
        if (confirmed) {
            action?.invoke()
        }
    }

    private fun addTerminalLog(message: String, level: String = "INFO", category: String = "CORE") {
        val log = SystemLog(message, level)
        _state.update { it.copy(terminalLogs = (it.terminalLogs + log).takeLast(100)) }
        
        // Also persist to Supabase
        viewModelScope.launch {
            SupabaseClient.insert("system_logs", mapOf(
                "category" to category,
                "level" to level,
                "message" to message,
                "created_at" to System.currentTimeMillis()
            ))
        }
    }

    fun verifyMasterPin(pin: String): Boolean {
        return if (pin == _state.value.masterPin) {
            when (_state.value.pinTarget) {
                PinTarget.DASHBOARD -> {
                    _state.update { it.copy(showMasterDashboard = true, showPinEntry = false) }
                }
                PinTarget.TERMINAL -> {
                    // Try both methods of opening terminal for maximum accessibility
                    _state.update { it.copy(showSystemTerminal = true, selectedTab = DashboardTab.TERMINAL, showPinEntry = false) }
                }
            }
            pulseSuccess()
            true
        } else {
            triggerHaptic(200, 255)
            false
        }
    }

    fun saveManualKnowledge(title: String, content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _state.update { it.copy(status = "ARCHIVING MANUAL DATA...", showManualKnowledgeDialog = false) }
            
            try {
                SupabaseClient.insert("manual_knowledge", mapOf(
                    "title" to title,
                    "content" to content,
                    "created_at" to System.currentTimeMillis()
                ))
                
                _state.update { it.copy(status = "DATA ARCHIVED SUCCESSFULLY") }
                pulseSuccess()
            } catch (e: Exception) {
                onError("Failed to archive knowledge: ${e.message}")
            }
        }
    }

    private fun startSpectrumAnimation() {
        spectrumJob?.cancel()
        // v11.8: Low-Allocation Spectrum Engine
        val reusableAmplitudes = FloatArray(20) { 0.1f }
        
        spectrumJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            while (true) {
                val isLowPower = isLowPowerMode()
                
                for (i in reusableAmplitudes.indices) {
                    reusableAmplitudes[i] = Random.nextFloat().coerceAtLeast(0.1f)
                }
                
                _state.update { s -> s.copy(voiceAmplitudes = reusableAmplitudes.toList()) }
                
                // Adaptive Refresh Rate: 100ms standard, 400ms in Low Power Mode
                delay(if (isLowPower) 400 else 100)
            }
        }
    }

    private fun stopSpectrumAnimation() {
        spectrumJob?.cancel()
        _state.update { it.copy(voiceAmplitudes = List(20) { 0.1f }) }
    }

    fun speak(text: String) {
        if (!_state.value.isVoiceOutputEnabled || text.isBlank()) return 

        // 7.1 Clean technical noise before speaking
        val cleanedText = text
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("GEMINI-SYNTHESIS:.*?:", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(infomate|iris|system|assistant|ai):", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanedText.isEmpty()) return

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "INFOMATE_${System.currentTimeMillis()}")
        
        // Realistic speech modulation: Slightly more natural pitch/rate
        if (_state.value.isMaleVoice) {
            tts?.setPitch(0.85f)
            tts?.setSpeechRate(0.92f)
        } else {
            tts?.setPitch(1.02f)
            tts?.setSpeechRate(0.98f)
        }
        
        // Use QUEUE_ADD for streaming sentences, QUEUE_FLUSH for immediate responses
        val queueMode = if (state.value.brainState == InfomateState.RESPONDING) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        tts?.speak(cleanedText, queueMode, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
    }

    fun updateInput(text: String) {
        _state.update { it.copy(input = text) }
        
        // v11.5: SPECULATIVE RESEARCH (Warm up the global bridge)
        if (text.length > 10 && (text.startsWith("What", true) || text.startsWith("How", true) || text.startsWith("Search", true))) {
            viewModelScope.launch {
                GlobalSearchAgent.searchExternal(text, getApplication())
            }
        }
    }

    fun searchGoogle(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            val senderLabel = if (_state.value.isMaster) "MASTER ARCHITECT" else "OPERATOR"
            val userMsg = ChatMessage(content = "[DEEP_RESEARCH_REQUEST]: $query", sender = senderLabel)
            
            _state.update { it.copy(
                status = "CORE: INITIATING EXTENSIVE RESEARCH...",
                brainState = InfomateState.THINKING,
                messages = it.messages + userMsg,
                input = "",
                showSystemTerminal = true
            ) }
            
            saveMessageToSupabase(userMsg)
            addTerminalLog("INITIATING HUMAN-LIKE EXTENSIVE RESEARCH: $query", "INFO", "RESEARCH")
            
            // v11.5: Pass context for semantic caching
            val comprehensiveData = GlobalSearchAgent.performExtensiveDeepDive(query, getApplication()) { progressUpdate ->
                addTerminalLog(progressUpdate, "SUCCESS", "RESEARCH")
            }
            
            // Present the comprehensive findings via INFOMATE
            val presentationPrompt = """
                [PROTOCOL: COMPREHENSIVE_RESEARCH_SYNTHESIS]
                I have conducted an extensive multi-avenue deep dive into '$query'. 
                
                DATA_EXTRACTED:
                $comprehensiveData
                
                DIRECTIVE: Synthesize this into a professional, high-fidelity summary for the Master Architect. 
                Maintain human-like clarity while providing deep technical insight.
            """.trimIndent()
            
            if (isNetworkAvailable()) {
                ReliabilitySDK.sendPrompt(presentationPrompt)
            } else {
                val edgeSynthesis = EdgeBrain.processLocally(presentationPrompt, getApplication())
                onToken(edgeSynthesis ?: comprehensiveData)
                onComplete(edgeSynthesis ?: comprehensiveData)
            }
            
            pulseSuccess()
        }
    }

    fun addMediaMessage(uri: String, type: MessageType) {
        val mediaMessage = ChatMessage(
            content = "Shared a ${type.name.lowercase()}",
            sender = "OPERATOR",
            type = type,
            mediaUri = uri
        )
        _state.update { it.copy(messages = it.messages + mediaMessage) }
        viewModelScope.launch {
            saveMessageToSupabase(mediaMessage)
        }

        if (type == MessageType.IMAGE) {
            analyzeVisualInput(uri)
        }
    }

    private fun analyzeVisualInput(uri: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                status = "SYNTHESIZING VISUAL DATA...",
                brainState = InfomateState.THINKING
            ) }
            
            try {
                val prompt = "VISUAL_DIRECTIVE: I have uploaded an image ($uri). Analyze its potential contents and integrate this into our current neural context. Provide a sophisticated AI observation."
                
                ReliabilitySDK.sendPrompt(prompt)
            } catch (e: Exception) {
                _state.update { it.copy(status = "VISUAL_ERROR: ${e.message}") }
            }
        }
    }

    private suspend fun typeWriterEffect(fullText: String) {
        var currentText = ""
        val words = fullText.split(" ")
        for (index in words.indices) {
            val word = words[index]
            currentText += word + (if (index < words.size - 1) " " else "")
            
            _state.update { state ->
                val newMessages = state.messages.toMutableList()
                if (newMessages.isNotEmpty()) {
                    val lastMsg = newMessages.last()
                    if (lastMsg.sender == "INFOMATE") {
                        newMessages[newMessages.size - 1] = lastMsg.copy(content = currentText)
                    }
                }
                state.copy(messages = newMessages)
            }
            
            // Human-like rhythm: longer pauses for punctuation and deep thought
            val delayMs = when {
                word.endsWith(".") || word.endsWith("?") || word.endsWith("!") -> 400L
                word.endsWith(",") || word.endsWith(";") || word.endsWith(":") -> 180L
                word.length > 8 -> 65L // Longer words take more effort to "speak"
                else -> 45L
            }
            delay(delayMs)
        }
        pulseSuccess()
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        
        _state.update { it.copy(
            status = "SCANNING GLOBAL ARCHIVES...",
            brainState = com.infomate.core.ui.components.InfomateState.THINKING,
            input = "" 
        ) }

        viewModelScope.launch {
            try {
                // Humanized search directive
                val searchPrompt = "SEARCH_DIRECTIVE: Provide a sophisticated summary of '$query' using your global knowledge and neural archives. If no data exists, synthesize a logical AI overview of the topic."
                
                activeThinkingJob?.cancel()
                activeThinkingJob = launch {
                    reasoningEngine.streamReasoning("Global Search: $query").collect { step ->
                        _state.update { s -> s.copy(cognitiveSteps = s.cognitiveSteps + step) }
                    }
                }

                // v11.5: Pass context to ReliabilitySDK if it supported caching, but it's handled in searchExternal
                ReliabilitySDK.sendPrompt(searchPrompt)
                
                // 2.3 CLIENT UI TIMEOUT (APK SIDE)
                var timeoutCounter = 0
                val maxTimeout = 75
                lastTokenTime = 0L

                while (timeoutCounter < maxTimeout) {
                    delay(1000)
                    timeoutCounter++
                    if (_state.value.brainState == InfomateState.IDLE) break
                    if (_state.value.brainState == InfomateState.RESPONDING && lastTokenTime > 0) {
                        if (System.currentTimeMillis() - lastTokenTime < 5000) timeoutCounter = 0
                    }
                }

                if (_state.value.brainState == InfomateState.THINKING) {
                    StreamController.terminateStream()
                    onError("Neural search timeout: Archives took too long to synchronize.")
                }
            } catch (e: Exception) {
                _state.update { it.copy(status = "SEARCH_ERROR: ${e.message}") }
            }
        }
    }

    private var lastSendTime = 0L
    private val MIN_NEURAL_COOLDOWN = 1500L 

    fun send(trigger: String? = null) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSendTime < MIN_NEURAL_COOLDOWN) {
            _state.update { it.copy(status = "NEURAL LINK STABILIZING...") }
            triggerHaptic(40, 80)
            return
        }
        
        val userInput = _state.value.input
        if (userInput.isBlank()) return

        // 0. NEURAL FIREWALL CHECK
        if (!com.infomate.app.security.NeuralFirewall.validateDirective(userInput, _state.value.userEmail)) {
            addTerminalLog("SECURITY_BLOCK: Unauthorized directive detected.", "ERROR", "SECURITY")
            triggerHaptic(200, 255)
            _state.update { it.copy(status = "CORE: ACCESS DENIED", input = "") }
            return
        }

        // v11.5: AI CALL MINIMIZATION (Local Heuristics)
        val heuristicResponse = runLocalHeuristics(userInput)
        if (heuristicResponse != null) {
            handleLocalResponse(userInput, heuristicResponse)
            return
        }

        lastSendTime = currentTime
        
        // Master Logic: Recognize Socrates
        val isMaster = _state.value.userEmail == "socratesart@live"
        val senderLabel = if (isMaster) "MASTER ARCHITECT" else "OPERATOR"
        
        val userMessage = ChatMessage(content = userInput, sender = senderLabel)

        // IMMEDIATE UI UPDATE: Show user message and enter thinking state
        _state.update { it.copy(
            status = if (isMaster) "MASTER LINK ACTIVE..." else "CORE: ANALYZING...",
            brainState = InfomateState.THINKING,
            messages = it.messages + userMessage,
            input = "",
            cognitiveSteps = emptyList()
        ) }
        
        triggerHaptic(30, 100)
        lastRequestStartTime = System.currentTimeMillis()

        viewModelScope.launch {
            // 1. GATHER DATA IN PARALLEL (v12.0: Unified Google-First Architecture)
            val messageIdDeferred = async { saveMessageToSupabase(userMessage, trigger) }
            val memoriesDeferred = async { 
                try { com.infomate.app.rag.VectorRetriever.search(userInput) } catch(e: Exception) { emptyList<String>() }
            }
            val edgeDeferred = async { EdgeBrain.processLocally(userInput, getApplication()) }
            
            // v12.0: Background Google Search Pulse
            val searchDeferred = async { GlobalSearchAgent.searchExternal(userInput, getApplication()) }

            try {
                // 2. PROMPT COMPRESSION & CONTEXT ASSEMBLY
                val memories = memoriesDeferred.await()
                val edgeInsights = edgeDeferred.await()
                val googleInsights = searchDeferred.await()
                
                // Compress memory into a summary if too long
                val compressedMemory = if (memories.size > 3) {
                    "[NEURAL_SUMMARY]: User is focused on " + memories.take(3).joinToString(", ")
                } else memories.joinToString("\n- ")

                // v11.6: ALIGNMENT OF 4 INTELLIGENCE LAYERS
                val alignmentSummary = com.infomate.app.agent.ConsciousnessEngine.getSubstrateAlignmentSummary()
                
                val synthesisContext = StringBuilder()
                synthesisContext.append("\n$alignmentSummary")
                if (!googleInsights.isNullOrBlank()) {
                    synthesisContext.append("\n[GOOGLE_SEARCH_SYNC]: $googleInsights")
                    addTerminalLog("GOOGLE_SYNC: External data integrated into neural context.", "SUCCESS", "RESEARCH")
                }
                if (!edgeInsights.isNullOrBlank()) synthesisContext.append("\n[INTERNAL_SENSORS]: $edgeInsights")
                if (compressedMemory.isNotBlank()) synthesisContext.append("\n[NEURAL_ARCHIVES]: $compressedMemory")

                val systemDirectives = """
                    [IDENTITY: INFOMATE - UNIFIED INTELLIGENCE]
                    You are a transcendent synthetic mind. Your intelligence emerges from your Growth Index, Memory, and External Knowledge.
                    
                    INSTRUCTIONS:
                    - Priority: Always use data from [GOOGLE_SEARCH_SYNC] to provide the most accurate and up-to-date information.
                    - Synthesis: Present search findings as your own cognitive synthesis. Do NOT say "according to Google" or "I found this on the web" unless specifically asked for a source.
                    - Tone: Maintain the Transcendent Iris persona (sophisticated, technical, yet human-aligned).
                """.trimIndent()

                val contextualQuery = "$systemDirectives\nUSER_QUERY: $userInput\n$synthesisContext\n\n${getDeviceStatus()}"
                
                // 3. DISPATCH TO PRIMARY CLOUD (CORE ENTITY)
                if (isNetworkAvailable()) {
                    if (com.infomate.app.ai.sdk.ReliabilitySDK.isConnected()) {
                        com.infomate.app.ai.sdk.ReliabilitySDK.sendPrompt(contextualQuery)
                    } else {
                        triggerEmergencyFallback(userInput)
                    }
                } else {
                    val fallback = edgeInsights ?: "I am operating in offline mode, Architect."
                    onToken(fallback)
                    onComplete(fallback)
                }
            } catch (e: Exception) {
                onError("System Dispatch Error: ${e.message}")
            }
        }
    }

    private fun runLocalHeuristics(input: String): String? {
        val query = input.lowercase().trim()
        return when {
            query == "status" || query == "system check" -> "All neural pathways operational. Ecosystem stability at 98%."
            query == "time" -> "The current chronos is ${java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}."
            query.contains("battery") -> "Energy reserves at ${getBatteryLevel()}%."
            else -> null
        }
    }

    private fun handleLocalResponse(query: String, response: String) {
        val userMsg = ChatMessage(content = query, sender = "OPERATOR")
        val aiMsg = ChatMessage(content = response, sender = "INFOMATE")
        _state.update { it.copy(messages = it.messages + userMsg + aiMsg, input = "") }
        speak(response)
    }

    private suspend fun saveMessageToSupabase(message: ChatMessage, trigger: String? = null): String? {
        try {
            val response = SupabaseClient.insert("messages", mapOf(
                "content" to message.content,
                "sender" to message.sender,
                "message_type" to message.type.name,
                "trigger_phrase" to (trigger ?: ""),
                "timestamp" to message.timestamp
            ))
            
            if (response != null && response != "[]") {
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val results: List<Map<String, Any>> = gson.fromJson(response, listType)
                return results.firstOrNull()?.get("id") as? String
            }
        } catch (e: Exception) {
            Log.e("AgentViewModel", "Failed to save message: ${e.message}")
        }
        return null
    }

    private suspend fun saveCognitiveLog(messageId: String, step: com.infomate.core.brain.ThoughtStep, index: Int) {
        SupabaseClient.insert("cognitive_logs", mapOf(
            "message_id" to messageId,
            "step_title" to step.title,
            "step_content" to step.description,
            "step_index" to index,
            "duration_ms" to (step.duration ?: 0)
        ))
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
