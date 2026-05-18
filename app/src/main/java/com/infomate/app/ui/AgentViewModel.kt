package com.infomate.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infomate.app.agent.AgentOrchestrator
import com.infomate.core.brain.ReasoningEngine
import com.infomate.core.ui.components.InfomateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentViewModel : ViewModel() {

    private val orchestrator = AgentOrchestrator()
    private val reasoningEngine = ReasoningEngine()

    private val _state = MutableStateFlow(UIState())
    val state: StateFlow<UIState> = _state.asStateFlow()

    fun updateInput(text: String) {
        _state.update { it.copy(input = text) }
    }

    fun send() {
        val userInput = _state.value.input
        if (userInput.isBlank()) return

        _state.update { it.copy(
            status = "NEURAL LINK ESTABLISHED",
            brainState = InfomateState.THINKING,
            messages = it.messages + "Operator: $userInput",
            input = "",
            cognitiveSteps = emptyList()
        ) }

        viewModelScope.launch {
            try {
                // Start streaming cognitive thoughts for "Neural Link" feel
                launch {
                    reasoningEngine.streamReasoning(userInput).collect { step ->
                        _state.update { s ->
                            s.copy(cognitiveSteps = s.cognitiveSteps + step)
                        }
                    }
                }

                val response = orchestrator.execute(userInput)
                
                _state.update { it.copy(
                    messages = it.messages + "INFOMATE: $response",
                    status = "CORE: ACTIVE",
                    brainState = InfomateState.RESPONDING
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    messages = it.messages + "SYSTEM: ERROR - ${e.message}",
                    status = "CORE: DEGRADED",
                    brainState = InfomateState.ERROR
                ) }
            }
        }
    }
}
