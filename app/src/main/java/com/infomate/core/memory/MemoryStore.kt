package com.infomate.core.memory

import android.util.Log

class MemoryStore {
    private val storage = mutableMapOf<String, Any>()

    fun store(key: String, value: Any) {
        Log.d("MemoryStore", "Storing knowledge: $key")
        storage[key] = value
    }

    fun retrieve(key: String): Any? {
        val data = storage[key]
        if (data != null) Log.d("MemoryStore", "Retrieved context for: $key")
        return data
    }
    
    fun getContinuousContext(): String {
        return storage.values.toList().takeLast(5).joinToString(" | ")
    }

    fun getRecentTopics(): List<String> {
        // Mocking recent topics for the greeting logic
        return listOf("Quantum Decoherence", "Mars Colony Logistics", "Riemann Hypothesis")
    }
}
