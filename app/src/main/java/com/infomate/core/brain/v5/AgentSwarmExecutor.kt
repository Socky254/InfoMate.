package com.infomate.core.brain.v5

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class AgentSwarmExecutor {

    suspend fun execute(
        agents: List<Agent>,
        query: String,
        context: String = "",
        plan: TaskPlan? = null
    ): List<AgentResult> = coroutineScope {
        agents.map { agent ->
            async {
                agent.run(query, context, plan)
            }
        }.awaitAll()
    }
}
