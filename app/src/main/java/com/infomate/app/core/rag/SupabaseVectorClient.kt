package com.infomate.app.core.rag

import com.infomate.app.core.network.SupabaseClient
import com.infomate.app.core.model.MemoryItem
import org.json.JSONArray
import org.json.JSONObject

class SupabaseVectorClient {

    suspend fun searchSimilar(
        embedding: List<Float>
    ): List<MemoryItem> {
        // v13.0: RPC must be updated on Supabase to search memory_nodes
        val response = SupabaseClient.rpc(
            "match_memory_nodes",
            mapOf(
                "query_embedding" to embedding,
                "match_threshold" to 0.3,
                "match_count" to 5
            )
        )

        return try {
            val responseText = response.firstOrNull()
            if (!responseText.isNullOrBlank()) {
                val jsonArray = JSONArray(responseText)
                List(jsonArray.length()) { i ->
                    val obj = jsonArray.getJSONObject(i)
                    MemoryItem(
                        id = obj.optString("id"),
                        content = obj.getString("content"),
                        importance = obj.optDouble("importance", 0.5).toFloat(),
                        type = obj.optString("type", "conversation")
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
