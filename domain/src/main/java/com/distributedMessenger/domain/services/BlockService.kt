package com.distributedMessenger.domain.services

import com.distributedMessenger.core.Block
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.logger.LoggingWrapper
import com.distributedMessenger.domain.iservices.IBlockService
import com.distributedMessenger.data.irepositories.IBlockRepository
import java.util.UUID
import java.time.Instant

class BlockService(private val blockRepository: IBlockRepository) : IBlockService {
    private val loggingWrapper = LoggingWrapper(
        origin = this,
        logger = Logger,
        tag = "BlockService"
    )

    override suspend fun blockUser(blockerId: UUID, blockedUserId: UUID, reason: String?): UUID =
        loggingWrapper {
            val block = Block(
                id = UUID.randomUUID(),
                blockerId = blockerId,
                blockedUserId = blockedUserId,
                reason = reason,
                timestamp = Instant.now()
            )
            blockRepository.addBlock(block)
        }

    override suspend fun getBlock(id: UUID): Block? =
        loggingWrapper {
            blockRepository.getBlock(id)
        }

    override suspend fun getUserBlocks(userId: UUID): List<Block> =
        loggingWrapper {
            blockRepository.getBlocksByUser(userId)
        }

    override suspend fun deleteBlock(id: UUID): Boolean =
        loggingWrapper {
            blockRepository.deleteBlock(id)
        }

    override suspend fun unblockUser(blockerId: UUID, blockedUserId: UUID): Boolean =
        loggingWrapper {
            blockRepository.deleteBlocksByUserId(blockerId, blockedUserId)
        }
}
