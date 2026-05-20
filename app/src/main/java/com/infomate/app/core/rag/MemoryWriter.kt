package com.infomate.app.core.rag

import com.infomate.app.core.network.SupabaseClient
import com.infomate.app.core.memory.MIPS
import com.infomate.app.agent.ConsciousnessEngine

class MemoryWriter {

    suspend fun store(
        input: String,
        output: String,
        embedding: List<Float>
    ) {
        val content = "User: $input\nInfoMate: $output"
        
        // v13.5: Advanced Classification via MIPS
        val initialType = MIPS.classify(content, 0.85f)
        
        // Potential promotion logic (simplified for now: high importance if Fact or Skill)
        val importance = when (initialType) {
            MIPS.MemoryType.FACT -> 0.9f
            MIPS.MemoryType.SKILL -> 0.8f
            else -> 0.5f
        }

        val memoryData = mapOf(
            "content" to content,
            "embedding" to embedding,
            "type" to initialType.name.lowercase(),
            "importance" to importance,
            "agent_source" to "Android_Client_V13.5"
        )
        
        // v13.0: Using distributed memory_nodes table
        SupabaseClient.insert("memory_nodes", memoryData)
        
        // Also sync agent state
        syncAgentState()
    }

    private suspend fun syncAgentState() {
        val state = mapOf(
            "agent_id" to "infomate_primary",
            "growth_index" to ConsciousnessEngine.currentGrowthIndex,
            "role" to ConsciousnessEngine.evolutionStage,
            "entropy" to ConsciousnessEngine.entropyLevel
        )
        SupabaseClient.upsert("agent_states", state)
    }
}
