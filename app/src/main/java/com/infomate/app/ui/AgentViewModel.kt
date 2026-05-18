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
import com.infomate.app.agent.AgentOrchestrator
import com.infomate.app.core.NeuralIngestor
import com.infomate.app.core.network.SupabaseClient
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

class AgentViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

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
        return " [SYSTEM_CONTEXT: Battery $level%, Time $time]"
    }

    init {
        tts = TextToSpeech(application, this)
        if (SpeechRecognizer.isRecognitionAvailable(application)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
            setupSpeechListener()
        }
        loadSessionData()
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            try {
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

    fun completeOnboarding() {
        _state.update { it.copy(needsOnboarding = false) }
    }

    private fun startSpectrumAnimation() {
        spectrumJob?.cancel()
        spectrumJob = viewModelScope.launch {
            while (true) {
                _state.update { s ->
                    s.copy(voiceAmplitudes = List(20) { Random.nextFloat().coerceAtLeast(0.1f) })
                }
                delay(100)
            }
        }
    }

    private fun stopSpectrumAnimation() {
        spectrumJob?.cancel()
        _state.update { it.copy(voiceAmplitudes = List(20) { 0.1f }) }
    }

    fun speak(text: String) {
        if (!_state.value.isVoiceOutputEnabled) return // RESPECT THE SILENCE TOGGLE

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "INFOMATE_SPEECH")
        
        if (_state.value.isMaleVoice) {
            tts?.setPitch(0.8f)
            tts?.setSpeechRate(0.9f)
        } else {
            tts?.setPitch(1.2f)
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
                val response = orchestrator.execute(prompt)
                
                val assistantMessage = ChatMessage(content = "", sender = "INFOMATE")
                _state.update { it.copy(messages = it.messages + assistantMessage) }
                
                typeWriterEffect(response)
                
                _state.update { it.copy(
                    status = "CORE: ACTIVE",
                    brainState = InfomateState.RESPONDING
                ) }
                speak(response)
            } catch (e: Exception) {
                _state.update { it.copy(status = "VISUAL_ERROR: ${e.message}") }
            }
        }
    }

    private suspend fun typeWriterEffect(fullText: String) {
        var currentText = ""
        val words = fullText.split(" ")
        for (index in words.indices) {
            currentText += words[index] + (if (index < words.size - 1) " " else "")
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
            val delayMs = when {
                words[index].endsWith(".") || words[index].endsWith("?") || words[index].endsWith("!") -> 200L
                words[index].endsWith(",") || words[index].endsWith(";") -> 100L
                else -> 35L
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
                // Specialized search query for the AI
                val searchPrompt = "SEARCH_REQUEST: $query. Search global knowledge and your archives. Provide a comprehensive summary. If you find nothing, do not say 'no intelligent output', instead provide a helpful AI summary of what that topic usually entails."
                
                launch {
                    reasoningEngine.streamReasoning("Global Search: $query").collect { step ->
                        _state.update { s -> s.copy(cognitiveSteps = s.cognitiveSteps + step) }
                    }
                }

                val response = orchestrator.execute(searchPrompt)
                
                val searchMessage = ChatMessage(
                    content = "SEARCH RESULTS FOR: \"$query\"\n\n$response",
                    sender = "INFOMATE"
                )

                _state.update { it.copy(
                    messages = it.messages + searchMessage,
                    status = "CORE: ACTIVE",
                    brainState = com.infomate.core.ui.components.InfomateState.RESPONDING
                ) }
                
                speak(response)
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
        val userMessage = ChatMessage(content = userInput, sender = "OPERATOR")

        _state.update { it.copy(
            status = "CORE: ANALYZING...",
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
                val contextualQuery = userInput + getDeviceStatus() + "\n$patterns"
                
                val thinkingJob = launch {
                    reasoningEngine.streamReasoning(userInput).collect { step ->
                        triggerHaptic(5, 30) 
                        _state.update { s ->
                            s.copy(cognitiveSteps = s.cognitiveSteps + step)
                        }
                    }
                }

                val response = orchestrator.execute(contextualQuery)
                thinkingJob.cancel() 

                val assistantMessage = ChatMessage(content = "", sender = "INFOMATE")

                _state.update { it.copy(
                    messages = it.messages + assistantMessage,
                    status = "CORE: RESPONDING",
                    brainState = InfomateState.RESPONDING
                ) }
                
                typeWriterEffect(response)
                
                _state.update { it.copy(status = "CORE: ACTIVE") }
                saveMessageToSupabase(ChatMessage(content = response, sender = "INFOMATE"))
                speak(response)
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
