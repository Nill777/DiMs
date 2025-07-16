package com.distributed_messenger.data.network.transport

import com.distributed_messenger.data.network.peer.IPeerDiscoverer.Peer

interface IP2PTransport {
    suspend fun connect(peer: Peer): Boolean
    suspend fun send(peer: Peer, data: ByteArray)
    fun setMessageListener(listener: (sourcePeer: Peer, data: ByteArray) -> Unit)
    fun startListening()
    fun stopListening()
}