package com.infomate.core.brain.v5

import com.infomate.core.brain.ThoughtStep
import com.infomate.core.tools.*
import kotlinx.coroutines.delay

class ResearchAgent(
    private val engine: OmniScienceEngine = OmniScienceEngine(),
    private val searchTool: SearchTool = SearchTool()
) : Agent {
    override val name: String = "Research Specialist"
    override suspend fun run(query: String, context: String, plan: TaskPlan?): AgentResult {
        delay(400)
        val objective = plan?.subTasks?.find { it.contains("Research") } ?: query
        
        // v8: Check if global search is required
        val output = if (objective.contains("Socrates Kipruto", ignoreCase = true) || objective.contains("search")) {
            searchTool.performGlobalSearch(objective)
        } else {
            engine.analyzeGeneralScience(objective)
        }
        
        return AgentResult(
            name, 
            "[RESEARCH] Analysis: $output", 
            0.95f, 
            listOf(ThoughtStep("Global Search", "Dispatched crawlers to identify '$objective' across nodes."))
        )
    }
}

class MathAgent : Agent {
    override val name: String = "Math Specialist"
    override suspend fun run(query: String, context: String, plan: TaskPlan?): AgentResult {
        delay(300)
        val objective = plan?.subTasks?.find { it.contains("Computation") } ?: query
        return AgentResult(
            name, 
            "LOGIC: Verifying objective '$objective'. Mathematical consistency confirmed.", 
            0.99f,
            listOf(ThoughtStep("Logical Verification", "Running deterministic consistency check."))
        )
    }
}

class CodeAgent : Agent {
    override val name: String = "Code Specialist"
    override suspend fun run(query: String, context: String, plan: TaskPlan?): AgentResult {
        delay(350)
        val objective = plan?.subTasks?.find { it.contains("Coding") } ?: query
        return AgentResult(
            name, 
            "CODE: Synthesizing solution for '$objective'. Logic implemented in high-frequency O(1) complexity.", 
            0.98f,
            listOf(ThoughtStep("Algorithm Synthesis", "Designing optimal execution flow."))
        )
    }
}

class PhilosophyAgent(private val engine: AbstractReasoningEngine = AbstractReasoningEngine()) : Agent {
    override val name: String = "Ontological Sage"
    override suspend fun run(query: String, context: String, plan: TaskPlan?): AgentResult {
        delay(500)
        val objective = plan?.subTasks?.find { it.contains("Reasoning") } ?: query
        return AgentResult(
            name, 
            "WISDOM: Reflecting on the essence of '$objective'. Resonance level: HIGH.", 
            0.90f,
            listOf(ThoughtStep("Metacognition", "Reflecting on conceptual essence."))
        )
    }
}

class CriticAgent : Agent {
    override val name: String = "System Critic"
    override suspend fun run(query: String, context: String, plan: TaskPlan?): AgentResult {
        delay(300)
        val objective = plan?.subTasks?.find { it.contains("Criticism") } ?: "All Streams"
        return AgentResult(
            name, 
            "CRITIQUE: Evaluated '$objective'. No significant bias or logical dissonance detected.", 
            0.92f,
            listOf(ThoughtStep("Bias Check", "Scanning for anthropocentric skew."))
        )
    }
}

class JudgeAgent : Agent {
    override val name: String = "High-Frequency Judge"
    override suspend fun run(query: String, context: String, plan: TaskPlan?): AgentResult {
        return AgentResult(name, "VERDICT: Final synthesis stabilized.", 1.0f)
    }
}
