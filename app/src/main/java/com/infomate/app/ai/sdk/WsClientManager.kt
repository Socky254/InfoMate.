package com.infomate.app.ai.sdk

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import kotlinx.coroutines.*

class WsClientManager(private val url: String) {
    private var ws: WebSocketClient? = null
    var isConnected = false
        private set
    
    private var reconnectAttempt = 0
    private val maxReconnectDelay = 8000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    fun connect() {
        if (isConnected) return
        
        // Ensure old client is closed before starting a new one
        try {
            ws?.close()
        } catch (e: Exception) {}

        try {
            Log.d("WsClientManager", "Connecting to Neural Core: $url")
            ws = object : WebSocketClient(URI(url)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    isConnected = true
                    reconnectAttempt = 0
                    Log.i("WsClientManager", "Neural Link Established with Core")
                    
                    // v11.0: Flush any directives queued during stasis
                    RetryQueue.flush(this@WsClientManager)
                }

                override fun onMessage(message: String?) {
                    if (message == null) return
                    Log.d("WsClientManager", "Neural Stream: $message")
                    StreamController.handle(message)
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    if (isConnected) {
                        Log.w("WsClientManager", "Neural Link Severed: $reason (Code: $code)")
                        isConnected = false
                        reconnect()
                    }
                }

                override fun onError(ex: Exception?) {
                    Log.e("WsClientManager", "Neural Link Fault: ${ex?.message}")
                    isConnected = false
                    reconnect()
                }
            }
            ws?.connect()
        } catch (e: Exception) {
            Log.e("WsClientManager", "Immediate Connection Failure: ${e.message}")
            reconnect()
        }
    }

    fun send(data: String) {
        if (isConnected) {
            try {
                ws?.send(data)
            } catch (e: Exception) {
                Log.e("WsClientManager", "Send failed, enqueuing: ${e.message}")
                RetryQueue.enqueue(data)
                reconnect()
            }
        } else {
            Log.w("WsClientManager", "Link Offline. Enqueuing directive.")
            RetryQueue.enqueue(data)
            connect()
        }
    }

    private fun reconnect() {
        if (reconnectJob?.isActive == true) return
        
        StreamController.state = AIState.RECONNECTING
        
        reconnectAttempt++
        val delayTime = when (reconnectAttempt) {
            1 -> 2000L
            2 -> 4000L
            else -> maxReconnectDelay
        }

        reconnectJob = scope.launch {
            Log.d("WsClientManager", "Re-establishing link in ${delayTime}ms (Attempt $reconnectAttempt)")
            delay(delayTime)
            connect()
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        isConnected = false
        ws?.close()
        ws = null
    }
}
