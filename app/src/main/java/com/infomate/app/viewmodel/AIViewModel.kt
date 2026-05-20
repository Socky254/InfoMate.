package com.infomate.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infomate.app.core.ai.BrainCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PHASE 7: ViewModel Connection
 */
class AIViewModel(
    private val brain: BrainCoordinator
) : ViewModel() {

    private val _response = MutableStateFlow("")
    val response = _response.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun ask(input: String) {
        if (input.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = brain.process(input)
            _response.value = result
            _isLoading.value = false
        }
    }
}
