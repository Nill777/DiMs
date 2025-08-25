package com.distributed_messenger.data.network.signaling

import com.distributed_messenger.data.network.PeerId
import com.distributed_messenger.data.network.model.SignalMessage
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Интерфейс для сигнального сервера.
 * Отвечает за обмен метаданными (Offer, Answer, ICE) для установки WebRTC-соединения.
 */
interface ISignalingClient {

    /**
     * Присоединиться к "комнате" на сигнальном сервере и начать слушать сигналы от других.
     * @param chatId Уникальный идентификатор комнаты (в нашем случае ID чата).
     * @param myId Наш уникальный ID в этой комнате.
     * @return Flow, который эмитит пару (ID пира-отправителя, Сигнальное сообщение).
     */
    fun joinRoom(chatId: UUID, myId: PeerId): Flow<Pair<PeerId, SignalMessage>>

    /**
     * Отправить сигнальное сообщение конкретному пиру в комнате.
     * @param chatId ID комнаты.
     * @param myId Наш ID (для идентификации отправителя).
     * @param targetId ID пира-получателя.
     * @param signalMessage Сигнальное сообщение для отправки.
     */
    fun sendSignal(chatId: UUID, myId: PeerId, targetId: PeerId, signalMessage: SignalMessage)
}