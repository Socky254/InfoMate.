package com.infomate.core.brain

enum class ReasoningDepth { FAST, BALANCED, DEEP, RESEARCH }
enum class VerificationMode { OFF, LIGHT, STRICT }
enum class ToolAutonomyLevel { NONE, SUGGEST_ONLY, AUTO_EXECUTE, FULL_AGENT_MODE }
enum class ContextRetrievalMode { BASIC, SEMANTIC_RANKED }

data class SystemConfig(
    val reasoningDepth: ReasoningDepth = ReasoningDepth.RESEARCH,
    val verificationMode: VerificationMode = VerificationMode.STRICT,
    val toolAutonomy: ToolAutonomyLevel = ToolAutonomyLevel.AUTO_EXECUTE,
    val memoryWeightingEnabled: Boolean = true,
    val contextRetrieval: ContextRetrievalMode = ContextRetrievalMode.SEMANTIC_RANKED,
    val contextExpansionDepth: Int = 2,
    val reflectionPassEnabled: Boolean = true,
    val confidenceScoringEnabled: Boolean = true,
    val contextCompressionEnabled: Boolean = true
)
