package com.infomate.app.storage

import android.content.Context
import android.content.SharedPreferences

class LocalMemory(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("infomate_local_cache", Context.MODE_PRIVATE)

    fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun get(key: String): String? {
        return prefs.getString(key, null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
