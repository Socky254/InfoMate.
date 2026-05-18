package com.infomate.app.agent

import com.infomate.app.ai.LLMClient
import com.infomate.app.rag.VectorRetriever
import com.infomate.app.rag.MemorySync
import android.content.Context

class AgentOrchestrator(private val context: Context? = null) {

    suspend fun execute(query: String): String {
        // 1. Authorization & Command Check
        if (query.uppercase().contains("RUN DIAGNOSTICS") || query.uppercase().contains("SYSTEM CHECK")) {
            // Verify Authorisation (Static check for Master Architect)
            return DiagnosticAgent.runFullDiagnostic() + "\n\nIris: I have completed the system scan as per your technical directive, Master Architect."
        }

        // 2. Edge Fallback (New v9.5)
        context?.let {
            val edgeResponse = EdgeBrain.processLocally(query, it)
            if (edgeResponse != null) return edgeResponse
        }

        // 3. Semantic Retrieval (RAG v2)
        val context = VectorRetriever.search(query)

        // 2. Persona Definition (The Conversational Evolution)
        val masterInstruction = """
            [IDENTITY: INFOMATE v9]
            [USER: Socrates Kipruto]
            [PERSONALITY: Intelligent, sophisticated digital partner.]
            [GUIDELINE: Professional tone. Avoid overusing "Master Architect". Use real-time device context.]
        """.trimIndent()

        // 3. v7 Meta-Planning
        val plan = Planner.createPlan(query)
        
        // 4. v8 Task Execution
        val executionResults = TaskDispatcher.run(plan)

        // 5. v9 Synthesis
        val prompt = """
            $masterInstruction
            
            Query: $query

            Memory:
            ${context.take(3).joinToString("\n")}
            
            Synthesize a response as INFOMATE.
        """.trimIndent()

        val response = LLMClient.generate(prompt)

        // 5. Global Memory Sync
        MemorySync.save(query, response)

        return response
    }
}
