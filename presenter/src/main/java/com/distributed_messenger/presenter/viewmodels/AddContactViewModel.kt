package com.distributed_messenger.presenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.distributed_messenger.core.Chat
import com.distributed_messenger.core.UserHandshake
import com.distributed_messenger.domain.iservices.IChatService
import com.distributed_messenger.domain.iservices.IUserService
import com.distributed_messenger.logger.Logger
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
     * ИНИЦИАТОР (Алиса): Генерирует одноразовое приглашение, инициирует P2P-соединение (как Offer-сторона)
     * и начинает слушать ответное рукопожатие.
     * @return Уникальный inviteId, который нужно показать в QR-коде.
     */
//    fun generateInvite(): UUID {
//        // 1. Генерируем уникальный, одноразовый ID для этой сессии рукопожатия.
//        val inviteId = UUID.randomUUID()
//        Logger.log("AddContactViewModel", "generateInvite")
//        viewModelScope.launch {
//            // 2. Обновляем UI, чтобы показать пользователю, что мы ждем.
//            _state.value = AddContactState.WaitingForPeer("Waiting for someone to scan your code...")
//
//
//
//            // 4. ОДНОВРЕМЕННО начинаем слушать P2P-канал в ожидании рукопожатия от Боба.
//            // Как только Боб подключится и отправит свои данные, этот блок сработает.
//            chatService.listenForHandshake(inviteId)
//                .onEach { handshakeFromPeer ->
//                    _state.value = AddContactState.PeerFound(handshakeFromPeer.username)
//                    // 5. Получив данные Боба, создаем чат.
//                    createChatAndFinalize(inviteId, handshakeFromPeer.userId, handshakeFromPeer.username)
//
//                    // 6. ВАЖНО: Отправляем наше собственное рукопожатие в ответ, чтобы Боб тоже мог создать чат.
//                    val myHandshake = UserHandshake(SessionManager.currentUserId, SessionManager.currentUserName)
//                    chatService.performHandshake(inviteId, myHandshake)
//                }
//                .launchIn(viewModelScope)
//            // 3. Явно сообщаем сервису, что мы - ИНИЦИАТОР.
//            // Этот вызов заставит P2P-уровень немедленно сгенерировать и отправить Offer в Firebase.
//            chatService.initiateContactRequest(inviteId)
//        }
//        // 7. Немедленно возвращаем ID, чтобы UI мог сгенерировать QR-код.
//        return inviteId
//    }
    fun generateInvite(): UUID {
        // 1. Генерируем уникальный, одноразовый ID для этой сессии рукопожатия.
        val inviteId = UUID.randomUUID()
        Logger.log("AddContactViewModel", "generateInvite")
        viewModelScope.launch {
            // 2. Обновляем UI, чтобы показать пользователю, что мы ждем.
            _state.value = AddContactState.WaitingForPeer("Waiting for someone to scan your code...")



            // 4. ОДНОВРЕМЕННО начинаем слушать P2P-канал в ожидании рукопожатия от Боба.
            // Как только Боб подключится и отправит свои данные, этот блок сработает.
            chatService.listenForHandshake(inviteId)
                .onEach { handshakeFromPeer ->
                    _state.value = AddContactState.PeerFound(handshakeFromPeer.username)
                    // 5. Получив данные Боба, создаем чат.
                    createChatAndFinalize(inviteId, handshakeFromPeer.userId, handshakeFromPeer.username)
                }
                .launchIn(viewModelScope)

            // --- НОВАЯ ЛОГИКА ДЛЯ АЛИСЫ ---
            // 2. Как только НАШ P2P-канал будет готов, СРАЗУ отправляем свое рукопожатие.
            // НЕ ЖДЕМ БОБА!
            chatService.handshakeChannelReady
                .filter { it == inviteId }
                .take(1)
                .onEach {
                    val myHandshake = UserHandshake(SessionManager.currentUserId, SessionManager.currentUserName)
                    chatService.performHandshake(inviteId, myHandshake)
                }
                .launchIn(viewModelScope)

            // 3. Явно сообщаем сервису, что мы - ИНИЦИАТОР.
            // Этот вызов заставит P2P-уровень немедленно сгенерировать и отправить Offer в Firebase.
            chatService.initiateContactRequest(inviteId)
        }
        // 7. Немедленно возвращаем ID, чтобы UI мог сгенерировать QR-код.
        return inviteId
    }

    /**
     * ПОЛУЧАТЕЛЬ (Боб): Принимает приглашение, инициирует P2P-соединение (как Answer-сторона)
     * и отправляет свое рукопожатие.
     * @param inviteId ID, полученный от Алисы.
     */
