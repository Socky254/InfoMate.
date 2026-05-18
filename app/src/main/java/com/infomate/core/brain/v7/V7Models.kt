package com.infomate.core.brain.v7

import com.infomate.core.brain.v5.Agent
import com.infomate.core.brain.v5.Complexity

data class SystemPlan(
    val requiredAgents: List<String>,
    val agentCount: Int,
    val isParallel: Boolean,
    val reasoningDepth: Int,
    val complexity: Complexity
) {
    fun describe(): String = "Plan: Agents=$requiredAgents, Count=$agentCount, Parallel=$isParallel, Depth=$reasoningDepth"
}

data class AgentGraph(
    val nodes: List<Agent>
)

data class MetaRecord(
    val taskType: String,
    val architectureDescription: String,
    val efficiencyScore: Float
)

sealed class SystemChange {
    object WeightAdjustment : SystemChange()
    object MemoryUpdate : SystemChange()
    object CoreLoopModification : SystemChange()
    object SafetyProtocolChange : SystemChange()
}
