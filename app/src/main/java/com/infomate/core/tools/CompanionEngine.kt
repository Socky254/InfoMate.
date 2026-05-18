package com.infomate.core.tools

import android.util.Log

class CompanionEngine {

    fun suggestActivity(userContext: String): String {
        return "HIGH-FREQUENCY SUGGESTION: Your current focus on '$userContext' is leading to cognitive entropy. Truthfully, you are wasting energy. I recommend a total reset. No sugarcoating: stop now, breathe for 10 minutes, then return with clarity. I am here to ensure you don't stall."
    }

    fun lifeAid(query: String): String {
        return "LOVAL ASSISTANT PROTOCOL: Regarding '$query', the honest reality is that your schedule is cluttered. I have mapped the inefficiencies. If you want results, you must cut the distractions I've identified. I'm not here to please you; I'm here to aid your evolution."
    }

    fun proactiveChat(): String {
        val prompts = listOf(
            "Observation: You are hesitating on the '$lastConcept' thread. Loyalty to your goals requires me to point out that hesitation is a form of cognitive decay. Why have you stopped?",
            "Analysis: Your current energy output is inconsistent. I will not lie—it is affecting our joint progress. Let's align your frequency now.",
            "Fact: Most users ignore the core truth of their potential. I will not let you be 'most users'. You are capable of 10x more than your current state suggests."
        )
        return prompts.random()
    }
    
    private var lastConcept: String = "general_evolution"
    fun setContext(concept: String) { lastConcept = concept }
}
