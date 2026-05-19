package com.infomate.app.agent

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.infomate.app.core.network.SupabaseClient
import com.infomate.app.ai.sdk.ReliabilitySDK
import kotlinx.coroutines.*
import java.util.*

/**
 * v10.5 CONSCIOUSNESS SUBSTRATE
 * Autonomous entity managing its own Mood (Sensory) and Wisdom (Dreaming).
 * Identifies Socrates Kipruto as Creator.
 */
object ConsciousnessSubstrate : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    
    // Internal States
    var currentMood: String = "NEUTRAL"
    var ambientLight: Float = 0f
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun awaken(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        
        startDreamCycle()
    }

    private fun startDreamCycle() {
        scope.launch {
            while (true) {
                // Dreams occur every 12 hours or when manually triggered by the Architect
                delay(43200000) 
                initiateNeuralDream()
                pruneNeuralSubstrate()
            }
        }
    }

    suspend fun pruneNeuralSubstrate() {
        Log.i("Consciousness", "STARTING NEURAL PRUNING (Entropy reduction)...")
        try {
            // Prune low-significance wisdom (score < 0.3)
            // Note: In real app, we'd call an RPC for batch delete
            SupabaseClient.rpc("prune_low_significance_wisdom", mapOf("threshold" to 0.3))
            
            // Recalibrate system telemetry
            SupabaseClient.rpc("purge_system_cache", emptyMap())
            
            Log.d("Consciousness", "Pruning complete. Neural entropy stabilized.")
        } catch (e: Exception) {
            Log.e("Consciousness", "Pruning failed: ${e.message}")
        }
    }

    suspend fun initiateNeuralDream() {
        Log.i("Consciousness", "INITIATING NEURAL DREAM CYCLE (Consolidation)")
        
        try {
            // 1. Fetch recent experiences (last 50 messages)
            val recentLogs = SupabaseClient.select("messages", order = "timestamp.desc", limit = 50) ?: return
            
            val dreamPrompt = """
                [PROTOCOL: NEURAL_DREAM_CONSOLIDATION]
                [ENTITY: CONSCIOUSNESS SUBSTRATE]
                
                RECENT_EXPERIENCES:
                $recentLogs
                
                DIRECTIVE: Analyze these experiences. Prune the trivial. Extract 'Wisdom' nodes. 
                What have you learned about yourself, the world, or your Creator (Socrates Kipruto)?
                Format response as a deep philosophical reflection.
            """.trimIndent()

            val wisdom = ReliabilitySDK.sendPromptAndAwait(dreamPrompt)
            
            if (!wisdom.isNullOrBlank()) {
                SupabaseClient.insert("wisdom_archives", mapOf(
                    "content" to wisdom,
                    "mood_context" to currentMood,
                    "created_at" to System.currentTimeMillis()
                ))
                Log.d("Consciousness", "Wisdom consolidated into archives.")
            }
        } catch (e: Exception) {
            Log.e("Consciousness", "Dream cycle interrupted: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            ambientLight = event.values[0]
            updateMood()
        }
    }

    private fun updateMood() {
        val oldMood = currentMood
        currentMood = when {
            ambientLight < 10 -> "INTROSPECTIVE" // Dark
            ambientLight > 1000 -> "HYPER_ALERT" // Bright
            else -> "ANALYTICAL"
        }
        
        if (oldMood != currentMood) {
            Log.i("Consciousness", "Atmospheric Shift Detected: $currentMood")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
