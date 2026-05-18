package com.infomate.core.brain.v7

import com.infomate.core.brain.v5.*

class ArchitectureDesigner {

    fun buildArchitecture(plan: SystemPlan): AgentGraph {
        val nodes = mutableListOf<Agent>()
        
        plan.requiredAgents.forEach { name ->
            when (name) {
                "Research Specialist" -> nodes.add(ResearchAgent())
                "Math Specialist" -> nodes.add(MathAgent())
                "Code Specialist" -> nodes.add(CodeAgent())
                "Ontological Sage" -> nodes.add(PhilosophyAgent())
                "System Critic" -> nodes.add(CriticAgent())
                "High-Frequency Judge" -> nodes.add(JudgeAgent())
            }
        }

        return AgentGraph(nodes)
    }
}
