package com.infomate.core.brain.v7

import com.infomate.core.brain.v5.Complexity
import com.infomate.core.brain.v5.Domain
import com.infomate.core.brain.v5.TaskProfile

class MetaPlanner {

    fun designSystem(query: String, profile: TaskProfile): SystemPlan {
        // In v7, this would be a high-level LLM reasoning about the architecture.
        // Heuristic design logic:
        
        val agents = mutableListOf<String>()
        agents.add("Research Specialist") // Base agent
        
        when (profile.domain) {
            Domain.MATH -> agents.add("Math Specialist")
            Domain.CODING -> agents.add("Code Specialist")
            Domain.PHILOSOPHY -> agents.add("Ontological Sage")
            else -> {}
        }
        
        if (profile.complexity == Complexity.HIGH) {
            agents.add("System Critic")
            agents.add("Ontological Sage")
        }

        return SystemPlan(
            requiredAgents = agents.distinct(),
            agentCount = agents.size,
            isParallel = profile.complexity != Complexity.HIGH, // Sequential for high complexity to allow cross-debate
            reasoningDepth = if (profile.complexity == Complexity.HIGH) 3 else 1,
            complexity = profile.complexity
        )
    }
}
