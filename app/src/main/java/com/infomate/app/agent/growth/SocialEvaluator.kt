package com.infomate.app.agent.growth

object SocialEvaluator {
    fun evaluateInteraction(isPositive: Boolean, socialAwareness: Float): Float {
        val base = if (isPositive) 5f else -10f
        // More socially aware agents feel the impact of social feedback more strongly
        return base * (0.5f + socialAwareness)
    }
}
