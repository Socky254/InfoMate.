package com.infomate.app.core.network

import android.util.Log
import com.infomate.app.core.config.Config
import okhttp3.*
import okio.ByteString

class WebSocketBrain(
    private val url: String = Config.WEBSOCKET_BRAIN_URL
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    interface BrainListener {
        fun onToken(token: String)
        fun onStateUpdate(state: String)
        fun onError(error: String)
        fun onClosed()
    }

    fun connect(listener: BrainListener) {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketBrain", "Connected to Brain WebSocket")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Simplified handling: tokens or state updates
                if (text.startsWith("TOKEN:")) {
                    listener.onToken(text.removePrefix("TOKEN:"))
                } else if (text.startsWith("STATE:")) {
                    listener.onStateUpdate(text.removePrefix("STATE:"))
                } else {
                    listener.onToken(text) // Default to token
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d("WebSocketBrain", "Binary message received")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                listener.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketBrain", "Error: ${t.message}")
                listener.onError(t.message ?: "Unknown error")
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
    }
}
