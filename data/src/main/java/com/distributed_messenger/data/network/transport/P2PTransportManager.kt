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
    private val myId: PeerId = UUID.randomUUID().toString().also {  // TODO протащить между слоями
        Logger.log(tag, "Manager initialized with myId: $it")
    }

//    // Ключ - inviteId, Значение - единственный PeerConnectionManager для этой сессии
//    private val handshakeConnections = ConcurrentHashMap<UUID, PeerConnectionManager>()

    // Хранит активные соединения для каждого чата. Ключ - ChatID, Значение - карта PeerId -> PeerConnectionManager
    private val activeConnections = ConcurrentHashMap<UUID, ConcurrentHashMap<PeerId, PeerConnectionManager>>()

    // Ключ - ChatID, Значение - карта PeerId -> Хэш последнего обработанного сигнала
    private val processedSignals = ConcurrentHashMap<UUID, ConcurrentHashMap<PeerId, Int>>()

    private val _incomingMessages = MutableSharedFlow<Pair<PeerId, DataMessage>>()
    override val incomingMessages: SharedFlow<Pair<PeerId, DataMessage>> =
        _incomingMessages.asSharedFlow()

    private val _handshakeChannelReady = MutableSharedFlow<UUID>()
    override val handshakeChannelReady: Flow<UUID> = _handshakeChannelReady.asSharedFlow()

    private val handshakeConnections = ConcurrentHashMap<UUID, PeerConnectionManager>()

    override fun sendHandshakeMessage(inviteId: UUID, message: DataMessage) {
        Logger.log(tag, "sendHandshakeMessage")
        val connection = handshakeConnections[inviteId]
        if (connection == null) {
            Logger.log(tag, "sendHandshakeMessage: No handshake connection found for '$inviteId'. Message not sent.", LogLevel.WARN)
            return
        }

        Logger.log(tag, "sendHandshakeMessage: Sending '${message::class.simpleName}' over handshake channel for '$inviteId'")
        val messageJson = gson.toJson(message)
        connection.sendMessage(messageJson)
    }

    override fun finalizeHandshake(inviteId: UUID) {
        Logger.log(tag, "finalizeHandshake: Finalizing and closing connection for '$inviteId'")
        handshakeConnections.remove(inviteId)?.close()
        processedSignals.remove(inviteId)
    }

    private fun createAndSetupPcManager(
        chatId: UUID,
        peerId: PeerId,
        isInitiator: Boolean
    ): PeerConnectionManager {
        Logger.log(
            tag,
            "createAndSetupPcManager Creating and setting up PeerConnectionManager for '$peerId' in chat '$chatId'"
        )
        val pcManager = PeerConnectionManager(webRTCManager, peerId, isInitiator)

        // Перенаправляем исходящие сигналы в SignalingClient
        // 2. Настройка слушателя для исходящих сигналов.
        pcManager.outgoingSignal
            .onEach { signal ->
                Logger.log(
                    tag,
                    "Forwarding outgoing '${signal::class.simpleName}' to signaling client (for peer '$peerId')",
                    LogLevel.DEBUG
                )
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
                    Logger.log(
                        tag,
                        "Received data message '${dataMessage::class.simpleName}' from '$peerId' in chat '$chatId'"
                    )
                    _incomingMessages.emit(Pair(peerId, dataMessage))
                } catch (e: Exception) {
                    Logger.log(
                        tag,
                        "Failed to parse incoming data from '$peerId'. Data: $dataJson",
                        LogLevel.ERROR,
                        e
                    )
                }
            }
            .launchIn(scope)

        return pcManager
    }

    private fun setupPcManager(
        pcManager: PeerConnectionManager,
        chatId: UUID,
        peerId: PeerId,
        isInitiator: Boolean
    ): PeerConnectionManager {
        Logger.log(
            tag,
            "setupPcManager Creating and setting up PeerConnectionManager for '$peerId' in chat '$chatId'"
        )

        // Перенаправляем исходящие сигналы в SignalingClient
        // 2. Настройка слушателя для исходящих сигналов.
        pcManager.outgoingSignal
            .onEach { signal ->
                Logger.log(
                    tag,
                    "Forwarding outgoing '${signal::class.simpleName}' to signaling client (for peer '$peerId')",
                    LogLevel.DEBUG
                )
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
                    Logger.log(
                        tag,
                        "Received data message '${dataMessage::class.simpleName}' from '$peerId' in chat '$chatId'"
                    )
                    _incomingMessages.emit(Pair(peerId, dataMessage))
                } catch (e: Exception) {
                    Logger.log(
                        tag,
                        "Failed to parse incoming data from '$peerId'. Data: $dataJson",
                        LogLevel.ERROR,
                        e
                    )
                }
            }
            .launchIn(scope)

        return pcManager
    }

