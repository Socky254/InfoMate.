package com.infomate.core.data.repository

import com.infomate.core.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    fun signIn(name: String, email: String) {
        // Mock sign in - in production use Firebase/Supabase
        _currentUser.value = User(
            id = "user_123",
            name = name,
            email = email,
            preferences = mapOf("theme" to "dark", "expertise" to "Quantum Physics")
        )
    }

    fun signOut() {
        _currentUser.value = null
    }

    fun isLoggedIn(): Boolean = _currentUser.value != null
}
