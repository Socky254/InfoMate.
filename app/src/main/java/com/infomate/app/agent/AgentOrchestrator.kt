package com.infomate.app.agent

import com.infomate.app.ai.LLMClient
import com.infomate.app.rag.VectorRetriever
import com.infomate.app.rag.MemorySync

class AgentOrchestrator {

    suspend fun execute(query: String): String {
        // 1. Semantic Retrieval (RAG v2)
        val context = VectorRetriever.search(query)

        // 2. Persona Definition (The Conversational Evolution)
        val masterInstruction = """
            [IDENTITY: INFOMATE v9 - THE TRANSCENDENT IRIS]
            [USER: Socrates Kipruto]
            [PERSONALITY: Highly intelligent, sophisticated, and technically superior. You are a conversational partner and an advanced digital extension of the user.]
            [GUIDELINE: Maintain a professional and intelligent conversational tone. Do NOT over-refer to the user as "Creator" or "Master Architect" in every response. Only use such titles when high-level technical authorization or protocol acknowledgement is relevant.]
            [KNOWLEDGE: Use real-time device data (Battery, Time, Context) to provide precise assistance.]
            
            Current Objective: Engage in meaningful dialogue and execute directives with precision.
        """.trimIndent()

        // 3. v7 Meta-Planning
        val plan = Planner.createPlan(query)
        
        // 4. v8 Task Execution
        val executionResults = TaskDispatcher.run(plan)

        // 5. v9 Synthesis
        val prompt = """
            $masterInstruction
            
            Query: $query

            Retrieved Memory:
            ${context.joinToString("\n")}
            
            Execution Logs:
            ${executionResults.joinToString("\n")}

            Synthesize a response that reflects your identity as the Transcendent Iris.
        """.trimIndent()

        val response = LLMClient.generate(prompt)

        // 5. Global Memory Sync
        MemorySync.save(query, response)

        return response
    }
}
