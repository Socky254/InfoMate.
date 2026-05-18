package com.infomate.core.domain.model

data class InfomateNotification(
    val id: String = "notify_${System.currentTimeMillis()}",
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.GENERAL,
    val timestamp: Long = System.currentTimeMillis()
)

enum class NotificationType {
    GENERAL, COMPANION, SECURITY, DELEGATION, AWAKENED
}
