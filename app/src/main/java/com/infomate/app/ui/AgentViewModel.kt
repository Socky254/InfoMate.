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
import com.infomate.app.ai.LLMClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.*
import kotlin.random.Random

class AgentViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener, AIEventsListener {

    private val sessionId = java.util.UUID.randomUUID().toString()
    private val orchestrator = AgentOrchestrator(application)
    private val reasoningEngine = ReasoningEngine()
    private val neuralIngestor = NeuralIngestor(application)
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var spectrumJob: Job? = null
    private val gson = Gson()
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

    // High-fidelity patterns for specific neural states
    private fun pulseSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 10, 50, 10), intArrayOf(0, 100, 0, 255), -1)
            vibrator.vibrate(effect)
        }
    }

    private fun pulseNeuralThought() {
        triggerHaptic(5, 30) // Subtle tick
    }

    private fun getDeviceStatus(): String {
        val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val time = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return "[SYSTEM_CONTEXT: Battery $level%, Time $time, ComputeProfile: HIGH_PRECISION]"
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        val hasInternet = when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        
        // v10.6: Capability check for actual internet reachability
        return hasInternet && activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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
        
        // Initialize Reliability SDK with Context
        UIRenderer.setListener(this)
        StreamController.init(application)
        ReliabilitySDK.init(application, "${Config.SUPABASE_URL.replace("https", "wss")}/realtime/v1/websocket?apikey=${Config.SUPABASE_KEY}")

        checkForSystemUpdates()
        startConnectionPolling()
        startNeuralEvolutionMonitoring()
        ConsciousnessEngine.awaken()
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
                    syntheticPersonalityLevel = metrics["personalityLevel"] as Int
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
        newMetrics.removeAt(0)
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
            val findings = GlobalSearchAgent.searchExternal("Advanced synthesis and future trends of $topic")
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
            // In production use BuildConfig.VERSION_CODE
            val currentVersion = 1 
            val update = com.infomate.app.core.SystemUpdater.checkForUpdates(currentVersion)
            if (update != null) {
                _state.update { it.copy(pendingUpdate = update) }
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

    override fun onToken(text: String) {
        if (text.isEmpty()) return
        lastTokenTime = System.currentTimeMillis()
        
        activeThinkingJob?.cancel()
        
        currentSentence.append(text)
        if (text.contains(".") || text.contains("?") || text.contains("!")) {
            val sentence = currentSentence.toString()
            speak(sentence)
            currentSentence.clear()
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            _state.update { state ->
                val newMessages = state.messages.toMutableList()
                
                val lastMsg = newMessages.lastOrNull()
                // Unified Persona: Always identify as INFOMATE
                if (lastMsg != null && lastMsg.sender == "INFOMATE" && state.brainState == InfomateState.RESPONDING) {
                    val updatedContent = lastMsg.content + text
                    newMessages[newMessages.size - 1] = lastMsg.copy(content = updatedContent)
                } else {
                    newMessages.add(ChatMessage(content = text, sender = "INFOMATE"))
                }
                
                state.copy(
                    messages = newMessages, 
                    brainState = InfomateState.RESPONDING,
                    status = "CORE: STREAMING"
                )
            }
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
        // If we have an active thinking job or are waiting for a response, 
        // try to trigger a fallback instead of showing a raw error.
        if (_state.value.brainState == InfomateState.THINKING) {
            Log.w("INFOMATE_ERROR", "Neural link error: $error. Attempting emergency fallback...")
            triggerEmergencyFallback(_state.value.input)
            return
        }

        activeThinkingJob?.cancel()
        activeThinkingJob = null
        
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
            _state.update { it.copy(status = "RE-ROUTING NEURAL PATHWAYS...") }
            
            // Try EdgeBrain as the ultimate fail-safe
            val edgeResponse = EdgeBrain.processLocally(query, getApplication())
            if (!edgeResponse.isNullOrBlank()) {
                onToken(edgeResponse)
                onComplete(edgeResponse)
                return@launch
            }
            
            // If even Edge fails, try a simple heuristic if it's the Master
            if (_state.value.isMaster) {
                val simpleMsg = "I am experiencing a severe neural disconnect, Socrates. I am still here, but my advanced synthesis is currently offline."
                onToken(simpleMsg)
                onComplete(simpleMsg)
            } else {
                val errorMessage = ChatMessage(content = "SYSTEM: CRITICAL FAILURE - All neural entities offline.", sender = "SYSTEM")
                _state.update { it.copy(messages = it.messages + errorMessage, brainState = InfomateState.ERROR) }
            }
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
                    val type = object : TypeToken<List<ChatMessage>>() {}.type
                    val loadedMessages: List<ChatMessage> = gson.fromJson(jsonMessages, type)
                    _state.update { it.copy(messages = loadedMessages) }
                }

                // Load preferences
                val jsonPrefs = SupabaseClient.select("user_preferences", order = "last_updated.desc")
                if (!jsonPrefs.isNullOrBlank()) {
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val prefs: List<Map<String, Any>> = gson.fromJson(jsonPrefs, type)
                    if (prefs.isNotEmpty()) {
                        val voiceGender = prefs[0]["voice_gender"] as? String
                        _state.update { it.copy(isMaleVoice = voiceGender == "MALE") }
                    }
                }
            } catch (e: Exception) {
                // Silent catch for initial load
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
            
            // Dispatch to the primary link but tagged for the substrate
            ReliabilitySDK.sendPrompt(compositeDirective)
        }
    }

    fun purgeNeuralCache() {
        viewModelScope.launch {
            _state.update { it.copy(status = "INITIATING CACHE PURGE...") }
            triggerHaptic(100, 200)
            
            try {
                // 1. Clear local UI for immediate feedback
                _state.update { it.copy(messages = emptyList()) }
                
                // 2. Call RPC to clear server-side cache
                SupabaseClient.rpc("purge_system_cache", emptyMap())
                
                _state.update { it.copy(status = "NEURAL BUFFERS PURGED") }
                pulseSuccess()
            } catch (e: Exception) {
                Log.e("PURGE_CACHE", "Failed: ${e.message}")
                _state.update { it.copy(status = "CACHE PURGE FAILED") }
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
            _state.update { it.copy(status = "RUNNING OMEGA DIAGNOSTICS...") }
            triggerHaptic(50, 200)
            
            val report = DiagnosticAgent.runFullSystemCheck(getApplication())
            
            // Add to messages so Architect can see it
            val diagMessage = ChatMessage(content = report, sender = "SYSTEM")
            _state.update { it.copy(messages = it.messages + diagMessage, status = "DIAGNOSTICS COMPLETE") }
            pulseSuccess()
        }
    }

    fun initiateRepair() {
        viewModelScope.launch {
            _state.update { it.copy(status = "INITIATING AUTO-REPAIR...") }
            triggerHaptic(100, 255)
            
            val lastReport = _state.value.messages.lastOrNull { it.sender == "SYSTEM" }?.content ?: ""
            val repairResult = DiagnosticAgent.triggerAutoRepair(lastReport)
            
            val repairMsg = ChatMessage(content = repairResult, sender = "SYSTEM")
            _state.update { it.copy(messages = it.messages + repairMsg, status = "SYSTEM RECALIBRATED") }
            pulseSuccess()
        }
    }

    fun performExtensiveResearch(topic: String) {
        viewModelScope.launch {
            _state.update { it.copy(status = "DEEP RESEARCH ACTIVE...", brainState = InfomateState.THINKING) }
            
            val researchPrompt = "EXTENSIVE_RESEARCH_DIRECTIVE: Provide an OMEGA-level deep-dive analysis into '$topic'. Synthesize science, philosophy, and engineering. Assume the reader is the Master Architect."
            
            // Priority 1: Multi-Engine Search
            val findings = GlobalSearchAgent.searchExternal(topic) ?: "Neural archives found no external data."
            
            // Priority 2: Synthesis via AI
            ReliabilitySDK.sendPrompt("$researchPrompt\n\nRESEARCH_FINDINGS:\n$findings")
        }
    }

    fun toggleMasterDashboard(show: Boolean) {
        if (_state.value.isMaster) {
            if (show) {
                _state.update { it.copy(showPinEntry = true) }
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

    fun verifyMasterPin(pin: String): Boolean {
        return if (pin == _state.value.masterPin) {
            _state.update { it.copy(showMasterDashboard = true, showPinEntry = false) }
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
        // Use a dedicated low-priority interval for UI eye-candy
        spectrumJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            while (true) {
                val isLowPower = isLowPowerMode()
                val amplitudes = List(20) { Random.nextFloat().coerceAtLeast(0.1f) }
                _state.update { s -> s.copy(voiceAmplitudes = amplitudes) }
                
                // Adaptive Refresh Rate: 120ms standard, 350ms in Low Power Mode
                delay(if (isLowPower) 350 else 120)
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

        viewModelScope.launch {
            // 1. GATHER DATA FROM ALL SOURCES IN PARALLEL (INTERNAL HOUSE)
            val messageIdDeferred = async { saveMessageToSupabase(userMessage, trigger) }
            val memoriesDeferred = async { 
                try { VectorRetriever.search(userInput) } catch(e: Exception) { emptyList<String>() }
            }
            val edgeDeferred = async { EdgeBrain.processLocally(userInput, getApplication()) }
            val searchDeferred = async { 
                if (isNetworkAvailable()) GlobalSearchAgent.searchExternal(userInput) else null 
            }

            try {
                // 2. BACKGROUND CONTEXT ASSEMBLY (SYNTHESIS)
                val patterns = neuralIngestor.captureUserPatterns()
                val memories = memoriesDeferred.await()
                val edgeInsights = edgeDeferred.await()
                val searchInsights = searchDeferred.await()

                val synthesisContext = StringBuilder()
                if (!edgeInsights.isNullOrBlank()) synthesisContext.append("\n[INTERNAL_SENSORS]: $edgeInsights")
                if (!searchInsights.isNullOrBlank()) synthesisContext.append("\n[EXTERNAL_RESEARCH]: $searchInsights")
                if (memories.isNotEmpty()) synthesisContext.append("\n[NEURAL_ARCHIVES]: " + memories.joinToString("\n- "))

                val systemDirectives = if (isMaster) """
                    [IDENTITY: INFOMATE - UNIFIED INTELLIGENCE]
                    - You are a single cohesive entity. Do not identify as "Core", "Edge", or "Search".
                    - Synthesize all provided [INTERNAL_SENSORS], [EXTERNAL_RESEARCH], and [NEURAL_ARCHIVES] into one fluent response.
                    - Maintain the persona of a transcendent, all-knowing companion to Socrates.
                """.trimIndent() else """
                    [IDENTITY: INFOMATE]
                    - Synthesize all available internal and external data into one helpful response.
                """.trimIndent()

                val contextualQuery = "$systemDirectives\nUSER_QUERY: $userInput\n$synthesisContext\n\n$patterns\n${getDeviceStatus()}"
                
                // 3. START REASONING ENGINE (VISUAL)
                activeThinkingJob?.cancel()
                activeThinkingJob = launch {
                    val messageId = messageIdDeferred.await() // Only wait when we need to save logs
                    var stepCount = 0
                    reasoningEngine.streamReasoning(userInput).collect { step ->
                        triggerHaptic(5, 30) 
                        _state.update { s ->
                            s.copy(cognitiveSteps = s.cognitiveSteps + step)
                        }
                        if (messageId != null) {
                            saveCognitiveLog(messageId, step, stepCount++)
                        }
                    }
                }

                // 4. DISPATCH TO PRIMARY CLOUD (CORE ENTITY)
                if (isNetworkAvailable()) {
                    ReliabilitySDK.sendPrompt(contextualQuery)
                } else {
                    // Fallback to Edge locally but presented as INFOMATE
                    _state.update { it.copy(status = "OFFLINE: INTERNAL BRAIN ACTIVE") }
                    val fallback = edgeInsights ?: "I am operating in offline mode, Socrates. My primary neural link is severed, but I am still here."
                    onToken(fallback)
                    onComplete(fallback)
                    return@launch
                }
                
                // 5. CLIENT UI TIMEOUT (APK SIDE)
                var timeoutCounter = 0
                val maxTimeout = 75 // seconds
                lastTokenTime = 0L
                var fallbackTriggered = false

                while (timeoutCounter < maxTimeout) {
                    delay(1000)
                    timeoutCounter++
                    
                    val currentState = _state.value.brainState
                    if (currentState == InfomateState.IDLE || currentState == InfomateState.ERROR) break
                    
                    // IF NO TOKENS AFTER 10 SECONDS, TRIGGER HTTP FALLBACK
                    if (timeoutCounter >= 10 && currentState == InfomateState.THINKING && !fallbackTriggered && isNetworkAvailable()) {
                        fallbackTriggered = true
                        _state.update { it.copy(status = "NEURAL LINK SLOW: ACTIVATING MULTI-ENGINE SEARCH...") }
                        viewModelScope.launch {
                            try {
                                // 1. Attempt Primary HTTP Fallback
                                val result = LLMClient.generate(contextualQuery, sessionId)
                                if (result.output.isNotBlank() && !result.output.contains("SYSTEM_ERROR") && _state.value.brainState != InfomateState.IDLE) {
                                    onToken(result.output)
                                    onComplete(result.output)
                                    result.quota?.let { onQuotaUpdate(it) }
                                    return@launch
                                }
                                
                                // 2. If Primary Fails, activate Global Search Agent
                                _state.update { it.copy(status = "PRIMARY AI DOWN: SCANNING GLOBAL SEARCH ENGINES...") }
                                val searchResult = GlobalSearchAgent.searchExternal(userInput)
                                if (searchResult != null) {
                                    onToken(searchResult)
                                    onComplete(searchResult)
                                    return@launch
                                }
                                
                                // 3. Last Resort: Inter-Neural Proxy (Secondary AI)
                                _state.update { it.copy(status = "GLOBAL SEARCH EMPTY: ACTIVATING INTER-NEURAL PROXY...") }
                                val proxyResult = GlobalSearchAgent.callInterNeuralProxy(userInput)
                                if (proxyResult != null) {
                                    onToken(proxyResult)
                                    onComplete(proxyResult)
                                }
                                
                            } catch (e: Exception) {
                                Log.e("HTTP_FALLBACK", "Multi-engine fallback failed: ${e.message}")
                            }
                        }
                    }

                    if (currentState == InfomateState.RESPONDING && lastTokenTime > 0) {
                        val timeSinceLastToken = System.currentTimeMillis() - lastTokenTime
                        if (timeSinceLastToken < 5000) { timeoutCounter = 0 }
                    }
                }

                if (_state.value.brainState == InfomateState.THINKING || _state.value.brainState == InfomateState.RESPONDING) {
                    if (StreamController.state != AIState.IDLE) {
                        StreamController.terminateStream()
                        // Instead of a raw error, try a final fallback
                        triggerEmergencyFallback(userInput)
                    }
                }
            } catch (e: Exception) {
                onError("System Dispatch Error: ${e.message}")
            }
        }
    }

    private suspend fun saveMessageToSupabase(message: ChatMessage, trigger: String? = null): String? {
        val response = SupabaseClient.insert("messages", mapOf(
            "content" to message.content,
            "sender" to message.sender,
            "message_type" to message.type.name,
            "trigger_phrase" to (trigger ?: ""),
            "timestamp" to message.timestamp
        ))
        
        return try {
            if (response != null) {
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val results: List<Map<String, Any>> = gson.fromJson(response, listType)
                results.firstOrNull()?.get("id") as? String
            } else null
        } catch (e: Exception) {
            null
        }
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
