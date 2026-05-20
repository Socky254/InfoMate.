package com.infomate.app.core.rag

import com.infomate.app.core.network.SupabaseClient
import org.json.JSONObject

class EmbeddingClient {

    suspend fun embed(text: String): List<Float> {
        // Calling Supabase Edge Function to generate Gemini embeddings
        return try {
            val response = SupabaseClient.callFunction("get-embeddings", mapOf("input" to text))
            if (!response.isNullOrBlank()) {
                val json = JSONObject(response)
                val data = json.getJSONArray("embedding")
                List(data.length()) { i -> data.getDouble(i).toFloat() }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
