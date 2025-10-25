package com.distributedMessenger.domain.iservices

import com.distributedMessenger.core.Chat
import com.distributedMessenger.core.UserHandshake
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface IChatService {
    val handshakeChannelReady: Flow<UUID>
    suspend fun createChat(
        name: String,
        creatorId: UUID,
        isGroupChat: Boolean = false,
        companionId: UUID? = null
    ): UUID

    suspend fun createChatById(
        chatId: UUID,
        name: String,
        creatorId: UUID,
        isGroupChat: Boolean = false,
        companionId: UUID? = null
    ): UUID

    suspend fun getChat(id: UUID): Chat?
    suspend fun getUserChats(userId: UUID): List<Chat>
    suspend fun updateChat(id: UUID, name: String): Boolean
    suspend fun deleteChat(id: UUID): Boolean
    suspend fun joinChatNetwork(chatId: UUID)
    suspend fun leaveChatNetwork(chatId: UUID)
    suspend fun performHandshake(inviteId: UUID, handshake: UserHandshake): Boolean
    suspend fun listenForHandshake(inviteId: UUID): Flow<UserHandshake>

    // Новые, более явные методы для рукопожатия
    suspend fun initiateContactRequest(inviteId: UUID)
    suspend fun acceptContactRequest(inviteId: UUID)
    suspend fun finalizeHandshake(inviteId: UUID)
}
