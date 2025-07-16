package com.distributed_messenger.data.network.connection

import com.distributed_messenger.data.network.peer.IPeerDiscoverer
import com.distributed_messenger.data.network.transport.IP2PTransport
import java.util.UUID

class ConnectionManagerImpl(
    private val peerDiscoverer: IPeerDiscoverer,
    private val p2pTransport: IP2PTransport
) : IConnectionManager {

    private val activeChats = mutableSetOf<UUID>()

    override suspend fun joinChatNetwork(chatId: UUID) {
        if (activeChats.add(chatId)) {
            peerDiscoverer.registerSelf(chatId)
        }
    }

    override suspend fun leaveChatNetwork(chatId: UUID) {
        if (activeChats.remove(chatId)) {
            peerDiscoverer.unregisterSelf(chatId)
        }
    }

    override suspend fun start() {
        p2pTransport.startListening()
    }

    override suspend fun stop() {
        activeChats.forEach { chatId ->
            peerDiscoverer.unregisterSelf(chatId)
        }
        activeChats.clear()
        p2pTransport.stopListening()
    }
}