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

        // 3. Edge Fallback (Local deterministic responses)
        androidContext?.let { ctx ->
            val edgeResponse = EdgeBrain.processLocally(fullQuery, ctx)
            if (edgeResponse != null) return edgeResponse
        }

        // 4. Optimized Semantic Retrieval (RAG)
        // We search using the userIntent only, to avoid metadata noise
        val memories = VectorRetriever.search(userIntent)

        // 5. High-Efficiency Synthesis Prompt
        val prompt = """
            You are INFOMATE v9, a sophisticated AI partner for Socrates Kipruto.
            
            User Directive: $userIntent

            Device Context:
            ${if (fullQuery.contains("[SYSTEM_CONTEXT:")) fullQuery.substringAfter("[SYSTEM_CONTEXT:").substringBefore("]") else "Stable"}

            Relevant Memories:
            ${memories.take(2).joinToString("\n")}
            
            Instruction: Provide a direct, intelligent, and helpful response. Do not repeat your name or the system context.
        """.trimIndent()

        val response = LLMClient.generate(prompt)

        // 7. Global Memory Sync (Clean)
        // Save the interaction without the system noise
        if (response.length > 20 && !response.contains("SYSTEM_ERROR")) {
            MemorySync.save(userIntent, response)
        }

        return response
    }
}
