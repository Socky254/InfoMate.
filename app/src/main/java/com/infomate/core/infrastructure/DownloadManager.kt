package com.infomate.core.infrastructure

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

class InfomateDownloadManager(private val context: Context) {
    
    fun downloadFile(url: String, fileName: String, description: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription(description)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}
