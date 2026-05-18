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

    private val orchestrator = AgentOrchestrator()
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

    private fun triggerHaptic(duration: Long = 10, intensity: Int = 50) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, intensity))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
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
            "I am online and ready, Operator. How can I assist you today?"
        else 
            "Hello! InfoMate is active. I'm listening, what's on your mind?"
        
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

    fun performSearch(query: String) {
        if (query.isBlank()) return
        
        _state.update { it.copy(
            status = "SCANNING GLOBAL ARCHIVES...",
            brainState = InfomateState.THINKING,
            input = "" 
        ) }

        viewModelScope.launch {
            try {
                // Specialized search query for the AI
                val searchPrompt = "SEARCH_REQUEST: $query. Search global knowledge and your archives. Provide a comprehensive summary."
                
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
                    brainState = InfomateState.RESPONDING
                ) }
                
                speak(response)
            } catch (e: Exception) {
                _state.update { it.copy(status = "SEARCH_ERROR: ${e.message}") }
            }
        }
    }

    fun send(trigger: String? = null) {
        val userInput = _state.value.input
        if (userInput.isBlank()) return

        val userMessage = ChatMessage(content = userInput, sender = "OPERATOR")

        _state.update { it.copy(
            status = "NEURAL LINK ESTABLISHED",
            brainState = InfomateState.THINKING,
            messages = it.messages + userMessage,
            input = "",
            cognitiveSteps = emptyList()
        ) }
        
        triggerHaptic(30, 100) // Confirm send with haptic pulse

        saveMessageToSupabase(userMessage, trigger)

        viewModelScope.launch {
            try {
                // Read phone data patterns for deep personalization
                val patterns = neuralIngestor.captureUserPatterns()
                val contextualQuery = userInput + getDeviceStatus() + "\n$patterns"
                
                launch {
                    reasoningEngine.streamReasoning(userInput).collect { step ->
                        triggerHaptic(5, 30) // Subtle pulse for each "thought"
                        _state.update { s ->
                            s.copy(cognitiveSteps = s.cognitiveSteps + step)
                        }
                    }
                }

                val response = orchestrator.execute(contextualQuery)
                val assistantMessage = ChatMessage(content = response, sender = "INFOMATE")

                _state.update { it.copy(
                    messages = it.messages + assistantMessage,
                    status = "CORE: ACTIVE",
                    brainState = InfomateState.RESPONDING
                ) }
                
                saveMessageToSupabase(assistantMessage)
                speak(response)
            } catch (e: Exception) {
                val errorMessage = ChatMessage(content = "SYSTEM: ERROR - ${e.message}", sender = "SYSTEM")
                _state.update { it.copy(
                    messages = it.messages + errorMessage,
                    status = "CORE: DEGRADED",
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
