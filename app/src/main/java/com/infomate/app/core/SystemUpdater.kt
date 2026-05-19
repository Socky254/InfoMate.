package com.infomate.app.core

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.infomate.app.core.network.SupabaseClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class UpdateInfo(
    val version_code: Int,
    val version_name: String,
    val download_url: String,
    val changelog: String,
    val critical: Boolean
)

object SystemUpdater {
    private val gson = Gson()

    suspend fun checkForUpdates(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.select("system_config", query = "value", order = "updated_at.desc")
            if (response != null) {
                // Supabase returns a list of results
                val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                val results: List<Map<String, Any>> = gson.fromJson(response, listType)
                
                val configMap = results.find { it["key"] == "latest_update" }
                @Suppress("UNCHECKED_CAST")
                val valueMap = configMap?.get("value") as? Map<String, Any>
                
                if (valueMap != null) {
                    val latestCode = (valueMap["version_code"] as? Double)?.toInt() ?: 0
                    if (latestCode > currentVersionCode) {
                        return@withContext UpdateInfo(
                            version_code = latestCode,
                            version_name = valueMap["version_name"] as? String ?: "",
                            download_url = valueMap["download_url"] as? String ?: "",
                            changelog = valueMap["changelog"] as? String ?: "",
                            critical = valueMap["critical"] as? Boolean ?: false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SystemUpdater", "Update check failed: ${e.message}")
        }
        return@withContext null
    }

    fun downloadAndInstall(context: Context, update: UpdateInfo) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "infomate_update.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(update.download_url))
            .setTitle("InfoMate System Upgrade")
            .setDescription("Downloading Neural Link v${update.version_name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Listen for completion
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    installApk(ctx, destination)
                    ctx.unregisterReceiver(this)
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
