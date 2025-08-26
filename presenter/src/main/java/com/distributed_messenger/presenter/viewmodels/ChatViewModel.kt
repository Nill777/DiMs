package com.distributed_messenger.presenter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.distributed_messenger.core.Message
import com.distributed_messenger.domain.iservices.IMessageService
import com.distributed_messenger.domain.iservices.IChatService
import com.distributed_messenger.domain.iservices.IUserService
import com.distributed_messenger.core.Chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val messageService: IMessageService,
                    private val chatService: IChatService,
                    private val chatId: UUID) : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _chatInfo = MutableStateFlow<Chat?>(null)
    val chatInfo: StateFlow<Chat?> = _chatInfo

    private val _deleteStatus = MutableStateFlow<Boolean?>(null)
    val deleteStatus: StateFlow<Boolean?> = _deleteStatus

    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage

//    private val _companionName = MutableStateFlow<String?>(null)
//    val companionName: StateFlow<String?> = _companionName

    init {
//        loadMessages()
//        loadChatInfo()
//        loadCompanionData()
        // ИЗМЕНЕНИЕ 1: Присоединяемся к P2P-сети чата при создании ViewModel
        // 2. Сразу после присоединения запрашиваем синхронизацию.
        viewModelScope.launch {
            chatService.joinChatNetwork(chatId)
            messageService.requestMessagesSync(chatId)
        }


        // ИЗМЕНЕНИЕ 2: Загружаем информацию о чате
        loadChatInfo()

        // ИЗМЕНЕНИЕ 3: Подписываемся на реактивное обновление сообщений из БД
        observeMessages()
    }

    // ИЗМЕНЕНИЕ 4: Новый метод для подписки на Flow сообщений
    private fun observeMessages() {
        viewModelScope.launch {
            messageService.getChatMessagesFlow(chatId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _messages.value = messageService.getChatMessages(chatId)
        }
    }
    private fun loadChatInfo() {
        viewModelScope.launch {
            _chatInfo.value = chatService.getChat(chatId)
        }
    }
//    private fun loadCompanionData() {
//        viewModelScope.launch {
//            _chatInfo.value?.let { chat ->
//                if (!chat.isGroupChat) {
//                    chat.companionId?.let { companionId ->
//                        _companionName.value = userService.getUser(companionId)?.username
//                    }
//                }
//            }
//        }
//    }

    fun sendMessage(content: String, fileId: UUID? = null) {
        viewModelScope.launch {
            val senderId = SessionManager.currentUserId
            messageService.sendMessage(
                senderId = senderId,
                chatId = chatId,
                content = content,
                fileId = fileId
            )
            // ИЗМЕНЕНИЕ 5: Больше не нужно вызывать loadMessages() вручную.
            // UI обновится автоматически благодаря Flow.
        }
    }

    fun deleteMessage(messageId: UUID) {
        viewModelScope.launch {
            messageService.deleteMessage(messageId)
//            val isDeleted = messageService.deleteMessage(messageId)
//            if (isDeleted) {
//                loadMessages()
//            }
        }
    }

    fun deleteChat() {
        viewModelScope.launch {
            _deleteStatus.value = chatService.deleteChat(chatId)
        }
    }

    fun clearDeleteStatus() {
        _deleteStatus.value = null
    }

    fun startEditing(message: Message) {
        _editingMessage.value = message
    }

    fun editMessage(messageId: UUID, newContent: String) {
        viewModelScope.launch {
            messageService.editMessage(messageId, newContent)
//            val isEdited = messageService.editMessage(messageId, newContent)
//            if (isEdited) {
//                loadMessages()
//            }
        }
    }

    fun cancelEditing() {
        _editingMessage.value = null
    }

    // ИЗМЕНЕНИЕ 6: Выходим из P2P-сети при уничтожении ViewModel
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            chatService.leaveChatNetwork(chatId)
        }
    }
}