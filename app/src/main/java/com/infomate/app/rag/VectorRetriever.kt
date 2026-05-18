package com.infomate.app.rag

import com.infomate.app.ai.EmbeddingClient
import com.infomate.app.core.network.SupabaseClient
import org.json.JSONArray
import org.json.JSONObject

object VectorRetriever {
    suspend fun search(query: String): List<String> {
        val embedding = EmbeddingClient.getEmbedding(query)
        val response = SupabaseClient.rpc(
            "match_vectors",
            mapOf(
                "query_embedding" to embedding,
                "match_threshold" to 0.75,
                "match_count" to 5
            )
        )

        return try {
            val responseText = response.firstOrNull()
            if (!responseText.isNullOrBlank()) {
                val jsonArray = JSONArray(responseText)
                List(jsonArray.length()) { i ->
                    jsonArray.getJSONObject(i).getString("content")
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
