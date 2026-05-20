package com.infomate.app.ai

/**
 * Validates AI output for stability and safety.
 */
object ResponseValidator {

    fun validate(response: String): String {
        if (response.isBlank()) return "SYSTEM_ERROR: NULL_SUBSTRATE_OUTPUT"
        
        if (response.contains("SYSTEM_ERROR:")) {
            return "Iris: I am experiencing a temporary neural disconnect. My primary reasoning core is unreachable."
        }
        
        // Clean technical markers that shouldn't reach the user
        return response
            .replace(Regex("\\[.*?\\]"), "")
            .trim()
    }
}
