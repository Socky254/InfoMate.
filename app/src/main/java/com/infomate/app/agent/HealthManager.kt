package com.infomate.app.agent

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HealthManager {

    // Structured Diagnostic Categories
    const val CAT_AI_CORE = "AI_CORE"
    const val CAT_NETWORK = "NETWORK"
    const val CAT_DATABASE = "DATABASE"
    const val CAT_AUTH = "AUTH"
    const val CAT_CACHE = "CACHE"
    const val CAT_FILESYSTEM = "FILESYSTEM"
    const val CAT_VOICE_ENGINE = "VOICE_ENGINE"
    const val CAT_STREAM_ENGINE = "STREAM_ENGINE"
    const val CAT_SELF_UPDATE = "SELF_UPDATE"
    const val CAT_SECURITY = "SECURITY"
    const val CAT_LOGGING = "LOGGING"
    const val CAT_MEMORY = "MEMORY"

    // Backward compatibility for old constants
    const val SUBSYSTEM_AI = CAT_AI_CORE
    const val SUBSYSTEM_NETWORK = CAT_NETWORK
    const val SUBSYSTEM_MEMORY = CAT_MEMORY
    const val SUBSYSTEM_DATABASE = CAT_DATABASE
    const val SUBSYSTEM_SECURITY = CAT_SECURITY

    private val scope = CoroutineScope(Dispatchers.IO)

    fun logHealth(
        subsystem: String,
        state: HealthState,
        details: String,
        severity: HealthSeverity = HealthSeverity.STABLE
    ) {
        Log.d("HealthManager", "[$subsystem][LVL:${severity.level}] $state: $details")
        
        scope.launch {
            val data = mapOf(
                "api_connected" to (state == HealthState.ONLINE),
                "status_code" to state.name,
                "error_log" to "[$subsystem][SEV:${severity.level}] $details",
                "severity_level" to severity.level,
                "timestamp" to System.currentTimeMillis()
            )
            SupabaseClient.insert("system_health", data)
        }
    }

    suspend fun getRecentLogs(): String? {
        return SupabaseClient.select("system_health", order = "created_at.desc")
    }
}
