package com.distributed_messenger.data.network.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Сообщения с полезной нагрузкой (чаты, синхронизация и т.д.).
 */
sealed class DataMessage {
    abstract val type: String
    /**
     * Обычное сообщение в чате.
     */
    data class ChatMessage(
        @SerializedName("type") override val type: String = "CHAT_MESSAGE",
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
        @SerializedName("type") override val type: String = "SYNC_REQUEST",
        val chatId: UUID,
        val fromTimestamp: Long // "Пришлите все сообщения, начиная с этой временной метки"
    ) : DataMessage()

    /**
     * Ответ на запрос синхронизации, содержащий пачку старых сообщений.
     */
    data class SyncResponse(
        @SerializedName("type") override val type: String = "SYNC_RESPONSE",
        val messages: List<ChatMessage>
    ) : DataMessage()

    /**
     * Рукопожатие
     */
    data class Handshake(
        @SerializedName("type") override val type: String = "HANDSHAKE",
        val userId: UUID,
        val username: String
    ) : DataMessage()
}