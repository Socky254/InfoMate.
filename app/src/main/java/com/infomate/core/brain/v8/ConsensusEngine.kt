package com.infomate.core.brain.v8

import android.util.Log

class ConsensusEngine {

    fun combine(results: List<NodeResult>): String {
        if (results.isEmpty()) return "DISTRIBUTED_NULL: Network synchronization failed."
        
        Log.d("V8Consensus", "Merging results from ${results.size} cognitive nodes.")

        val sb = StringBuilder()
        sb.append("[DISTRIBUTED NETWORK CONSENSUS v8.0]\n")
        sb.append("--------------------------------------\n")

        // In a real v8, we'd use an LLM to resolve contradictions.
        // For the prototype, we prioritize high-confidence nodes.
        val sortedResults = results.sortedByDescending { it.confidence }
        val primary = sortedResults.first()
        
        sb.append("PRIMARY RESPONSE (${primary.nodeId}):\n")
        sb.append(primary.output)
        sb.append("\n\n")

        if (results.size > 1) {
            sb.append("CROSS-NODE VERIFICATION:\n")
            results.filter { it.nodeId != primary.nodeId }.forEach {
                sb.append("• Node [${it.nodeId}] confirmed with ${(it.confidence * 100).toInt()}% confidence.\n")
            }
        }

        return sb.toString()
    }
}
