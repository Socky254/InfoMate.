package com.infomate.app.agent.growth

object StageManager {
    fun getParameters(stage: GrowthStage): StageParameters {
        return when (stage) {
            GrowthStage.INFANT -> StageParameters(
                memorySpan = 3,
                planningDepth = 0,
                explorationRate = 0.9f,
                socialAwareness = 0.1f
            )
            GrowthStage.CHILD -> StageParameters(
                memorySpan = 15,
                planningDepth = 1,
                explorationRate = 0.7f,
                socialAwareness = 0.4f
            )
            GrowthStage.ADOLESCENT -> StageParameters(
                memorySpan = 50,
                planningDepth = 3,
                explorationRate = 0.4f,
                socialAwareness = 0.7f
            )
            GrowthStage.ADULT -> StageParameters(
                memorySpan = 1000, // Effectively persistent
                planningDepth = 5,
                explorationRate = 0.1f,
                socialAwareness = 0.95f
            )
        }
    }

    fun determineStage(xp: Float): GrowthStage {
        return when {
            xp >= 2000f -> GrowthStage.ADULT
            xp >= 500f -> GrowthStage.ADOLESCENT
            xp >= 100f -> GrowthStage.CHILD
            else -> GrowthStage.INFANT
        }
    }
}
