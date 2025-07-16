package com.distributed_messenger.data.network.transport

import android.content.Context
import com.distributed_messenger.data.network.peer.IPeerDiscoverer.Peer

class WebRtcTransport(context: Context) : IP2PTransport {
    private var messageListener: ((Peer, ByteArray) -> Unit)? = null

    override suspend fun connect(peer: Peer): Boolean {
        return true // Всегда успешное соединение для теста
    }

    override suspend fun send(peer: Peer, data: ByteArray) {
        // Заглушка для тестирования
    }

    override fun setMessageListener(listener: (Peer, ByteArray) -> Unit) {
        messageListener = listener
    }

    override fun startListening() {
        // Заглушка
    }

    override fun stopListening() {
        // Заглушка
    }
}