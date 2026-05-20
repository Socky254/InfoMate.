package com.infomate.app.core.memory

import android.content.Context
import com.infomate.app.storage.MemoryEntry
import com.infomate.app.storage.WarmDatabase

/**
 * PHASE 5: Memory System Logic
 */
class MemoryRepository(context: Context) {
    private val warmDao = WarmDatabase.getDatabase(context).warmDao()

    suspend fun store(input: String, output: String) {
        val entry = MemoryEntry(
            input = input,
            response = output
        )
        warmDao.insertMemory(entry)
    }

    suspend fun buildContext(input: String): String {
        // Fetch last 5 relevant interactions for context window optimization (PHASE 10)
        val recent = warmDao.getRecentMemories(5)
        if (recent.isEmpty()) return "No prior context."
        
        return recent.reversed().joinToString("\n") { 
            "User: ${it.input}\nInfoMate: ${it.response}"
        }
    }
}
