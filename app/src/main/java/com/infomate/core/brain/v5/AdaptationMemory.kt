package com.infomate.core.brain.v5

import com.infomate.core.memory.CognitiveArchive
import android.util.Log

class AdaptationMemory(private val archive: CognitiveArchive) {

    suspend fun record(query: String, bestAgent: String, score: Float) {
        Log.d("AdaptationMemory", "Recording performance: $bestAgent scored $score for query: $query")
        
        // Storing in existing archive
        archive.storeNode(
            concept = "PERFORMANCE_LOG",
            relations = listOf("QUERY:$query", "AGENT:$bestAgent", "SCORE:$score"),
            importance = score
        )
    }
}
