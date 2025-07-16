package com.distributed_messenger.data.network.peer

import java.util.UUID

class DhtPeerDiscoverer(private val dht: DhtNetwork) : IPeerDiscoverer {
    override suspend fun discoverPeers(chatId: UUID): List<IPeerDiscoverer.Peer> {
        return dht.getPeers(chatId.toString()).map {
            IPeerDiscoverer.Peer(
                id = it.id,
                publicKey = it.publicKey,
                endpoints = it.endpoints
            )
        }
    }

    override suspend fun registerSelf(chatId: UUID) {
        dht.announce(chatId.toString(), getLocalPeerInfo())
    }

    override suspend fun unregisterSelf(chatId: UUID) {
        dht.leave(chatId.toString())
    }

    private fun getLocalPeerInfo(): DhtNetwork.PeerInfo {
        // Логика получения информации о текущем устройстве
        // Временная заглушка - возвращаем фиктивные данные
        return DhtNetwork.PeerInfo(
            id = "local-peer-id", // В реальности: уникальный ID устройства
            publicKey = "public-key-placeholder", // В реальности: публичный ключ устройства
            endpoints = listOf("192.168.1.100:49001") // В реальности: IP/порт устройства
        )
    }
}