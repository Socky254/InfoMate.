package com.infomate.core.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUrl: String? = null,
    val preferences: Map<String, String> = emptyMap()
)
