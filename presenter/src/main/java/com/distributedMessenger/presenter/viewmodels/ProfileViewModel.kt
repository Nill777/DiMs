package com.distributedMessenger.presenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.distributedMessenger.core.User
import com.distributedMessenger.domain.iservices.IUserService
import com.distributedMessenger.logger.LogLevel
import com.distributedMessenger.logger.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val userService: IUserService) : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val state: StateFlow<ProfileState> = _state

    // Функция для получения текущего пользователя
    @Suppress("TooGenericExceptionCaught")
    fun loadCurrentUser() {
        val userId = SessionManager.currentUserId
        Logger.log("ProfileViewModel", "Loading current user with ID: $userId")

        viewModelScope.launch {
            _state.value = ProfileState.Loading
            try {
                val fetchedUser = userService.getUser(userId)
                _user.value = fetchedUser
                _state.value = ProfileState.Idle
                Logger.log("ProfileViewModel", "User loaded successfully: ${fetchedUser!!.id}")
            } catch (e: Exception) {
                Logger.log("ProfileViewModel", "Error loading user: ${e.message}", LogLevel.ERROR, e)
                _state.value = ProfileState.Error(e.message ?: "Failed to load user")
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun updateUsername(newName: String): Job {
        val userId = SessionManager.currentUserId
        Logger.log("ProfileViewModel", "Updating username for user: $userId")

        return viewModelScope.launch {
            _state.value = ProfileState.Loading
            try {
                val success = userService.updateUser(userId, newName)
                if (success) {
                    SessionManager.updateUserName(newName)
                    _user.value = _user.value?.copy(username = newName)
                    _state.value = ProfileState.Idle
                    Logger.log("ProfileViewModel", "Username updated successfully")
                } else {
                    error("Failed to update username")
                }
            } catch (e: Exception) {
                Logger.log("ProfileViewModel", "Error updating username: ${e.message}", LogLevel.ERROR, e)
                _state.value = ProfileState.Error(e.message ?: "Failed to update username")
                // Откатываем локальные изменения
                _user.value?.let {
                    SessionManager.updateUserName(it.username)
                }
            }
        }
    }

    sealed class ProfileState {
        object Idle : ProfileState()
        object Loading : ProfileState()
        data class Error(val message: String) : ProfileState()
    }
}
