package com.infomate.core.core

import android.content.Context
import com.infomate.core.brain.InfomateBrain
import com.infomate.core.brain.InfomateResponse
import com.infomate.core.memory.MemoryStore
import com.infomate.core.memory.CognitiveArchive
import com.infomate.core.device.ContextSensors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.infomate.core.network.InfomateCloud
import java.net.URI

class SystemOrchestrator(context: Context) {
    private val archive = CognitiveArchive(context)
    private val cloud = InfomateCloud(URI("wss://your-supabase-project.supabase.co/functions/v1/infomate-brain"))
    private val brain = InfomateBrain(archive, cloud)
    private val memory = MemoryStore()
    private val sensors = ContextSensors(context)

    // Optimized for speed using Coroutines on IO/Default dispatchers
    suspend fun handleEvent(event: String): InfomateResponse = withContext(Dispatchers.Default) {
        // Parallel execution of context gathering and reasoning
        sensors.getSensoryContext()
        
        val result = brain.process(event)
        
        // Background memory update
        withContext(Dispatchers.IO) {
            memory.store("event_${System.currentTimeMillis()}", event)
        }
        
        result
    }
}
