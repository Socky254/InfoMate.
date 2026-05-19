package com.infomate.core.domain.model

import com.infomate.core.brain.ThoughtStep
import com.infomate.core.tools.MediaOutput

data class EmotionalVector(
    val valence: Float, // 0 to 1 (Sad to Happy)
    val arousal: Float, // 0 to 1 (Calm to Excited)
    val dominance: Float // 0 to 1 (Submissive to Assertive)
)

data class AgentResponse(
    val output: String,
    val steps: List<ThoughtStep>,
    val requiresTool: Boolean = false,
    val toolName: String? = null,
    val recommendation: String? = null,
    val media: List<MediaOutput> = emptyList(),
    val layer: String = "UNIFIED",
    val emotionalVector: EmotionalVector = EmotionalVector(0.5f, 0.5f, 0.5f)
)
