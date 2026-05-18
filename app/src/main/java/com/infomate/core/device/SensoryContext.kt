package com.infomate.core.device

data class SensoryContext(
    val ambientLight: Float = 0f,
    val acousticNoiseLevel: Double = 0.0,
    val geospatialVector: String = "Unknown",
    val biometricHeartRate: Int = 0, // Simulated via wearable or camera if enabled
    val deviceEnergyState: Int = 0,
    val electromagneticFrequency: Float = 0f
)
