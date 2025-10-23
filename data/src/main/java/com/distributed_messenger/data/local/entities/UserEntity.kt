package com.distributed_messenger.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import com.distributed_messenger.core.UserRole
import java.time.Instant
import java.util.UUID

// Добавляем индекс, чтобы сделать поле "username" уникальным на уровне базы данных.
// Это предотвратит создание двух пользователей с одинаковыми именами.
//@Entity(tableName = "users")
@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "failed_login_attempts", defaultValue = "0") val failedLoginAttempts: Int = 0,
    @ColumnInfo(name = "locked_until") val lockedUntil: Instant? = null,
    @ColumnInfo(name = "two_factor_code") val twoFactorCode: String? = null,
    @ColumnInfo(name = "two_factor_expires_at") val twoFactorCodeExpiresAt: Instant? = null,
    @ColumnInfo(name = "role") val role: UserRole,
    @ColumnInfo(name = "blocked_users_id") val blockedUsersId: UUID? = null,
    @ColumnInfo(name = "profile_settings_id") val profileSettingsId: UUID,
    @ColumnInfo(name = "app_settings_id") val appSettingsId: UUID
)