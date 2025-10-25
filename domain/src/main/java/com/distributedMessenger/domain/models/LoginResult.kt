package com.distributedMessenger.domain.models

import java.time.Instant
import java.util.UUID

sealed class LoginResult {
    data class Success(val userId: UUID) : LoginResult()
    object UserNotFound : LoginResult()
    data class WrongPassword(val remainingAttempts: Int) : LoginResult()
    data class AccountLocked(val lockedUntil: Instant) : LoginResult()
}
