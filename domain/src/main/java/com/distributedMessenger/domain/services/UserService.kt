package com.distributedMessenger.domain.services

import com.distributedMessenger.core.User
import com.distributedMessenger.core.UserRole
import com.distributedMessenger.data.irepositories.IUserRepository
import com.distributedMessenger.domain.iservices.IUserService
import com.distributedMessenger.domain.models.LoginResult
import com.distributedMessenger.domain.util.PasswordHasher
import com.distributedMessenger.logger.LogLevel
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.logger.LoggingWrapper
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class UserService(private val userRepository: IUserRepository,
                  private val pepper: String = "peper"
) : IUserService {
    companion object {
        const val MAX_LOGIN_ATTEMPTS = 3
        const val LOCKOUT_DURATION_MINUTES = 30L
    }
    // Создаём LoggingWrapper для текущего сервиса
    private val loggingWrapper = LoggingWrapper(
        origin = this,
        logger = Logger,
        tag = "UserService"
    )
    private val tag = "UserService"

    override suspend fun register(username: String, role: UserRole, password: String): UUID {
        Logger.log(tag, "Attempting to register user '$username'.")
        if (userRepository.findByUsername(username) != null) {
            Logger.log(tag, "Registration failed: username '$username' is already taken.", LogLevel.WARN)
            throw IllegalArgumentException("Username '$username' is already taken.")
        }
        val user = User(
            id = UUID.randomUUID(),
            username = username,
            passwordHash = PasswordHasher.hashPassword(password, pepper),
            role = role,
            profileSettingsId = UUID.randomUUID(),
            appSettingsId = UUID.randomUUID()
        )
        return userRepository.addUser(user)
    }

    @Suppress("ReturnCount")
    override suspend fun login(username: String, password: String): LoginResult {
        Logger.log(tag, "Login attempt for user '$username'")
        val user = userRepository.findByUsername(username)
            ?: return LoginResult.UserNotFound
        val now = Instant.now()
        val lockedUntil = user.lockedUntil

        // Проверка временной блокировки
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            Logger.log(tag, "Login failed: user '$username' is locked until $lockedUntil", LogLevel.WARN)
            return LoginResult.AccountLocked(lockedUntil)
        }

        // Блокировка истекла, пользователь пытается войти, сбрасываем счетчик
        val userToProcess = if (lockedUntil != null && lockedUntil.isBefore(now)) {
            Logger.log(tag, "User '$username' lockout period has expired. Resetting attempts.")
            user.copy(failedLoginAttempts = 0, lockedUntil = null)
        } else {
            user
        }

        if (PasswordHasher.verifyPassword(password, userToProcess.passwordHash, pepper)) {
            Logger.log(tag, "Login successful for user '$username'")
            val newPasswordHash = PasswordHasher.hashPassword(password, pepper)
            // Обновляем хэш и сбрасываем счетчик/блокировку
            val updatedUser = userToProcess.copy(
                passwordHash = newPasswordHash,
                failedLoginAttempts = 0,
                lockedUntil = null
            )
            userRepository.updateUser(updatedUser)
            return LoginResult.Success(updatedUser.id)
        } else {
            Logger.log(tag, "Login failed: incorrect password for user '$username'", LogLevel.WARN)
            val newAttemptCount = userToProcess.failedLoginAttempts + 1
            var lockTime: Instant? = null
            // разветвил чтобы не выдавал "осталось 0 попыток"
            if (newAttemptCount < MAX_LOGIN_ATTEMPTS) {
                // остались попытки
                val updatedUser = userToProcess.copy(failedLoginAttempts = newAttemptCount)
                userRepository.updateUser(updatedUser)

                val remainingAttempts = MAX_LOGIN_ATTEMPTS - newAttemptCount
                return LoginResult.WrongPassword(remainingAttempts)
            } else {
                // Если последняя или последующая попытка
                val lockTime = Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES)
                val updatedUser = userToProcess.copy(
                    failedLoginAttempts = newAttemptCount,
                    lockedUntil = lockTime
                )
                userRepository.updateUser(updatedUser)

                Logger.log(tag, "User '$username' has been locked for $LOCKOUT_DURATION_MINUTES minutes", LogLevel.WARN)
                return LoginResult.AccountLocked(lockTime)
            }
        }
    }

    override suspend fun changePassword(userId: UUID, oldPassword: String, newPassword: String): Boolean {
        return userRepository.getUser(userId)?.let { user ->
            // Проверяем, что старый пароль верный
            if (PasswordHasher.verifyPassword(oldPassword, user.passwordHash, pepper)) {
                // Хэшируем и сохраняем новый пароль
                val newPasswordHash = PasswordHasher.hashPassword(newPassword, pepper)
                val updatedUser = user.copy(passwordHash = newPasswordHash)
                userRepository.updateUser(updatedUser)
            } else {
                false
            }
        } ?: false
    }

    override suspend fun unlockUser(userId: UUID): Boolean =
        loggingWrapper {
            val user = userRepository.getUser(userId) ?: return@loggingWrapper false
            val unlockedUser = user.copy(
                failedLoginAttempts = 0,
                lockedUntil = null
            )
            userRepository.updateUser(unlockedUser)
        }

    override suspend fun findByUserName(username: String): User? =
        loggingWrapper {
            userRepository.findByUsername(username)
        }
    override suspend fun getUser(id: UUID): User? =
        loggingWrapper {
            userRepository.getUser(id)
        }

    override suspend fun getAllUsers(): List<User> =
        loggingWrapper {
            userRepository.getAllUsers()
        }

    override suspend fun updateUser(id: UUID, username: String): Boolean =
        loggingWrapper {
            // return@label используется для явного указания, из какого контекста или лямбда-выражения
            val user = userRepository.getUser(id) ?: return@loggingWrapper false
            val updatedUser = user.copy(id = id, username = username)
            userRepository.updateUser(updatedUser)
        }

    override suspend fun updateUserRole(id: UUID, newRole: UserRole): Boolean =
        loggingWrapper {
            val user = userRepository.getUser(id) ?: return@loggingWrapper false
            val updatedUser = user.copy(id = id, role = newRole)
            userRepository.updateUser(updatedUser)
        }

    override suspend fun deleteUser(id: UUID): Boolean =
        loggingWrapper {
            userRepository.deleteUser(id)
        }
}
