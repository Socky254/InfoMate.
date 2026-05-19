package com.infomate.app.ai.sdk

object RetryQueue {
    private val queue = mutableListOf<String>()

    fun enqueue(msg: String) {
        queue.add(msg)
    }

    fun flush(ws: WsClientManager) {
        val copy = queue.toList()
        queue.clear()
        copy.forEach {
            ws.send(it)
        }
    }
}
