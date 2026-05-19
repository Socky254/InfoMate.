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

    fun isConnected(): Boolean = wsManager?.isConnected ?: false

    fun init(context: Context, url: String) {
        try {
            appContext = context.applicationContext
            SessionManager.init(appContext!!)
            wsManager = WsClientManager(url).apply { connect() }
            startHeartbeat()
        } catch (e: Exception) {
            android.util.Log.e("ReliabilitySDK", "Failed to initialize: ${e.message}")
        }
    }

    fun sendPrompt(prompt: String) {
        // STEP 1 — Validate payload BEFORE sending (v11.2: Added sanitization)
        val sanitizedPrompt = prompt.trim()
        if (sanitizedPrompt.isEmpty()) {
            UIRenderer.onError("System Error: Empty directive rejected.")
            return
        }

        val reqId = UUID.randomUUID().toString()
        val sId = SessionManager.sessionId ?: ""
        
        if (sId.isBlank()) {
            UIRenderer.onError("System Error: Neural session invalid.")
            com.infomate.app.agent.HealthManager.logHealth(
                com.infomate.app.agent.HealthManager.CAT_AUTH,
                com.infomate.app.agent.HealthState.DEGRADED,
                "Blank sessionId during prompt dispatch",
                com.infomate.app.agent.HealthSeverity.WARNING
            )
            appContext?.let { SessionManager.init(it) }
            return
        }

        // v11.0: Active Connection Check (Auto-reconnect on dispatch)
        if (wsManager?.isConnected == false) {
            android.util.Log.w("ReliabilitySDK", "Neural link is STANDBY. Attempting high-priority reconnection.")
            wsManager?.connect()
        }

        // 3.3 Persistent Queue (Write -> Store -> Send)
        appContext?.let {
            PersistenceManager.addPendingMessage(it, ChatMessage(sanitizedPrompt, "OPERATOR"))
            PersistenceManager.saveSession(it, sId, reqId)
        }

        SessionManager.lastRequestId = reqId
        SessionManager.reset()
        StreamController.state = AIState.SENDING

        // Start Foreground Service for active stream (v11.3: Improved lifecycle)
        startStreamService()

        // STEP 2 — Unified Core Protocol (event: start_stream)
        val payload = JSONObject().apply {
            put("event", "start_stream")
            put("requestId", reqId)
            put("sessionId", sId)
            put("userId", sId)
            put("prompt", sanitizedPrompt)
            put("timestamp", System.currentTimeMillis())
        }

        wsManager?.send(payload.toString())
    }

    private fun startStreamService() {
        try {
            appContext?.let {
                val intent = Intent(it, StreamService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.startForegroundService(intent)
                } else {
                    it.startService(intent)
                }
            }
        } catch (e: Exception) {
            // Android 12+ Background Start Restriction or other Service issues
            android.util.Log.w("ReliabilitySDK", "StreamService start deferred: ${e.message}")
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
