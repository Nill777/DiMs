package com.distributed_messenger.domain.services

import com.distributed_messenger.core.Chat
import com.distributed_messenger.core.UserHandshake
import com.distributed_messenger.logger.Logger
import com.distributed_messenger.logger.LoggingWrapper
import com.distributed_messenger.domain.iservices.IChatService
import com.distributed_messenger.data.irepositories.IChatRepository
import com.distributed_messenger.data.network.model.DataMessage
import com.distributed_messenger.data.network.transport.IP2PTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatService(private val chatRepository: IChatRepository,
                  private val p2pTransport: IP2PTransport
) : IChatService {
    private val loggingWrapper = LoggingWrapper(
        origin = this,
        logger = Logger,
        tag = "ChatService"
    )

    override suspend fun createChat(
        name: String,
        creatorId: UUID,
        isGroupChat: Boolean,
        companionId: UUID?
    ): UUID =
        loggingWrapper {
            val chat = Chat(
                id = UUID.randomUUID(),
                name = name,
                creatorId = creatorId,
                companionId = companionId,
                isGroupChat = isGroupChat
            )
            chatRepository.addChat(chat)
        }

    override suspend fun createChatById(
        chatId: UUID,
        name: String,
        creatorId: UUID,
        isGroupChat: Boolean,
        companionId: UUID?
    ): UUID =
        loggingWrapper {
            val chat = Chat(
                id = chatId,
                name = name,
                creatorId = creatorId,
                companionId = companionId,
                isGroupChat = isGroupChat
            )
            chatRepository.addChat(chat)
        }

    override suspend fun getChat(id: UUID): Chat? =
        loggingWrapper {
            chatRepository.getChat(id)
        }

    override suspend fun getUserChats(userId: UUID): List<Chat> =
        loggingWrapper {
            chatRepository.getChatsByUser(userId)
        }

    override suspend fun updateChat(id: UUID, name: String): Boolean =
        loggingWrapper {
            val chat = chatRepository.getChat(id) ?: return@loggingWrapper false
            val updatedChat = chat.copy(id = id, name = name)
            chatRepository.updateChat(updatedChat)
        }

    override suspend fun deleteChat(id: UUID): Boolean =
        loggingWrapper {
            chatRepository.deleteChat(id)
        }

    // Метод для присоединения к чату (вызывать при открытии экрана чата)
    override suspend fun joinChatNetwork(chatId: UUID) {
        p2pTransport.joinChat(chatId)
    }

    // Метод для выхода из чата (вызывать при закрытии экрана чата)
    override suspend fun leaveChatNetwork(chatId: UUID) {
        p2pTransport.leaveChat(chatId)
    }

    override suspend fun performHandshake(inviteId: UUID, handshake: UserHandshake): Boolean {
        // Преобразуем в DTO и отправляем
        val handshakeDto = DataMessage.Handshake(handshake.userId, handshake.username)
        // TODO: Нужен механизм ожидания установки P2P соединения перед отправкой
        // Это сложный момент. Простой вариант - небольшая задержка.
        kotlinx.coroutines.delay(3000) // Ждем 3 секунды в надежде, что P2P установится
        p2pTransport.sendMessageToChat(inviteId, handshakeDto)
        return true
    }

    override fun listenForHandshake(inviteId: UUID): Flow<UserHandshake> {
        return p2pTransport.incomingMessages
            .filter { (peerId, dataMessage) -> dataMessage is DataMessage.Handshake }
            .map { (_, dataMessage) ->
                val handshake = dataMessage as DataMessage.Handshake
                UserHandshake(handshake.userId, handshake.username)
            }
    }
}
