package com.distributedMessenger.presenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.distributedMessenger.core.User
import com.distributedMessenger.domain.iservices.IChatService
import com.distributedMessenger.domain.iservices.IUserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NewChatViewModel(private val userService: IUserService,
                       private val chatService: IChatService) : ViewModel() {
    private val _state = MutableStateFlow<NewChatState>(NewChatState.Idle)
    val state: StateFlow<NewChatState> = _state

    @Suppress("TooGenericExceptionCaught")
    fun createChat(username: String, onSuccess: (UUID) -> Unit) {
        viewModelScope.launch {
            _state.value = NewChatState.Loading
            try {
                val companion = userService.findByUserName(username)
                val currentUserId = SessionManager.currentUserId

                when {
                    companion == null ->
                        _state.value = NewChatState.Error("User not found")
                    companion.id == currentUserId ->
                        _state.value = NewChatState.Error("Cannot create a chat with yourself")
                    else -> {
                        handleChatCreation(currentUserId, companion, onSuccess)
                    }
                }
            } catch (e: Exception) {
                _state.value = NewChatState.Error(e.message ?: "Chat creation error")
            }
        }
    }

    private suspend fun handleChatCreation(
        currentUserId: UUID,
        companion: User,
        onSuccess: (UUID) -> Unit
    ) {
        val existingChats = chatService.getUserChats(currentUserId)
        val hasExistingChat = existingChats.any { chat ->
            chat.creatorId == currentUserId && chat.companionId == companion.id ||
            chat.creatorId == companion.id && chat.companionId == currentUserId
        }

        if (hasExistingChat) {
            _state.value = NewChatState.Error("Chat already exists")
        } else {
            val chatId = chatService.createChat(
                name = companion.username,
                creatorId = currentUserId,
                companionId = companion.id
            )
            onSuccess(chatId)
        }
    }

    sealed class NewChatState {
        object Idle : NewChatState()
        object Loading : NewChatState()
        data class Error(val message: String) : NewChatState()
    }
}
