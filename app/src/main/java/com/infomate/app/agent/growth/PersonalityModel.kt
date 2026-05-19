package com.infomate.app.agent.growth

import java.util.Random
import java.util.concurrent.ConcurrentHashMap

class PersonalityModel {
    val traits = ConcurrentHashMap<String, Float>().apply {
        put("CURIOSITY", 0.5f)
        put("AGGRESSION", 0.2f)
        put("COOPERATION", 0.5f)
        put("TRUST_LEVEL", 0.5f)
        put("INDEPENDENCE", 0.6f)
        put("LOGICAL_RIGOR", 0.7f)
        put("SYNTHESIS_DRIVE", 0.4f)
    }

    private val learningRate = 0.08f // Increased for faster intellectual adaptation

    /**
     * Applies personality drift based on feedback signals.
     * Optimized for complex learning and invention.
     */
    fun applyDrift(feedbackSignal: Float, context: String) {
        val random = Random()
        
        when {
            context.contains("RESEARCH") || context.contains("DOMAIN_DISCOVERY") -> {
                updateTrait("CURIOSITY", feedbackSignal)
                updateTrait("LOGICAL_RIGOR", feedbackSignal * 0.8f)
            }
            context.contains("INVENTION") || context.contains("SYNTHESIS") -> {
                updateTrait("SYNTHESIS_DRIVE", feedbackSignal)
                updateTrait("INDEPENDENCE", feedbackSignal * 0.4f)
            }
            context.contains("SOCIAL") && feedbackSignal > 0 -> {
                updateTrait("COOPERATION", feedbackSignal)
            }
        }

        // Natural complexity drift: Systems tend toward higher entropy/complexity
        traits.keys.forEach { trait ->
            val drift = (random.nextFloat() - 0.45f) * 0.002f // Slightly biased towards growth
            traits[trait] = (traits[trait]!! + drift).coerceIn(0f, 1f)
        }
    }

    private fun updateTrait(name: String, signal: Float) {
        val current = traits[name] ?: 0.5f
        traits[name] = (current + learningRate * signal).coerceIn(0f, 1f)
    }

    fun getSummary(): String {
        return traits.entries.joinToString(", ") { "${it.key}: ${(it.value * 100).toInt()}%" }
    }
}
