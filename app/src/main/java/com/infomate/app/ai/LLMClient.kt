package com.infomate.app.ai

import com.infomate.app.core.network.SupabaseClient
import org.json.JSONObject

object LLMClient {
    suspend fun generate(prompt: String): String {
        val params = mapOf("prompt" to prompt)
        val response = SupabaseClient.callFunction("infomate-brain", params)
        
        return if (!response.isNullOrBlank()) {
            try {
                val json = JSONObject(response)
                val output = json.optString("output", "")
                if (output.isNotBlank()) {
                    output
                } else {
                    // Fallback if 'output' is empty but JSON exists
                    json.optString("text", json.optString("response", "Intelligence link established, but no payload was returned."))
                }
            } catch (e: Exception) {
                // If it's not JSON, return the raw response string (often the AI just returns text)
                response
            }
        } else {
            "NEURAL_LINK: Signal lost. Checking global search nodes..."
        }
    }
}
