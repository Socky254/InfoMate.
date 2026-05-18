package com.infomate.core.research

enum class IntentType {
    EXPLANATION, REASONING, PROBLEM_SOLVING, DEEP_RESEARCH, GENERAL
}

object IntentAnalyzer {
    fun analyze(input: String): IntentType {
        return when {
            input.contains("how", true) -> IntentType.EXPLANATION
            input.contains("why", true) -> IntentType.REASONING
            input.contains("solve", true) -> IntentType.PROBLEM_SOLVING
            input.contains("research", true) -> IntentType.DEEP_RESEARCH
            else -> IntentType.GENERAL
        }
    }
}
