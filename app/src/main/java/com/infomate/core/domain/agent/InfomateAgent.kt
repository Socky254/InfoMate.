package com.infomate.core.domain.agent

import com.infomate.core.brain.InfomateBrain
import com.infomate.core.brain.ThoughtStep
import com.infomate.core.domain.model.AgentResponse
import com.infomate.core.memory.CognitiveArchive
import com.infomate.core.device.ContextSensors
import com.infomate.core.tools.ToolRouter
import com.infomate.core.security.ExecutionFirewall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class InfomateAgent(
    private val brain: InfomateBrain,
    private val archive: CognitiveArchive,
    private val sensors: ContextSensors,
    private val toolRouter: ToolRouter,
    private val firewall: ExecutionFirewall = ExecutionFirewall()
) {
    suspend fun process(input: String): AgentResponse = withContext(Dispatchers.Default) {
        try {
            // 1. Sensory Capture
            val sensoryData = sensors.getSensoryContext()

            // Special handling for Greeting/Init intent
            if (input == "INIT_GREETING") {
                val recent = archive.getRecentTopics()
                return@withContext AgentResponse(
                    output = "System online. Greetings, Operator. Active research nodes detected: ${recent.joinToString(", ")}. How shall we proceed with our next cognitive iteration?",
                    steps = listOf(ThoughtStep("Core Init", "Neural pathways synchronized. Context buffer restored.")),
                    recommendation = "Select a research node or initialize a new query."
                )
            }

            if (input == "PROACTIVE_THOUGHT" || input == "SAGE_OBSERVATION") {
                val brainResponse = brain.process(input, sensoryData)
                return@withContext AgentResponse(
                    output = brainResponse.output,
                    steps = brainResponse.steps,
                    recommendation = brainResponse.recommendation,
                    layer = brainResponse.layer,
                    emotionalVector = brainResponse.emotionalVector
                )
            }

            // 2. Brain Reasoning with Environmental modulation
            val brainResponse = brain.process(input, sensoryData)
            
            // 3. Deterministic Tool Orchestration + Firewall Gate
            var finalOutput = brainResponse.output
            var toolExecuted = false
            
            if (brainResponse.output.contains("Quantum") || brainResponse.output.contains("calc")) {
                val toolToUse = if (brainResponse.output.contains("Quantum")) "quantum_sim" else "math"
                
                // Security Check
                when (val validation = firewall.validateToolCall(toolToUse, input)) {
                    is ExecutionFirewall.ValidationResult.Allowed -> {
                        toolRouter.route(toolToUse, input)
                        toolExecuted = true
                        finalOutput += "\n[Verified Tool Execution: $toolToUse]"
                    }
                    is ExecutionFirewall.ValidationResult.Denied -> {
                        finalOutput += "\n[Security Block: ${validation.reason}]"
                    }
                }
            }

            // 4. Archive Update (Persistent Knowledge Graph)
            withContext(Dispatchers.IO) {
                archive.storeNode(
                    concept = input,
                    relations = listOf("sensory_${sensoryData.ambientLight}", "layer_${brainResponse.layer}"),
                    importance = if (brainResponse.layer == "RESEARCH" || brainResponse.layer == "UNIFIED") 0.9f else 0.5f,
                    ambientLight = sensoryData.ambientLight,
                    noiseLevel = sensoryData.acousticNoiseLevel
                )
            }

            AgentResponse(
                output = finalOutput,
                steps = brainResponse.steps,
                requiresTool = toolExecuted,
                recommendation = brainResponse.recommendation,
                media = brainResponse.media,
                layer = brainResponse.layer,
                emotionalVector = brainResponse.emotionalVector
            )
        } catch (e: Exception) {
            Log.e("InfomateAgent", "Loop Failure", e)
            // 5. Fallback Logic: Return cached or safe degradation response
            AgentResponse(
                output = "CRITICAL SYSTEM DEGRADATION: Cognitive loop interrupted. Fallback to basic heuristics. Error trace recorded.",
                steps = emptyList(),
                recommendation = "Suggestion: Re-verify directive or check uplink status."
            )
        }
    }
}
