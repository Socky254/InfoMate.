package com.infomate.core.brain.v6

data class PerformanceRecord(
    val queryType: String,
    val agentName: String,
    val score: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class ToolEfficiency(
    val toolName: String,
    val successRate: Float,
    val usageCount: Int
)
