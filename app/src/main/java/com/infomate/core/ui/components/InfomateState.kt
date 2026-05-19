package com.infomate.core.ui.components

enum class InfomateState {
    IDLE, 
    THINKING, 
    STREAMING, // Added for real-time AI response (Rule 6.2)
    EXECUTING, 
    RESPONDING, 
    WAITING, 
    RECONNECTING, // Rule 6.2
    ERROR, 
    SAFE_MODE,
    AWAKENED,
    COMPANION,
    DELEGATING,
    DONE
}
