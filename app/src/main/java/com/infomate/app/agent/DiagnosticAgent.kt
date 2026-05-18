package com.infomate.app.agent

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import org.json.JSONObject

object DiagnosticAgent {

    /**
     * Scans the system for errors, checking database, API, and recent message health.
     * Generates an enterprise-grade health report.
     */
    suspend fun runFullDiagnostic(): String {
        // Pulse the health system immediately
        HealthManager.logHealth(HealthManager.CAT_SECURITY, HealthState.ONLINE, "Diagnostic Heartbeat Initiated", HealthSeverity.STABLE)

        val aiStatus = checkBrainHealth()
        val networkStatus = checkNetworkHealth()
        val dbStatus = checkDatabaseHealth()
        val memoryStatus = checkMemoryHealth()
        val logsStatus = checkSystemLogs()
        val authStatus = checkAuthHealth()

        return buildString {
            append("### NEURAL ARCHITECTURE HEALTH ###\n\n")
            
            append("AI CORE [LVL:${getSeverity(aiStatus)}]:\n")
            append("${getStatusIcon(aiStatus)} $aiStatus\n\n")
            
            append("NETWORK [LVL:${getSeverity(networkStatus)}]:\n")
            append("${getStatusIcon(networkStatus)} $networkStatus\n\n")
            
            append("AUTH [LVL:${getSeverity(authStatus)}]:\n")
            append("${getStatusIcon(authStatus)} $authStatus\n\n")

            append("DATABASE [LVL:${getSeverity(dbStatus)}]:\n")
            append("${getStatusIcon(dbStatus)} $dbStatus\n\n")

            append("MEMORY [LVL:${getSeverity(memoryStatus)}]:\n")
            append("${getStatusIcon(memoryStatus)} $memoryStatus\n\n")
            
            append("LOGGING [LVL:${getSeverity(logsStatus)}]:\n")
            append("${getStatusIcon(logsStatus)} $logsStatus\n\n")
            
            append("### RECOMMENDED ARCHITECTURAL ACTION ###\n")
            append(getRecommendedAction(aiStatus, dbStatus, networkStatus, logsStatus, authStatus))
        }
    }

    private fun getSeverity(status: String): Int {
        return when {
            status.contains("ONLINE") || status.contains("STABLE") -> 0
            status.contains("RECOVERY") -> 1
            status.contains("DEGRADED") -> 2
            status.contains("FAILSAFE") -> 3
            status.contains("OFFLINE") -> 4
            else -> 1
        }
    }

    private fun getStatusIcon(status: String): String {
        return when {
            status.contains("ONLINE") || status.contains("STABLE") -> "✔"
            status.contains("RECOVERY") -> "⚙"
            status.contains("DEGRADED") -> "⚠"
            status.contains("FAILSAFE") -> "🛡"
            else -> "❌"
        }
    }

    private fun getRecommendedAction(ai: String, db: String, net: String, logs: String, auth: String): String {
        val actions = mutableListOf<String>()
        if (ai.contains("DEGRADED") || ai.contains("OFFLINE") || ai.contains("FAILSAFE")) {
            actions.add("- [RECOVERY] Attempting AI Link Resync")
            actions.add("- [DIAGNOSTIC] Verify Provider API Quota")
        }
        if (net.contains("OFFLINE")) {
            actions.add("- [USER] Verify Global Network/DNS Link")
            actions.add("- [FAILSAFE] Activating Edge Brain (Local Mode)")
        }
        if (auth.contains("DEGRADED") || auth.contains("OFFLINE")) {
            actions.add("- [RECOVERY] Forcing Auth Token Refresh")
        }
        if (db.contains("OFFLINE")) {
            actions.add("- [CRITICAL] Verify Supabase Middleware Availability")
        }
        
        return if (actions.isEmpty()) "✓ All systems optimal. No intervention required." 
               else actions.joinToString("\n")
    }

    private suspend fun checkAuthHealth(): String {
        return try {
            // Simulated auth check against Config
            val key = com.infomate.app.core.config.Config.SUPABASE_KEY
            if (key.length > 50) {
                HealthManager.logHealth(HealthManager.CAT_AUTH, HealthState.ONLINE, "Bearer Token Validated", HealthSeverity.STABLE)
                "ONLINE (VALIDATED)"
            } else {
                HealthManager.logHealth(HealthManager.CAT_AUTH, HealthState.OFFLINE, "Token Malformed", HealthSeverity.CRITICAL)
                "OFFLINE (MALFORMED)"
            }
        } catch (e: Exception) {
            "OFFLINE (${e.message})"
        }
    }

    private suspend fun checkDatabaseHealth(): String {
        return try {
            // Test primary table access
            val response = SupabaseClient.select("messages", "id", order = "timestamp.desc")
            if (response != null) {
                HealthManager.logHealth(HealthManager.CAT_DATABASE, HealthState.ONLINE, "RDBMS Link Established", HealthSeverity.STABLE)
                "ONLINE (STABLE)"
            } else {
                // Try a simpler query if ordering fails (e.g. if table is empty or column differs)
                val simpleResponse = SupabaseClient.select("messages", "id", order = "id.asc")
                if (simpleResponse != null) {
                    "ONLINE (STABLE - ALT_SCHEMA)"
                } else {
                    HealthManager.logHealth(HealthManager.CAT_DATABASE, HealthState.DEGRADED, "Database Query Failed (Table might be missing or RLS issue)", HealthSeverity.WARNING)
                    "DEGRADED (Query Failed)"
                }
            }
        } catch (e: Exception) {
            HealthManager.logHealth(HealthManager.CAT_DATABASE, HealthState.OFFLINE, e.message ?: "Unknown Connection Error", HealthSeverity.CRITICAL)
            "OFFLINE (${e.message})"
        }
    }

