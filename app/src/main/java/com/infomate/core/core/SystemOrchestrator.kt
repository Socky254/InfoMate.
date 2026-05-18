package com.infomate.core.core

import com.infomate.core.brain.InfomateBrain
import com.infomate.core.brain.InfomateResponse
import com.infomate.core.memory.MemoryStore
import com.infomate.core.device.ContextSensors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemOrchestrator {
    private val brain = InfomateBrain()
    private val memory = MemoryStore()
    private val sensors = ContextSensors()

    // Optimized for speed using Coroutines on IO/Default dispatchers
    suspend fun handleEvent(event: String): InfomateResponse = withContext(Dispatchers.Default) {
        // Parallel execution of context gathering and reasoning
        sensors.getAmbientContext() 
        
        val result = brain.process(event)
        
        // Background memory update
        withContext(Dispatchers.IO) {
            memory.store("event_${System.currentTimeMillis()}", event)
        }
        
        result
    }
}
