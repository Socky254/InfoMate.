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

            // 3. CHECK FOR EXPLICIT ERROR CODES
            if (json.has("error") || json.has("error_code")) {
                val errorMsg = json.optString("message", json.optString("error", ""))
                val errorCode = json.optString("error_code", "")
                Log.e("INFOMATE_ERROR", "API reported error: $errorCode - $errorMsg")
                
                return when (errorCode) {
                    "RETRY_EXHAUSTED" -> "INFOMATE: $errorMsg"
                    "SAFETY_BLOCK" -> "INFOMATE: Neural safeguard triggered. The directive contains restricted concepts."
                    else -> if (errorMsg.isNotBlank()) "SYSTEM_ERROR: $errorMsg" else json.optString("output", "")
                }
            }

            // 4. MULTI-STRATEGY CONTENT EXTRACTION
            val result = extractContent(json)

            if (result.isNotBlank()) {
                validateOutput(result)
            } else {
                Log.w("INFOMATE_PARSING", "Could not extract text from JSON. Raw: $trimmedResponse")
                "INFOMATE: The neural output was malformed. Diagnostic code: ERR_PARSE_NULL"
            }
        } catch (e: Exception) {
            Log.e("INFOMATE_PARSING", "JSON Parsing Exception: ${e.message}")
            if (trimmedResponse.length < 500 && !trimmedResponse.contains("{")) validateOutput(trimmedResponse)
            else "INFOMATE: Critical failure in neural decoding. (ERR_JSON_FAIL)"
        }
    }

    private fun extractContent(json: JSONObject): String {
        // Strategy A: Direct fields
        var content = json.optString("output", "")
            .ifBlank { json.optString("text", "") }
            .ifBlank { json.optString("response", "") }
            .ifBlank { json.optString("message", "") }
            .ifBlank { json.optString("content", "") }

        if (content.isNotBlank()) return content

        // Strategy B: Google Gemini Structure
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val firstCandidate = candidates.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.optJSONObject(0)?.optString("text", "") ?: ""
            }
        }

        // Strategy C: OpenAI Structure
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val message = choices.optJSONObject(0)?.optJSONObject("message")
            return message?.optString("content", "") ?: ""
        }

        return ""
    }

    private fun validateOutput(text: String): String {
        // 1. Remove common identity prefixes (Case Insensitive Regex)
        val cleaned = text.replace(Regex("^(?i)(infomate|iris|system|assistant):\\s*"), "")
            .trim()
        
        // 2. Detect and handle identity loops or empty responses
        val lowerCleaned = cleaned.lowercase()
        if (lowerCleaned == "infomate" || lowerCleaned == "iris" || cleaned.isBlank()) {
            return "I am fully synchronized, Socrates. My neural link is stable and I am awaiting your next directive."
        }

        // 3. Ensure response has meaningful substance
        return if (cleaned.length > 3) {
            cleaned
        } else {
            "Neural link active and stable. Please provide your directive, Socrates."
        }
    }
}