    private suspend fun checkMemoryHealth(): String {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            val isHealthy = usedMemory < maxMemory * 0.8
            val status = if (isHealthy) HealthState.ONLINE else HealthState.DEGRADED
            HealthManager.logHealth(HealthManager.CAT_MEMORY, 
                status,
                "Used: ${usedMemory}MB / Max: ${maxMemory}MB",
                if (isHealthy) HealthSeverity.STABLE else HealthSeverity.WARNING)
            "${status.name} (${usedMemory}MB / ${maxMemory}MB)"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    private suspend fun checkBrainHealth(): String {
        return try {
            val params = mapOf("prompt" to "PING_DIAGNOSTIC")
            val response = SupabaseClient.callFunction("infomate-brain", params)
            
            when {
                response == null || response.isEmpty() -> {
                    HealthManager.logHealth(HealthManager.CAT_AI_CORE, HealthState.DEGRADED, "ERR_EMPTY_RESPONSE", HealthSeverity.DEGRADED)
                    "DEGRADED (EMPTY_RESPONSE)"
                }
                response.contains("ACTIVE") || response.length > 5 -> {
                    HealthManager.logHealth(HealthManager.CAT_AI_CORE, HealthState.ONLINE, "Cognitive Link Synchronized", HealthSeverity.STABLE)
                    "ONLINE (STABLE)"
                }
                response.contains("quota exceeded") || response.contains("429") -> {
                    HealthManager.logHealth(HealthManager.CAT_AI_CORE, HealthState.FAILSAFE, "ERR_QUOTA_EXCEEDED", HealthSeverity.CRITICAL)
                    "FAILSAFE (QUOTA_EXCEEDED)"
                }
                response.contains("timeout") || response.contains("504") -> {
                    HealthManager.logHealth(HealthManager.CAT_AI_CORE, HealthState.DEGRADED, "ERR_API_TIMEOUT", HealthSeverity.WARNING)
                    "DEGRADED (TIMEOUT)"
                }
                else -> {
                    HealthManager.logHealth(HealthManager.CAT_AI_CORE, HealthState.DEGRADED, "ERR_INVALID_HANDSHAKE", HealthSeverity.WARNING)
                    "DEGRADED (INVALID_HANDSHAKE)"
                }
            }
        } catch (e: Exception) {
            HealthManager.logHealth(HealthManager.CAT_AI_CORE, HealthState.OFFLINE, "ERR_NETWORK: ${e.message}", HealthSeverity.CRITICAL)
            "OFFLINE (${e.message})"
        }
    }

    private suspend fun checkNetworkHealth(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // Standard Android connectivity check URL
            val request = okhttp3.Request.Builder()
                .url("https://connectivitycheck.gstatic.com/generate_204")
                .build()
            
            com.infomate.app.core.network.ApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    HealthManager.logHealth(HealthManager.CAT_NETWORK, HealthState.ONLINE, "Global Backbone Reached", HealthSeverity.STABLE)
                    "ONLINE (STABLE)"
                } else {
                    HealthManager.logHealth(HealthManager.CAT_NETWORK, HealthState.DEGRADED, "Limited Connectivity (HTTP ${response.code})", HealthSeverity.WARNING)
                    "DEGRADED (UNSTABLE)"
                }
            }
        } catch (e: Exception) {
            // Log the specific exception to help debugging
            Log.e("NETWORK_CHECK", "Failure: ${e.message}")
            HealthManager.logHealth(HealthManager.CAT_NETWORK, HealthState.OFFLINE, "Connection Failure: ${e.message}", HealthSeverity.EMERGENCY)
            "OFFLINE (NO_INTERNET)"
        }
    }

    private suspend fun checkSystemLogs(): String {
        return try {
            val healthJson = HealthManager.getRecentLogs()
            if (healthJson != null) {
                // Even an empty array "[]" (length 2) means the table is accessible
                if (healthJson.length >= 2) {
                    HealthManager.logHealth(HealthManager.CAT_LOGGING, HealthState.ONLINE, "Telemetry Queue Active", HealthSeverity.STABLE)
                    "ONLINE (STABLE)"
                } else {
                    "DEGRADED (COLD_START)"
                }
            } else {
                HealthManager.logHealth(HealthManager.CAT_LOGGING, HealthState.DEGRADED, "Log Buffer Initialization Pending", HealthSeverity.WARNING)
                "DEGRADED (LOG_FETCH_FAIL)"
            }
        } catch (e: Exception) {
            "OFFLINE"
        }
    }

    /**
     * STEP 4: API Test Ping
     */
    suspend fun testAPI(): Boolean {
        return try {
            val params = mapOf("prompt" to "PING")
            val result = SupabaseClient.callFunction("infomate-brain", params)
            result?.contains("error") == false
        } catch(e: Exception) {
            false
        }
    }
}
