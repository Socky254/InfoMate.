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

import com.infomate.core.data.database.CognitiveDao
import com.infomate.core.data.database.CognitiveNodeEntity
import com.infomate.core.data.database.InfomateDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CognitiveArchive(private val context: Context) {
    private val database = InfomateDatabase.getDatabase(context)
    private val dao = database.cognitiveDao()

    fun storeNode(concept: String, relations: List<String>, importance: Float = 0.5f, ambientLight: Float = 0f, noiseLevel: Double = 0.0) {
        CoroutineScope(Dispatchers.IO).launch {
            val node = CognitiveNodeEntity(
                id = "node_${System.currentTimeMillis()}",
                concept = concept,
                connections = relations.joinToString(","),
                timestamp = System.currentTimeMillis(),
                importance = importance,
                ambientLight = ambientLight,
                noiseLevel = noiseLevel
            )
            dao.insertNode(node)
            Log.i("CognitiveArchive", "Neural Node Persisted to Relational Memory: $concept")
        }
    }

    suspend fun getRecentTopics(): List<String> = withContext(Dispatchers.IO) {
        val nodes = dao.getRecentNodes()
        if (nodes.isEmpty()) {
            listOf("Quantum Decoherence", "Mars Colony Logistics", "Riemann Hypothesis")
        } else {
            nodes.map { it.concept }
        }
    }

    suspend fun getNodesFromDarkness(threshold: Float): List<String> = withContext(Dispatchers.IO) {
        dao.getNodesFromDarkEnvironment(threshold).map { it.concept }
    }
}
