package com.distributed_messenger.data.network.connection

import java.util.UUID

interface IConnectionManager {
    suspend fun joinChatNetwork(chatId: UUID)
    suspend fun leaveChatNetwork(chatId: UUID)
    suspend fun start()
    suspend fun stop()
}