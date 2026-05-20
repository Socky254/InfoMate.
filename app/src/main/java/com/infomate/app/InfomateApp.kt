package com.infomate.app

import android.app.Application
import androidx.work.*
import com.infomate.app.background.SyncWorker
import java.util.concurrent.TimeUnit

class InfomateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            setupBackgroundSync()
        } catch (e: Exception) {
            android.util.Log.e("InfomateApp", "Background sync initialization failed: ${e.message}")
        }
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "neural_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
