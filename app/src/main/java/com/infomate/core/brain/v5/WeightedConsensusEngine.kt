package com.infomate.core.brain.v5

import android.util.Log
import com.infomate.core.brain.v6.AgentRegistry

class WeightedConsensusEngine {

    fun compute(results: List<AgentResult>, query: String, domain: String = "GENERAL"): String {
        if (results.isEmpty()) return "NEURAL_VOYAGE_NULL: No agents returned a valid signal."

        Log.d("Consensus", "Computing consensus for ${results.size} agents in domain: $domain")

        val scored = results.map { result ->
            val score = evaluate(result, query, domain)
            ScoredResult(result.output, score, result.agentName)
        }

        // Judge Selection: Find the highest-scoring output
        val best = scored.maxBy { it.score }
        
        // Multi-Agent synthesis for v4/v5 feel
        val sb = StringBuilder()
        sb.append("[UNIFIED CONSENSUS v6.0-ADAPTIVE]\n")
        sb.append("----------------------------\n")
        
        // Main Answer
        sb.append(best.output)
        sb.append("\n\n")

        // Integration of supporting agents if relevant
        val supporting = scored.filter { it.agentName != best.agentName && it.score > 0.7f }
        if (supporting.isNotEmpty()) {
            sb.append("COLLABORATIVE INSIGHTS:\n")
            supporting.forEach { 
                val label = if (it.output.contains("CRITIQUE")) "• ANALYSIS" else "• SUPPORT"
                sb.append("$label (${it.agentName}): ${it.output.take(150)}...\n") 
            }
        }

        return sb.toString()
    }

    private fun evaluate(result: AgentResult, query: String, domain: String): Float {
        var score = result.confidence
        
        // v6: Apply Trust Weights from Registry
        val trustWeight = AgentRegistry.getWeight(domain, result.agentName)
        score *= trustWeight
        
        // Heuristic: Boost agents that match the query domain
        if (query.contains("math") && result.agentName.contains("Math")) score += 0.1f
        if (query.contains("code") && result.agentName.contains("Code")) score += 0.1f
        if (result.output.contains("CRITIQUE")) score -= 0.05f
        
        return score.coerceIn(0f, 1f)
    }
}
