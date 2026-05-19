package com.infomate.app.ai.sdk

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WsClientManager(private val url: String) {
    private var ws: WebSocketClient? = null
    var isConnected = false
        private set

    fun connect() {
        try {
            ws = object : WebSocketClient(URI(url)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    isConnected = true
                    RetryQueue.flush(this@WsClientManager)
                    
                    // 3.7 Exact Reconnect Logic (Resume Only)
                    if (SessionManager.lastRequestId != null) {
                        Log.d("WsClientManager", "Resuming session context for ${SessionManager.lastRequestId}")
                        // Send resume event instead of re-sending prompt
                        send(org.json.JSONObject().apply {
                            put("event", "resume")
                            put("requestId", SessionManager.lastRequestId)
                            put("sessionId", SessionManager.sessionId)
                        }.toString())
                    }
                }

                override fun onMessage(message: String?) {
                    StreamController.handle(message)
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    isConnected = false
                    reconnect()
                }

                override fun onError(ex: Exception?) {
                    isConnected = false
                    reconnect()
                }
            }
            ws?.connect()
        } catch (e: Exception) {
            reconnect()
        }
    }

    fun send(data: String) {
        if (isConnected) {
            ws?.send(data)
        } else {
            RetryQueue.enqueue(data)
        }
    }

    private fun reconnect() {
        StreamController.state = AIState.RECONNECTING
        // Delay and reconnect
        Thread {
            try {
                Thread.sleep(3000)
                connect()
            } catch (e: Exception) {}
        }.start()
    }
}