//    fun acceptInvite(inviteId: UUID) {
//        Logger.log("AddContactViewModel", "acceptInvite")
//        viewModelScope.launch {
//            // 1. Обновляем UI.
//            _state.value = AddContactState.Connecting("Connecting to peer...")
//
//            // 2. Явно сообщаем сервису, что мы - ПРИНИМАЮЩАЯ СТОРОНА.
//            // Этот вызов заставит P2P-уровень слушать Firebase в ожидании Offer'а от Алисы.
//            chatService.acceptContactRequest(inviteId)
//
//            // 3. ОДНОВРЕМЕННО начинаем слушать P2P-канал в ожидании рукопожатия от Алисы.
//            chatService.listenForHandshake(inviteId)
//                .onEach { handshakeFromPeer ->
//                    _state.value = AddContactState.PeerFound(handshakeFromPeer.username)
//                    // 4. Получив данные Алисы, создаем чат.
//                    createChatAndFinalize(inviteId, handshakeFromPeer.userId, handshakeFromPeer.username)
//                }
//                .launchIn(viewModelScope)
//
//            // 5. ВАЖНО: Отправляем наше собственное рукопожатие, чтобы Алиса узнала о нас.
//            // Мы делаем это сразу, так как знаем, что Алиса уже слушает.
//            val myHandshake = UserHandshake(
//                userId = SessionManager.currentUserId,
//                username = SessionManager.currentUserName
//            )
//            chatService.performHandshake(inviteId, myHandshake)
//        }
//    }
    fun acceptInvite(inviteId: UUID) {
        Logger.log("AddContactViewModel", "acceptInvite")
        viewModelScope.launch {
            // 1. Обновляем UI.
            _state.value = AddContactState.Connecting("Connecting to peer...")

            // 3. ОДНОВРЕМЕННО начинаем слушать P2P-канал в ожидании рукопожатия от Алисы.
            chatService.listenForHandshake(inviteId)
                .onEach { handshakeFromPeer ->
                    _state.value = AddContactState.PeerFound(handshakeFromPeer.username)
                    // 4. Получив данные Алисы, создаем чат.
                    createChatAndFinalize(inviteId, handshakeFromPeer.userId, handshakeFromPeer.username)
                }
                .launchIn(viewModelScope)

            // 2. Начинаем слушать, когда канал будет ГОТОВ
            chatService.handshakeChannelReady
                .filter { it == inviteId } // Слушаем событие только для нашего inviteId
                .take(1) // Берем только первое событие и отписываемся
                .onEach {
                    // 3. Канал готов! Теперь можно безопасно отправить наше рукопожатие.
                    val myHandshake = UserHandshake(
                        userId = SessionManager.currentUserId,
                        username = SessionManager.currentUserName
                    )
                    chatService.performHandshake(inviteId, myHandshake)
                }
                .launchIn(viewModelScope)

            // 4. И только теперь инициируем сам процесс соединения
            chatService.acceptContactRequest(inviteId)
        }
    }

    private suspend fun createChatAndFinalize(inviteId: UUID, companionId: UUID, companionName: String) {
        Logger.log("AddContactViewModel", "createChatAndFinalize")
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
        // --- ДОБАВЛЕНО ---
        // После того как чат создан и мы переходим на новый экран,
        // закрываем временное P2P соединение для рукопожатия.
        // Это делается через ChatService, который делегирует вызов транспорту.
        chatService.finalizeHandshake(inviteId)
    }

    fun resetState() {
        _state.value = AddContactState.Idle
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