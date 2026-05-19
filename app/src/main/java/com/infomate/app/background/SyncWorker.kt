package com.infomate.app.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.infomate.app.ai.sdk.ReliabilitySDK
import com.infomate.app.storage.PersistenceManager

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Executing proactive neural sync [SOCRATES_MODE: ACTIVE]...")
        
        val pendingMessages = PersistenceManager.getPendingMessages(applicationContext)
        
        // 1. Flush Pending AI Directives
        if (pendingMessages.isNotEmpty()) {
            try {
                pendingMessages.forEach { msg ->
                    ReliabilitySDK.sendPrompt(msg.content)
                }
                PersistenceManager.clearPendingMessages(applicationContext)
                Log.i("SyncWorker", "Successfully flushed ${pendingMessages.size} pending directives to archives.")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Neural flush failed: ${e.message}")
                return Result.retry()
            }
        }

        // 2. Proactive Health Check & Telemetry
        try {
            // Heartbeat for Master Architect visibility
            val status = "SYSTEM_IDLE_FLUSH_COMPLETE"
            com.infomate.app.agent.HealthManager.logHealth(
                com.infomate.app.agent.HealthManager.CAT_LOGGING,
                com.infomate.app.agent.HealthState.ONLINE,
                "Proactive sync successful. Neural buffers clear.",
                com.infomate.app.agent.HealthSeverity.STABLE
            )
        } catch (e: Exception) {
            // Non-critical telemetry failure
        }

        return Result.success()
    }
}
