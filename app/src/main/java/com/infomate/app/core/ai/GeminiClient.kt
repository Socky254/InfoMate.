package com.infomate.app.core.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.infomate.app.core.config.Config
import kotlinx.coroutines.delay

/**
 * PHASE 2: Gemini Integration (Core Brain)
 * v13.0: Routing Layer implemented.
 */
class GeminiClient(apiKey: String = Config.GEMINI_API_KEY) {

    enum class ModelRoute {
        FLASH_LITE, FLASH, PRO
    }

    private val flashLiteModel = GenerativeModel(
        modelName = "gemini-1.5-flash", // v13.5: Using 1.5-flash as the lite baseline
        apiKey = apiKey
    )

    private val flashModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    private val proModel = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey
    )

    suspend fun generate(prompt: String, route: ModelRoute = ModelRoute.FLASH): String {
        val model = when (route) {
            ModelRoute.FLASH_LITE -> flashLiteModel
            ModelRoute.FLASH -> flashModel
            ModelRoute.PRO -> proModel
        }
        
        return try {
            val response = callModel(model, prompt)
            if (response.isBlank()) throw Exception("EMPTY_RESPONSE")
            response
        } catch (e: Exception) {
            Log.w("GeminiClient", "Route ${route.name} failed: ${e.message}. Retrying with Pro...")
            retryWithPro(prompt)
        }
    }

    private suspend fun callModel(model: GenerativeModel, prompt: String): String {
        val response = model.generateContent(prompt)
        return response.text ?: throw Exception("EMPTY_RESPONSE")
    }

    private suspend fun retryWithPro(prompt: String): String {
        return try {
            delay(1000)
            callModel(proModel, prompt)
        } catch (e: Exception) {
            Log.e("GeminiClient", "Final Pro fallback failed", e)
            "SYSTEM_ERROR: NEURAL_ENGINE_FAILURE: ${e.message}"
        }
    }
}