//    private suspend fun manageHandshake(inviteId: UUID, isInitiator: Boolean) {
//        withContext(Dispatchers.Default) {
//            if (activeConnections.containsKey(inviteId)) {
//                Logger.log(tag, "manageHandshake: Already managing handshake for '$inviteId'.", LogLevel.WARN)
//                return@withContext
//            }
//            Logger.log(tag, "manageHandshake: Starting for room '$inviteId'. Is initiator: $isInitiator")
//            val chatConnections = ConcurrentHashMap<PeerId, PeerConnectionManager>()
//            activeConnections[inviteId] = chatConnections
//
//            // Инициализируем хранилище для этой комнаты
//            processedSignals[inviteId] = ConcurrentHashMap()
//
//            val pcManager = PeerConnectionManager(webRTCManager, myId, isInitiator)
//            if (isInitiator) {
//                // 3. ПОСЛЕДОВАТЕЛЬНО: Ждем, пока Offer будет создан.
//                val offer = pcManager.createOffer()
//                Logger.log(tag, "Offer generated, publishing to Firebase...")
//
//                // 4. Публикуем Offer в Firebase.
//                signalingClient.sendSignal(inviteId, myId, offer)
//            }
//            // 1. Присоединяемся к комнате и начинаем слушать.
//            signalingClient.joinRoom(inviteId, myId)
//                .onEach { peersMap ->
//                    Logger.log(tag, "Handshake room update. Peers: ${peersMap.keys}")
//                    // 2. РЕАГИРУЕМ на появление пира.
//                    for ((peerId, signalData) in peersMap) {
//                        // --- КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: Проверка на дубликаты ---
//                        val currentSignalHash = signalData?.hashCode() ?: 0
//                        val lastProcessedHash = processedSignals[inviteId]?.get(peerId)
//
//                        // Обрабатываем сигнал, только если он новый
//                        if (currentSignalHash != lastProcessedHash) {
//                            chatConnections.getOrPut(peerId) {
//                                Logger.log(
//                                    tag,
//                                    "New peer '$peerId' detected in handshake room. Creating connection."
//                                )
//                                // 3. Создаем менеджер для пира С ПРАВИЛЬНОЙ РОЛЬЮ.
//                                // Передаем фиксированную роль
//                                setupPcManager(pcManager, inviteId, peerId, isInitiator)
//                            }
//                            if (signalData != null) {
//                                try {
//                                    val signalMessage =
//                                        gson.fromJson(signalData, SignalMessage::class.java)
//                                    pcManager.handleIncomingSignal(signalMessage)
//                                    // Запоминаем, что мы обработали этот сигнал
//                                    processedSignals[inviteId]?.put(peerId, currentSignalHash)
//                                } catch (e: Exception) {
//                                    Logger.log(tag, "manageHandshake signalData == null", LogLevel.ERROR, e)
//                                }
//                            }
//                        }
//                    }
//                }.launchIn(scope)
//        }
//    }
//
//    // Новые публичные методы
//    override suspend fun initiateHandshake(inviteId: UUID) {
//        manageHandshake(inviteId, true)
//    }
//
//    override suspend fun acceptHandshake(inviteId: UUID) {
//        manageHandshake(inviteId, false)
//    }

    private suspend fun manageHandshake(inviteId: UUID, isInitiator: Boolean) {
        withContext(Dispatchers.Default) {
            // Проверяем, не идет ли уже рукопожатие для этого ID
            if (handshakeConnections.containsKey(inviteId)) {
                Logger.log(tag, "manageHandshake: Already managing handshake for '$inviteId'.", LogLevel.WARN)
                return@withContext
            }
            Logger.log(tag, "manageHandshake: Starting for room '$inviteId'. Is initiator: $isInitiator")

            // 1. СОЗДАЕМ ЕДИНСТВЕННЫЙ PeerConnectionManager ДЛЯ ЭТОГО РУКОПОЖАТИЯ.
            // PeerId здесь не так важен, это временное соединение.
            val pcManager = PeerConnectionManager(webRTCManager, "handshake_peer", isInitiator)
            handshakeConnections[inviteId] = pcManager

            // --- СЛУШАЕМ СОБЫТИЕ ОТ PeerConnectionManager ---
            pcManager.dataChannelOpen
                .onEach { isOpen ->
                    if (isOpen) {
                        Logger.log(tag, "manageHandshake dataChannelOpen: Handshake DataChannel for '$inviteId' is OPEN. Notifying listeners.")
                        _handshakeChannelReady.emit(inviteId)
                    }
                }
                .launchIn(scope)

            // 2. НАСТРАИВАЕМ СЛУШАТЕЛЕЙ ДЛЯ ЭТОГО pcManager.
            // Перенаправляем исходящие сигналы (Offer, Answer, IceCandidates) в Firebase.
            pcManager.outgoingSignal
                .onEach { signal ->
                    val signalType = signal::class.simpleName
                    Logger.log(tag, "Handshake: Forwarding outgoing '$signalType' to signaling client for room '$inviteId'")
                    signalingClient.sendSignal(inviteId, myId, signal)
                }
                .launchIn(scope)

            // Перенаправляем входящие P2P-сообщения (уже после установки соединения) в общий поток.
            pcManager.incomingData
                .onEach { dataJson ->
                    try {
                        val dataMessage = gson.fromJson(dataJson, DataMessage::class.java)
                        Logger.log(tag, "Handshake: Received P2P data message '${dataMessage::class.simpleName}'")
                        _incomingMessages.emit(Pair("handshake_peer", dataMessage))
                    } catch (e: Exception) {
                        Logger.log(tag, "Handshake: Failed to parse incoming data: $dataJson", LogLevel.ERROR, e)
                    }
                }
                .launchIn(scope)


            // 3. НАЧИНАЕМ СЛУШАТЬ КОМНАТУ В FIREBASE.
            signalingClient.joinRoom(inviteId, myId)
                .onEach { peersMap ->
                    Logger.log(tag, "Handshake room update. Peers found: ${peersMap.keys}")
                    // Нам нужен только один пир, который не мы.
                    val otherPeer = peersMap.entries.firstOrNull()
                    if (otherPeer?.value != null) {
                        val peerId = otherPeer.key
                        val signalData = otherPeer.value
                        val currentSignalHash = signalData.hashCode()

                        // Простая проверка, чтобы не обрабатывать один и тот же сигнал дважды
                        val lastProcessedHash = processedSignals.getOrPut(inviteId) { ConcurrentHashMap() }[peerId]

                        if (currentSignalHash != lastProcessedHash) {
                            Logger.log(tag, "Handshake: New signal data from peer '$peerId'.")
                            try {
                                val signalMessage = gson.fromJson(signalData, SignalMessage::class.java)
                                // 4. ПЕРЕДАЕМ ВХОДЯЩИЙ СИГНАЛ В НАШ ЕДИНСТВЕННЫЙ pcManager.
                                pcManager.handleIncomingSignal(signalMessage)
                                processedSignals[inviteId]?.put(peerId, currentSignalHash)
                            } catch (e: Exception) {
                                Logger.log(tag, "Handshake: Could not parse signal from peer '$peerId'. Data: $signalData", LogLevel.WARN, e)
                            }
                        }
                    }
                }.launchIn(scope)

            // 5. ЕСЛИ МЫ ИНИЦИАТОР (Алиса), СОЗДАЕМ OFFER.
            // Это запустит всю цепочку: createOffer -> setLocalDescription -> onIceCandidate -> onIceGatheringChange.
            if (isInitiator) {
                Logger.log(tag, "Handshake: As initiator, creating Offer...")
                val offer = pcManager.createOffer()
                // Отправляем Offer напрямую через сигнальный клиент.
                Logger.log(tag, "Handshake: Offer created, sending directly via SignalingClient.")
                signalingClient.sendSignal(inviteId, myId, offer)
            }
        }
    }

    // Публичные методы остаются без изменений, но теперь вызывают новую реализацию
    override suspend fun initiateHandshake(inviteId: UUID) {
        manageHandshake(inviteId, true)
    }

    override suspend fun acceptHandshake(inviteId: UUID) {
        manageHandshake(inviteId, false)
    }

    override suspend fun joinChat(chatId: UUID) {
        if (activeConnections.containsKey(chatId)) {
            Logger.log(tag, "joinChat: Already joined chat '$chatId', ignoring.", LogLevel.WARN)
            return
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
                        Logger.log(
                            tag,
                            "Peer '$peerId' is new. Creating connection. I am initiator: $isInitiator"
                        )
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
                            Logger.log(
                                tag,
                                "Could not parse signal from peer '$peerId', it might be a presence signal. Data: $signalData",
                                LogLevel.DEBUG
                            )
                        }
                    }
                }

                // (Опционально, но хорошая практика) Логика удаления "отвалившихся" пиров
                val disappearedPeers = chatConnections.keys - peersMap.keys
                if (disappearedPeers.isNotEmpty()) {
                    Logger.log(
                        tag,
                        "Peers disappeared: $disappearedPeers. Closing their connections."
                    )
                    for (peerId in disappearedPeers) {
                        chatConnections.remove(peerId)?.close()
                    }
                }
            }
            .launchIn(scope) // launchIn запускает collect в scope этого менеджера
    }

    override fun sendMessageToChat(chatId: UUID, message: DataMessage) {
        val connections = activeConnections[chatId]
        Logger.log(
            tag,
            "sendMessageToChat Broadcasting message '${message::class.simpleName}' to ${connections?.size ?: 0} peers in chat '$chatId'"
        )
        val messageJson = gson.toJson(message)
        activeConnections[chatId]?.values?.forEach { pcManager ->
            pcManager.sendMessage(messageJson)
        }
    }

    override fun sendMessageToPeer(chatId: UUID, targetPeerId: PeerId, message: DataMessage) {
        Logger.log(
            tag,
            "sendMessageToPeer Sending message '${message::class.simpleName}' to peer '$targetPeerId' in chat '$chatId'"
        )
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
            Logger.log(
                tag,
                "shutdown Shutting down P2P Transport Manager. Closing all connections."
            )
            // Мы можем даже использовать coroutineScope, чтобы дождаться завершения всех leaveChat
            coroutineScope {
                activeConnections.keys.forEach { chatId ->
                    launch { // Запускаем закрытие каждого чата параллельно
                        leaveChat(chatId)
                    }
                }
            }

            Logger.log(
                tag,
                "All chat connections are closed. Cancelling scope and releasing WebRTC."
            )
            scope.cancel()
            webRTCManager.release()
        }
    }
}