package com.infomate.app.agent.growth

object ExperienceTracker {
    fun calculateReward(
        survival: Float = 0f,
        social: Float = 0f,
        task: Float = 0f,
        exploration: Float = 0f
    ): Float {
        return survival + social + task + exploration
    }
}
