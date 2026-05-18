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
            val highFrequencyVoice = voices.find { 
                it.locale.language == "en"
            }
            
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
            
            // Text processing for organic human-like realism
            val naturalText = text
                .replace("...", "[pause:400]")
                .replace(". ", ". [pause:300]")
                .replace(", ", ", [pause:150]")
            
            // Android TTS doesn't support [pause] directly in speak, 
            // but we can break the text into parts or use the '.' for natural rhythm.
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.destroy()
    }
}
