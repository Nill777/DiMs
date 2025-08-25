package com.distributed_messenger.data.network.transport

import com.distributed_messenger.data.network.PeerId
import com.distributed_messenger.data.network.model.DataMessage
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    override fun joinChat(chatId: UUID) {
        // Если уже в чате, ничего не делаем
        if (activeConnections.containsKey(chatId)) {
            Logger.log(tag, "Already joined chat '$chatId', ignoring.", LogLevel.WARN)
            return
        }
        Logger.log(tag, "Joining chat '$chatId'")
        val chatConnections = ConcurrentHashMap<PeerId, PeerConnectionManager>()
        activeConnections[chatId] = chatConnections

        scope.launch {
            signalingClient.joinRoom(chatId, myId)
                .collect { (peerId, signalMessage) ->
                    Logger.log(tag, "Received signal for peer '$peerId' in chat '$chatId'")
                    // Получаем или создаем PeerConnectionManager для этого пира
                    val pcManager = chatConnections.getOrPut(peerId) {
                        // isInitiator = true, если наш ID "больше" ID пира.
                        // Это простой способ гарантировать, что только один из двух создаст offer.
                        val isInitiator = myId > peerId
                        Logger.log(tag, "Peer '$peerId' is new for this chat. Creating connection. I am initiator: $isInitiator")
                        createAndSetupPcManager(chatId, peerId, isInitiator)
                    }
                    pcManager.handleIncomingSignal(signalMessage)
                }
        }
    }

    override fun sendMessageToChat(chatId: UUID, message: DataMessage) {
        val connections = activeConnections[chatId]
        Logger.log(tag, "Broadcasting message '${message::class.simpleName}' to ${connections?.size ?: 0} peers in chat '$chatId'")
        val messageJson = gson.toJson(message)
        activeConnections[chatId]?.values?.forEach { pcManager ->
            pcManager.sendMessage(messageJson)
        }
    }

    override fun sendMessageToPeer(chatId: UUID, targetPeerId: PeerId, message: DataMessage) {
        Logger.log(tag, "Sending message '${message::class.simpleName}' to peer '$targetPeerId' in chat '$chatId'")
        val messageJson = gson.toJson(message)
        // Находим конкретное соединение с нужным пиром и отправляем только ему
        activeConnections[chatId]?.get(targetPeerId)?.sendMessage(messageJson)
    }

    override fun leaveChat(chatId: UUID) {
        Logger.log(tag, "Leaving chat '$chatId'")
        activeConnections.remove(chatId)?.values?.forEach { it.close() }
    }

    private fun createAndSetupPcManager(chatId: UUID, peerId: PeerId, isInitiator: Boolean): PeerConnectionManager {
        Logger.log(tag, "Creating and setting up PeerConnectionManager for '$peerId' in chat '$chatId'")
        val pcManager = PeerConnectionManager(webRTCManager, peerId, isInitiator)

        // Перенаправляем исходящие сигналы в SignalingClient
        pcManager.outgoingSignal
            .onEach { signal ->
                Logger.log(tag, "Forwarding outgoing '${signal::class.simpleName}' from '$peerId' to signaling client", LogLevel.DEBUG)
                signalingClient.sendSignal(chatId, myId, peerId, signal)
            }
            .launchIn(scope)

        // Перенаправляем входящие данные в общий поток
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

    override fun shutdown() {
        Logger.log(tag, "Shutting down P2P Transport Manager. Closing all connections.")
        activeConnections.keys.forEach { leaveChat(it) }
        scope.cancel()
        webRTCManager.release()
    }
}