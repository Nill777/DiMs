package com.distributedMessenger.data.repositories

import com.distributedMessenger.core.Message
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.logger.LoggingWrapper
import com.distributedMessenger.data.irepositories.IMessageRepository
import com.distributedMessenger.data.local.dao.MessageDao
import com.distributedMessenger.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class MessageRepository(private val messageDao: MessageDao) : IMessageRepository {
    private val loggingWrapper = LoggingWrapper(
        origin = this,
        logger = Logger,
        tag = "MessageRepository"
    )

    override suspend fun getMessage(id: UUID): Message? =
        loggingWrapper {
            messageDao.getMessageById(id)?.toDomain()
        }

    override suspend fun getAllMessages(): List<Message> =
        loggingWrapper {
            messageDao.getAllMessages().map { it.toDomain() }
        }

    override suspend fun getMessagesAfter(chatId: UUID, timestamp: Instant): List<Message> =
        loggingWrapper {
            messageDao.getMessagesAfter(chatId, timestamp).map { it.toDomain() }
        }

    override suspend fun getMessagesByChat(chatId: UUID): List<Message> =
        loggingWrapper {
            messageDao.getMessagesByChatId(chatId).map { it.toDomain() }
        }

    override fun getMessagesByChatFlow(chatId: UUID): Flow<List<Message>> {
        // нет loggingWrapper, так как это холодный Flow
        return messageDao.getMessagesByChatIdFlow(chatId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getLastMessageByChat(chatId: UUID): Message? =
        loggingWrapper {
            messageDao.getLastMessageByChatId(chatId)?.toDomain()
        }

    override suspend fun addMessage(message: Message): UUID =
        loggingWrapper {
            val rowId = messageDao.insertMessage(message.toEntity())
            check(rowId != -1L) { "Failed to insert message" }
            message.id
        }

    override suspend fun addMessageFromNetwork(message: Message): UUID =
        loggingWrapper {
            val rowId = messageDao.insertMessage(message.toEntity())
            check(rowId != -1L) { "Failed to insert message from network" }
            message.id
        }

    override suspend fun updateMessage(message: Message): Boolean =
        loggingWrapper {
            messageDao.updateMessage(message.toEntity()) > 0
        }

    override suspend fun deleteMessage(id: UUID): Boolean =
        loggingWrapper {
            messageDao.deleteMessage(id) > 0
        }

    private fun Message.toEntity(): MessageEntity {
        return MessageEntity(
            messageId = id,
            senderId = senderId,
            chatId = chatId,
            content = content,
            fileId = fileId,
            timestamp = timestamp
        )
    }

    private fun MessageEntity.toDomain(): Message {
        return Message(
            id = messageId,
            senderId = senderId,
            chatId = chatId,
            content = content,
            fileId = fileId,
            timestamp = timestamp
        )
    }
}
