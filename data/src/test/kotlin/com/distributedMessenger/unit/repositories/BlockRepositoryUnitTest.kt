package com.distributedMessenger.unit.repositories


import com.distributedMessenger.RepositoryTestBase
import com.distributedMessenger.TestObjectMother
import com.distributedMessenger.data.repositories.BlockRepository
import com.distributedMessenger.data.repositories.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockRepositoryUnitTest : RepositoryTestBase() {

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
