package com.infomate.core.domain.model

import com.infomate.core.brain.ThoughtStep
import com.infomate.core.tools.MediaOutput

data class AgentResponse(
    val output: String,
    val steps: List<ThoughtStep>,
    val requiresTool: Boolean = false,
    val toolName: String? = null,
    val recommendation: String? = null,
    val media: List<MediaOutput> = emptyList(),
    val layer: String = "UNIFIED"
)
