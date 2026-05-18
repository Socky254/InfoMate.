package com.infomate.app.core.network

import com.infomate.app.core.config.Config
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseClient {

    private val client = OkHttpClient()
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

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // Log error if necessary
            }
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

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                listOf(response.body?.string() ?: "")
            } else {
                emptyList()
            }
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

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }
}
