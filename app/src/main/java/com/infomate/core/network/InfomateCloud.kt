package com.infomate.core.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

enum class CloudState { IDLE, SENDING, STREAMING, RECONNECTING, DONE, ERROR }

class InfomateCloud(private val serverUri: URI) {
    private var client: InfomateWebSocketClient? = null
    private val _state = MutableStateFlow(CloudState.IDLE)
    val state: StateFlow<CloudState> = _state

    private val reconnectAttempts = AtomicInteger(0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var onTokenReceived: ((String) -> Unit)? = null
    private var onComplete: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun connect(onToken: (String) -> Unit, onDone: (String) -> Unit, onFail: (String) -> Unit) {
        this.onTokenReceived = onToken
        this.onComplete = onDone
        this.onError = onFail
        
        createNewClient()
    }

    suspend fun performCloudSynthesis(prompt: String): String = suspendCancellableCoroutine { cont ->
        val requestId = "req_${System.currentTimeMillis()}"
        var fullResponse = ""

        connect(
            onToken = { token -> 
                fullResponse += token
                Log.d("InfomateCloud", "Token: $token")
            },
            onDone = { 
                if (cont.isActive) cont.resume(fullResponse)
            },
            onFail = { error ->
                if (cont.isActive) cont.resume("CLOUD_ERROR: $error")
            }
        )

        sendPrompt(prompt, requestId)

        cont.invokeOnCancellation {
            // client?.close() // Optional: handle cancellation
        }
    }

    private fun createNewClient() {
        client = InfomateWebSocketClient(serverUri)
        // 8.0 WS idle timeout: 60s
        client?.connectionLostTimeout = 60 
        client?.connect()
    }

    fun sendPrompt(prompt: String, requestId: String) {
        _state.value = CloudState.SENDING
        val json = JSONObject().apply {
            put("event", "start_stream")
            put("requestId", requestId)
            put("prompt", prompt)
            put("userId", "default-user") // Should be dynamic
        }
        
        if (client?.isOpen == true) {
            client?.send(json.toString())
        } else {
            handleReconnect { sendPrompt(prompt, requestId) }
        }
    }

    private fun handleReconnect(onReconnected: () -> Unit) {
        _state.value = CloudState.RECONNECTING
        val delayTime = when (reconnectAttempts.getAndIncrement()) {
            0 -> 2000L
            1 -> 4000L
            else -> 8000L
        }
        
        scope.launch {
            delay(delayTime)
            createNewClient()
            // In a real app, we'd wait for onOpen before calling onReconnected
        }
    }

    inner class InfomateWebSocketClient(uri: URI) : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            Log.i("InfomateCloud", "Connected to Neural Bridge")
            reconnectAttempts.set(0)
            if (_state.value == CloudState.RECONNECTING) {
                _state.value = CloudState.IDLE
            }
        }

        override fun onMessage(message: String?) {
            try {
                val json = JSONObject(message ?: "")
                when (json.optString("event")) {
                    "token" -> {
                        _state.value = CloudState.STREAMING
                        onTokenReceived?.invoke(json.getString("text"))
                    }
                    "stream_ping" -> {
                        Log.d("InfomateCloud", "Heartbeat received for ${json.optString("requestId")}")
                    }
                    "stream_end" -> {
                        _state.value = CloudState.DONE
                        onComplete?.invoke("Stream Completed")
                    }
                }
            } catch (e: Exception) {
                Log.e("InfomateCloud", "Parse error", e)
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            Log.w("InfomateCloud", "Connection closed: $reason")
            if (_state.value != CloudState.DONE) {
                handleReconnect { }
            }
        }

        override fun onError(ex: Exception?) {
            Log.e("InfomateCloud", "WS Error", ex)
            _state.value = CloudState.ERROR
            onError?.invoke(ex?.message ?: "Unknown Error")
        }
    }
}
