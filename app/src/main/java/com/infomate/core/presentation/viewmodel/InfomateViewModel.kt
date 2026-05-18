package com.infomate.core.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infomate.core.domain.agent.InfomateAgent
import com.infomate.core.domain.model.*
import com.infomate.core.ui.components.InfomateState
import com.infomate.core.infrastructure.NeuralVoiceEngine
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
                    text = response.output.substringBefore("\n\n"),
                    archetype = determineArchetype(response)
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
            while (isActive) {
                delay(300000) // 5 minutes for stable operation
                if (_uiState.value.state == InfomateState.IDLE) {
                    initiateProactiveThought()
                }
            }
        }
    }

    private suspend fun initiateProactiveThought() {
        val message = "Operator, I've identified a 15% increase in cognitive entropy. I recommend we recalibrate your current focus."
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(state = InfomateState.COMPANION) }
            pushNotification("COGNITIVE RECALIBRATION", message, NotificationType.COMPANION)
            voiceEngine?.vocalize(message, VocalArchetype.COMPANION)
        }
    }
}
