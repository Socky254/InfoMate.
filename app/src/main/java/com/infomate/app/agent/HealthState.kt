package com.infomate.app.agent

enum class HealthState {
    ONLINE,      // Stable
    DEGRADED,    // Functional but unstable/limited
    FAILSAFE,    // Critical failure, operating in restricted/offline mode
    RECOVERY,    // Attempting self-healing
    OFFLINE,     // Total communication loss
    UNKNOWN
}

enum class HealthSeverity(val level: Int) {
    STABLE(0),
    WARNING(1),
    DEGRADED(2),
    CRITICAL(3),
    EMERGENCY(4)
}
