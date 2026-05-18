package com.infomate.app.ai

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
        
        if (response.isNullOrBlank()) {
            return "ERROR: Neural Link frequency unstable. Verify API key, network connectivity, and quota."
        }

        val trimmedResponse = response.trim()

        // STRATEGY 1: If it's not JSON, it's likely a raw text response (Fast-path)
        if (!trimmedResponse.startsWith("{") && !trimmedResponse.startsWith("[")) {
            return trimmedResponse
        }

        return try {
            val json = JSONObject(trimmedResponse)

            // STRATEGY 2: Check for Custom Wrappers (output, text, response, message)
            val commonFields = listOf("output", "text", "response", "message", "content")
            for (field in commonFields) {
                val value = json.optString(field, "")
                if (value.isNotBlank()) return value
            }

            // STRATEGY 3: Gemini/Google Structure (candidates -> content -> parts -> text)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val geminiText = firstPart?.optString("text", "")
            if (geminiText.isNotBlank()) return geminiText

            // STRATEGY 4: OpenAI Structure (choices -> message -> content)
            val choices = json.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val msgObj = firstChoice?.optJSONObject("message")
            val openAiText = msgObj?.optString("content", "")
            if (openAiText.isNotBlank()) return openAiText

            // STRATEGY 5: Check for Error or Safety signals
            val error = json.optJSONObject("error")?.optString("message", "")
            if (!error.isNullOrBlank()) return "SYSTEM_ERROR: $error"

            val promptFeedback = json.optJSONObject("promptFeedback")
            if (promptFeedback != null) return "SAFETY_BLOCK: The request was restricted by AI safety filters."

            // STRATEGY 6: Final Fallback - Return raw response if it's non-empty
            trimmedResponse
        } catch (e: Exception) {
            // If parsing fails for any reason, return the raw response string
            trimmedResponse
        }
    }
}
