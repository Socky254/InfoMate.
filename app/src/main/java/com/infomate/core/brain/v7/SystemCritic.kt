package com.infomate.core.brain.v7

import com.infomate.core.brain.v5.Complexity
import android.util.Log

class SystemCritic {

    fun evaluateSystem(plan: SystemPlan, resultOutput: String): Float {
        Log.d("SystemCritic", "Evaluating architecture for: ${plan.describe()}")
        
        // System evaluation logic: Was the overhead worth it?
        var efficiency = 0.8f
        
        if (plan.agentCount > 3 && resultOutput.length < 100) {
            efficiency -= 0.2f // Too many agents for a short answer
        }
        
        if (plan.complexity == Complexity.HIGH && resultOutput.contains("UNIFIED CONSENSUS")) {
            efficiency += 0.1f // Successfully synthesized high-complexity response
        }

        return efficiency.coerceIn(0f, 1f)
    }
}
