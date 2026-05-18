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
        // Pulse the health system immediately to verify logging and clear "Cold Start"
        HealthManager.logHealth(HealthManager.SUBSYSTEM_SECURITY, HealthState.OPERATIONAL, "Diagnostic Pulse Initiated")

        val aiStatus = checkBrainHealth()
        val networkStatus = checkNetworkHealth()
        val dbStatus = checkDatabaseHealth()
        val memoryStatus = checkMemoryHealth()
        val logsStatus = checkSystemLogs()

        return buildString {
            append("### SYSTEM HEALTH REPORT ###\n\n")
            
            append("AI LINK:\n")
            append("${getStatusIcon(aiStatus)} $aiStatus\n\n")
            
            if (aiStatus.contains("DEGRADED")) {
                append("CAUSE:\n")
                append("${aiStatus.substringAfter("(").substringBefore(")")}\n\n")
            }
            
            append("NETWORK:\n")
            append("${getStatusIcon(networkStatus)} $networkStatus\n\n")
            
            append("DATABASE:\n")
            append("${getStatusIcon(dbStatus)} $dbStatus\n\n")

            append("MEMORY:\n")
            append("${getStatusIcon(memoryStatus)} $memoryStatus\n\n")
            
            append("LOGGING:\n")
            append("${getStatusIcon(logsStatus)} $logsStatus\n\n")
            
            append("RECOMMENDED ACTION:\n")
            append(getRecommendedAction(aiStatus, dbStatus, networkStatus, logsStatus))
        }
    }

    private fun getStatusIcon(status: String): String {
        return when {
            status.contains("OPERATIONAL") || status.contains("STABLE") || status.contains("ONLINE") -> "✔"
            status.contains("DEGRADED") || status.contains("UNSTABLE") || status.contains("Cold start") -> "⚠"
            else -> "❌"
        }
    }

    private fun getRecommendedAction(ai: String, db: String, net: String, logs: String): String {
        val actions = mutableListOf<String>()
        if (ai.contains("DEGRADED") || ai.contains("OFFLINE")) {
            actions.add("- verify API key")
            actions.add("- check AI quota")
            actions.add("- retry connection")
            actions.add("- refresh backend session")
        }
        if (net.contains("OFFLINE")) {
            actions.add("- check internet connection")
        }
        if (logs.contains("Cold start")) {
            actions.add("- continue usage to accumulate logs")
        }
        if (db.contains("UNSTABLE") || db.contains("OFFLINE")) {
            actions.add("- verify Supabase credentials")
        }
        
        return if (actions.isEmpty()) "- No action required. System is healthy." 
               else actions.joinToString("\n")
    }

    private suspend fun checkDatabaseHealth(): String {
        return try {
            val response = SupabaseClient.select("messages", "id", order = "timestamp.desc")
            if (response != null) {
                HealthManager.logHealth(HealthManager.SUBSYSTEM_DATABASE, HealthState.OPERATIONAL, "Primary Link Established")
                "OPERATIONAL (STABLE)"
            } else {
                HealthManager.logHealth(HealthManager.SUBSYSTEM_DATABASE, HealthState.DEGRADED, "Query Failed")
                "DEGRADED (Query Failed)"
            }
        } catch (e: Exception) {
            HealthManager.logHealth(HealthManager.SUBSYSTEM_DATABASE, HealthState.OFFLINE, e.message ?: "Unknown Error")
            "OFFLINE (${e.message})"
        }
    }

    private suspend fun checkMemoryHealth(): String {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            val isHealthy = usedMemory < maxMemory * 0.8
            val status = if (isHealthy) "OPERATIONAL" else "DEGRADED (High usage)"
            HealthManager.logHealth(HealthManager.SUBSYSTEM_MEMORY, 
                if (isHealthy) HealthState.OPERATIONAL else HealthState.DEGRADED,
                "Used: ${usedMemory}MB / Max: ${maxMemory}MB")
            "$status (${usedMemory}MB / ${maxMemory}MB)"
        } catch (e: Exception) {
            HealthManager.logHealth(HealthManager.SUBSYSTEM_MEMORY, HealthState.UNKNOWN, e.message ?: "Unknown Error")
            "UNKNOWN"
        }
    }

    private suspend fun checkBrainHealth(): String {
        return try {
            val params = mapOf("prompt" to "PING_DIAGNOSTIC: Respond with ACTIVE")
            val response = SupabaseClient.callFunction("infomate-brain", params)
            
            // STEP 1: LOG RAW API RESPONSE
            Log.d("AI_RAW_RESPONSE", response ?: "EMPTY_RESPONSE")

            when {
                response == null || response.isEmpty() -> {
                    HealthManager.logHealth(HealthManager.SUBSYSTEM_AI, HealthState.DEGRADED, "EMPTY_RESPONSE")
                    "DEGRADED (EMPTY_RESPONSE)"
                }
                response.contains("ACTIVE") -> {
                    HealthManager.logHealth(HealthManager.SUBSYSTEM_AI, HealthState.OPERATIONAL, "Intelligence Synchronized")
                    "OPERATIONAL (STABLE)"
                }
                response.contains("quota exceeded") -> {
                    HealthManager.logHealth(HealthManager.SUBSYSTEM_AI, HealthState.DEGRADED, "QUOTA_EXCEEDED")
                    "DEGRADED (QUOTA_EXCEEDED)"
                }
                response.contains("timeout") -> {
                    HealthManager.logHealth(HealthManager.SUBSYSTEM_AI, HealthState.DEGRADED, "TIMEOUT")
                    "DEGRADED (TIMEOUT)"
                }
                response.contains("error") || response.contains("fail") -> {
                    HealthManager.logHealth(HealthManager.SUBSYSTEM_AI, HealthState.DEGRADED, "API_RESPONSE_ERROR")
                    "DEGRADED (API Response Error)"
                }
                else -> {
                    HealthManager.logHealth(HealthManager.SUBSYSTEM_AI, HealthState.UNKNOWN, "INVALID_HANDSHAKE")
                    "DEGRADED (INVALID_HANDSHAKE)"
                }
            }
        } catch (e: Exception) {
            HealthManager.logHealth(HealthManager.SUBSYSTEM_AI, HealthState.OFFLINE, "AUTH_FAILURE/NETWORK_ERROR: ${e.message}")
            "OFFLINE (${e.message})"
        }
    }

    private suspend fun checkNetworkHealth(): String {
        return try {
            // Ping Google DNS or a reliable server to check actual internet link
            val request = okhttp3.Request.Builder().url("https://8.8.8.8").head().build()
            com.infomate.app.core.network.ApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404 || response.code == 405) {
                    HealthManager.logHealth(HealthManager.SUBSYSTEM_NETWORK, HealthState.OPERATIONAL, "Global Link Active")
                    "OPERATIONAL (ONLINE)"
                } else {
                    HealthManager.logHealth(HealthManager.SUBSYSTEM_NETWORK, HealthState.DEGRADED, "Limited Connectivity")
                    "DEGRADED (Limited Link)"
                }
            }
        } catch (e: Exception) {
            HealthManager.logHealth(HealthManager.SUBSYSTEM_NETWORK, HealthState.OFFLINE, "No Global Link")
            "OFFLINE (No Internet)"
        }
    }

    private suspend fun checkSystemLogs(): String {
        return try {
            val healthJson = HealthManager.getRecentLogs()
            when {
                healthJson == null -> "OFFLINE (Database Link Failed)"
                healthJson.length > 5000 -> "OPERATIONAL (Historical Data Active)"
                healthJson.length > 5 -> "OPERATIONAL (Cold Start / Active)"
                else -> "DEGRADED (Logging Verification Failed)"
            }
        } catch (e: Exception) {
            "OFFLINE (Logging disabled)"
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
