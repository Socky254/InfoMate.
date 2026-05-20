package com.infomate.app.core.memory

/**
 * Memory Intelligence Policy System (MIPS) v13.5
 * Handles classification, compression, and advanced knowledge promotion.
 */
object MIPS {

    enum class MemoryType {
        FACT, SKILL, EPISODIC, CONVERSATION, SUMMARY, HYPOTHESIS
    }

    /**
     * FACT ACQUISITION POLICY: Stores only verified knowledge.
     */
    fun classify(content: String, confidence: Float): MemoryType {
        val normalized = content.lowercase()
        return when {
            confidence > 0.8 && (normalized.contains("is a") || normalized.contains("defined as")) -> MemoryType.FACT
            normalized.contains("how to") || normalized.contains("step by step") -> MemoryType.SKILL
            normalized.contains("i hypothesize") || normalized.contains("possibility is") -> MemoryType.HYPOTHESIS
            else -> MemoryType.CONVERSATION
        }
    }

    /**
     * KNOWLEDGE PROMOTION SYSTEM
     * Upgrades memory status based on repeated use or architect validation.
     */
    fun shouldPromote(currentType: MemoryType, useCount: Int, relevance: Float): MemoryType {
        if (relevance > 0.9f) {
            return when (currentType) {
                MemoryType.CONVERSATION -> MemoryType.FACT
                MemoryType.HYPOTHESIS -> MemoryType.SKILL
                else -> currentType
            }
        }
        if (useCount > 3) return MemoryType.SKILL
        return currentType
    }

    /**
     * FORGETTING POLICY
     * Prevents degradation by decaying unused memory weights.
     */
    fun decayImportance(importance: Float, daysSinceLastUse: Int): Float {
        return if (daysSinceLastUse > 30) (importance * 0.5f).coerceAtLeast(0.1f) else importance
    }

    /**
     * COMPRESSION POLICY
     */
    fun shouldCompress(memoryCount: Int): Boolean = memoryCount >= 10
}
