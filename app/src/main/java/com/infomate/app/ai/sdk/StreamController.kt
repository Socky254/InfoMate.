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
        if (raw == null) return
        
        try {
            val msg = JSONObject(raw)
            when (msg.optString("event")) {
                "stream_start" -> {
                    state = AIState.STREAMING
                }
                "token" -> {
                    val chunk = msg.optString("chunk", "")
                    SessionManager.partialResponse.append(chunk)
                    SessionManager.lastSequence++
                    
                    // 3.6 Crash Safe State Persistence
                    appContext?.let {
                        PersistenceManager.savePartialResponse(it, SessionManager.partialResponse.toString())
                    }
                    
                    UIRenderer.onToken(chunk)
                }
                "stream_end" -> {
                    state = AIState.DONE
                    ReliabilitySDK.stopStreamService()
                    UIRenderer.onComplete(SessionManager.partialResponse.toString())
                }
                "error" -> {
                    state = AIState.ERROR
                    ReliabilitySDK.stopStreamService()
                    UIRenderer.onError(msg.optString("message", "Unknown error"))
                }
            }
        } catch (e: Exception) {
            UIRenderer.onError("Stream parse error")
        }
    }
}
