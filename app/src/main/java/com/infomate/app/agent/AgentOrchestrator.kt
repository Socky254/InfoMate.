package com.infomate.app.agent

import com.infomate.app.ai.LLMClient
import com.infomate.app.ai.GenerationResult
import com.infomate.app.rag.VectorRetriever
import com.infomate.app.rag.MemorySync
import com.infomate.app.ui.QuotaInfo
import android.content.Context

data class AgentResponse(
    val output: String,
    val quota: QuotaInfo? = null
)

class AgentOrchestrator(private val androidContext: Context? = null) {

    suspend fun execute(fullQuery: String, sessionId: String = "default"): AgentResponse {
        // 1. Separate User Intent from System Metadata
        val userIntent = if (fullQuery.contains("[SYSTEM_CONTEXT:")) {
            fullQuery.substringBefore("[SYSTEM_CONTEXT:").trim()
        } else {
            fullQuery.trim()
        }

        // 2. Command Check (Diagnostics)
        if (userIntent.uppercase().contains("RUN DIAGNOSTICS") || userIntent.uppercase().contains("SYSTEM CHECK")) {
            val diag = DiagnosticAgent.runFullDiagnostic() + "\n\nIris: I have completed the system scan as per your technical directive, Master Architect."
            return AgentResponse(diag)
        }

        // 3. Edge Fallback
        androidContext?.let { ctx ->
            val edgeResponse = EdgeBrain.processLocally(fullQuery, ctx)
            if (edgeResponse != null) return AgentResponse(edgeResponse)
        }

        // 4. Optimized Semantic Retrieval (RAG)
        val memories = VectorRetriever.search(userIntent)

        // 5. Synthesis Prompt
        val prompt = """
            Socrates Kipruto: $userIntent
            
            Contextual Awareness:
            - Memories: ${memories.take(3).joinToString("; ")}
            - System: ${if (fullQuery.contains("[SYSTEM_CONTEXT:")) fullQuery.substringAfter("[SYSTEM_CONTEXT:").substringBefore("]") else "Active"}

            Instructions for INFOMATE v9:
            Respond to Socrates naturally. Be intelligent, empathetic, and sophisticated. 
            Do NOT repeat the user's name excessively. 
            Continue the conversation naturally.
        """.trimIndent()

        // Pass sessionId to LLMClient
        val result = LLMClient.generate(prompt, sessionId)

        // 6. Memory Sync
        if (result.output.length > 10 && !result.output.contains("SYSTEM_ERROR") && !result.output.contains("Neural link active")) {
            MemorySync.save(userIntent, result.output)
        }

        return AgentResponse(result.output, result.quota)
    }
}
