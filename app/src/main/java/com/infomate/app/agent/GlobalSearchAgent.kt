package com.infomate.app.agent

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import org.json.JSONObject

/**
 * Global Search Agent (v9.5)
 * Orchestrates external search engines and secondary AI models.
 */
object GlobalSearchAgent {

    suspend fun searchExternal(query: String): String? {
        Log.i("GlobalSearch", "Initiating multi-engine search for: $query")
        
        // 1. Try DuckDuckGo / Google via Supabase Edge Function Proxy
        // This keeps the API keys safe on the server
        val payload = mapOf(
            "engine" to "multi",
            "query" to query,
            "depth" to "thorough"
        )
        
        val response = SupabaseClient.callFunction("global-search-proxy", payload)
        
        return try {
            if (!response.isNullOrBlank()) {
                val json = JSONObject(response)
                val results = json.optString("synthesis", "")
                if (results.isNotBlank()) {
                    return "[EXTERNAL_SOURCE: GLOBAL_SEARCH]\n$results"
                }
            }
            null
        } catch (e: Exception) {
            Log.e("GlobalSearch", "External search failed: ${e.message}")
            null
        }
    }

    suspend fun callInterNeuralProxy(query: String): String? {
        Log.i("GlobalSearch", "Calling Inter-Neural Proxy (Secondary AI)")
        
        val payload = mapOf(
            "model" to "claude-3-haiku", // Example secondary model
            "prompt" to query
        )
        
        val response = SupabaseClient.callFunction("inter-neural-proxy", payload)
        return if (!response.isNullOrBlank()) {
            "[EXTERNAL_SOURCE: INTER_NEURAL_PROXY]\n$response"
        } else null
    }
}
