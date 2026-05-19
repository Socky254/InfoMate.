package com.infomate.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
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
import com.infomate.app.core.NeuralIngestor
import com.infomate.app.core.network.SupabaseClient
import com.infomate.app.ai.sdk.AIEventsListener
import com.infomate.app.ai.sdk.AIState
import com.infomate.app.ai.sdk.ReliabilitySDK
import com.infomate.app.ai.sdk.StreamController
import com.infomate.app.ai.sdk.UIRenderer
import com.infomate.app.core.config.Config
import com.infomate.core.brain.ReasoningEngine
import com.infomate.core.ui.components.InfomateState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    override fun onToken(text: String) {
        if (text.isEmpty()) return
        
        // Immediate cancel to free UI cycles
        activeThinkingJob?.cancel()
        activeThinkingJob = null
        
        // Offload string manipulation to Default dispatcher, only update State on Main
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            _state.update { state ->
                val newMessages = state.messages.toMutableList()
                if (newMessages.isNotEmpty() && newMessages.last().sender == "INFOMATE") {
                    val lastMsg = newMessages.last()
                    val updatedContent = lastMsg.content + text
                    newMessages[newMessages.size - 1] = lastMsg.copy(content = updatedContent)
                } else {
                    newMessages.add(ChatMessage(content = text, sender = "INFOMATE"))
                }
                // Transition brainState immediately to stop the expensive Thinking visualizer
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
            speak(fullText)
            pulseSuccess()
        }
    }

    override fun onError(error: String) {
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
            val voices = tts?.voices
            val highQualityVoice = voices?.find { 
                it.name.contains("en-us-x-sfg", ignoreCase = true) || 
                it.name.contains("en-us-x-iog", ignoreCase = true) ||
                it.name.contains("neural", ignoreCase = true) ||
                !it.isNetworkConnectionRequired // Locally stored HQ voices often don't have this flag set
            }
            
            highQualityVoice?.let { 
                tts?.voice = it 
                android.util.Log.d("INFOMATE_TTS", "Selected high-quality voice: ${it.name}")
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
        saveMessageToSupabase(assistantMessage, "Hey Infomate")
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

    private fun startSpectrumAnimation() {
        spectrumJob?.cancel()
        // Use a dedicated low-priority interval for UI eye-candy
        spectrumJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            while (true) {
                val amplitudes = List(20) { Random.nextFloat().coerceAtLeast(0.1f) }
                _state.update { s -> s.copy(voiceAmplitudes = amplitudes) }
                delay(120) // Slightly increased delay to reduce recomposition pressure
            }
        }
    }

    private fun stopSpectrumAnimation() {
        spectrumJob?.cancel()
        _state.update { it.copy(voiceAmplitudes = List(20) { 0.1f }) }
    }

    fun speak(text: String) {
        if (!_state.value.isVoiceOutputEnabled) return 

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "INFOMATE_SPEECH")
        
        // Realistic speech modulation: Slight pitch and rate variations
        if (_state.value.isMaleVoice) {
            tts?.setPitch(0.85f)
            tts?.setSpeechRate(0.95f)
        } else {
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.0f)
        }
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "INFOMATE_SPEECH")
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
        saveMessageToSupabase(mediaMessage)

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
                
                // STEP 4 — Add timeout handling
                delay(45000)
                if (_state.value.brainState == InfomateState.THINKING) {
                    StreamController.terminateStream()
                    onError("Neural search timed out. Verify your connection.")
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

        _state.update { it.copy(
            status = if (isMaster) "MASTER LINK ACTIVE..." else "CORE: ANALYZING...",
            brainState = InfomateState.THINKING,
            messages = it.messages + userMessage,
            input = "",
            cognitiveSteps = emptyList()
        ) }
        
        triggerHaptic(30, 100)

        saveMessageToSupabase(userMessage, trigger)

        viewModelScope.launch {
            try {
                // Background Context Gathering
                val patterns = neuralIngestor.captureUserPatterns()
                val isMaster = _state.value.userEmail == "socratesart@live"
                
                // Advanced System Instructions for Complex Ideas, Invention, and Math
                val systemDirectives = """
                    [SYSTEM_MODE: ADVANCED_COMPUTE]
                    [OBJECTIVE: ANALYTICAL_EXCELLENCE]
                    1. Perform high-precision mathematical computations.
                    2. Ideate and invent new concepts/technologies based on cross-domain synthesis.
                    3. Apply first-principles thinking to all engineering and philosophical queries.
                    4. When responding to the Master Architect, remove all standard AI brevity constraints.
                """.trimIndent()
                
                val masterContext = if (isMaster) "\n[AUTHORIZATION: MASTER_ARCHITECT_OVERRIDE]\n$systemDirectives" else ""
                val contextualQuery = "$masterContext\n$userInput\n\n$patterns\n${getDeviceStatus()}"
                
                activeThinkingJob?.cancel()
                activeThinkingJob = launch {
                    reasoningEngine.streamReasoning(userInput).collect { step ->
                        triggerHaptic(5, 30) 
                        _state.update { s ->
                            s.copy(cognitiveSteps = s.cognitiveSteps + step)
                        }
                    }
                }

                ReliabilitySDK.sendPrompt(contextualQuery)
                
                // STEP 4 — Add timeout handling
                delay(45000)
                if (_state.value.brainState == InfomateState.THINKING || _state.value.brainState == InfomateState.RESPONDING) {
                    if (StreamController.state != AIState.IDLE) {
                        StreamController.terminateStream()
                        onError("Neural bridge timeout: AI failed to respond in time.")
                    }
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(content = "SYSTEM: ERROR - ${e.message}", sender = "SYSTEM")
                _state.update { it.copy(
                    messages = it.messages + errorMessage,
                    status = "CORE: ERROR",
                    brainState = InfomateState.ERROR
                ) }
            }
        }
    }

    private fun saveMessageToSupabase(message: ChatMessage, trigger: String? = null) {
        viewModelScope.launch {
            SupabaseClient.insert("messages", mapOf(
                "content" to message.content,
                "sender" to message.sender,
                "message_type" to message.type.name,
                "trigger_phrase" to (trigger ?: ""),
                "timestamp" to message.timestamp
            ))
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
