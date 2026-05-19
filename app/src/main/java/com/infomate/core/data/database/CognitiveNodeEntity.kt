package com.infomate.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cognitive_nodes")
data class CognitiveNodeEntity(
    @PrimaryKey val id: String,
    val concept: String,
    val connections: String, // Comma-separated or JSON
    val timestamp: Long,
    val importance: Float,
    val ambientLight: Float,
    val noiseLevel: Double
)
