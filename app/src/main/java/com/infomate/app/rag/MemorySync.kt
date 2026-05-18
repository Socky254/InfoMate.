package com.infomate.app.rag

import com.infomate.app.ai.EmbeddingClient
import com.infomate.app.core.network.SupabaseClient

object MemorySync {
    suspend fun save(query: String, result: String) {
        val embedding = EmbeddingClient.getEmbedding(query)
        SupabaseClient.insert(
            "memory",
            mapOf(
                "content" to result,
                "embedding" to embedding
            )
        )
    }
}
