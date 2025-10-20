package com.distributed_messenger.presenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.distributed_messenger.core.UserRole
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import com.distributed_messenger.domain.iservices.IUserService
import com.distributed_messenger.domain.models.LoginResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class AuthViewModel(private val userService: IUserService) : ViewModel() {
    // Состояния UI
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Возвращаем Job, чтобы вызывающий код мог дождаться завершения
    fun register(username: String, password: String, role: UserRole): Job {
        Logger.log("AuthViewModel", "Attempting registration for: $username ($role)")
        if (username.isBlank() || password.isBlank()) {
            Logger.log("AuthViewModel", "Empty username or password in registration", LogLevel.WARN)
            _authState.value = AuthState.Error("Username and password cannot be empty")
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val userId = userService.register(username, role, password)
                SessionManager.login(userId, username, role)
                _authState.value = AuthState.RegistrationSuccess(userId)
                Logger.log("AuthViewModel", "Registration successful. User ID: $userId")
            } catch (e: Exception) {
                Logger.log("AuthViewModel", "Registration failed: ${e.message}", LogLevel.ERROR, e)
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun login(username: String, password: String): Job {
        Logger.log("AuthViewModel", "Attempting login for: $username")
        if (username.isBlank() || password.isBlank()) {
            Logger.log("AuthViewModel", "Empty username or password in login", LogLevel.WARN)
            _authState.value = AuthState.Error("Username and password cannot be empty")
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = userService.login(username, password)
                when (result) {
                    is LoginResult.Success -> {
                        val userId = result.userId
                        SessionManager.login(userId, username, userService.getUser(userId)?.role ?: UserRole.UNAUTHORIZED_USER)
                        _authState.value = AuthState.LoginSuccess(userId)
                        Logger.log("AuthViewModel", "Login successful. User ID: ${userId}")
                    }
                    is LoginResult.UserNotFound -> {
                        _authState.value = AuthState.Error("User not found")
                    }
                    is LoginResult.WrongPassword -> {
                        _authState.value = AuthState.Error("Invalid password. Attempts remaining: ${result.remainingAttempts}")
                    }
                    is LoginResult.AccountLocked -> {
                        // Передаем в UI время окончания блокировки
                        _authState.value = AuthState.Locked(result.lockedUntil)
                    }
                }
            } catch (e: Exception) {
                Logger.log("AuthViewModel", "Login error: ${e.message}", LogLevel.ERROR, e)
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                SessionManager.logout()
                resetState()
                Logger.log("AuthViewModel", "Logout successful")
            } catch (e: Exception) {
                Logger.log("AuthViewModel", "Logout error: ${e.message}", LogLevel.ERROR, e)
            }
        }
    }
//    fun getCurrentUserId(): UUID {
//        if (!::currentUserId.isInitialized) {
//            Logger.log("AuthViewModel", "Accessing uninitialized currentUserId", LogLevel.WARN)
//        }
//        return currentUserId
//    }

    private fun resetState() {
        _authState.value = AuthState.Idle
        Logger.log("AuthViewModel", "Auth state reset")
    }
    
    // Модель состояний
    // "запечатанный" (ограниченный) класс-контейнер для состояний
    sealed class AuthState {
        object Idle : AuthState()   // object - синглтон (один экземпляр на всё приложение)
        object Loading : AuthState()
        data class RegistrationSuccess(val userId: UUID) : AuthState()  // класс для хранения данных
        data class LoginSuccess(val userId: UUID) : AuthState()
        // Общая ошибка для простых сообщений
        data class Error(val message: String) : AuthState()
        // Специальное состояние для заблокированного аккаунта
        data class Locked(val lockedUntil: Instant) : AuthState()
    }
}