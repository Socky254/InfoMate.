package com.infomate.app.ai.sdk

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI

class WsClientManager(private val url: String) {
    private var ws: WebSocketClient? = null
    var isConnected = false
        private set
    private var ref = 0

    fun connect() {
        try {
            ws = object : WebSocketClient(URI(url)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    isConnected = true
                    Log.d("WsClientManager", "Neural Link Established")
                    
                    // Supabase Realtime requires joining a channel to receive broadcasts
                    joinChannel("realtime:infomate")
                    
                    RetryQueue.flush(this@WsClientManager)
                    
                    if (SessionManager.lastRequestId != null) {
                        resumeSession(SessionManager.lastRequestId!!, SessionManager.sessionId ?: "")
                    }
                }

                override fun onMessage(message: String?) {
                    Log.d("WsClientManager", "Raw Data: $message")
                    try {
                        val json = JSONObject(message)
                        val event = json.optString("event")
                        
                        // Handle Phoenix protocol messages
                        if (event == "phx_reply") return
                        
                        // Extract actual payload from Phoenix envelope if it exists
                        val payload = if (json.has("payload")) {
                            val p = json.getJSONObject("payload")
                            when {
                                p.has("response") -> p.get("response").toString()
                                p.has("content") -> p.get("content").toString()
                                else -> json.get("payload").toString()
                            }
                        } else {
                            message
                        }
                        
                        StreamController.handle(payload ?: "")
                    } catch (e: Exception) {
                        StreamController.handle(message ?: "")
                    }
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

    private fun joinChannel(topic: String) {
        val joinMsg = JSONObject().apply {
            put("topic", topic)
            put("event", "phx_join")
            put("payload", JSONObject())
            put("ref", (++ref).toString())
        }
        sendRaw(joinMsg.toString())
    }

    private fun resumeSession(requestId: String, sessionId: String) {
        val resumeMsg = JSONObject().apply {
            put("topic", "realtime:infomate")
            put("event", "resume")
            put("payload", JSONObject().apply {
                put("requestId", requestId)
                put("sessionId", sessionId)
            })
            put("ref", (++ref).toString())
        }
        sendRaw(resumeMsg.toString())
    }

    fun send(data: String) {
        // Wrap data in Phoenix broadcast format for Supabase Realtime
        try {
            val originalPayload = JSONObject(data)
            val broadcastMsg = JSONObject().apply {
                put("topic", "realtime:infomate")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "chat")
                    put("content", originalPayload)
                })
                put("ref", (++ref).toString())
            }
            sendRaw(broadcastMsg.toString())
        } catch (e: Exception) {
            sendRaw(data)
        }
    }

    private fun sendRaw(data: String) {
        if (isConnected) {
            ws?.send(data)
        } else {
            RetryQueue.enqueue(data)
        }
    }

    private fun reconnect() {
        StreamController.state = AIState.RECONNECTING
        Thread {
            try {
                Thread.sleep(3000)
                connect()
            } catch (e: Exception) {}
        }.start()
    }
}
