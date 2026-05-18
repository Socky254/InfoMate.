package com.infomate.core.brain.v5

import com.infomate.core.brain.ThoughtStep

data class TaskProfile(
    val complexity: Complexity,
    val domain: Domain,
    val requiredExpertise: List<String>,
    val riskLevel: RiskLevel
)

enum class Complexity { LOW, MEDIUM, HIGH }
enum class Domain { MATH, CODING, RESEARCH, GENERAL, PHILOSOPHY, COSMOS }
enum class RiskLevel { LOW, HIGH }

interface Agent {
    val name: String
    suspend fun run(query: String, context: String = "", plan: TaskPlan? = null): AgentResult
}

data class AgentResult(
    val agentName: String,
    val output: String,
    val confidence: Float,
    val reasoning: List<ThoughtStep> = emptyList()
)

data class ScoredResult(
    val output: String,
    val score: Float,
    val agentName: String
)
