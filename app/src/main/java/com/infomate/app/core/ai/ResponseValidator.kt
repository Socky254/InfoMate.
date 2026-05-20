package com.infomate.app.core.ai

/**
 * PHASE 6: Response Validation
 */
object ResponseValidator {

    fun validate(response: String): String {
        return when {
            response.isBlank() -> "AI temporarily unavailable. Try again."
            response.contains("SYSTEM_ERROR") -> "AI temporarily unavailable. Try again."
            response.length > 5000 -> response.take(5000) + "..." // Size limit safety
            else -> response.trim()
        }
    }
}
