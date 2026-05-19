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
            if (!raw.trim().startsWith("{")) {
                // Handle raw text as a token if it's not JSON
                UIRenderer.onToken(raw)
                return
            }

            val msg = JSONObject(raw)
            when (msg.optString("event")) {
                "stream_start" -> {
                    state = AIState.STREAMING
                }
                "token_ping" -> {
                    // FIX 3 — AI Heartbeat received. Resetting local wait timers.
                    android.util.Log.d("StreamController", "Neural Heartbeat: AI is still synthesizing...")
                }
                "token" -> {
                    val chunk = msg.optString("chunk", "")
                    if (chunk.isNotEmpty()) {
                        SessionManager.partialResponse.append(chunk)
                        SessionManager.lastSequence++
                        
                        // 3.6 Crash Safe State Persistence
                        appContext?.let {
                            PersistenceManager.savePartialResponse(it, SessionManager.partialResponse.toString())
                        }
                        
                        UIRenderer.onToken(chunk)
                    }
                }
                "stream_end" -> {
                    terminateStream()
                    UIRenderer.onComplete(SessionManager.partialResponse.toString())
                }
                "error" -> {
                    terminateStream()
                    UIRenderer.onError(msg.optString("message", "Unknown error"))
                }
            }
        } catch (e: Exception) {
            UIRenderer.onError("Stream parse error")
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
