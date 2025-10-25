package com.distributedMessenger.data.network.transport

import com.distributedMessenger.data.network.PeerId
import com.distributedMessenger.data.network.model.DataMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID

/**
 * Интерфейс для высокоуровневого управления P2P-транспортом.
 * Абстрагирует сложность WebRTC и сигнализации.
 */
interface IP2PTransport {

    /**
     * Горячий Flow, который эмитит все входящие сообщения (DataMessage) от других пиров.
     * Пара содержит ID пира-отправителя и само сообщение.
     */
    val incomingMessages: SharedFlow<Pair<PeerId, DataMessage>>
    /**
     * Flow, который эмитит inviteId, когда P2P канал для рукопожатия готов к передаче данных.
     */
    val handshakeChannelReady: Flow<UUID>

    /**
     * Отправить сообщение по временному каналу рукопожатия.
     * @param inviteId ID временной комнаты.
     * @param message Сообщение для отправки (Handshake).
     */
    fun sendHandshakeMessage(inviteId: UUID, message: DataMessage)

    /**
     * Завершает рукопожатие, закрывая временное соединение.
     * @param inviteId ID временной комнаты.
     */
    fun finalizeHandshake(inviteId: UUID)

    /**
     * Присоединиться к P2P-сети для конкретного чата.
     * Инициирует процесс обнаружения пиров и установки соединений.
     * @param chatId ID чата для присоединения.
     */
    suspend fun joinChat(chatId: UUID)

    /**
     * Инициирует запрос на добавление контакта.
     * Эта сторона будет создавать Offer.
     * @param inviteId Временный ID комнаты для рукопожатия.
     */
    suspend fun initiateHandshake(inviteId: UUID)

    /**
     * Принимает запрос на добавление контакта.
     * Эта сторона будет ждать Offer и создавать Answer.
     * @param inviteId Временный ID комнаты для рукопожатия.
     */
    suspend fun acceptHandshake(inviteId: UUID)

    /**
     * Отправить сообщение всем участникам чата.
     * @param chatId ID чата, в который отправляется сообщение.
     * @param message Сообщение для отправки (например, ChatMessage, SyncRequest).
     */
    fun sendMessageToChat(chatId: UUID, message: DataMessage)

    /**
     * Отправить сообщение только одному пиру
     * @param chatId ID чата, в который отправляется сообщение.
     * @param targetPeerId ID целевого пира
     * @param message Сообщение для отправки (например, ChatMessage, SyncRequest).
     */
    fun sendMessageToPeer(chatId: UUID, targetPeerId: PeerId, message: DataMessage)

    /**
     * Покинуть P2P-сеть для конкретного чата.
     * Закрывает все активные соединения для этого чата.
     * @param chatId ID чата для выхода.
     */
    suspend fun leaveChat(chatId: UUID)

    /**
     * Полностью остановить и очистить все ресурсы P2P-транспорта.
     * Вызывается при закрытии приложения.
     */
    suspend fun shutdown()
}
