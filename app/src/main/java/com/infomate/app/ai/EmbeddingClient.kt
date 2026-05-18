package com.infomate.app.ai

import com.infomate.app.core.network.SupabaseClient
import org.json.JSONObject

object EmbeddingClient {
    
    suspend fun getEmbedding(text: String): List<Float> {
        // v9: Calling Supabase Edge Function to generate real Gemini embeddings
        val response = SupabaseClient.callFunction("get-embeddings", mapOf("input" to text))
        
        return try {
            if (!response.isNullOrBlank()) {
                val json = JSONObject(response)
                val data = json.getJSONArray("embedding")
                List(data.length()) { i -> data.getDouble(i).toFloat() }
            } else List(768) { 0.0f }
        } catch (e: Exception) {
            List(768) { 0.0f } 
        }
    }
}
