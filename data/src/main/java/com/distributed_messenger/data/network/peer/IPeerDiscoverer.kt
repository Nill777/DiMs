package com.distributed_messenger.data.network.peer

import java.util.UUID

interface IPeerDiscoverer {
    suspend fun discoverPeers(chatId: UUID): List<Peer>
    suspend fun registerSelf(chatId: UUID)
    suspend fun unregisterSelf(chatId: UUID)

    data class Peer(
        val id: String,
        val publicKey: String,
        val endpoints: List<String>
    )
}