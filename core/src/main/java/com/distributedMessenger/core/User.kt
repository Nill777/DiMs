package com.distributedMessenger.core

import java.time.Instant
import java.util.UUID

enum class UserRole {
    UNAUTHORIZED_USER,
    USER,
    MODERATOR,
    ADMINISTRATOR
}

data class User(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val failedLoginAttempts: Int = 0,
    val lockedUntil: Instant? = null,
    val role: UserRole,
    val blockedUsersId: UUID? = null,
    val profileSettingsId: UUID,
    val appSettingsId: UUID
)
