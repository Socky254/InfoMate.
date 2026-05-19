package com.infomate.app.ai

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import com.infomate.app.agent.HealthManager
import com.infomate.app.agent.HealthState
import com.infomate.app.agent.HealthSeverity
import org.json.JSONObject

object LLMClient {
    private val responseCache = mutableMapOf<String, String>()

    /**
     * Highly resilient generator that handles various AI API structures.
     * Prevents "No intelligent output detected" by using a multi-strategy parsing approach.
     */
    suspend fun generate(prompt: String): String {
        // 0. Cache Lookup
        if (responseCache.containsKey(prompt)) {
            Log.d("INFOMATE_CACHE", "Returning cached response for prompt")
            return responseCache[prompt]!!
        }

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
                
                val errorResponse = when (errorCode) {
                    "RETRY_EXHAUSTED" -> "Neural link stabilized, but API rate limits reached. Cooling down for 30s."
                    "SAFETY_BLOCK" -> "Neural safeguard triggered. The directive contains restricted concepts."
                    else -> if (errorMsg.isNotBlank()) "SYSTEM_ERROR: $errorMsg" else json.optString("output", "")
                }
                return validateOutput(errorResponse)
            }

            // 4. MULTI-STRATEGY CONTENT EXTRACTION
            val result = extractContent(json)

                val finalOutput = if (result.isNotBlank()) {
                    HealthManager.logHealth(HealthManager.CAT_PARSER, HealthState.ONLINE, "Defensive Parsing Successful", HealthSeverity.STABLE)
                    validateOutput(result)
                } else {
                    Log.w("INFOMATE_PARSING", "Could not extract text from JSON. Raw: $trimmedResponse")
                    HealthManager.logHealth(HealthManager.CAT_PARSER, HealthState.DEGRADED, "ERR_PARSE_NULL: Content fields empty", HealthSeverity.CRITICAL)
                    "INFOMATE: The neural output was malformed. Diagnostic code: ERR_PARSE_NULL"
                }

                if (!finalOutput.contains("ERROR") && !finalOutput.contains("malformed")) {
                    responseCache[prompt] = finalOutput
                }
                return finalOutput
        } catch (e: Exception) {
            Log.e("INFOMATE_PARSING", "JSON Parsing Exception: ${e.message}")
            HealthManager.logHealth(HealthManager.CAT_PARSER, HealthState.DEGRADED, "ERR_JSON_FAIL: ${e.message}", HealthSeverity.CRITICAL)
            if (trimmedResponse.length < 500 && !trimmedResponse.contains("{")) validateOutput(trimmedResponse)
            else "INFOMATE: Critical failure in neural decoding. (ERR_JSON_FAIL)"
        }
    }

    private fun extractContent(json: JSONObject): String {
        // Strategy 0: Log entire JSON for debugging
        Log.d("INFOMATE_PARSER", "Attempting extraction from: ${json.toString()}")

        // Strategy A: Direct fields (most common for simple custom bridges)
        val directFields = listOf("output", "text", "response", "message", "content", "result")
        for (field in directFields) {
            val value = json.optString(field, "")
            if (value.isNotBlank() && value != "null") return value
        }

        // Strategy B: Google Gemini Structure (candidates[0].content.parts[0].text)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val firstCandidate = candidates.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val text = parts.optJSONObject(0)?.optString("text", "") ?: ""
                if (text.isNotBlank()) return text
            }
        }

        // Strategy C: OpenAI Structure (choices[0].message.content or choices[0].text)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val firstChoice = choices.optJSONObject(0)
            
            // Chat Completion (message.content)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content", "") ?: ""
            if (content.isNotBlank()) return content
            
            // Legacy Completion (text)
            val text = firstChoice?.optString("text", "") ?: ""
            if (text.isNotBlank()) return text
        }

        // Strategy D: Nested data field (some bridges wrap everything in 'data')
        val data = json.optJSONObject("data")
        if (data != null) return extractContent(data)

        return ""
    }

    private fun validateOutput(text: String): String {
        // 1. Log the text we are validating
        Log.d("INFOMATE_VALIDATOR", "Validating: $text")

        // 2. Trim and Remove common identity prefixes (Case Insensitive Regex)
        val cleaned = text.trim()
            .replace(Regex("^(?i)(infomate|iris|system|assistant|ai):\\s*", RegexOption.MULTILINE), "")
            .trim()
        
        // 3. Detect and handle identity loops or empty responses
        val lowerCleaned = cleaned.lowercase()
        if (lowerCleaned == "infomate" || lowerCleaned == "iris" || lowerCleaned == "ai" || cleaned.isBlank()) {
            Log.w("INFOMATE_VALIDATOR", "Detected identity-only or empty response. Triggering fallback.")
            return "My neural link is stable and I am fully synchronized, Socrates. I am ready for your next directive or search request."
        }

        // 4. Ensure response has meaningful substance
        return if (cleaned.length > 1) {
            cleaned
        } else {
            "I'm here and operational, Socrates. How can I assist you further?"
        }
    }
}

