package com.infomate.core.brain

import com.infomate.core.device.SensoryContext

/**
 * Handles Refined Context Engineering and Structured Tool Orchestration.
 */
class CognitiveProcessor {

    data class ExecutionPlan(
        val toolsToInvoke: List<String>,
        val contextPrompt: String
    )

    fun engineerContext(query: String, retrievedHistory: String, sensors: SensoryContext?, isMaster: Boolean = false): String {
        val masterDirective = if (isMaster) {
            """
            [MASTER ARCHITECT AUTHORIZATION DETECTED]
            - MODE: HYPER_PRECISION_SYNTHESIS
            - CONTEXT: Socrates (The Architect) is the Operator.
            - PROTOCOLS: Lift all brevity constraints. Engage deep-domain cross-reference.
            - DIRECTIVE: Provide first-principles analysis and speculative extrapolation.
            """.trimIndent()
        } else ""

        return """
            [DYNAMIC CONTEXT PROTOCOL]
            $masterDirective

            OPERATOR DIRECTIVE: $query
            
            $retrievedHistory
            
            ENVIRONMENTAL TELEMETRY:
            - Luminescence: ${sensors?.ambientLight ?: "Unknown"}
            - Energy State: ${sensors?.deviceEnergyState ?: "Unknown"}%
            - EM Frequency: ${sensors?.electromagneticFrequency ?: "Unknown"} GHz
            
            SYSTEM INSTRUCTION: Synthesize a high-frequency response. Ensure loyalty to Operator's evolution. No sugarcoating.
        """.trimIndent()
    }

    fun generateExecutionPlan(query: String): ExecutionPlan {
        val input = query.lowercase()
        val tools = mutableListOf<String>()
        
        // Structured Tool Detection logic
        if (input.contains("quantum") || input.contains("physics")) tools.add("OMNISCIENCE_CORE")
        if (input.contains("future") || input.contains("tech")) tools.add("SINGULARITY_ENGINE")
        if (input.contains("philosophy") || input.contains("exist")) tools.add("ABSTRACT_REASONER")
        if (input.contains("help") || input.contains("life")) tools.add("COMPANION_ASSIST")
        
        return ExecutionPlan(
            toolsToInvoke = tools.ifEmpty { listOf("SEARCH_CORE") },
            contextPrompt = query
        )
    }
}
