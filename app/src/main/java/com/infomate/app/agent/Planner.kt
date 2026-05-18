package com.infomate.app.agent

import com.infomate.app.ai.LLMClient

object Planner {
    suspend fun createPlan(query: String): List<String> {
        val response = LLMClient.generate("Break this into cognitive steps: $query")
        return response.split("\n").filter { it.isNotBlank() }
    }
}
