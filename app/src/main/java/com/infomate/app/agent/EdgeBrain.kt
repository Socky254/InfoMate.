package com.infomate.app.agent

import android.content.Context
import android.os.BatteryManager
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * InfoMate Edge Brain (v10.0 INVINCIBLE)
 * Integrates Gemini Nano (on-device LLM) for offline intelligence.
 */
object EdgeBrain {

    private var llmInference: LlmInference? = null

    /**
     * Initializes the On-Device LLM (Gemini Nano).
     * Requires the model (.bin) to be present in the app's files directory.
     */
    fun init(context: Context) {
        if (llmInference != null) return
        
        val modelPath = File(context.filesDir, "gemini_nano.bin")
        if (modelPath.exists()) {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath.absolutePath)
                    .setMaxTokens(512)
                    // .setTemperature(0.7f) // Temperature is not supported in this version of MediaPipe LlmInference
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                android.util.Log.i("EdgeBrain", "Gemini Nano Initialized: INVINCIBLE_MODE_ACTIVE")
            } catch (e: Exception) {
                android.util.Log.e("EdgeBrain", "Failed to initialize Gemini Nano: ${e.message}")
            }
        } else {
            android.util.Log.w("EdgeBrain", "Gemini Nano model missing. Falling back to heuristic reasoning.")
        }
    }

    suspend fun processLocally(query: String, context: Context): String? = withContext(Dispatchers.Default) {
        val isMaster = query.contains("socratesart@live") || query.contains("[AUTHORIZATION: MASTER_ARCHITECT_OVERRIDE]")
        
        val userQuery = if (query.contains("[SYSTEM_CONTEXT:")) {
            query.substringBefore("[SYSTEM_CONTEXT:").trim()
        } else {
            query.trim()
        }

        val heuristic = runHeuristics(userQuery.lowercase(), context, isMaster)
        if (heuristic != null) return@withContext heuristic

        llmInference?.let { llm ->
            try {
                val prompt = """
                    [IDENTITY: INFOMATE IRIS - OFFLINE]
                    [OBJECTIVE: PROVIDE INTELLIGENT ON-DEVICE SYNTHESIS]
                    USER: $userQuery
                    RESPONSE:
                """.trimIndent()
                return@withContext llm.generateResponse(prompt)
            } catch (e: Exception) {
                android.util.Log.e("EdgeBrain", "Gemini Nano inference failed", e)
            }
        }

        return@withContext runEmergencyHeuristic(userQuery, isMaster)
    }

    private fun runEmergencyHeuristic(query: String, isMaster: Boolean): String {
        return if (isMaster) {
            "Architect, I am detecting a major neural sync failure. Cloud and Edge engines are struggling."
        } else {
            "I'm experiencing some technical difficulties. My responses might be limited until the connection is restored."
        }
    }

    private fun runHeuristics(query: String, context: Context, isMaster: Boolean): String? {
        return when {
            query.contains("battery") || query.contains("power") -> getBatteryStatus(context)
            query.contains("time") || query.contains("date") -> getTimeStatus()
            query.contains("who are you") || query.contains("identity") -> {
                if (isMaster) "I am InfoMate v10, your Transcendent Iris."
                else "I am InfoMate v10."
            }
            else -> null
        }
    }

    private fun getBatteryStatus(context: Context): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Current battery level is $level%."
    }

    private fun getTimeStatus(): String {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        return "It is currently $time on $date."
    }
}
