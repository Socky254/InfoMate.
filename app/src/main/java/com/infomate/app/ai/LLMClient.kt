package com.infomate.app.ai

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import org.json.JSONObject

object LLMClient {
    /**
     * Highly resilient generator that handles various AI API structures.
     * Prevents "No intelligent output detected" by using a multi-strategy parsing approach.
     */
    suspend fun generate(prompt: String): String {
        val params = mapOf("prompt" to prompt)
        val response = SupabaseClient.callFunction("infomate-brain", params)
        
        // 1. RAW RESPONSE LOGGING (CRITICAL)
        Log.d("INFOMATE_RAW", "Raw API Response: $response")

        if (response.isNullOrBlank()) {
            return """
                INFOMATE could not complete reasoning.
                Possible causes:
                - Network instability or timeout
                - Neural Link (API) frequency unstable
                - Neural Bridge (Supabase) disruption
                
                Please verify your connection and try again.
            """.trimIndent()
        }

        val trimmedResponse = response.trim()

        // 2. SAFE RESPONSE PARSING
        // If it's not JSON, it might be raw text from a simple endpoint
        if (!trimmedResponse.startsWith("{") && !trimmedResponse.startsWith("[")) {
            return validateOutput(trimmedResponse)
        }

        return try {
            val json = JSONObject(trimmedResponse)

            // Check for explicit error from the Edge Function/Supabase
            if (json.has("error") || json.has("error_code")) {
                val errorMsg = json.optString("message", json.optString("error", ""))
                val errorCode = json.optString("error_code", "")
                Log.e("INFOMATE_ERROR", "API reported error: $errorCode - $errorMsg")
                
                return when (errorCode) {
                    "RETRY_EXHAUSTED" -> "INFOMATE: $errorMsg"
                    "SAFETY_BLOCK" -> "INFOMATE: Neural safeguard triggered. The directive contains restricted concepts. Please rephrase."
                    else -> if (errorMsg.isNotBlank()) "SYSTEM_ERROR: $errorMsg" else json.optString("output", "")
                }
            }

            // Strategy: Check most common AI response fields safely
            val result = json.optString("output", "")
                .ifBlank { json.optString("text", "") }
                .ifBlank { json.optString("response", "") }
                .ifBlank { json.optString("message", "") }
                .ifBlank { json.optString("content", "") }
                .ifBlank { 
                    // Gemini/Google Structure (candidates -> content -> parts -> text)
                    json.optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")?.optJSONObject(0)
                        ?.optString("text", "") ?: ""
                }
                .ifBlank {
                    // OpenAI Structure (choices -> message -> content)
                    json.optJSONArray("choices")?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content", "") ?: ""
                }

            if (result.isNotBlank()) {
                validateOutput(result)
            } else {
                Log.w("INFOMATE_PARSING", "Could not extract text from JSON. Raw: $trimmedResponse")
                """
                    INFOMATE reasoning failed to synthesize.
                    The neural output was malformed or empty.
                    
                    Diagnostic code: ERR_PARSE_NULL
                """.trimIndent()
            }
        } catch (e: Exception) {
            Log.e("INFOMATE_PARSING", "JSON Parsing Exception: ${e.message}")
            // Fallback: Return raw response if it's reasonably short, else error
            if (trimmedResponse.length < 500) validateOutput(trimmedResponse)
            else "INFOMATE: Critical failure in neural decoding. (ERR_JSON_FAIL)"
        }
    }

    private fun validateOutput(text: String): String {
        val trimmed = text.trim()
        
        // Check if the response is just the name of the app (common failure mode for some LLMs)
        if (trimmed.equals("infomate", ignoreCase = true) || trimmed.equals("iris", ignoreCase = true)) {
            return """
                INFOMATE: Neural link generated a partial identity pulse but failed to synthesize a full response.
                
                This usually occurs when the prompt is too complex or the backend is under high load. Please retry your directive.
            """.trimIndent()
        }

        return if (trimmed.isNotBlank() && trimmed.length > 5) {
            trimmed
        } else {
            """
                INFOMATE: Neural output detected but insufficient for communication.
                The generated response was too short or blank.
                
                Retrying or rephrasing the directive may help.
            """.trimIndent()
        }
    }
}

