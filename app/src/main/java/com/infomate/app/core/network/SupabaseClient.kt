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

    private var userToken: String? = null

    fun setUserToken(token: String?) {
        userToken = token
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS) // Optimized for Gemini Real-time
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()
        
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun insert(table: String, data: Map<String, Any>): String? = withContext(Dispatchers.IO) {
        val json = gson.toJson(data)
        val body = json.toRequestBody(mediaType)
        
        var lastError: String? = null
        for (attempt in 1..4) { // Increased to 4 attempts
            val request = Request.Builder()
                .url("${Config.SUPABASE_URL}/rest/v1/$table")
                .addHeader("apikey", Config.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${userToken ?: Config.SUPABASE_KEY}")
                .addHeader("Prefer", "return=representation")
                .addHeader("Connection", "keep-alive")
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) return@withContext responseBody
                    
                    lastError = "HTTP ${response.code}: $responseBody"
                    Log.e("SUPABASE_INSERT", "Attempt $attempt failed: $lastError")
                }
            } catch (e: Exception) {
                lastError = e.message
                Log.e("SUPABASE_INSERT", "Exception (Attempt $attempt): $lastError")
            }
            if (attempt < 4) {
                // Exponential backoff: 2s, 4s, 8s
                val delayTime = (Math.pow(2.0, attempt.toDouble()) * 1000).toLong()
                kotlinx.coroutines.delay(delayTime)
            }
        }
        null
    }

    suspend fun callFunction(name: String, params: Map<String, Any>): String? = withContext(Dispatchers.IO) {
        val json = gson.toJson(params)
        val body = json.toRequestBody(mediaType)
        
        var lastError: String? = null
        for (attempt in 1..4) { // Increased to 4 attempts
            val request = Request.Builder()
                .url("${Config.SUPABASE_URL}/functions/v1/$name")
                .addHeader("apikey", Config.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${userToken ?: Config.SUPABASE_KEY}")
                .addHeader("Connection", "keep-alive")
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) return@withContext responseBody
                    
                    lastError = "HTTP ${response.code}: $responseBody"
                    Log.e("SUPABASE_FUNC", "Attempt $attempt ($name) failed: $lastError")
                }
            } catch (e: Exception) {
                lastError = e.message
                Log.e("SUPABASE_FUNC", "Exception (Attempt $attempt, $name): $lastError")
            }
            if (attempt < 4) {
                // Exponential backoff: 2s, 4s, 8s
                val delayTime = (Math.pow(2.0, attempt.toDouble()) * 1000).toLong()
                kotlinx.coroutines.delay(delayTime)
            }
        }
        null
    }

    suspend fun select(
        table: String,
        query: String = "*",
        order: String = "timestamp.desc",
        limit: Int? = null
    ): String? = withContext(Dispatchers.IO) {
        val urlBuilder = StringBuilder("${Config.SUPABASE_URL}/rest/v1/$table?select=$query&order=$order")
        limit?.let { urlBuilder.append("&limit=$it") }
        
        val request = Request.Builder()
            .url(urlBuilder.toString())
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${userToken ?: Config.SUPABASE_KEY}")
            .addHeader("Connection", "keep-alive")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful) body else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun rpc(function: String, params: Map<String, Any>): List<String> = withContext(Dispatchers.IO) {
        val json = gson.toJson(params)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/rpc/$function")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${userToken ?: Config.SUPABASE_KEY}")
            .addHeader("Connection", "keep-alive")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d("SUPABASE_RPC", "Function: $function, Response: $responseBody")
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

    suspend fun upsert(table: String, data: Map<String, Any>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(data)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/$table")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${userToken ?: Config.SUPABASE_KEY}")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .addHeader("Connection", "keep-alive")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) Log.e("SUPABASE_UPSERT", "Failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("SUPABASE_UPSERT", "Exception: ${e.message}")
        }
    }
}
