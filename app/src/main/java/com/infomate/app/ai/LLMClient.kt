package com.infomate.app.ai

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import com.infomate.app.ui.QuotaInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

data class GenerationResult(
    val output: String,
    val quota: QuotaInfo? = null
)

object LLMClient {
    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private const val MIN_REQUEST_INTERVAL = 1500L

    suspend fun generate(prompt: String, sessionId: String = "session_active"): GenerationResult {
        return mutex.withLock {
            var retryCount = 0
            val maxRetries = 1 // GOLDEN_CONFIG: Max 1 retry
            
            while (retryCount <= maxRetries) {
                val requestId = java.util.UUID.randomUUID().toString()
                
                // 1. MANDATORY REQUEST ENVELOPE (APK STABILITY)
                val envelope = mapOf(
                    "requestId" to requestId,
                    "sessionId" to sessionId,
                    "timestamp" to System.currentTimeMillis() / 1000,
                    "type" to "chat",
                    "payload" to mapOf("prompt" to prompt)
                )

                try {
                    val response = SupabaseClient.callFunction("infomate-brain", envelope)
                    Log.d("INFOMATE_RAW", "Raw API Response: $response")

                    if (response.isNullOrBlank()) {
                        throw Exception("EMPTY_RESPONSE")
                    }

                    val json = JSONObject(response)

                    // 2. Handle Stream/Error Events
                    if (json.optBoolean("error", false) || json.optString("event") == "error") {
                        val code = json.optString("code", json.optString("error_code"))
                        
                        val errorOutput = when (code) {
                            "RATE_LIMITED" -> "Quota exceeded, Socrates. The neural link will reset in ${json.optInt("retryAfter", 3600) / 60} minutes."
                            "UNAUTHORIZED" -> "Neural link rejected: Authentication required."
                            "INVALID_REQUEST" -> "Neural link rejected: Malformed directive."
                            else -> null
                        }

                        if (errorOutput != null) return GenerationResult(errorOutput)
                        
                        // Retryable errors (Server issues, timeouts)
                        if (retryCount < maxRetries) {
                            retryCount++
                            // Jittered Exponential Backoff
                            val delay = (1000L * Math.pow(2.0, retryCount.toDouble()).toLong()) + (0..500).random()
                            kotlinx.coroutines.delay(delay)
                            continue
                        }
                        return GenerationResult("SYSTEM_ERROR: ${json.optString("message", "Unknown AI failure")}")
                    }

                    // 3. Quota Visibility
                    val quotaJson = json.optJSONObject("quota")
                    val quotaInfo = quotaJson?.let {
                        QuotaInfo(
                            requestsUsed = it.optInt("requestsUsed"),
                            requestsLimit = it.optInt("requestsLimit"),
                            tokensUsed = it.optLong("tokensUsed")
                        )
                    }

                    val output = json.optString("output", "")
                    if (output.isNotBlank()) return GenerationResult(validateOutput(output), quotaInfo)

                    throw Exception("MALFORMED_OUTPUT")

                } catch (e: Exception) {
                    Log.e("INFOMATE_RETRY", "Attempt $retryCount failed: ${e.message}")
                    if (retryCount < maxRetries && e.message != "UNAUTHORIZED") {
                        retryCount++
                        val delay = (1000L * Math.pow(2.0, retryCount.toDouble()).toLong()) + (0..500).random()
                        kotlinx.coroutines.delay(delay)
                        continue
                    }
                    return GenerationResult("SYSTEM_ERROR: Connection lost during neural sync. Please verify your network.")
                }
            }
            return GenerationResult("SYSTEM_ERROR: Neural bridge failed after $maxRetries retries.")
        }
    }

    private fun validateOutput(text: String): String {
        val cleaned = text.trim()
            .replace(Regex("^(?i)(infomate|iris|system|assistant|ai):\\s*", RegexOption.MULTILINE), "")
            .trim()
        
        return if (cleaned.length > 1) {
            cleaned
        } else {
            "I'm here and operational, Socrates. How can I assist you further?"
        }
    }
}
