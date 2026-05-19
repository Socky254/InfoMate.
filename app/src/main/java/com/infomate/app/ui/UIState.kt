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

data class QuotaInfo(
    val requestsUsed: Int = 0,
    val requestsLimit: Int = 0,
    val tokensUsed: Long = 0
)

data class SystemLog(
    val message: String,
    val level: String = "INFO", // "INFO", "WARN", "ERROR", "SUCCESS"
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
    val voiceAmplitudes: List<Float> = List(20) { 0.1f },
    val isVoiceOutputEnabled: Boolean = true, // Master switch for AI Voice
    val needsOnboarding: Boolean = true, // Flag for permission context screen
    val quota: QuotaInfo? = null,
    val isMaster: Boolean = false, // Master account flag
    val userEmail: String? = null,
    val pendingUpdate: com.infomate.app.core.UpdateInfo? = null,
    val showManualKnowledgeDialog: Boolean = false,
    val showMasterDashboard: Boolean = false,
    val showPinEntry: Boolean = false,
    val masterPin: String = "kiprutoArtK194!!",
    val isConnected: Boolean = false,
    val showGrowthDashboard: Boolean = false,
    val showConsciousnessStream: Boolean = false,
    val showSystemTerminal: Boolean = false,
    val showEvolutionLog: Boolean = false,
    val showGlobalNodeMonitor: Boolean = false,
    val showConfirmationDialog: Boolean = false,
    val confirmationTitle: String = "",
    val confirmationMessage: String = "",
    val onConfirmAction: (() -> Unit)? = null,
    val terminalLogs: List<SystemLog> = emptyList(),
    val neuralDensity: Float = 0.0f, // 0.0 to 1.0
    val totalInsights: Int = 0,
    val syntheticPersonalityLevel: Int = 1, // Evolution stage
    val telemetryHistory: List<Float> = List(10) { 0.5f }, // For charts
    val activeSimulationLogs: List<String> = emptyList(), // Live process stream
    val showDirectNeuralLink: Boolean = false,
    val growthPriorityLevel: Float = 0.5f, // 0.0 to 1.0 (Low to Maximum)
    val isSubstrateAwake: Boolean = false,
    val substrateLastPulse: Long = 0L,
    // v10.9 Entity Analysis Data
    val personalityTraits: Map<String, Float> = emptyMap(),
    val energyLevel: Float = 1.0f,
    val evolutionStage: String = "INITIALIZING",
    val experiencePoints: Int = 0,
    val discoveriesCount: Int = 0,
    val showVitalSigns: Boolean = false
)
