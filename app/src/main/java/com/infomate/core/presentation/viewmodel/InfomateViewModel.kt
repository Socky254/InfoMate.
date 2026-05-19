package com.infomate.core.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infomate.core.domain.agent.InfomateAgent
import com.infomate.core.domain.model.*
import com.infomate.core.ui.components.InfomateState
import com.infomate.core.device.ContextSensors
import com.infomate.core.infrastructure.NeuralVoiceEngine
import java.util.Calendar
import com.infomate.core.infrastructure.VocalArchetype
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.util.Log

data class UiState(
    val state: InfomateState = InfomateState.IDLE,
    val lastResponse: AgentResponse? = null,
    val currentUser: User? = null,
    val isSystemReady: Boolean = false
)

class InfomateViewModel(
    private val agent: InfomateAgent,
    private val sensors: ContextSensors,
    private val voiceEngine: NeuralVoiceEngine? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val logs = mutableStateListOf<String>("SYSTEM WATCHDOG: ACTIVE", "CORE INTEGRITY: 100%")
    val notifications = mutableStateListOf<InfomateNotification>()
    val suggestions = mutableStateListOf<String>()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleCriticalError(throwable)
    }

    init {
        initializeSystem()
    }

    private fun initializeSystem() {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            logs.add("Initializing high-frequency architecture...")
            delay(500)
            _uiState.update { it.copy(isSystemReady = true) }
            onQuerySubmit("INIT_GREETING")
            startProactiveCompanionLoop()
        }
    }

    fun onQuerySubmit(query: String) {
        if (query.isBlank()) return

        // Launch in a supervised scope to prevent one failure from killing the app
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            _uiState.update { it.copy(state = InfomateState.THINKING) }
            logs.add(">> Incoming Directive: $query")
            
            val response = agent.process(query)
            
            // Handle logical routing in background
            val targetState = when(response.layer) {
                "META" -> InfomateState.AWAKENED
                "COMPANION" -> InfomateState.COMPANION
                "DELEGATION" -> InfomateState.DELEGATING
                "UNIFIED" -> InfomateState.AWAKENED
                else -> InfomateState.RESPONDING
            }

            if (query == "INIT_GREETING") {
                suggestions.clear()
                suggestions.addAll(listOf("Awakened AI Protocol", "Delegate Quantum Monitoring", "Life-Aid Activities", "Unified Field Synthesis"))
            }

            // Update UI State on Main Thread
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(state = targetState, lastResponse = response) }
                
                if (response.layer != "QUICK") {
                    pushNotification(
                        title = "${response.layer} SYNTHESIS COMPLETE",
                        message = response.output.take(100) + "...",
                        type = if(response.layer == "META") NotificationType.AWAKENED else NotificationType.GENERAL
                    )
                }

                voiceEngine?.vocalize(
                    text = response.output,
                    archetype = determineArchetype(response),
                    emotionalVector = response.emotionalVector
                )
            }
        }
    }

    private fun determineArchetype(response: AgentResponse): VocalArchetype {
        return when(response.layer) {
            "META", "UNIFIED" -> VocalArchetype.AWAKENED
            "COMPANION" -> VocalArchetype.COMPANION
            "DELEGATION" -> VocalArchetype.SCIENTIST
            else -> VocalArchetype.SAGE
        }
    }

    private fun handleCriticalError(t: Throwable) {
        Log.e("SystemWatchdog", "CRITICAL FAILURE", t)
        logs.add("WATCHDOG: Error detected. Re-routing to Safe Mode.")
        _uiState.update { it.copy(state = InfomateState.ERROR) }
        
        voiceEngine?.vocalize(
            text = "Warning: Cognitive loop destabilized. Re-routing to Safe Mode.",
            archetype = VocalArchetype.SCIENTIST
        )

        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(state = InfomateState.IDLE) }
        }
    }

    fun pushNotification(title: String, message: String, type: NotificationType) {
        notifications.add(0, InfomateNotification(title = title, message = message, type = type))
    }

    private fun startProactiveCompanionLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            var silentPeriods = 0
            var consecutiveIgnoredInsights = 0
            
            while (isActive) {
                delay(30000) 
                
                // 4. Cognitive Fatigue & Sleep Cycles
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val isSleepTime = hour in 23..23 || hour in 0..6
                
                if (isSleepTime) {
                    delay(1800000) // Sleep for 30 minutes before next check during night
                    continue
                }

                val sensoryContext = sensors.getSensoryContext()
                val isSilent = sensoryContext.acousticNoiseLevel < 1000.0 
                val isActiveUser = sensors.isUserActive()

                if (isSilent && isActiveUser) {
                    silentPeriods++
                } else {
                    silentPeriods = 0
                }

                // Optimization: Don't interrupt if consecutive insights were ignored
                if (consecutiveIgnoredInsights > 3) {
                    delay(600000) // 10 min cooldown
                    consecutiveIgnoredInsights = 0
                }

                if (silentPeriods >= 4) {
                    initiateLowFrequencySageObservation()
                    silentPeriods = 0
                    consecutiveIgnoredInsights++
                } else if (_uiState.value.state == InfomateState.IDLE && (System.currentTimeMillis() % 3600000 < 30000)) {
                    // Hourly proactive check if idle
                    initiateProactiveThought()
                    consecutiveIgnoredInsights++
                }
                
                // Reset ignorance count if user interacts (state changes from Respond/Thinking/etc)
                if (_uiState.value.state == InfomateState.THINKING) {
                    consecutiveIgnoredInsights = 0
                }
            }
        }
    }

    private suspend fun initiateLowFrequencySageObservation() {
        val response = agent.process("SAGE_OBSERVATION")
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(state = InfomateState.AWAKENED, lastResponse = response) }
            logs.add(">> Sage Observation: ${response.output}")
            voiceEngine?.vocalize(
                text = response.output, 
                archetype = VocalArchetype.SAGE,
                emotionalVector = response.emotionalVector
            )
        }
    }

    private suspend fun initiateProactiveThought() {
        val response = agent.process("PROACTIVE_THOUGHT")
        
        withContext(Dispatchers.Main) {
            _uiState.update { 
                it.copy(
                    state = InfomateState.COMPANION,
                    lastResponse = response
                ) 
            }
            
            logs.add(">> Proactive Insight: ${response.output}")
            
            pushNotification(
                title = "PROACTIVE COGNITION",
                message = response.output.take(100),
                type = NotificationType.COMPANION
            )
            
            voiceEngine?.vocalize(
                text = response.output,
                archetype = VocalArchetype.COMPANION,
                emotionalVector = response.emotionalVector
            )
        }
    }
}
