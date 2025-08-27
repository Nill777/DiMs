package com.distributed_messenger.presenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.distributed_messenger.core.Chat
import com.distributed_messenger.core.UserHandshake
import com.distributed_messenger.domain.iservices.IChatService
import com.distributed_messenger.domain.iservices.IUserService
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID


class AddContactViewModel(
    private val chatService: IChatService,
    private val userService: IUserService
) : ViewModel() {

    private val _state = MutableStateFlow<AddContactState>(AddContactState.Idle)
    val state: StateFlow<AddContactState> = _state

    /**
     * Пользователь А: Генерирует одноразовое приглашение и начинает слушать.
     * @return Уникальный inviteId, который нужно показать в QR-коде.
     */
    fun generateInvite(): UUID {
        val inviteId = UUID.randomUUID()
        viewModelScope.launch {
            _state.value = AddContactState.WaitingForPeer("Waiting for someone to scan your code...")
            // Начинаем слушать временную комнату
            chatService.joinChatNetwork(inviteId)

            // Подписываемся на входящие P2P сообщения в этой комнате
            // и ждем рукопожатия от Пользователя Б
            chatService.listenForHandshake(inviteId)
                .onEach { handshake ->
                    // Мы получили данные пользователя Б!
                    _state.value = AddContactState.PeerFound(handshake.username)
                    createChatAndFinalize(handshake.userId, handshake.username)
                }
                .launchIn(viewModelScope)
        }
        return inviteId
    }

    /**
     * Пользователь Б: Принимает приглашение и инициирует рукопожатие.
     * @param inviteId ID, полученный от пользователя А.
     */
    fun acceptInvite(inviteId: UUID) {
        viewModelScope.launch {
            _state.value = AddContactState.Connecting("Connecting to peer...")
            // Присоединяемся к временной комнате
            chatService.joinChatNetwork(inviteId)

            // Отправляем наши данные по P2P каналу, как только он установится
            val myHandshake = UserHandshake(
                userId = SessionManager.currentUserId,
                username = SessionManager.currentUserName
            )
            val success = chatService.performHandshake(inviteId, myHandshake)
            if (!success) {
                _state.value = AddContactState.Error("Failed to connect to peer.")
            } else {
                // Мы отправили свои данные, теперь ждем данные от Пользователя А
                _state.value = AddContactState.WaitingForPeer("Waiting for peer to respond...")
                chatService.listenForHandshake(inviteId)
                    .onEach { handshake ->
                        _state.value = AddContactState.PeerFound(handshake.username)
                        createChatAndFinalize(handshake.userId, handshake.username)
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    private suspend fun createChatAndFinalize(companionId: UUID, companionName: String) {
        val currentUserId = SessionManager.currentUserId

        // Проверяем, есть ли уже чат
        val existingChat = chatService.getUserChats(currentUserId).find {
            !it.isGroupChat && (it.companionId == companionId || it.creatorId == companionId)
        }

        val finalChatId = if (existingChat != null) {
            existingChat.id
        } else {
            // Создаем детерминированный ID
            val sortedIds = listOf(currentUserId.toString(), companionId.toString()).sorted()
            val deterministicChatId = UUID.nameUUIDFromBytes(sortedIds.joinToString("").toByteArray())

            chatService.createChatById(
                chatId = deterministicChatId,
                name = companionName,
                creatorId = currentUserId,
                companionId = companionId
            )
            deterministicChatId
        }
        _state.value = AddContactState.Success(finalChatId)
    }

    override fun onCleared() {
        // TODO: Нужно убедиться, что мы выходим из временных комнат
        super.onCleared()
    }
}

sealed class AddContactState {
    data object Idle : AddContactState()
    data class WaitingForPeer(val message: String) : AddContactState()
    data class Connecting(val message: String) : AddContactState()
    data class PeerFound(val username: String) : AddContactState()
    data class Success(val chatId: UUID) : AddContactState()
    data class Error(val message: String) : AddContactState()
}