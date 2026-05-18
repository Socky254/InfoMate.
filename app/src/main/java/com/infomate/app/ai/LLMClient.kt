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
                json.optString("output", "ERROR: No response from brain.")
            } else {
                "ERROR: Brain returned empty response."
            }
        } catch (e: Exception) {
            "ERROR: Neural link failed."
        }
    }
}
