package com.infomate.app.ai.sdk

import android.util.Log

interface AIEventsListener {
    fun onToken(text: String)
    fun onComplete(fullText: String)
    fun onError(error: String)
    fun onStateChange(state: AIState)
    fun onQuotaUpdate(quota: com.infomate.app.ui.QuotaInfo)
}

object UIRenderer {
    private var listener: AIEventsListener? = null

    fun setListener(l: AIEventsListener) {
        listener = l
    }

    fun onToken(text: String) {
        Log.d("UIRenderer", "Token: $text")
        listener?.onToken(text)
    }

    fun onComplete(fullText: String) {
        Log.d("UIRenderer", "Complete")
        listener?.onComplete(fullText)
    }

    fun onError(error: String) {
        Log.e("UIRenderer", "Error: $error")
        listener?.onError(error)
    }
    
    fun onStateChange(state: AIState) {
        listener?.onStateChange(state)
    }

    fun onQuotaUpdate(quota: com.infomate.app.ui.QuotaInfo) {
        listener?.onQuotaUpdate(quota)
    }
}
