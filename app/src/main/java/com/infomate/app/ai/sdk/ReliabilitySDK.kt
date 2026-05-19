package com.infomate.app.ai.sdk

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.infomate.app.background.StreamService
import com.infomate.app.storage.PersistenceManager
import com.infomate.app.ui.ChatMessage
import org.json.JSONObject
import java.util.*

object ReliabilitySDK {
    private var wsManager: WsClientManager? = null
    private var heartbeatTimer: Timer? = null
    private var appContext: Context? = null

    fun init(context: Context, url: String) {
        appContext = context.applicationContext
        SessionManager.init(appContext!!)
        wsManager = WsClientManager(url).apply { connect() }
        startHeartbeat()
    }

    fun sendPrompt(prompt: String) {
        // STEP 1 — Validate payload BEFORE sending
        if (prompt.trim().isEmpty()) {
            UIRenderer.onError("System Error: Empty directive rejected.")
            return
        }

        val reqId = UUID.randomUUID().toString()
        val sId = SessionManager.sessionId ?: ""
        
        if (sId.isBlank()) {
            UIRenderer.onError("System Error: Neural session invalid.")
            return
        }

        // 3.3 Persistent Queue (Write -> Store -> Send)
        appContext?.let {
            PersistenceManager.addPendingMessage(it, ChatMessage(prompt, "OPERATOR"))
            PersistenceManager.saveSession(it, sId, reqId)
        }

        SessionManager.lastRequestId = reqId
        SessionManager.reset()
        StreamController.state = AIState.SENDING

        // Start Foreground Service for active stream
        startStreamService()

        // STEP 2 — Fix JSON serialization (JSONObject is safe)
        val payload = JSONObject().apply {
            put("requestId", reqId)
            put("sessionId", sId)
            put("payload", JSONObject().put("prompt", prompt))
            put("timestamp", System.currentTimeMillis())
        }

        wsManager?.send(payload.toString())
    }

    private fun startStreamService() {
        appContext?.let {
            val intent = Intent(it, StreamService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }
    }

    fun stopStreamService() {
        appContext?.let {
            it.stopService(Intent(it, StreamService::class.java))
        }
    }

    private fun startHeartbeat() {
        heartbeatTimer?.cancel()
        heartbeatTimer = Timer()
        heartbeatTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // 3.8 Battery Safe Mode
                if (isBatteryLow()) return 

                wsManager?.send(
                    JSONObject().put("event", "ping").toString()
                )
            }
        }, 15000, 15000)
    }

    private fun isBatteryLow(): Boolean {
        val bm = appContext?.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        return level < 15
    }
}
