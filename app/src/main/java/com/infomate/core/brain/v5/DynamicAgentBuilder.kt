package com.infomate.core.brain.v5

class DynamicAgentBuilder {

    fun buildAgents(profile: TaskProfile): List<Agent> {
        val agents = mutableListOf<Agent>()

        // 1. Domain-specific selection
        when (profile.domain) {
            Domain.RESEARCH -> agents.add(ResearchAgent())
            Domain.MATH -> agents.add(MathAgent())
            Domain.CODING -> agents.add(CodeAgent())
            Domain.PHILOSOPHY -> agents.add(PhilosophyAgent())
            Domain.COSMOS -> agents.add(ResearchAgent())
            Domain.GENERAL -> agents.add(ResearchAgent())
        }

        // 2. Complexity-based scaling
        if (profile.complexity == Complexity.MEDIUM || profile.complexity == Complexity.HIGH) {
            agents.add(PhilosophyAgent()) // Add ontological depth
        }
        
        if (profile.domain == Domain.CODING || profile.domain == Domain.MATH) {
             agents.add(ResearchAgent()) // Add research for technical verification
        }

        // 3. Risk-aware verification (Always include critic for high risk/complexity)
        if (profile.complexity == Complexity.HIGH || profile.riskLevel == RiskLevel.HIGH) {
            agents.add(CriticAgent())
        }

        return agents.distinctBy { it.name }
    }
}
