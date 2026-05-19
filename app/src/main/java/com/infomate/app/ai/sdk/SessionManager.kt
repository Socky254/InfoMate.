package com.infomate.app.ai.sdk

import android.content.Context
import com.infomate.app.storage.PersistenceManager
import java.util.UUID

object SessionManager {
    var sessionId: String? = null
    var lastRequestId: String? = null
    var lastSequence: Int = 0
    val partialResponse = StringBuilder()

    fun init(context: Context) {
        val saved = PersistenceManager.getSession(context)
        sessionId = saved.first ?: UUID.randomUUID().toString()
        lastRequestId = saved.second
        partialResponse.append(PersistenceManager.getPartialResponse(context))
        
        if (saved.first == null) {
            PersistenceManager.saveSession(context, sessionId!!, lastRequestId)
        }
    }

    fun save(context: Context) {
        PersistenceManager.saveSession(context, sessionId ?: "", lastRequestId)
        PersistenceManager.savePartialResponse(context, partialResponse.toString())
    }

    fun reset() {
        lastSequence = 0
        partialResponse.clear()
    }
}
