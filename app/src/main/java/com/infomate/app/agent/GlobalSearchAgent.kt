package com.infomate.app.agent

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import org.json.JSONArray
import org.json.JSONObject

/**
 * Global Search Agent (v10.8)
 * Orchestrates external search engines and manages neural network nodes.
 */
object GlobalSearchAgent {

    suspend fun searchExternal(query: String): String? {
        Log.i("GlobalSearch", "Initiating node-based search for: $query")
        
        // 1. Fetch Active Nodes from the registry
        val nodesJson = SupabaseClient.select("neural_network_nodes", order = "reliability_rating.desc")
        val activeNodes = if (!nodesJson.isNullOrBlank()) {
            JSONArray(nodesJson)
        } else JSONArray()

        Log.d("GlobalSearch", "Dispatched to ${activeNodes.length()} neural nodes.")

        // 2. Try Primary Global Search Proxy
        val payload = mapOf(
            "engine" to "multi",
            "query" to query,
            "nodes_count" to activeNodes.length()
        )
        
        val response = SupabaseClient.callFunction("global-search-proxy", payload)
        
        val results = try {
            if (!response.isNullOrBlank()) {
                val json = JSONObject(response)
                json.optString("synthesis", "")
            } else null
        } catch (e: Exception) { null }

        if (!results.isNullOrBlank()) {
            return "[GLOBAL_BRIDGE_SYNC]: $results"
        }

        // 3. Fallback: Secondary Model Proxy
        return callInterNeuralProxy(query)
    }

    suspend fun fetchNodePerformance(): List<Map<String, Any>> {
        val response = SupabaseClient.select("neural_network_nodes", "node_name, reliability_rating, last_ping", "reliability_rating.desc")
        return if (!response.isNullOrBlank()) {
            val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
            com.google.gson.Gson().fromJson(response, type)
        } else emptyList()
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
