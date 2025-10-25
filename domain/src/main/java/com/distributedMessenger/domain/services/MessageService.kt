package com.distributedMessenger.domain.services

import com.distributedMessenger.core.Message
import com.distributedMessenger.core.MessageHistory
import com.distributedMessenger.data.irepositories.IMessageHistoryRepository
import com.distributedMessenger.data.irepositories.IMessageRepository
import com.distributedMessenger.data.network.model.DataMessage
import com.distributedMessenger.data.network.transport.IP2PTransport
import com.distributedMessenger.domain.iservices.IMessageService
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.logger.LoggingWrapper
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

class MessageService(private val messageRepository: IMessageRepository,
                     private val messageHistoryRepository: IMessageHistoryRepository,
                     private val p2pTransport: IP2PTransport
) : IMessageService {
    private val loggingWrapper = LoggingWrapper(
    origin = this,
    logger = Logger,
    tag = "MessageService"
)
    override suspend fun sendMessage(senderId: UUID, chatId: UUID, content: String, fileId: UUID?): UUID =
        loggingWrapper {
            // Шаг 1: Создаем доменный объект сообщения с уникальным ID.
            // Этот объект будет использоваться и для БД, и для отправки по сети.
            val message = Message(
                id = UUID.randomUUID(),
                senderId = senderId,
                chatId = chatId,
                content = content,
                timestamp = Instant.now(),
                fileId = fileId
            )

            // Шаг 2: Говорим репозиторию сохранить сообщение в локальную базу данных.
            // Теперь addMessage просто сохраняет и ничего не отправляет.
            val savedMessageId = messageRepository.addMessage(message)

            // Шаг 3: Создаем сетевую модель (DTO) на основе того же самого сообщения.
            // Используем savedMessageId == message.id, который мы сгенерировали на шаге 1.
            val chatMessage = DataMessage.ChatMessage(
                messageId = savedMessageId,
                originalSenderId = senderId,
                chatId = chatId,
                content = content,
                timestamp = message.timestamp.toEpochMilli()
            )

            // Шаг 4: Говорим транспортному уровню отправить сетевую модель всем пирам в чате.
            p2pTransport.sendMessageToChat(chatId, chatMessage)

            // Шаг 5: Возвращаем ID созданного сообщения, чтобы UI мог его использовать.
            savedMessageId
        }

    override suspend fun getMessage(id: UUID): Message? =
        loggingWrapper {
            messageRepository.getMessage(id)
        }

    override suspend fun getChatMessages(chatId: UUID): List<Message> =
        loggingWrapper {
            messageRepository.getMessagesByChat(chatId)
        }

    override suspend fun getChatMessagesFlow(chatId: UUID): Flow<List<Message>> =
        loggingWrapper {
            messageRepository.getMessagesByChatFlow(chatId)
        }

    override suspend fun getLastMessage(chatId: UUID): Message? =
        loggingWrapper {
            messageRepository.getLastMessageByChat(chatId)
        }

    override suspend fun editMessage(id: UUID, newContent: String): Boolean =
        loggingWrapper {
            val message = messageRepository.getMessage(id) ?: return@loggingWrapper false

            // Сохранить старую версию в историю
            messageHistoryRepository.addMessageHistory(
                MessageHistory(
                    historyId = UUID.randomUUID(),
                    messageId = id,
                    editedContent = message.content,
                    editTimestamp = Instant.now()
                )
            )

            val updatedMessage = message.copy(id = id, content = newContent)
            messageRepository.updateMessage(updatedMessage)
        }

    override suspend fun getMessageHistory(messageId: UUID): List<MessageHistory> =
        loggingWrapper {
            messageHistoryRepository.getHistoryForMessage(messageId)
        }

    override suspend fun deleteMessage(id: UUID): Boolean =
        loggingWrapper {
            messageRepository.deleteMessage(id)
        }

    override suspend fun requestMessagesSync(chatId: UUID) {
        // 1. Находим последнее сообщение в нашей локальной БД.
        val lastMessage = messageRepository.getLastMessageByChat(chatId)
        val lastTimestamp = lastMessage?.timestamp?.toEpochMilli() ?: 0L

        // 2. Создаем и отправляем запрос всем в чате.
        val request = DataMessage.SyncRequest(
            chatId = chatId,
            fromTimestamp = lastTimestamp
        )
        p2pTransport.sendMessageToChat(chatId, request)
    }
}
