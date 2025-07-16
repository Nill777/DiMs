package com.distributed_messenger.data.network.peer

data class DhtConfig(
    val bootstrapNodes: List<String>,
    val port: Int,
    val networkId: String
) {
    companion object {
        fun defaultConfig() = DhtConfig(
            bootstrapNodes = listOf("router.bittorrent.com:6881", "dht.transmissionbt.com:6881"),
            port = 49001,
            networkId = "distributed-messenger"
        )
    }
}