package com.infomate.app.core.rag

import com.infomate.app.core.ai.GeminiClient
import com.infomate.app.core.ai.PromptBuilder
import com.infomate.app.core.model.MemoryItem

class RagOrchestrator(
    private val embedder: EmbeddingClient,
    private val vectorDb: SupabaseVectorClient,
    private val gemini: GeminiClient,
    private val memory: MemoryWriter
) {

    suspend fun query(input: String): String {
        // PERFORMANCE: avoid embedding every message unnecessarily
        if (input.length < 5) {
            return gemini.generate(input)
        }

        // 1. Embed query (with blockage protection)
        val vector = try {
            embedder.embed(input)
        } catch (e: Exception) {
            emptyList<Float>() // Fallback to empty on error to avoid blockage
        }

        // 2. Retrieve memory (with data loss protection)
        val memories = if (vector.isNotEmpty()) {
            try {
                vectorDb.searchSimilar(vector)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // 3. Build context
        val context = if (memories.isNotEmpty()) {
            memories.joinToString("\n") { it.content }
        } else {
            "NO_PRIOR_MEMORY_FOUND"
        }

        // 4. Build prompt (Injecting Learning Instructions)
        val prompt = PromptBuilder.buildRAGPrompt(input, context)

        // 5. Call Gemini with Routing
        val route = when {
            input.length < 50 -> GeminiClient.ModelRoute.FLASH_LITE
            memories.isEmpty() -> GeminiClient.ModelRoute.FLASH
            else -> GeminiClient.ModelRoute.FLASH 
        }
        val response = gemini.generate(prompt, route)

        // 6. Store new memory (Learning Loop)
        if (vector.isNotEmpty() && response.isNotEmpty()) {
            try {
                memory.store(input, response, vector)
            } catch (e: Exception) {
                // Background error logging only, don't block response
            }
        }

        return response
    }
}
