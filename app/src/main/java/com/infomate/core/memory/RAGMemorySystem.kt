package com.infomate.core.memory

import android.util.Log
import kotlin.math.ln

/**
 * Advanced RAG Memory System for INFOMATE.
 * Implements semantic retrieval, ranking, and importance scoring.
 */
class RAGMemorySystem(private val archive: CognitiveArchive) {

    fun retrieveRelevantContext(query: String, config: com.infomate.core.brain.SystemConfig): String {
        val keywords = query.lowercase().split(" ").filter { it.length > 3 }
        val allNodes = archive.getRecentTopicsDetailed()
        
        Log.i("RAGMemory", "Retrieving context for: '$query' [Mode: ${config.contextRetrieval}]")

        val scoredNodes = allNodes.map { node ->
            // 1. Relevance Score (Keyword match density)
            val relevance = keywords.count { node.concept.lowercase().contains(it) }.toFloat()
            
            // 2. Recency Score (Time decay)
            val ageInHours = (System.currentTimeMillis() - node.timestamp) / (1000 * 60 * 60)
            val recency = 1.0f / (1.0f + ageInHours.toFloat())
            
            // 3. Importance (Valence)
            val importance = node.valence

            // Total Rank Calculation
            val totalScore = (relevance * 0.5f) + (recency * 0.2f) + (importance * 0.3f)
            node to totalScore
        }.filter { it.second > 0 }
         .sortedByDescending { it.second }
         .take(5) // Top-K results

        if (scoredNodes.isEmpty()) return "[SYSTEM_INFO]: No high-confidence historical links found."

        val contextBuilder = StringBuilder("[RANKED HISTORICAL CONTEXT (Top-${scoredNodes.size})]\n")
        scoredNodes.forEachIndexed { index, pair ->
            val node = pair.first
            val score = pair.second
            val displayIndex = index + 1
            contextBuilder.append("$displayIndex. CONCEPT: ${node.concept} [Score: ${"%.2f".format(score)}] -> Connections: ${node.connections.joinToString(", ")}\n")
        }
        
        return contextBuilder.toString()
    }
    
    fun compressContext(context: String): String {
        // Basic summarization for large context windows
        if (context.length < 500) return context
        return "[COMPRESSED]: " + context.lines().take(5).joinToString("\n") + "\n[...Summary of older memory nodes...]"
    }
}
