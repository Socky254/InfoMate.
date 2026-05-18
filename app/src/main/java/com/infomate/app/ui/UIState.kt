package com.infomate.app.ui

import com.infomate.core.brain.ThoughtStep
import com.infomate.core.ui.components.InfomateState

enum class MessageType {
    TEXT, IMAGE, VIDEO
}

data class ChatMessage(
    val content: String,
    val sender: String, // "OPERATOR" or "INFOMATE" or "SYSTEM"
    val type: MessageType = MessageType.TEXT,
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class UIState(
    val input: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val status: String = "CORE: ACTIVE",
    val brainState: InfomateState = InfomateState.IDLE,
    val cognitiveSteps: List<ThoughtStep> = emptyList(),
    val isSpeaking: Boolean = false, // AI speaking
    val isListening: Boolean = false, // App listening to user
    val isMaleVoice: Boolean = false,
    val voiceAmplitudes: List<Float> = List(20) { 0.1f }
)
