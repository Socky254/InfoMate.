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
        Log.d("SyncWorker", "Executing background neural sync...")
        
        val pendingMessages = PersistenceManager.getPendingMessages(applicationContext)
        if (pendingMessages.isEmpty()) return Result.success()

        try {
            pendingMessages.forEach { msg ->
                // Attempt to send pending messages that were queued during offline/process death
                ReliabilitySDK.sendPrompt(msg.content)
            }
            PersistenceManager.clearPendingMessages(applicationContext)
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}")
            return Result.retry()
        }
    }
}
