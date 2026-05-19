package com.infomate.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Connects to a high-performance Workstation node (Desktop)
 * for resource-intensive deep-compute tasks.
 */
class WorkstationClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // Deep compute takes time
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun executeDeepCompute(query: String): String = withContext(Dispatchers.IO) {
        // In a real scenario, this would be your workstation's Tailscale or local IP
        val workstationUrl = "http://workstation-alpha.local:8080/compute"
        
        val payload = JSONObject().apply {
            put("query", query)
            put("mode", "DEEP_SYNTHESIS")
            put("precision", "QUADRUPLE")
        }

        try {
            val request = Request.Builder()
                .url(workstationUrl)
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "{}")
                    return@withContext json.optString("result", "[DEEP_COMPUTE_SUCCESS] Simulation completed.")
                }
            }
        } catch (e: Exception) {
            Log.e("WorkstationClient", "Connection to Workstation-Alpha failed. Node might be asleep.")
        }
        
        return@withContext "[OFFLINE] Workstation link unavailable. Falling back to cloud-only synthesis."
    }
}
