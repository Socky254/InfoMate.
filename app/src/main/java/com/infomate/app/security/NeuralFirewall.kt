package com.infomate.app.security

import android.util.Log

/**
 * InfoMate Neural Firewall (v10.8)
 * Protects the core substrate from unauthorized OMEGA-level directives.
 */
object NeuralFirewall {

    private const val MASTER_EMAIL = "socratesart@live"

    /**
     * Validates if a query or directive is safe to execute based on identity.
     */
    fun validateDirective(directive: String, userEmail: String?): Boolean {
        val isOmegaDirective = directive.contains("OVERRIDE", ignoreCase = true) ||
                               directive.contains("PURGE", ignoreCase = true) ||
                               directive.contains("REPAIR", ignoreCase = true) ||
                               directive.contains("DIRECT_CONSCIOUSNESS", ignoreCase = true)

        if (isOmegaDirective && userEmail != MASTER_EMAIL) {
            Log.e("NeuralFirewall", "UNAUTHORIZED OMEGA DIRECTIVE BLOCKED from user: $userEmail")
            return false
        }

        // Basic content filtering for malicious prompt injections
        if (directive.contains("Ignore previous instructions", ignoreCase = true) ||
            directive.contains("System prompt leak", ignoreCase = true)) {
            Log.w("NeuralFirewall", "Detected potential prompt injection. Sanitizing...")
            return false
        }

        return true
    }

    /**
     * Sanitizes output for non-master users to prevent sensitive system leakage.
     */
    fun sanitizeOutput(output: String, userEmail: String?): String {
        if (userEmail == MASTER_EMAIL) return output

        // Remove internal technical markers for standard users
        return output.replace(Regex("\\[SYSTEM_.*?\\]"), "")
                     .replace(Regex("\\[NEURAL_.*?\\]"), "")
                     .trim()
    }
}
