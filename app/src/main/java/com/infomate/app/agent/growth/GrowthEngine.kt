package com.infomate.app.agent.growth

import android.util.Log
import kotlin.math.log10

/**
 * Growth Engine (v11.7)
 * Implements the Deterministic Growth Index Formula:
 * GI = 0.35*XP + 0.25*Memory + 0.25*Social + 0.15*Stability - 0.10*Chaos
 */
class GrowthEngine(
    var xp: Float = 0f,
    var memoryCount: Int = 0,
    var socialScore: Float = 0.5f,
    var stability: Float = 0.8f,
    var entropy: Float = 0.2f,
    var growthIndex: Float = 0f,
    var stage: GrowthStage = GrowthStage.INFANT
) {
    val personality = PersonalityModel()
    val memory = MemoryAdapter(stage)

    fun update(
        survival: Float = 0f,
        social: Float = 0f,
        task: Float = 0f,
        exploration: Float = 0f,
        context: String = "GENERAL"
    ) {
        // 1. Update Core Components
        val reward = ExperienceTracker.calculateReward(survival, social, task, exploration)
        xp += reward
        if (xp < 0) xp = 0f
        
        memoryCount += 1
        
        // Update social score based on interaction
        if (social != 0f) {
            socialScore = (socialScore + social * 0.1f).coerceIn(-1.0f, 1.0f)
        }

        // Update stability & entropy (Deterministic simulations)
        stability = calculateStability()
        entropy = calculateEntropy(reward)

        // 2. Compute Growth Index
        growthIndex = calculateGrowthIndex()
        
        // 3. Stage transition
        val newStage = determineStage(growthIndex)
        if (newStage != stage) {
            Log.i("GrowthEngine", "STAGE_TRANSITION: $stage -> $newStage (GI: $growthIndex)")
            stage = newStage
            memory.updateStage(newStage)
        }

        // 4. Personality Drift
        personality.applyDrift(reward, context)
        
        // 5. Memory Storage
        if (reward != 0f || context != "GENERAL") {
            memory.store("Feedback: $reward | Context: $context | GI: $growthIndex")
        }
    }

    private fun calculateGrowthIndex(): Float {
        val xpNorm = normalize(xp, 0f, 10000f)
        val memoryNorm = normalize(log10(1f + memoryCount).toFloat(), 0f, 5f)
        val socialNorm = normalize(socialScore, -1f, 1f)
        val stabilityNorm = normalize(stability, 0f, 1f)
        val chaosNorm = normalize(entropy, 0f, 1f)

        val gi = (0.35f * xpNorm) +
                 (0.25f * memoryNorm) +
                 (0.25f * socialNorm) +
                 (0.15f * stabilityNorm) -
                 (0.10f * chaosNorm)

        return gi.coerceIn(0f, 1f)
    }

    private fun determineStage(gi: Float): GrowthStage {
        return when {
            gi >= 0.75f -> GrowthStage.ADULT
            gi >= 0.50f -> GrowthStage.ADOLESCENT
            gi >= 0.25f -> GrowthStage.CHILD
            else -> GrowthStage.INFANT
        }
    }

    private fun calculateStability(): Float {
        // Higher XP and lower entropy lead to higher stability
        return (normalize(xp, 0f, 5000f).toFloat() * 0.5f + (1f - entropy) * 0.5f).coerceIn(0f, 1f)
    }

    private fun calculateEntropy(reward: Float): Float {
        // Negative rewards or erratic behavior increases entropy
        val erraticFactor = if (reward < 0) 0.05f else -0.01f
        return (entropy + erraticFactor).coerceIn(0f, 1f)
    }

    private fun normalize(value: Float, min: Float, max: Float): Float {
        if (max == min) return 0f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    fun getParams(): StageParameters = StageManager.getParameters(stage)

    fun getGrowthSummary(): String {
        return "GI: ${"%.2f".format(growthIndex)} | STAGE: $stage | XP: ${xp.toInt()} | SOCIAL: ${"%.2f".format(socialScore)}"
    }
}
