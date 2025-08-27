package com.distributed_messenger.data.network.transport

import com.distributed_messenger.data.network.PeerId
import com.distributed_messenger.data.network.model.DataMessage
import com.distributed_messenger.data.network.model.SignalMessage
import com.distributed_messenger.data.network.signaling.ISignalingClient
import com.distributed_messenger.data.network.webRTC.PeerConnectionManager
import com.distributed_messenger.data.network.webRTC.WebRTCManager
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class P2PTransportManager(
    private val webRTCManager: WebRTCManager,
    private val signalingClient: ISignalingClient,
    private val gson: Gson
) : IP2PTransport {
    private val tag = "P2PTransportManager"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val myId: PeerId = UUID.randomUUID().toString().also {
        Logger.log(tag, "Manager initialized with myId: $it")
    }

    // Хранит активные соединения для каждого чата. Ключ - ChatID, Значение - карта PeerId -> PeerConnectionManager
    private val activeConnections = ConcurrentHashMap<UUID, ConcurrentHashMap<PeerId, PeerConnectionManager>>()

    private val _incomingMessages = MutableSharedFlow<Pair<PeerId, DataMessage>>()
    override val incomingMessages: SharedFlow<Pair<PeerId, DataMessage>> = _incomingMessages.asSharedFlow()

    private fun createAndSetupPcManager(chatId: UUID, peerId: PeerId, isInitiator: Boolean): PeerConnectionManager {
        Logger.log(tag, "createAndSetupPcManager Creating and setting up PeerConnectionManager for '$peerId' in chat '$chatId'")
        val pcManager = PeerConnectionManager(webRTCManager, peerId, isInitiator)

        // Перенаправляем исходящие сигналы в SignalingClient
        // 2. Настройка слушателя для исходящих сигналов.
        pcManager.outgoingSignal
            .onEach { signal ->
                Logger.log(tag, "Forwarding outgoing '${signal::class.simpleName}' to signaling client (for peer '$peerId')", LogLevel.DEBUG)
                // Этот блок кода НЕ выполняется сейчас.
                // Он будет выполнен ПОЗЖЕ, когда Flow `outgoingSignal` что-то выпустит.
                signalingClient.sendSignal(chatId, myId, signal)
            }
            .launchIn(scope)

        // Перенаправляем входящие данные в общий поток
        // 3. Настройка слушателя для входящих данных.
        pcManager.incomingData
            .onEach { dataJson ->
                try {
                    val dataMessage = gson.fromJson(dataJson, DataMessage::class.java)
                    Logger.log(tag, "Received data message '${dataMessage::class.simpleName}' from '$peerId' in chat '$chatId'")
                    _incomingMessages.emit(Pair(peerId, dataMessage))
                } catch (e: Exception) {
                    Logger.log(tag, "Failed to parse incoming data from '$peerId'. Data: $dataJson", LogLevel.ERROR, e)
                }
            }
            .launchIn(scope)

        return pcManager
    }

    override suspend fun joinChat(chatId: UUID) {
    /**    // Если уже в чате, ничего не делаем
        if (activeConnections.containsKey(chatId)) {
            Logger.log(tag, "joinChat Already joined chat '$chatId', ignoring.", LogLevel.WARN)
            return
        }
        Logger.log(tag, "Joining chat '$chatId'")
        val chatConnections = ConcurrentHashMap<PeerId, PeerConnectionManager>()
        activeConnections[chatId] = chatConnections

        scope.launch {
            signalingClient.joinRoom(chatId, myId)
//                .collect { (peerId, signalMessage) ->
//                    Logger.log(tag, "Received signal for peer '$peerId' in chat '$chatId'")
//                    // Получаем или создаем PeerConnectionManager для этого пира
//                    val pcManager = chatConnections.getOrPut(peerId) {
//                        // isInitiator = true, если наш ID "больше" ID пира.
//                        // Это простой способ гарантировать, что только один из двух создаст offer.
//                        val isInitiator = myId > peerId
//                        Logger.log(tag, "Peer '$peerId' is new for this chat. Creating connection. I am initiator: $isInitiator")
//                        createAndSetupPcManager(chatId, peerId, isInitiator)
//                    }
//                    pcManager.handleIncomingSignal(signalMessage)
//                }
                .onEach { peersMap ->
                    Logger.log(tag, "Received room update with peers: ${peersMap.keys}")

                    // Для каждого пира из карты...
                    for ((peerId, signalData) in peersMap) {
                        // 1. Получаем или создаем для него PeerConnectionManager
                        val pcManager = chatConnections.getOrPut(peerId) {
                            val isInitiator = myId > peerId
                            Logger.log(tag, "Peer '$peerId' is new. Creating connection. I am initiator: $isInitiator")
                            createAndSetupPcManager(chatId, peerId, isInitiator)
                        }

                        // 2. Если для этого пира есть сигнальные данные, обрабатываем их
                        if (signalData != null) {
                            try {
                                val signalMessage = gson.fromJson(signalData, SignalMessage::class.java)
                                pcManager.handleIncomingSignal(signalMessage)
                            } catch (e: Exception) { /* ... */ }
                        }
                    }

                    // TODO: Добавить логику удаления пиров, которых больше нет в peersMap
                }
        }
    **/
        // Мы используем withContext, чтобы быть уверенными, что работа с ConcurrentHashMap
        // и запуск Flow происходят в правильном контексте.
        withContext(Dispatchers.Default) {
            if (activeConnections.containsKey(chatId)) {
                Logger.log(tag, "joinChat: Already joined chat '$chatId', ignoring.", LogLevel.WARN)
                return@withContext
            }
            Logger.log(tag, "Joining chat '$chatId'")
            val chatConnections = ConcurrentHashMap<PeerId, PeerConnectionManager>()
            activeConnections[chatId] = chatConnections

            // Запускаем слушатель в фоновом режиме
            signalingClient.joinRoom(chatId, myId)
                // ИСПРАВЛЕНО: Теперь мы получаем `peersMap`
                .onEach { peersMap ->
                    Logger.log(tag, "Received room update with peers: ${peersMap.keys}")

                    // Для каждого пира из карты...
                    for ((peerId, signalData) in peersMap) {
                        // 1. Получаем или создаем для него PeerConnectionManager.
                        // Эта логика гарантирует, что createAndSetupPcManager вызовется
                        // только один раз для каждого нового пира.
                        val pcManager = chatConnections.getOrPut(peerId) {
                            val isInitiator = myId > peerId
                            Logger.log(tag, "Peer '$peerId' is new. Creating connection. I am initiator: $isInitiator")
                            createAndSetupPcManager(chatId, peerId, isInitiator)
                        }

                        // 2. Если для этого пира есть сигнальные данные (не `true` и не `null`), обрабатываем их.
                        if (signalData != null) {
                            try {
                                val signalMessage = gson.fromJson(signalData, SignalMessage::class.java)
                                // Передаем сигнал в соответствующий PeerConnectionManager
                                pcManager.handleIncomingSignal(signalMessage)
                            } catch (e: Exception) {
                                // Это может случиться, если данные еще не являются валидным JSON, игнорируем
                                Logger.log(tag, "Could not parse signal from peer '$peerId', it might be a presence signal. Data: $signalData", LogLevel.DEBUG)
                            }
                        }
                    }

                    // (Опционально, но хорошая практика) Логика удаления "отвалившихся" пиров
                    val disappearedPeers = chatConnections.keys - peersMap.keys
                    if (disappearedPeers.isNotEmpty()) {
                        Logger.log(tag, "Peers disappeared: $disappearedPeers. Closing their connections.")
                        for (peerId in disappearedPeers) {
                            chatConnections.remove(peerId)?.close()
                        }
                    }
                }
                .launchIn(scope) // launchIn запускает collect в scope этого менеджера
        }
    }

    override fun sendMessageToChat(chatId: UUID, message: DataMessage) {
        val connections = activeConnections[chatId]
        Logger.log(tag, "sendMessageToChat Broadcasting message '${message::class.simpleName}' to ${connections?.size ?: 0} peers in chat '$chatId'")
        val messageJson = gson.toJson(message)
        activeConnections[chatId]?.values?.forEach { pcManager ->
            pcManager.sendMessage(messageJson)
        }
    }

    override fun sendMessageToPeer(chatId: UUID, targetPeerId: PeerId, message: DataMessage) {
        Logger.log(tag, "sendMessageToPeer Sending message '${message::class.simpleName}' to peer '$targetPeerId' in chat '$chatId'")
        val messageJson = gson.toJson(message)
        // Находим конкретное соединение с нужным пиром и отправляем только ему
        activeConnections[chatId]?.get(targetPeerId)?.sendMessage(messageJson)
    }

    override suspend fun leaveChat(chatId: UUID) {
        Logger.log(tag, "leaveChat Leaving chat '$chatId'")
        activeConnections.remove(chatId)?.values?.forEach { it.close() }
    }

    override suspend fun shutdown() {
        withContext(Dispatchers.Default) {
            Logger.log(tag, "shutdown Shutting down P2P Transport Manager. Closing all connections.")
            // Мы можем даже использовать coroutineScope, чтобы дождаться завершения всех leaveChat
            coroutineScope {
                activeConnections.keys.forEach { chatId ->
                    launch { // Запускаем закрытие каждого чата параллельно
                        leaveChat(chatId)
                    }
                }
            }

            Logger.log(tag, "All chat connections are closed. Cancelling scope and releasing WebRTC.")
            scope.cancel()
            webRTCManager.release()
        }
    }
}