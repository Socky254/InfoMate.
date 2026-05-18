package com.infomate.app.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}
