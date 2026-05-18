package com.infomate.app.ai

import com.infomate.app.core.network.SupabaseClient
import org.json.JSONObject

object LLMClient {
    suspend fun generate(prompt: String): String {
        val params = mapOf("prompt" to prompt)
        val response = SupabaseClient.callFunction("infomate-brain", params)
        
        return try {
            if (!response.isNullOrBlank()) {
                val json = JSONObject(response)
                // v9 refinement: Ensure we capture the 'output' field from the brain's JSON response
                json.optString("output", "INTELLIGENCE_LINK: Active. No immediate output payload.")
            } else {
                "INTELLIGENCE_LINK: Active. Synchronizing neural clusters..."
            }
        } catch (e: Exception) {
            "NEURAL_ERROR: Link frequency unstable. Retrying link..."
        }
    }
}
