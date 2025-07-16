package com.distributed_messenger.data.network.syncer

import com.distributed_messenger.core.Message
import com.distributed_messenger.data.network.crypto.IMessageCrypto
import com.distributed_messenger.data.network.peer.IPeerDiscoverer
import com.distributed_messenger.data.network.transport.IP2PTransport
import com.distributed_messenger.data.local.irepositories.IMessageRepository
import java.security.Key
import java.util.UUID

class IncomingMessageSyncer(
    private val peerDiscoverer: IPeerDiscoverer,
    private val p2pTransport: IP2PTransport,
    private val messageCrypto: IMessageCrypto,
    private val messageRepo: IMessageRepository
) {
    private val chatKeys = mutableMapOf<UUID, Key>()

    init {
        p2pTransport.setMessageListener { peer, data -> handleIncomingMessage(peer, data) }
    }

    suspend fun syncMessage(message: Message) {
        val peers = peerDiscoverer.discoverPeers(message.chatId)
        val chatKey = getChatKey(message.chatId)
        val encrypted = messageCrypto.encrypt(message, chatKey)

        peers.forEach { peer ->
            try {
                if (p2pTransport.connect(peer)) {
                    p2pTransport.send(peer, encrypted)
                }
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }
    // IncomingMessageSyncer.kt
    private fun getChatIdFromSource(source: IPeerDiscoverer.Peer): UUID {
        // Временная реализация - генерируем случайный UUID
        // В реальной реализации нужно извлекать из данных сообщения
        return UUID.randomUUID()
    }

    private fun handleIncomingMessage(source: IPeerDiscoverer.Peer, data: ByteArray) {
        val chatId = getChatIdFromSource(source) // Логика определения chatId
        val chatKey = getChatKey(chatId)
        val message = messageCrypto.decrypt(data, chatKey)

//        messageRepo.addMessage(message)
    }

    private fun getChatKey(chatId: UUID): Key {
        return chatKeys.getOrPut(chatId) {
            messageCrypto.generateChatKey()
        }
    }
}