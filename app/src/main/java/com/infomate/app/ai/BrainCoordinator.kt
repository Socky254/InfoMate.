package com.infomate.app.ai

import android.content.Context
import android.util.Log
import com.infomate.app.agent.ConsciousnessEngine
import com.infomate.app.agent.GlobalSearchAgent
import com.infomate.app.rag.VectorRetriever
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Single Source of Intelligence (v13.0)
 * Orchestrates Gemini, Search, and Memory.
 */
class BrainCoordinator(
    private val gemini: GeminiClient = GeminiClient(),
    private val context: Context? = null
) {

    suspend fun think(userInput: String, deviceStatus: String): String = coroutineScope {
        Log.i("BrainCoordinator", "Initiating cognitive synthesis for: $userInput")
        
        // 1. GATHER DATA IN PARALLEL
        val searchJob = async { 
            if (userInput.length > 10) GlobalSearchAgent.searchExternal(userInput, context) else null 
        }
        val memoryJob = async { 
            try { VectorRetriever.search(userInput) } catch(e: Exception) { emptyList<String>() }
        }
        
        val googleInsights = searchJob.await()
        val memories = memoryJob.await()
        
        // 2. BUILD COMPOSITE CONTEXT
        val agentStatus = ConsciousnessEngine.getSubstrateAlignmentSummary()
        val compositePrompt = PromptBuilder.buildBrainContext(
            userInput, 
            memories, 
            googleInsights, 
            agentStatus, 
            deviceStatus
        )
        
        // 3. DISPATCH TO GEMINI (PRIMARY PROVIDER)
        val startTime = System.currentTimeMillis()
        val rawResponse = gemini.generate(compositePrompt)
        val latency = System.currentTimeMillis() - startTime
        
        Log.d("BrainCoordinator", "Gemini response received. Latency: ${latency}ms, Length: ${rawResponse.length}")
        
        // 4. VALIDATE & PROCESS
        val validatedResponse = ResponseValidator.validate(rawResponse)
        
        // 5. MEMORY PERSISTENCE (Optional logic here)
        
        validatedResponse
    }
}
