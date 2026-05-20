package com.infomate.app.ai.sdk

import java.util.concurrent.ConcurrentLinkedQueue

object RetryQueue {
    private val queue = ConcurrentLinkedQueue<String>()

    fun enqueue(msg: String) {
        queue.add(msg)
    }

    fun flush(ws: WsClientManager) {
        while (queue.isNotEmpty()) {
            queue.poll()?.let {
                ws.send(it)
            }
        }
    }
}
