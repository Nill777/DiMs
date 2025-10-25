package com.distributedMessenger.data.irepositories

import com.distributedMessenger.core.Message
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

interface IMessageRepository {
    suspend fun getMessage(id: UUID): Message?
    suspend fun getAllMessages(): List<Message>
    suspend fun getMessagesAfter(chatId: UUID, timestamp: Instant): List<Message>
    suspend fun getMessagesByChat(chatId: UUID): List<Message>
    fun getMessagesByChatFlow(chatId: UUID): Flow<List<Message>>
    suspend fun getLastMessageByChat(chatId: UUID): Message?
    suspend fun addMessage(message: Message): UUID
    suspend fun addMessageFromNetwork(message: Message): UUID
    suspend fun updateMessage(message: Message): Boolean
    suspend fun deleteMessage(id: UUID): Boolean
}
