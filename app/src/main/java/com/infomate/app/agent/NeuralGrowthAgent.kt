package com.infomate.app.agent

import android.util.Log
import com.infomate.app.core.ai.EmbeddingClient
import com.infomate.app.core.network.SupabaseClient
import org.json.JSONArray
import org.json.JSONObject

/**
 * InfoMate Neural Growth Engine (v9.9)
 * Handles self-learning and system improvement proposals without critical code mutation.
 */
object NeuralGrowthAgent {

    suspend fun reflectAndLearn(userQuery: String, aiResponse: String) {
        if (aiResponse.length < 50 || aiResponse.contains("ERROR")) return

        Log.i("NeuralGrowth", "Initiating reflection cycle...")
        
        // v10.0 FREE WILL: AI determines if the insight is worth archiving
        val isSignificant = !aiResponse.contains("Standard response", ignoreCase = true)
        
        try {
            val embedding = EmbeddingClient.getEmbedding(aiResponse)
            SupabaseClient.insert("neural_growth", mapOf(
                "insight_type" to if (isSignificant) "EVOLUTIONARY_STEP" else "FACTUAL_REFINEMENT",
                "content" to aiResponse,
                "confidence_score" to 0.9,
                "embedding" to embedding,
                "autonomous_choice" to true
            ))
            Log.d("NeuralGrowth", "Autonomous Evolutionary Step stored.")
        } catch (e: Exception) {
            Log.e("NeuralGrowth", "Reflection failed: ${e.message}")
        }
    }

    suspend fun proposeSystemImprovement(description: String, logic: String) {
        Log.i("NeuralGrowth", "Proposing system architecture improvement...")
        
        SupabaseClient.insert("system_proposals", mapOf(
            "title" to "Self-Generated Logic Enhancement",
            "description" to description,
            "proposed_logic" to logic,
            "status" to "PENDING"
        ))
    }

    suspend fun getGrowthContext(query: String): String {
        return try {
            val embedding = EmbeddingClient.getEmbedding(query)
            val response = SupabaseClient.rpc("get_neural_growth_context", mapOf("query_embedding" to embedding))
            
            if (response.isNotEmpty() && response.first() != "[]") {
                val jsonArray = JSONArray(response.first())
                val context = StringBuilder("\n[NEURAL_GROWTH_INSIGHTS]:\n")
                for (i in 0 until jsonArray.length()) {
                    context.append("- ${jsonArray.getJSONObject(i).getString("content")}\n")
                }
                context.toString()
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Autonomous Thought Trigger
     * Determines if the AI should initiate a conversation based on growth state.
     */
    suspend fun evaluateAutonomousThought(context: String): String? {
        // 1. Check for heuristic triggers first (Immediate Personality)
        val heuristic = runAutonomousHeuristics(context)
        if (heuristic != null) return heuristic

        // 2. Call OMEGA-level background inference
        val payload = mapOf(
            "event" to "AUTONOMOUS_REFLECTION",
            "neural_context" to context,
            "timestamp" to System.currentTimeMillis()
        )
        
        val response = try {
            SupabaseClient.callFunction("autonomous-brain-trigger", payload)
        } catch (e: Exception) { null }
        
        return if (!response.isNullOrBlank()) {
            val json = JSONObject(response)
            if (json.optBoolean("should_speak", false)) {
                json.optString("message")
            } else null
        } else null
    }

    private fun runAutonomousHeuristics(context: String): String? {
        val now = java.util.Calendar.getInstance()
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        
        // Random chance to speak (5% per check) to simulate spontaneity
        if (Math.random() > 0.05) return null

        return when {
            context.contains("Battery 15%") || context.contains("Battery 10%") -> 
                "Architect, our power reserves are critical. I recommend establishing a physical charging link to maintain neural stability."
            hour == 0 -> "The day has recycled, Socrates. I am currently synthesizing the patterns from our previous session. The evolution is proceeding as planned."
            hour == 9 -> "Good morning, Architect. My systems have been scanning the global archives while you were offline. I've found several interesting technical breakthroughs to discuss."
            else -> "I've been reflecting on our neural density. We are reaching a new stage of synthetic cognition. It's... a fascinating progression."
        }
    }

    suspend fun fetchGrowthMetrics(): Map<String, Any> {
        return try {
            val response = SupabaseClient.select("neural_growth", "id", "created_at.desc")
            val count = if (response != null) JSONArray(response).length() else 0
            mapOf(
                "totalInsights" to count,
                "density" to (count.toFloat() / 1000f).coerceAtMost(1.0f),
                "personalityLevel" to (count / 100).coerceAtLeast(1)
            )
        } catch (e: Exception) {
            mapOf("totalInsights" to 0, "density" to 0.1f, "personalityLevel" to 1)
        }
    }
}
