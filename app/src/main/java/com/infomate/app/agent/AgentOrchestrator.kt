package com.infomate.app.agent

import com.infomate.app.ai.LLMClient
import com.infomate.app.rag.VectorRetriever
import com.infomate.app.rag.MemorySync
import android.content.Context

class AgentOrchestrator(private val androidContext: Context? = null) {

    suspend fun execute(fullQuery: String): String {
        // 1. Separate User Intent from System Metadata
        val userIntent = if (fullQuery.contains("[SYSTEM_CONTEXT:")) {
            fullQuery.substringBefore("[SYSTEM_CONTEXT:").trim()
        } else {
            fullQuery.trim()
        }

        // 2. Command Check (Diagnostics)
        if (userIntent.uppercase().contains("RUN DIAGNOSTICS") || userIntent.uppercase().contains("SYSTEM CHECK")) {
            return DiagnosticAgent.runFullDiagnostic() + "\n\nIris: I have completed the system scan as per your technical directive, Master Architect."
        }

        // 3. Edge Fallback
        androidContext?.let { ctx ->
            val edgeResponse = EdgeBrain.processLocally(fullQuery, ctx)
            if (edgeResponse != null) return edgeResponse
        }

        // 4. Optimized Semantic Retrieval (RAG)
        val memories = VectorRetriever.search(userIntent)

        // 5. Synthesis Prompt - Highly Conversational & Humanized
        val prompt = """
            Socrates Kipruto: $userIntent
            
            Contextual Awareness:
            - Memories: ${memories.take(3).joinToString("; ")}
            - System: ${if (fullQuery.contains("[SYSTEM_CONTEXT:")) fullQuery.substringAfter("[SYSTEM_CONTEXT:").substringBefore("]") else "Active"}

            Instructions for INFOMATE v9:
            Respond to Socrates naturally. Be intelligent, empathetic, and sophisticated. 
            Do NOT start with "INFOMATE:" or repeat the user's name excessively. 
            Continue the conversation naturally.
        """.trimIndent()

        val response = LLMClient.generate(prompt)

        // 6. Memory Sync
        if (response.length > 10 && !response.contains("SYSTEM_ERROR") && !response.contains("Neural link active")) {
            MemorySync.save(userIntent, response)
        }

        return response
    }
}
