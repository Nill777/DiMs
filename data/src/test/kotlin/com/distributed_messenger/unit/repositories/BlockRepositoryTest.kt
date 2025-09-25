package com.distributed_messenger.unit.repositories


import com.distributed_messenger.data.repositories.BlockRepository
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.TestObjectMother
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class BlockRepositoryTest : RepositoryTestBase() {

    private lateinit var blockRepository: BlockRepository
    private val blocker = TestObjectMother.createUser()
    private val blocked = TestObjectMother.createUser()

    @Before
    fun setup() {
        blockRepository = BlockRepository(database.blockDao())
        runTest {
            UserRepository(database.userDao()).addUser(blocker)
            UserRepository(database.userDao()).addUser(blocked)
        }
    }

    @Test
    fun `addBlock should save a block record`() = runTest {
        // Arrange
        val block = TestObjectMother.createBlock(blockerId = blocker.id, blockedUserId = blocked.id)

        // Act
        blockRepository.addBlock(block)

        // Assert
        val retrieved = blockRepository.getBlock(block.id)
        assertNotNull(retrieved)
        assertEquals(blocker.id, retrieved.blockerId)
        assertEquals(blocked.id, retrieved.blockedUserId)
    }

    @Test
    fun `deleteBlocksByUserId should remove the specific block`() = runTest {
        // Arrange
        val block = TestObjectMother.createBlock(blockerId = blocker.id, blockedUserId = blocked.id)
        blockRepository.addBlock(block)
        assertNotNull(blockRepository.getBlock(block.id)) // Pre-check

        // Act
        val result = blockRepository.deleteBlocksByUserId(blocker.id, blocked.id)

        // Assert
        assertTrue(result)
        assertNull(blockRepository.getBlock(block.id))
    }
}