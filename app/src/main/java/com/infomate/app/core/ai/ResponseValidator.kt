package com.infomate.app.core.ai

/**
 * PHASE 6: Response Validation
 */
object ResponseValidator {

    fun validate(response: String): String {
        return when {
            response.isBlank() -> "NEURAL_LINK_STALLED: Empty response from substrate."
            response.contains("SYSTEM_ERROR") -> "AI_TEMPORARILY_UNAVAILABLE: Core brain reported a system-level fault."
            response.length > 5000 -> response.take(5000) + " [TRUNCATED_FOR_STABILITY]"
            else -> response.trim()
        }
    }
}
