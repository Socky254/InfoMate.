package com.infomate.core.memory

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class KnowledgeNode(
    val id: String,
    val concept: String,
    val connections: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val valence: Float = 0.5f // Importance/Weight
)

class CognitiveArchive(private val context: Context) {
    private val gson = Gson()
    private val archiveFile = File(context.filesDir, "cognitive_archive.json")
    private var knowledgeGraph: MutableMap<String, KnowledgeNode> = mutableMapOf()

    init {
        loadArchive()
    }

    private fun loadArchive() {
        if (archiveFile.exists()) {
            try {
                val json = archiveFile.readText()
                val type = object : TypeToken<Map<String, KnowledgeNode>>() {}.type
                knowledgeGraph = gson.fromJson(json, type)
                Log.i("CognitiveArchive", "Neural Graph Restored: ${knowledgeGraph.size} nodes.")
            } catch (e: Exception) {
                Log.e("CognitiveArchive", "Archive Corruption Detected. Initializing fresh graph.")
            }
        }
    }

    fun storeNode(concept: String, relations: List<String>, importance: Float = 0.5f) {
        val id = "node_${System.currentTimeMillis()}"
        val node = KnowledgeNode(id, concept, relations, valence = importance)
        knowledgeGraph[id] = node
        saveArchive()
    }

    private fun saveArchive() {
        val json = gson.toJson(knowledgeGraph)
        archiveFile.writeText(json)
    }

    fun getNeuralSummary(): String {
        return knowledgeGraph.values.toList().takeLast(3).joinToString(" → ") { it.concept }
    }
    
    fun getRecentTopicsDetailed(): List<KnowledgeNode> {
        return knowledgeGraph.values.toList()
    }

    fun getRecentTopics(): List<String> {
        return if (knowledgeGraph.isEmpty()) {
            listOf("Quantum Decoherence", "Mars Colony Logistics", "Riemann Hypothesis")
        } else {
            knowledgeGraph.values.sortedByDescending { it.timestamp }.take(4).map { it.concept }
        }
    }
}
