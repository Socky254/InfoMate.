package com.infomate.app.core.network

import com.infomate.app.core.config.Config
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
        
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun insert(table: String, data: Map<String, Any>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(data)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/$table")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
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
        val json = gson.toJson(params)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/functions/v1/$name")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
            .post(body)
            .build()

        // Implement retry logic (repeat 3 times)
        var lastError = ""
        for (i in 1..3) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d("INFOMATE_RAW", "Function: $name, Attempt: $i, Response: $responseBody")
                    
                    if (response.isSuccessful) return@withContext responseBody
                    
                    if (response.code == 429) {
                        return@withContext "{\"error\": \"AI quota exceeded. Please wait a moment before retrying.\"}"
                    }
                    
                    lastError = "HTTP ${response.code}: $responseBody"
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                Log.e("SUPABASE_FUNC", "Attempt $i failed: $lastError")
            }
            if (i < 3) kotlinx.coroutines.delay(1000)
        }
        return@withContext "{\"error\": \"System timeout. Connection to neural link failed after 3 attempts. $lastError\"}"
    }

    suspend fun select(table: String, query: String = "*", order: String = "timestamp.desc"): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${Config.SUPABASE_URL}/rest/v1/$table?select=$query&order=$order")
            .addHeader("apikey", Config.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${Config.SUPABASE_KEY}")
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

