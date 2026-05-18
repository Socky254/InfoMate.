package com.infomate.app.core.network

import android.util.Log
import com.infomate.app.core.config.Config
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object SupabaseClient {

    // Optimized OkHttpClient with connection pooling and stabilization
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS) // Increased for complex reasoning
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true) // Automatically handle simple connection drops
        .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES)) // Keep connections warm
        .build()
        
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Circuit Breaker / Cooldown mechanism
    private var cooldownUntil = 0L
    private const val COOLDOWN_DURATION = 30_000L // 30 seconds

    private fun checkCooldown(): String? {
        val now = System.currentTimeMillis()
        if (now < cooldownUntil) {
            val remaining = (cooldownUntil - now) / 1000
            return "{\"error\": \"System Cooling Down\", \"error_code\": \"RETRY_EXHAUSTED\", \"message\": \"Neural link is stabilizing. Please wait $remaining seconds.\"}"
        }
        return null
    }

    private fun triggerCooldown() {
        cooldownUntil = System.currentTimeMillis() + COOLDOWN_DURATION
    }

    suspend fun insert(table: String, data: Map<String, Any>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(data)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/$table")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
            .addHeader("Connection", "keep-alive") // Signal to keep link open
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SUPABASE_INSERT", "Failed: ${response.code} - ${response.body?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("SUPABASE_INSERT", "Exception: ${e.message}")
        }
    }

    suspend fun rpc(function: String, params: Map<String, Any>): List<String> = withContext(Dispatchers.IO) {
        val json = gson.toJson(params)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/rpc/$function")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d("SUPABASE_RPC_RAW", "Table: $function, Response: $responseBody")
                if (response.isSuccessful) {
                    listOf(responseBody ?: "")
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("SUPABASE_RPC", "Exception: ${e.message}")
            emptyList()
        }
    }

    suspend fun callFunction(name: String, params: Map<String, Any>): String? = withContext(Dispatchers.IO) {
        val cooldownMessage = checkCooldown()
        if (cooldownMessage != null) return@withContext cooldownMessage

        val json = gson.toJson(params)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/functions/v1/$name")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
            .addHeader("Connection", "keep-alive")
            .post(body)
            .build()

        // Implement jittered exponential backoff for neural stability
        var lastError = ""
        for (i in 1..4) { // Increased attempts to 4
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    
                    // STEP 1: LOG RAW API RESPONSE (User requested tag)
                    Log.d("AI_RAW_RESPONSE", responseBody ?: "NULL")
                    
                    if (response.isSuccessful) return@withContext responseBody
                    
                    if (response.code == 429) {
                        triggerCooldown()
                        return@withContext "{\"error\": \"AI quota exceeded\", \"error_code\": \"RETRY_EXHAUSTED\", \"message\": \"API rate limits reached. Cooling down for 30s.\"}"
                    }

                    // Log the failure to HealthManager
                    lastError = "HTTP ${response.code}: $responseBody"
                    Log.w("SUPABASE_FUNC", "Attempt $i failed: $lastError")
                    
                    // If we get a 5xx error, it's a server issue, worth retrying
                    // If we get a 4xx (other than 429), it's likely a client error, don't retry
                    if (response.code in 400..499 && response.code != 429 && response.code != 408) {
                        return@withContext "{\"error\": \"Client error\", \"error_code\": \"CLIENT_ERR\", \"message\": \"$lastError\"}"
                    }
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                Log.e("SUPABASE_FUNC", "Attempt $i Exception: $lastError")
                
                // If it's a timeout or connection issue, we retry.
            }
            
            if (i < 4) {
                // Exponential backoff: 1s, 2s, 4s with jitter
                val delayTime = (Math.pow(2.0, i.toDouble() - 1) * 1000).toLong() + (0..500).random()
                kotlinx.coroutines.delay(delayTime)
            }
        }
        
        triggerCooldown() // Failure after 4 attempts triggers a cooldown
        return@withContext "{\"error\": \"Neural link unstable\", \"error_code\": \"RETRY_EXHAUSTED\", \"message\": \"Communication failed after 4 attempts. $lastError\"}"
    }

    suspend fun select(table: String, query: String = "*", order: String = "timestamp.desc"): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/$table?select=$query&order=$order")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
            .addHeader("Connection", "keep-alive")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun upsert(table: String, data: Map<String, Any>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(data)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/$table")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SUPABASE_UPSERT", "Failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("SUPABASE_UPSERT", "Exception: ${e.message}")
        }
    }
}

