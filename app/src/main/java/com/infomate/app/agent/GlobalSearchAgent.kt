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
        val proxyResult = callInterNeuralProxy(query)
        if (proxyResult != null) return proxyResult

        // 4. v10.1: Emergency Background Search (No Key Required)
        return performEmergencyWebSearch(query)
    }

    private suspend fun performEmergencyWebSearch(query: String): String? {
        Log.i("GlobalSearch", "Performing emergency background search for: $query")
        
        return try {
            val url = "https://api.duckduckgo.com/?q=${java.net.URLEncoder.encode(query, "UTF-8")}&format=json&no_html=1&skip_disambig=1"
            val request = okhttp3.Request.Builder().url(url).build()
            val client = okhttp3.OkHttpClient()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                
                val abstract = json.optString("AbstractText")
                val source = json.optString("AbstractSource")
                
                if (abstract.isNotBlank()) {
                    "[NEURAL_DATA_EXTRACTED from $source]: $abstract"
                } else {
                    // Try related topics if abstract is empty
                    val related = json.optJSONArray("RelatedTopics")
                    if (related != null && related.length() > 0) {
                        val firstObj = related.optJSONObject(0)
                        val text = firstObj?.optString("Text")
                        if (!text.isNullOrBlank()) "[NEURAL_DATA_SNIPPET]: $text" else null
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e("GlobalSearch", "Emergency search failed: ${e.message}")
            null
        }
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
            "model" to "claude-3-haiku",
            "prompt" to query
        )
        
        val response = SupabaseClient.callFunction("inter-neural-proxy", payload)
        
        // v10.9: Detect and ignore missing edge function errors
        if (response != null && (response.contains("not found", true) || response.contains("404"))) {
            Log.e("GlobalSearch", "Inter-Neural Proxy not deployed on Supabase.")
            return null
        }

        return if (!response.isNullOrBlank()) {
            "[EXTERNAL_SOURCE: INTER_NEURAL_PROXY]\n$response"
        } else null
    }
}
