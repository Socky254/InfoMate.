package com.infomate.app.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.infomate.app.core.config.Config
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Isolated Gemini Client (v13.0)
 * Handles direct communication with Google's Generative AI SDK.
 */
class GeminiClient(apiKey: String = Config.GEMINI_API_KEY) {

    private val generativeModel = GenerativeModel(
        modelName = Config.GEMINI_MODEL,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 2048
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
        )
    )

    suspend fun generate(prompt: String): String {
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "SYSTEM_ERROR: EMPTY_GEMINI_RESPONSE"
        } catch (e: Exception) {
            Log.e("GeminiClient", "Generation failed", e)
            "SYSTEM_ERROR: ${e.message}"
        }
    }

    fun streamGenerate(prompt: String): Flow<String> {
        return generativeModel.generateContentStream(prompt).map { chunk ->
            chunk.text ?: ""
        }
    }
}
