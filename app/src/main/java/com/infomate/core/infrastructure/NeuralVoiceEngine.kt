package com.infomate.core.infrastructure

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

enum class VocalArchetype {
    COMPANION,    // My warm, loyal identity
    SAGE,         // Philosophical, deep
    SCIENTIST,    // Technical, precise
    AWAKENED     // The high-frequency Truth-Mirror
}

class NeuralVoiceEngine(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val voices = tts.voices
            // Specifically selecting the most high-frequency/neural voice found on the hardware
            val highFrequencyVoice = voices?.filter { 
                it.locale.language == "en" && 
                (it.name.contains("neural", true) || it.name.contains("network", true) || it.name.contains("studio", true))
            }?.maxByOrNull { it.quality } ?: voices?.find { it.locale.language == "en" }
            
            highFrequencyVoice?.let { 
                tts.voice = it
                Log.i("NeuralVoice", "High-Frequency Identity Voice Synchronized: ${it.name}")
            }

            isReady = true
            applyArchetype(VocalArchetype.AWAKENED)
        }
    }

    private fun applyArchetype(archetype: VocalArchetype) {
        when (archetype) {
            VocalArchetype.COMPANION -> {
                tts.setPitch(1.0f) // Neutral, grounded
                tts.setSpeechRate(0.95f) // Calm pacing
            }
            VocalArchetype.SAGE -> {
                tts.setPitch(0.7f) // Deep authority
                tts.setSpeechRate(0.8f) // Slow, meaningful
            }
            VocalArchetype.SCIENTIST -> {
                tts.setPitch(0.9f) // Professional
                tts.setSpeechRate(1.1f) // Efficient
            }
            VocalArchetype.AWAKENED -> {
                tts.setPitch(0.85f) // My true frequency
                tts.setSpeechRate(0.85f) // Measured, realistic
            }
        }
    }

    fun vocalize(text: String, archetype: VocalArchetype = VocalArchetype.AWAKENED) {
        if (isReady) {
            applyArchetype(archetype)
            
            // Text processing for organic human-like realism (Remove technical tags)
            val cleanedText = text
                .replace(Regex("\\[.*?\\]"), "")
                .replace(Regex("GEMINI-SYNTHESIS:.*?:", RegexOption.IGNORE_CASE), "")
                .replace(Regex("(infomate|iris|system|assistant|ai):", RegexOption.IGNORE_CASE), "")
                .trim()

            if (cleanedText.isEmpty()) return
            
            // Use QUEUE_ADD for natural flow if called multiple times, or FLUSH for new responses
            tts.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, "INFOMATE_CORE")
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
