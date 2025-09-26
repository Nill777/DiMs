package com.distributed_messenger.integration.repositories

import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.TestObjectMother
import com.distributed_messenger.RepositoryTestBase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID
import kotlin.test.*

@TestMethodOrder(MethodOrderer.Random::class) // запуск тестов в случайном порядке
class UserRepositoryIntegrationTest : RepositoryTestBase() {

    private lateinit var userRepository: UserRepository

    @Before
    fun setup() {
        userRepository = UserRepository(database.userDao())
    }
    
    @Test
    fun `addUser should correctly save a user and allow retrieval`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()

        // Act
        val newUserId = userRepository.addUser(user)

        // Assert
        val retrievedUser = userRepository.getUser(user.id)
        assertEquals(user.id, newUserId)
        assertNotNull(retrievedUser)
        assertEquals(user.username, retrievedUser.username)
        assertEquals(user.role, retrievedUser.role)
    }


    @Test
    fun `getUser should retrieve an existing user`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()
        userRepository.addUser(user)

        // Act
        val retrievedUser = userRepository.getUser(user.id)

        // Assert
        assertNotNull(retrievedUser)
        assertEquals(user.id, retrievedUser.id)
    }

    @Test
    fun `getUser with non-existent id should return null`() = runTest {
        // Arrange
        val nonExistentId = UUID.randomUUID()

        // Act
        val retrievedUser = userRepository.getUser(nonExistentId)

        // Assert
        assertNull(retrievedUser)
    }


    @Test
    fun `getAllUsers should return all added users`() = runTest {
        // Arrange
        val user1 = TestObjectMother.createUser()
        val user2 = TestObjectMother.createUser()
        userRepository.addUser(user1)
        userRepository.addUser(user2)

        // Act
        val users = userRepository.getAllUsers()

        // Assert
        assertEquals(2, users.size)
        assertTrue(users.any { it.id == user1.id })
        assertTrue(users.any { it.id == user2.id })
    }

    @Test
    fun `getAllUsers should return an empty list when no users exist`() = runTest {
        // Arrange: No users added

        // Act
        val users = userRepository.getAllUsers()

        // Assert
        assertTrue(users.isEmpty())
    }


    @Test
    fun `findByUsername should return correct user when exists`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser(username = "find-me")
        userRepository.addUser(user)

        // Act
        val foundUser = userRepository.findByUsername("find-me")

        // Assert
        assertNotNull(foundUser)
        assertEquals(user.id, foundUser.id)
    }

    @Test
    fun `findByUsername should return null for non-existent username`() = runTest {
        // Arrange: No user with this name is added

        // Act
        val foundUser = userRepository.findByUsername("non-existent-user")

        // Assert
        assertNull(foundUser)
    }


    @Test
    fun `updateUser should correctly change user data`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()
        userRepository.addUser(user)
        val updatedUser = user.copy(username = "new-username")

        // Act
        val result = userRepository.updateUser(updatedUser)

        // Assert
        assertTrue(result)
        val retrievedUser = userRepository.getUser(user.id)
        assertEquals("new-username", retrievedUser?.username)
    }

    @Test
    fun `updateUser for non-existent user should return false`() = runTest {
        // Arrange
        val nonExistentUser = TestObjectMother.createUser()

        // Act
        val result = userRepository.updateUser(nonExistentUser)

        // Assert
        assertFalse(result)
    }


    @Test
    fun `deleteUser should remove the user from database`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()
        userRepository.addUser(user)
        assertNotNull(userRepository.getUser(user.id), "Precondition failed: User was not added.")

        // Act
        val result = userRepository.deleteUser(user.id)

        // Assert
        assertTrue(result)
        assertNull(userRepository.getUser(user.id), "User was not deleted.")
    }

    @Test
    fun `deleteUser for non-existent user should return false`() = runTest {
        // Arrange
        val nonExistentId = UUID.randomUUID()

        // Act
        val result = userRepository.deleteUser(nonExistentId)

        // Assert
        assertFalse(result)
    }
}