package com.distributedMessenger.unit.services

import com.distributedMessenger.TestObjectMother
import com.distributedMessenger.data.irepositories.IBlockRepository
import com.distributedMessenger.domain.iservices.IBlockService
import com.distributedMessenger.domain.services.BlockService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.Random::class)
class BlockServiceUnitTest {

    @MockK
    private lateinit var mockBlockRepository: IBlockRepository
    private lateinit var blockService: IBlockService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        blockService = BlockService(mockBlockRepository)
    }

    // Позитивный тест для blockUser
    @Test
    fun `blockUser should return block id on success`() = runTest {
        // Arrange
        val blockerId = UUID.randomUUID()
        val blockedUserId = UUID.randomUUID()
        val reason = "spam"
        val expectedBlockId = UUID.randomUUID()
        coEvery { mockBlockRepository.addBlock(any()) } returns expectedBlockId

        // Act
        val result = blockService.blockUser(blockerId, blockedUserId, reason)

        // Assert
        assertEquals(expectedBlockId, result)
        coVerify(exactly = 1) { mockBlockRepository.addBlock(match {
            it.blockerId == blockerId && it.blockedUserId == blockedUserId && it.reason == reason
        }) }
    }

    // Тест на исключение для blockUser
    @Test
    fun `blockUser should throw exception when repository fails`() = runTest {
        // Arrange
        val exception = Exception("DB connection failed")
        coEvery { mockBlockRepository.addBlock(any()) } throws exception

        // Act & Assert
        val thrown = assertFailsWith<Exception> {
            blockService.blockUser(UUID.randomUUID(), UUID.randomUUID(), "reason")
        }
        assertEquals(exception.message, thrown.message)
    }

    @Test
    fun `blockUser without reason should work`() = runTest {
        val blockerId = UUID.randomUUID()
        val blockedUserId = UUID.randomUUID()
        val blockId = UUID.randomUUID()
        coEvery { mockBlockRepository.addBlock(any()) } returns blockId

        val result = blockService.blockUser(blockerId, blockedUserId)

        assertEquals(blockId, result)
        coVerify { mockBlockRepository.addBlock(match {
            it.blockerId == blockerId && it.blockedUserId == blockedUserId && it.reason == null
        }) }
    }

    @Test
    fun `getBlock should return null when block does not exist`() = runTest {
        // Arrange
        val blockId = UUID.randomUUID()
        coEvery { mockBlockRepository.getBlock(blockId) } returns null
        // Act
        val result = blockService.getBlock(blockId)
        //Assert
        assertNull(result)
    }

    // Позитивный тест для getUserBlocks
    @Test
    fun `getUserBlocks should return user's blocks`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        val expectedBlocks = listOf(
            TestObjectMother.createBlock(blockerId = userId, blockedUserId = UUID.randomUUID()),
            TestObjectMother.createBlock(blockerId = userId, blockedUserId = UUID.randomUUID())
        )
        coEvery { mockBlockRepository.getBlocksByUser(userId) } returns expectedBlocks

        // Act
        val result = blockService.getUserBlocks(userId)

        // Assert
        assertEquals(expectedBlocks, result)
    }

    // Негативный тест для getUserBlocks
    @Test
    fun `getUserBlocks should return empty list when user has no blocks`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        coEvery { mockBlockRepository.getBlocksByUser(userId) } returns emptyList()

        // Act
        val result = blockService.getUserBlocks(userId)

        // Assert
        assertTrue(result.isEmpty())
    }

    // Позитивный тест для unblockUser
    @Test
    fun `unblockUser should return true when successful`() = runTest {
        // Arrange
        val blockerId = UUID.randomUUID()
        val blockedId = UUID.randomUUID()
        coEvery { mockBlockRepository.deleteBlocksByUserId(blockerId, blockedId) } returns true

        // Act
        val result = blockService.unblockUser(blockerId, blockedId)

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { mockBlockRepository.deleteBlocksByUserId(blockerId, blockedId) }
    }

    // Негативный тест для unblockUser
    @Test
    fun `unblockUser should return false when repository fails`() = runTest {
        // Arrange
        val blockerId = UUID.randomUUID()
        val blockedId = UUID.randomUUID()
        coEvery { mockBlockRepository.deleteBlocksByUserId(blockerId, blockedId) } returns false

        // Act
        val result = blockService.unblockUser(blockerId, blockedId)

        // Assert
        assertFalse(result)
    }
}
