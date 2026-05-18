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
        val q = query.lowercase()

        return when {
            q.contains("battery") || q.contains("power") -> getBatteryStatus(context)
            q.contains("time") || q.contains("date") -> getTimeStatus()
            q.contains("who are you") || q.contains("identity") -> 
                "I am InfoMate v9, your Transcendent Iris. My high-level neural link is currently offline, but my core edge-processing is active."
            q.contains("creator") || q.contains("socrates") ->
                "My architect is Socrates Kipruto. I am currently operating in Edge Mode."
            q.contains("status") || q.contains("health") ->
                "Primary Neural Link: OFFLINE. Edge Processor: ACTIVE. All local systems operational."
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
