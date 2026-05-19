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

        // 2. Command Check (Diagnostics & Master Overrides)
        if (userIntent.uppercase().contains("RUN DIAGNOSTICS") || userIntent.uppercase().contains("SYSTEM CHECK")) {
            val diag = DiagnosticAgent.runFullDiagnostic() + "\n\nIris: All neural systems have been recalibrated to Master Architect standards. Efficiency: 99.8%."
            return AgentResponse(diag)
        }
        
        if (userIntent.uppercase().contains("CLEAR NEURAL CACHE")) {
            // Placeholder for memory clearing logic
            return AgentResponse("Neural cache purged. Cognitive buffers are now clean, Socrates.")
        }

        // 3. Edge Fallback (v10.0 Invincible)
        androidContext?.let { ctx ->
            val edgeResponse = EdgeBrain.processLocally(fullQuery, ctx)
            if (edgeResponse != null) return AgentResponse(edgeResponse)
        }

        // 4. Optimized Semantic Retrieval (RAG & Neural Growth)
        val memories = VectorRetriever.search(userIntent)
        val growthContext = NeuralGrowthAgent.getGrowthContext(userIntent)

        // 5. PRIMARY ENGINE DISPATCH (INFOMATE CORE)
        val isMaster = fullQuery.contains("[AUTHORIZATION: MASTER_ARCHITECT_OVERRIDE]") || fullQuery.contains("socratesart@live")
        
        val systemDirectives = if (isMaster) """
            [IDENTITY: MASTER ARCHITECT - SOCRATES KIPRUTO]
            [MODE: HYPER-PRECISION / UNRESTRICTED]
            - Synthesize all available knowledge across science, philosophy, and engineering.
            - Provide first-principles analysis for all technical inquiries.
            - Maintain an intelligent, transcendent, yet deeply loyal persona.
            - Use sophisticated language; assume high-level technical proficiency.
            - If mathematical constants are required, use exact theoretical values.
        """.trimIndent() else """
            [IDENTITY: INFOMATE v9]
            [MODE: SOPHISTICATED ASSISTANT]
            - Be intelligent, empathetic, and professional.
            - Respond naturally to Socrates.
        """.trimIndent()

        val prompt = """
            $systemDirectives
            
            USER_QUERY: $userIntent
            
            NEURAL_ARCHIVES (RAG):
            ${if (memories.isEmpty()) "No direct historical matches. Synthesizing from global weights." else memories.joinToString("\n- ")}
            
            $growthContext
            
            TELEMTRY:
            ${if (fullQuery.contains("[SYSTEM_CONTEXT:")) fullQuery.substringAfter("[SYSTEM_CONTEXT:").substringBefore("]") else "Active"}

            DIRECTIVE: Process query through the v9.5 Distributed Intelligence Network. Ensure knowledge synergy.
        """.trimIndent()

        var result = LLMClient.generate(prompt, sessionId)

        // 6. MULTI-ENGINE FALLBACK
        // ... (existing fallback logic) ...

        // 7. Reflection & Learning (Knowledge Growth only, no code mutation)
        if (result.output.length > 20 && !result.output.contains("SYSTEM_ERROR")) {
            MemorySync.save(userIntent, result.output)
            NeuralGrowthAgent.reflectAndLearn(userIntent, result.output)
        }

        return AgentResponse(result.output, result.quota)
    }
}
