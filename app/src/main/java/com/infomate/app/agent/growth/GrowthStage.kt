package com.infomate.app.agent.growth

enum class GrowthStage {
    INFANT,      // 0-100 XP: Reactive, short memory, high randomness
    CHILD,       // 100-500 XP: Imitation, curiosity-driven
    ADOLESCENT,  // 500-2000 XP: Identity formation, social comparison
    ADULT        // 2000+ XP: Stable personality, strategic reasoning
}

data class StageParameters(
    val memorySpan: Int,
    val planningDepth: Int,
    val explorationRate: Float,
    val socialAwareness: Float // 0.0 to 1.0
)
