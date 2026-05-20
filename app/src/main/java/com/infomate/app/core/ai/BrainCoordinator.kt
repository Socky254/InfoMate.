package com.infomate.app.core.ai

import com.infomate.app.core.rag.*
import com.infomate.app.agent.ConsciousnessEngine
import com.infomate.app.core.agent.AgentLogic
import com.infomate.app.core.network.WebSocketBrain

/**
 * PHASE 3: Brain Coordinator (Main Control Layer)
 * Single entry point for all AI requests.
 * Orchestrates RAG and other AI subsystems.
 */
class BrainCoordinator(
    private val ragOrchestrator: RagOrchestrator = RagOrchestrator(
        EmbeddingClient(),
        SupabaseVectorClient(),
        GeminiClient(),
        MemoryWriter()
    ),
    private val wsBrain: WebSocketBrain = WebSocketBrain()
) {
    // Secondary constructor for backward compatibility
    constructor(gemini: GeminiClient, memory: com.infomate.app.core.memory.MemoryRepository) : this()

    fun streamProcess(input: String, listener: WebSocketBrain.BrainListener) {
        wsBrain.connect(listener)
        wsBrain.sendMessage(input)
    }

    suspend fun process(input: String): String {
        return try {
            // 1. Planner Agent
            val steps = AgentLogic.plan(input)
            
            // 2. Research Agent (via RAG)
            val response = AgentLogic.research(input, ragOrchestrator)
            
            // 3. Critic Agent
            val cleanResponse = AgentLogic.critique(response)

            // 4. Merge Results (Response Synthesis Engine)
            val finalOutput = ResponseSynthesizer.synthesize(
                plannerOutput = steps,
                geminiOutput = response,
                memoryContext = "", // Can be passed if needed
                criticFeedback = cleanResponse
            )
            
            // 5. Social Agent (Growth Index)
            val success = if (finalOutput.length > 50) 0.9f else 0.5f
            AgentLogic.updateGI(relevance = 0.8f, success = success)
            
            finalOutput
        } catch (e: Exception) {
            "AI temporarily unavailable. Try again."
        }
    }
}
