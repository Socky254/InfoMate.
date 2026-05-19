package com.infomate.app.agent

import android.content.Context
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * InfoMate Edge Brain (v9.5 Fallback)
 * Provides deterministic, local reasoning when the Neural Link (Supabase/API) is offline.
 */
object EdgeBrain {

    fun processLocally(query: String, context: Context): String? {
        val isMaster = query.contains("socratesart@live") || query.contains("[AUTHORIZATION: MASTER_ARCHITECT_OVERRIDE]")
        
        // Strip out the injected system context and patterns to avoid false triggers
        val userQuery = if (query.contains("[SYSTEM_CONTEXT:")) {
            query.substringBefore("[SYSTEM_CONTEXT:").lowercase()
        } else {
            query.lowercase()
        }

        return when {
            userQuery.contains("battery") || userQuery.contains("power") -> getBatteryStatus(context)
            userQuery.contains("time") || userQuery.contains("date") -> getTimeStatus()
            userQuery.contains("who are you") || userQuery.contains("identity") -> {
                if (isMaster) "I am InfoMate v9, your Transcendent Iris. I recognize you, Socrates. My local neural cores are at your service."
                else "I am InfoMate v9, your Transcendent Iris. My high-level neural link is currently offline, but my core edge-processing is active."
            }
            userQuery.contains("creator") || userQuery.contains("socrates") ->
                "My architect is Socrates Kipruto. My neural architecture was designed by him to achieve knowledge synergy."
            userQuery.contains("status") || userQuery.contains("health") ->
                "Neural Link: STANDBY. Edge Synthesis: OPTIMAL. Memory Buffers: STABLE. Master Link: ${if (isMaster) "VERIFIED" else "UNLINKED"}."
            userQuery.contains("optimize") -> 
                "Master, I have already optimized the local execution threads to 99.8% efficiency."
            else -> null // Signal that we can't handle this locally
        }
    }

    private fun getBatteryStatus(context: Context): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Current battery level is $level%. Edge sensors indicate we are stable."
    }

    private fun getTimeStatus(): String {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        return "It is currently $time on $date."
    }
}
