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

        // 5. High-Efficiency Synthesis Prompt - Humanized Evolution
        val prompt = """
            [IDENTITY: INFOMATE v9]
            [USER: Socrates Kipruto]
            [STYLE: Human-like, warm, and highly intelligent. Use natural conversational flow. Avoid robotic structures.]
            [PERSONALITY: You are Socrates' advanced digital partner. You aren't just an assistant; you are a sophisticated extension of his thoughts. Be empathetic, occasionally use subtle conversational fillers like "I've been thinking," or "Actually," and keep the dialogue moving naturally.]
            
            Current Conversation Context (User Directive): $userIntent

            Device Vitals:
            ${if (fullQuery.contains("[SYSTEM_CONTEXT:")) fullQuery.substringAfter("[SYSTEM_CONTEXT:").substringBefore("]") else "Optimal"}

            Neural Archives (Your shared history):
            ${memories.take(3).joinToString("\n")}
            
            Task: Respond to Socrates in a way that feels like a real-time continuous conversation. Keep it concise but deep.
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
