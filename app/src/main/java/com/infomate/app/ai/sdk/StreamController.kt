package com.infomate.app.ai.sdk

import com.infomate.app.storage.PersistenceManager
import org.json.JSONObject
import android.content.Context

object StreamController {
    var state: AIState = AIState.IDLE
        set(value) {
            field = value
            UIRenderer.onStateChange(value)
        }
    
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun handle(raw: String?) {
        if (raw.isNullOrBlank()) return
        
        try {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("{")) {
                // Handle raw text as a token if it's not JSON
                accumulateToken(trimmed)
                UIRenderer.onToken(trimmed)
                return
            }

            val msg = JSONObject(trimmed)
            // v11.4: Multi-format support (Handles raw events or Phoenix-wrapped)
            val event = msg.optString("event")
            val payload = if (msg.has("payload")) msg.optJSONObject("payload") else msg

            when (event) {
                "stream_start", "phx_reply" -> {
                    if (event == "stream_start") state = AIState.STREAMING
                }
                "stream_ping", "token_ping", "phx_reply" -> {
                    // Internal keep-alive
                }
                "token", "broadcast" -> {
                    val dataObj = if (event == "broadcast") payload?.optJSONObject("content") else payload
                    val chunk = dataObj?.optString("text") ?: dataObj?.optString("chunk") ?: ""
                    
                    if (chunk.isNotEmpty()) {
                        accumulateToken(chunk)
                        UIRenderer.onToken(chunk)
                    }
                }
                "stream_end" -> {
                    val quotaJson = payload?.optJSONObject("quota")
                    quotaJson?.let {
                        val quota = com.infomate.app.ui.QuotaInfo(
                            requestsUsed = it.optInt("requestsUsed"),
                            requestsLimit = it.optInt("requestsLimit"),
                            tokensUsed = it.optLong("tokensUsed")
                        )
                        UIRenderer.onQuotaUpdate(quota)
                    }
                    terminateStream()
                    UIRenderer.onComplete(SessionManager.partialResponse.toString())
                }
                "error", "phx_error" -> {
                    terminateStream()
                    UIRenderer.onError(payload?.optString("message", "Neural Link Protocol Error"))
                }
            }
        } catch (e: Exception) {
            // Fallback: If it fails to parse as JSON but has content, treat as raw token
            accumulateToken(raw)
            UIRenderer.onToken(raw)
        }
    }

    private fun accumulateToken(token: String) {
        SessionManager.partialResponse.append(token)
        SessionManager.lastSequence++
        
        // 3.6 Crash Safe State Persistence
        appContext?.let {
            PersistenceManager.savePartialResponse(it, SessionManager.partialResponse.toString())
        }
    }

    // STEP 4 — Add timeout handling / Safe Termination
    fun terminateStream() {
        if (state == AIState.IDLE) return
        
        state = AIState.IDLE
        ReliabilitySDK.stopStreamService()
        // Clear last request to prevent "ghost" resumes (STEP 5)
        SessionManager.lastRequestId = null
        appContext?.let {
            SessionManager.save(it)
        }
    }
}
