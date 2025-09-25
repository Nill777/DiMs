package com.distributed_messenger.unit.repositories


import com.distributed_messenger.data.repositories.FileRepository
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.TestObjectMother
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.*

class FileRepositoryTest : RepositoryTestBase() {

    private lateinit var fileRepository: FileRepository
    private val testUser1 = TestObjectMother.createUser()
    private val testUser2 = TestObjectMother.createUser()

    @Before
    fun setup() {
        fileRepository = FileRepository(database.fileDao())
        val userRepository = UserRepository(database.userDao())
        runTest {
            userRepository.addUser(testUser1)
            userRepository.addUser(testUser2)
        }
    }

    @Test
    fun `addFile should correctly save a file`() = runTest {
        // Arrange
        val file = TestObjectMother.createFile(uploaderId = testUser1.id)

        // Act
        fileRepository.addFile(file)

        // Assert
        val retrievedFile = fileRepository.getFile(file.id)
        assertNotNull(retrievedFile)
        assertEquals(file.name, retrievedFile.name)
        assertEquals(testUser1.id, retrievedFile.uploadedBy)
    }

    @Test
    fun `getFile with a non-existent id should return null`() = runTest {
        // Arrange
        val nonExistentId = UUID.randomUUID()

        // Act
        val retrievedFile = fileRepository.getFile(nonExistentId)

        // Assert
        assertNull(retrievedFile)
    }

    @Test
    fun `getFilesByUser should return files only for the specified user`() = runTest {
        // Arrange
        val file1User1 = TestObjectMother.createFile(uploaderId = testUser1.id)
        val file2User1 = TestObjectMother.createFile(uploaderId = testUser1.id)
        val file1User2 = TestObjectMother.createFile(uploaderId = testUser2.id)
        fileRepository.addFile(file1User1)
        fileRepository.addFile(file2User1)
        fileRepository.addFile(file1User2)

        // Act
        val user1Files = fileRepository.getFilesByUser(testUser1.id)

        // Assert
        assertEquals(2, user1Files.size)
        val expectedIds = setOf(file1User1.id, file2User1.id)
        val actualIds = user1Files.map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }
}