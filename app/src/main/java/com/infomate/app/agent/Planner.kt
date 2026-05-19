package com.infomate.app.agent

import com.infomate.app.ai.LLMClient

object Planner {
    suspend fun createPlan(query: String): List<String> {
        val result = LLMClient.generate("Break this into cognitive steps: $query")
        return result.output.split("\n").filter { it.isNotBlank() }
    }
}
