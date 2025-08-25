package com.distributed_messenger.domain.services

import com.distributed_messenger.core.Chat
import com.distributed_messenger.logger.Logger
import com.distributed_messenger.logger.LoggingWrapper
import com.distributed_messenger.domain.iservices.IChatService
import com.distributed_messenger.data.irepositories.IChatRepository
import com.distributed_messenger.data.network.transport.IP2PTransport
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
}
