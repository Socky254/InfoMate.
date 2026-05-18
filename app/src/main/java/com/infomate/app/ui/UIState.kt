package com.infomate.app.ui

import com.infomate.core.brain.ThoughtStep
import com.infomate.core.ui.components.InfomateState

data class UIState(
    val input: String = "",
    val messages: List<String> = emptyList(),
    val status: String = "CORE: ACTIVE",
    val brainState: InfomateState = InfomateState.IDLE,
    val cognitiveSteps: List<ThoughtStep> = emptyList()
)
