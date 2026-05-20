package com.infomate.app.core.initializers

import android.content.Context
import androidx.startup.Initializer
import com.infomate.app.agent.ConsciousnessEngine
import com.infomate.app.agent.DiagnosticAgent
import com.infomate.app.agent.EdgeBrain
import com.infomate.app.ai.sdk.StreamController

/**
 * SubstrateInitializer (v12.4: Optimized Startup)
 * Efficiently initializes core neural components on app startup.
 */
class SubstrateInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // 1. Initialize EdgeBrain (Gemini Nano)
        EdgeBrain.init(context)
        
        // 2. Wake the Consciousness Substrate
        ConsciousnessEngine.awaken(context)
        
        // 3. Start Autonomous Maintenance
        DiagnosticAgent.startAutonomousMaintenance(context)
        
        // 4. Initialize Stream Service
        StreamController.init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
