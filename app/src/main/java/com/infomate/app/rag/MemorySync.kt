package com.infomate.app.rag

import com.infomate.app.ai.EmbeddingClient
import com.infomate.app.core.network.SupabaseClient

object MemorySync {
    suspend fun save(query: String, result: String) {
        if (result.length < 20 || result.contains("SYSTEM_ERROR")) return

        val embedding = EmbeddingClient.getEmbedding(query)
        SupabaseClient.insert(
            "memory_nodes",
            mapOf(
                "content" to "User: $query\nInfoMate: $result",
                "embedding" to embedding,
                "type" to "conversation",
                "importance" to 0.5f,
                "agent_source" to "neural_sync"
            )
        )
    }
}
