package com.infomate.app.core.model

data class MemoryItem(
    val id: String? = null,
    val user_id: String? = null,
    val content: String,
    val embedding: List<Float>? = null,
    val type: String = "conversation",
    val importance: Float = 0.5f,
    val created_at: String? = null
)
