package com.infomate.app.agent

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HealthManager {

    // Subsystems as suggested for enterprise-grade observability
    const val SUBSYSTEM_AI = "AI_LINK"
    const val SUBSYSTEM_NETWORK = "NETWORK"
    const val SUBSYSTEM_MEMORY = "MEMORY"
    const val SUBSYSTEM_DATABASE = "DATABASE"
    const val SUBSYSTEM_FEED = "FEED"
    const val SUBSYSTEM_STREAMING = "STREAMING"
    const val SUBSYSTEM_SECURITY = "SECURITY"

    private val scope = CoroutineScope(Dispatchers.IO)

    fun logHealth(subsystem: String, state: HealthState, details: String) {
        Log.d("HealthManager", "[$subsystem] $state: $details")
        
        scope.launch {
            // Mapping to existing 'system_health' table in infomate_v9_FINAL_STAMP.sql
            val data = mapOf(
                "api_connected" to (state == HealthState.OPERATIONAL),
                "status_code" to state.name,
                "error_log" to "[$subsystem] $details"
            )
            SupabaseClient.insert("system_health", data)
        }
    }

    suspend fun getRecentLogs(): String? {
        return SupabaseClient.select("system_health", order = "created_at.desc")
    }
}
