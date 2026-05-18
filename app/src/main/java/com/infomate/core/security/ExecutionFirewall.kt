package com.infomate.core.security

import android.util.Log

class ExecutionFirewall {
    
    sealed class ValidationResult {
        object Allowed : ValidationResult()
        data class Denied(val reason: String) : ValidationResult()
    }

    fun validateToolCall(toolName: String, parameters: Any): ValidationResult {
        Log.i("ExecutionFirewall", "Validating tool: $toolName")
        
        // Strict allow-list
        val allowedTools = listOf("search", "math", "quantum_sim")
        
        if (!allowedTools.contains(toolName)) {
            return ValidationResult.Denied("Unauthorized tool: $toolName")
        }
        
        // Add parameter inspection for prompt injection or malicious patterns
        val paramString = parameters.toString().lowercase()
        val dangerousPatterns = listOf("rm -rf", "delete", "format", "drop table")
        
        if (dangerousPatterns.any { paramString.contains(it) }) {
            return ValidationResult.Denied("Dangerous command pattern detected")
        }

        return ValidationResult.Allowed
    }
}
