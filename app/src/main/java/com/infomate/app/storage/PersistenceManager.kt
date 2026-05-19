package com.infomate.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.infomate.app.ui.ChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PersistenceManager {
    private const val PREFS_NAME = "infomate_production_prefs"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_LAST_REQUEST_ID = "last_request_id"
    private const val KEY_PARTIAL_TEXT = "partial_text"
    private const val KEY_PENDING_MESSAGES = "pending_messages"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(context: Context, sessionId: String, lastRequestId: String?) {
        getPrefs(context).edit().apply {
            putString(KEY_SESSION_ID, sessionId)
            putString(KEY_LAST_REQUEST_ID, lastRequestId)
            apply()
        }
    }

    fun getSession(context: Context): Pair<String?, String?> {
        val prefs = getPrefs(context)
        return Pair(prefs.getString(KEY_SESSION_ID, null), prefs.getString(KEY_LAST_REQUEST_ID, null))
    }

    fun savePartialResponse(context: Context, text: String) {
        getPrefs(context).edit().putString(KEY_PARTIAL_TEXT, text).apply()
    }

    fun getPartialResponse(context: Context): String {
        return getPrefs(context).getString(KEY_PARTIAL_TEXT, "") ?: ""
    }

    fun addPendingMessage(context: Context, message: ChatMessage) {
        val pending = getPendingMessages(context).toMutableList()
        pending.add(message)
        savePendingMessages(context, pending)
    }

    fun getPendingMessages(context: Context): List<ChatMessage> {
        val json = getPrefs(context).getString(KEY_PENDING_MESSAGES, null) ?: return emptyList()
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearPendingMessages(context: Context) {
        getPrefs(context).edit().remove(KEY_PENDING_MESSAGES).apply()
    }

    private fun savePendingMessages(context: Context, messages: List<ChatMessage>) {
        val json = gson.toJson(messages)
        getPrefs(context).edit().putString(KEY_PENDING_MESSAGES, json).apply()
    }
}
