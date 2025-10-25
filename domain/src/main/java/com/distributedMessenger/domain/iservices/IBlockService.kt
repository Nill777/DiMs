package com.distributedMessenger.domain.iservices

import com.distributedMessenger.core.Block
import java.util.UUID

interface IBlockService {
    suspend fun blockUser(blockerId: UUID, blockedUserId: UUID, reason: String? = null): UUID
    suspend fun getBlock(id: UUID): Block?
    suspend fun getUserBlocks(userId: UUID): List<Block>
    suspend fun deleteBlock(id: UUID): Boolean
    suspend fun unblockUser(blockerId: UUID, blockedUserId: UUID): Boolean
}
