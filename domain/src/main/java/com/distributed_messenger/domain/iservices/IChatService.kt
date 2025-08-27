package com.distributed_messenger.domain.iservices

import com.distributed_messenger.core.Chat
import com.distributed_messenger.core.UserHandshake
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface IChatService {
    suspend fun createChat(name: String, creatorId: UUID, isGroupChat: Boolean = false, companionId: UUID? = null): UUID
    suspend fun createChatById(chatId: UUID, name: String, creatorId: UUID, isGroupChat: Boolean = false, companionId: UUID? = null): UUID
    suspend fun getChat(id: UUID): Chat?
    suspend fun getUserChats(userId: UUID): List<Chat>
    suspend fun updateChat(id: UUID, name: String): Boolean
    suspend fun deleteChat(id: UUID): Boolean
    suspend fun joinChatNetwork(chatId: UUID)
    suspend fun leaveChatNetwork(chatId: UUID)
    suspend fun performHandshake(inviteId: UUID, handshake: UserHandshake): Boolean
    fun listenForHandshake(inviteId: UUID): Flow<UserHandshake>
}
