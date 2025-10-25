package com.distributedMessenger.data.repositories

import com.distributedMessenger.core.Chat
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.logger.LoggingWrapper
import com.distributedMessenger.data.irepositories.IChatRepository
import com.distributedMessenger.data.local.dao.ChatDao
import com.distributedMessenger.data.local.entities.ChatEntity
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) : IChatRepository {
    private val loggingWrapper = LoggingWrapper(
        origin = this,
        logger = Logger,
        tag = "ChatRepository"
    )

    override suspend fun getChat(id: UUID): Chat? =
        loggingWrapper {
            chatDao.getChatById(id)?.toDomain()
        }

    override suspend fun getAllChats(): List<Chat> =
        loggingWrapper {
            chatDao.getAllChats().map { it.toDomain() }
        }

    override suspend fun getChatsByUser(userId: UUID): List<Chat> =
        loggingWrapper {
            chatDao.getChatsByUserId(userId).map { it.toDomain() }
        }

    override suspend fun addChat(chat: Chat): UUID =
        loggingWrapper {
            val rowId = chatDao.insertChat(chat.toEntity())
            check(rowId != -1L) { "Failed to insert chat" }
            chat.id
        }

    override suspend fun updateChat(chat: Chat): Boolean =
        loggingWrapper {
            chatDao.updateChat(chat.toEntity()) > 0
        }

    override suspend fun deleteChat(id: UUID): Boolean =
        loggingWrapper {
            chatDao.deleteChat(id) > 0
        }
    private fun Chat.toEntity(): ChatEntity {
        return ChatEntity(
            chatId = id,
            chatName = name,
            userId = creatorId,
            companionId = companionId,
            isGroupChat = isGroupChat
        )
    }

    private fun ChatEntity.toDomain(): Chat {
        return Chat(
            id = chatId,
            name = chatName,
            creatorId = userId,
            companionId = companionId,
            isGroupChat = isGroupChat
        )
    }
}
