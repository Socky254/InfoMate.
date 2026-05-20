package com.infomate.app.ui

import com.infomate.app.agent.ThoughtStep
// InfomateState is now in the same package

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

enum class DashboardTab {
    CHAT, DASHBOARD, VITALS, STREAM, REFLECT, SIMULATION, TERMINAL, PROCESS_MONITOR
}

enum class PinTarget {
    DASHBOARD, TERMINAL
}

data class UIState(
    val selectedTab: DashboardTab = DashboardTab.CHAT,
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
    val pinTarget: PinTarget = PinTarget.DASHBOARD,
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
    val isInternetAvailable: Boolean = true,
    val isSystemInitializing: Boolean = true,
    // v10.9 Entity Analysis Data
    val personalityTraits: Map<String, Float> = emptyMap(),
    val energyLevel: Float = 1.0f,
    val evolutionStage: String = "INITIALIZING",
    val experiencePoints: Int = 0,
    val discoveriesCount: Int = 0,
    val showVitalSigns: Boolean = false,
    val ecosystemStatus: String = "Initializing...",
    // v11.8: Advanced Growth Metrics
    val currentGrowthIndex: Float = 0.0f,
    val stabilityScore: Float = 0.0f,
    val entropyLevel: Float = 0.0f,
    val memoryCount: Int = 0,
    val socialScore: Float = 0.0f,
    val frequencySimulationData: List<Float> = List(30) { 0.0f },
    // v11.9: Real-time Process Monitoring
    val activeProcesses: List<ActiveProcess> = emptyList()
)

data class ActiveProcess(
    val id: String,
    val name: String,
    val progress: Float, // 0.0 to 1.0
    val status: String, // "EXECUTING", "SYNCING", "IDLE"
    val category: String = "CORE"
)
