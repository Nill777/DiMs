package com.distributedMessenger.data.repositories

import com.distributedMessenger.core.MessageHistory
import com.distributedMessenger.data.local.dao.MessageHistoryDao
import com.distributedMessenger.data.local.entities.MessageHistoryEntity
import com.distributedMessenger.data.irepositories.IMessageHistoryRepository
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.logger.LoggingWrapper
import java.util.UUID

class MessageHistoryRepository(private val messageHistoryDao: MessageHistoryDao) :
    IMessageHistoryRepository {
    private val loggingWrapper = LoggingWrapper(
        origin = this,
        logger = Logger,
        tag = "MessageHistoryRepository"
    )

    override suspend fun addMessageHistory(messageHistory: MessageHistory): UUID =
        loggingWrapper {
            val entity = messageHistory.toEntity()
            val rowId = messageHistoryDao.insertMessageHistory(entity)
            check(rowId != -1L) { "Failed to insert message history" }
            messageHistory.historyId
        }

    override suspend fun getHistoryForMessage(messageId: UUID): List<MessageHistory> =
        loggingWrapper {
            messageHistoryDao.getHistoryForMessage(messageId)
                .map { it.toDomain() }
        }

    override suspend fun getAllMessageHistory(): List<MessageHistory> =
        loggingWrapper {
            messageHistoryDao.getAllMessageHistory()
                .map { it.toDomain() }
        }

    private fun MessageHistory.toEntity(): MessageHistoryEntity =
        MessageHistoryEntity(
            historyId = historyId,
            messageId = messageId,
            editedContent = editedContent,
            editTimestamp = editTimestamp
        )

    private fun MessageHistoryEntity.toDomain(): MessageHistory =
        MessageHistory(
            historyId = historyId,
            messageId = messageId,
            editedContent = editedContent,
            editTimestamp = editTimestamp
        )
}
