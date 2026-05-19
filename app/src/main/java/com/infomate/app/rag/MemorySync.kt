package com.infomate.app.rag

import com.infomate.app.ai.EmbeddingClient
import com.infomate.app.core.network.SupabaseClient

object MemorySync {
    suspend fun save(query: String, result: String) {
        if (result.length < 20 || result.contains("SYSTEM_ERROR")) return

        val embedding = EmbeddingClient.getEmbedding(query)
        SupabaseClient.insert(
            "memory",
            mapOf(
                "content" to result,
                "embedding" to embedding,
                "metadata" to mapOf(
                    "query" to query,
                    "version" to "v9.5",
                    "source" to "neural_sync"
                )
            )
        )
    }
}
