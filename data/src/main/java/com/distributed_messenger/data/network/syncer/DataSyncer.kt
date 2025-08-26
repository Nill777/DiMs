package com.distributed_messenger.data.network.syncer

import com.distributed_messenger.core.Message
import com.distributed_messenger.data.irepositories.IMessageRepository
import com.distributed_messenger.data.network.PeerId
import com.distributed_messenger.data.network.model.DataMessage
import com.distributed_messenger.data.network.transport.IP2PTransport
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant

class DataSyncer(
    private val p2pTransport: IP2PTransport,
    private val messageRepository: IMessageRepository
) {
    private val tag = "DataSyncer"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        Logger.log(tag, "start Starting and listening for incoming messages.")
        p2pTransport.incomingMessages
            .onEach { (peerId, dataMessage) ->
                val messageType = dataMessage::class.simpleName
                Logger.log(tag, "Received '$messageType' from peer '$peerId'")
                when (dataMessage) {
                    is DataMessage.ChatMessage -> handleChatMessage(dataMessage)
                    is DataMessage.SyncRequest -> handleSyncRequest(peerId, dataMessage)
                    is DataMessage.SyncResponse -> handleSyncResponse(dataMessage)
                }
            }
            .launchIn(scope)
    }

    private fun handleChatMessage(msg: DataMessage.ChatMessage) {
        scope.launch {
            Logger.log(tag, "handleChatMessage Handling ChatMessage with id '${msg.messageId}'", LogLevel.DEBUG)
            // Проверяем, нет ли у нас уже такого сообщения
            if (messageRepository.getMessage(msg.messageId) == null) {
                Logger.log(tag, "Message '${msg.messageId}' is new. Adding to repository.")
                val domainMessage = Message(
                    id = msg.messageId,
                    senderId = msg.originalSenderId,
                    chatId = msg.chatId,
                    content = msg.content,
                    fileId = null, // TODO: Добавить передачу файлов
                    timestamp = Instant.ofEpochMilli(msg.timestamp)
                )
                // Важно: Вызываем метод, который просто вставляет в БД, без повторной отправки в сеть!
                // Вам нужно будет добавить такой метод в репозиторий.
                messageRepository.addMessageFromNetwork(domainMessage) // ПРЕДПОЛАГАЕТСЯ НОВЫЙ МЕТОД
            } else {
                Logger.log(tag, "handleChatMessage Message '${msg.messageId}' already exists. Ignoring.", LogLevel.DEBUG)
            }
        }
    }

    /**
     * Обрабатывает входящий запрос на синхронизацию от другого пира.
     * @param requesterId ID пира, который просит данные.
     * @param request Сам запрос, содержащий временную метку.
     */
    private fun handleSyncRequest(requesterId: PeerId, request: DataMessage.SyncRequest) {
        scope.launch {
            Logger.log(tag, "handleSyncRequest Handling SyncRequest from '$requesterId' for chat '${request.chatId}'")
            // 1. Получаем из БД все сообщения для нужного чата после нужной временной метки.
            val messagesToSend = messageRepository.getMessagesAfter(
                chatId = request.chatId,
                timestamp = Instant.ofEpochMilli(request.fromTimestamp)
            )

            // 2. Если есть что отправить, формируем и отправляем ответ.
            if (messagesToSend.isNotEmpty()) {
                Logger.log(tag, "Found ${messagesToSend.size} messages to send back to '$requesterId'")
                val response = DataMessage.SyncResponse(
                    messages = messagesToSend.map {
                        // Конвертируем доменную модель в сетевую DTO
                        DataMessage.ChatMessage(
                            messageId = it.id,
                            originalSenderId = it.senderId,
                            chatId = it.chatId,
                            content = it.content,
                            timestamp = it.timestamp.toEpochMilli()
                        )
                    }
                )
                // Отправляем ответ только тому, кто спросил
                p2pTransport.sendMessageToPeer(request.chatId, requesterId, response)
            } else {
                Logger.log(tag, "handleSyncRequest No new messages found for sync request from '$requesterId'", LogLevel.DEBUG)
            }
        }
    }

    /**
     * Обрабатывает ответ на наш запрос о синхронизации.
     * @param response Пакет с недостающими сообщениями.
     */
    private fun handleSyncResponse(response: DataMessage.SyncResponse) {
        scope.launch {
            Logger.log(tag, "handleSyncResponse Handling SyncResponse with ${response.messages.size} messages.")
            // Просто перебираем все сообщения и добавляем их, если у нас таких нет.
            // Логика идентична handleChatMessage.
            for (msg in response.messages) {
                handleChatMessage(msg)
            }
        }
    }
}