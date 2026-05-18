package com.infomate.app.agent

import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import org.json.JSONObject

object DiagnosticAgent {

    /**
     * Scans the system for errors, checking database, API, and recent message health.
     */
    suspend fun runFullDiagnostic(): String {
        val report = StringBuilder("### SYSTEM DIAGNOSTIC REPORT ###\n\n")
        
        // 1. Check Database (Ping messages table)
        val dbStatus = checkDatabaseHealth()
        report.append("1. DATABASE: $dbStatus\n")
        
        // 2. Check Neural Link (Brain Ping)
        val aiStatus = checkBrainHealth()
        report.append("2. NEURAL LINK: $aiStatus\n")
        
        // 3. Check System Logs
        val logs = checkSystemLogs()
        report.append("3. HEALTH LOGS: $logs\n")

        return report.toString()
    }

    private suspend fun checkDatabaseHealth(): String {
        return try {
            val response = SupabaseClient.select("messages", "id", order = "timestamp.desc")
            if (response != null) "STABLE (Primary Link Established)" else "UNSTABLE (Query Failed)"
        } catch (e: Exception) {
            "OFFLINE (${e.message})"
        }
    }

    private suspend fun checkBrainHealth(): String {
        return try {
            val params = mapOf("prompt" to "PING_DIAGNOSTIC: Respond with ACTIVE")
            val response = SupabaseClient.callFunction("infomate-brain", params)
            if (response?.contains("ACTIVE") == true) "STABLE (Intelligence Synchronized)" 
            else if (response?.contains("error") == true) "DEGRADED (API Response Error)"
            else "UNSTABLE (Invalid Handshake)"
        } catch (e: Exception) {
            "CRITICAL FAILURE (${e.message})"
        }
    }

    private suspend fun checkSystemLogs(): String {
        return try {
            val healthJson = SupabaseClient.select("system_health", order = "created_at.desc")
            if (healthJson != null && healthJson.length > 5) "RECORDS FOUND (Monitoring Active)"
            else "NO LOGS FOUND (Cold Start)"
        } catch (e: Exception) {
            "LOGGING OFFLINE"
        }
    }
}
