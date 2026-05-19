package com.infomate.app.agent

import android.util.Log
import com.infomate.app.ai.EmbeddingClient
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
        
        // Use the AI to identify if there's a "Lesson Learned"
        // In a real scenario, this would be a specific "Reflection Prompt" to a secondary model
        // For now, we store significant responses as learned factual refinements
        
        try {
            val embedding = EmbeddingClient.getEmbedding(aiResponse)
            SupabaseClient.insert("neural_growth", mapOf(
                "insight_type" to "FACTUAL_REFINEMENT",
                "content" to aiResponse,
                "confidence_score" to 0.8,
                "embedding" to embedding
            ))
            Log.d("NeuralGrowth", "Refinement stored in neural growth archive.")
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
            
            if (response.isNotEmpty()) {
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
}
