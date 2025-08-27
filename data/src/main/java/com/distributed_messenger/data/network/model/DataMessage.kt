package com.distributed_messenger.data.network.model

import java.util.UUID

/**
 * Сообщения с полезной нагрузкой (чаты, синхронизация и т.д.).
 */
sealed class DataMessage {
    /**
     * Обычное сообщение в чате.
     */
    data class ChatMessage(
        val messageId: UUID,
        val originalSenderId: UUID,
        val chatId: UUID,
        val content: String,
        val timestamp: Long
    ) : DataMessage()

    /**
     * Запрос на синхронизацию истории.
     * Отправляется клиентом, когда он подключается к чату.
     */
    data class SyncRequest(
        val chatId: UUID,
        val fromTimestamp: Long // "Пришлите все сообщения, начиная с этой временной метки"
    ) : DataMessage()

    /**
     * Ответ на запрос синхронизации, содержащий пачку старых сообщений.
     */
    data class SyncResponse(
        val messages: List<ChatMessage>
    ) : DataMessage()

    /**
     * Рукопожатие
     */
    data class Handshake(
        val userId: UUID,
        val username: String
    ) : DataMessage()
}