package com.distributed_messenger.unit.services

import com.distributed_messenger.core.UserRole
import com.distributed_messenger.data.irepositories.IUserRepository
import com.distributed_messenger.domain.iservices.IUserService
import com.distributed_messenger.domain.services.UserService
import com.distributed_messenger.TestObjectMother
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID
import kotlin.test.*

@TestMethodOrder(MethodOrderer.Random::class) // запуск тестов в случайном порядке
class UserServiceTest {

    @MockK
    private lateinit var mockUserRepository: IUserRepository
    private lateinit var userService: IUserService

    @Before
    fun setup() {
        MockKAnnotations.init(this) // Инициализация mock-объектов для JUnit 4
        userService = UserService(mockUserRepository)
    }

    @Test
    fun `register should call repository and return new user id`() = runTest {
        // Arrange
        val expectedUserId = UUID.randomUUID()
        val username = "testUser"
        val userSlot = slot<com.distributed_messenger.core.User>()
        coEvery { mockUserRepository.addUser(capture(userSlot)) } returns expectedUserId

        // Act
        val result = userService.register(username, UserRole.USER)

        // Assert
        assertEquals(expectedUserId, result)
        assertEquals(username, userSlot.captured.username)
        assertEquals(UserRole.USER, userSlot.captured.role)
        coVerify(exactly = 1) { mockUserRepository.addUser(any()) }
    }

    @Test
    fun `register should propagate exception from repository`() = runTest {
        // Arrange (Требование 2)
        val errorMessage = "Database connection lost"
        coEvery { mockUserRepository.addUser(any()) } throws Exception(errorMessage)

        // Act & Assert
        val exception = assertThrows<Exception> {
            userService.register("anyUser", UserRole.USER)
        }
        assertEquals(errorMessage, exception.message)
    }


    @Test
    fun `login should return user id for existing username`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser(username = "existingUser")
        coEvery { mockUserRepository.findByUsername("existingUser") } returns user

        // Act
        val result = userService.login("existingUser")

        // Assert
        assertEquals(user.id, result)
    }

    @Test
    fun `login should return null for non-existent username`() = runTest {
        // Arrange
        coEvery { mockUserRepository.findByUsername("nonExistentUser") } returns null

        // Act
        val result = userService.login("nonExistentUser")

        // Assert
        assertNull(result)
    }


    @Test
    fun `getUser should return user when user exists`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()
        coEvery { mockUserRepository.getUser(user.id) } returns user

        // Act
        val result = userService.getUser(user.id)

        // Assert
        assertNotNull(result)
        assertEquals(user, result)
    }

    @Test
    fun `getUser should return null when user does not exist`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        coEvery { mockUserRepository.getUser(userId) } returns null

        // Act
        val result = userService.getUser(userId)

        // Assert
        assertNull(result)
    }


    @Test
    fun `getAllUsers should return a list of users`() = runTest {
        // Arrange
        val expectedUsers = listOf(TestObjectMother.createUser(), TestObjectMother.createUser())
        coEvery { mockUserRepository.getAllUsers() } returns expectedUsers

        // Act
        val result = userService.getAllUsers()

        // Assert
        assertEquals(expectedUsers, result)
    }

    @Test
    fun `getAllUsers should return an empty list when no users exist`() = runTest {
        // Arrange
        coEvery { mockUserRepository.getAllUsers() } returns emptyList()

        // Act
        val result = userService.getAllUsers()

        // Assert
        assertTrue(result.isEmpty())
    }


    @Test
    fun `updateUser should return true when user exists and is updated`() = runTest {
        // Arrange
        val existingUser = TestObjectMother.createUser()
        val newUsername = "new-username"
        coEvery { mockUserRepository.getUser(existingUser.id) } returns existingUser
        coEvery { mockUserRepository.updateUser(any()) } returns true

        // Act
        val result = userService.updateUser(existingUser.id, newUsername)

        // Assert
        assertTrue(result)
        coVerify { mockUserRepository.updateUser(match { it.id == existingUser.id && it.username == newUsername }) }
    }

    @Test
    fun `updateUser should return false when user not found`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        coEvery { mockUserRepository.getUser(userId) } returns null

        // Act
        val result = userService.updateUser(userId, "new name")

        // Assert
        assertFalse(result)
        coVerify(exactly = 0) { mockUserRepository.updateUser(any()) }
    }


    @Test
    fun `updateUserRole should return true for existing user`() = runTest {
        // Arrange
        val existingUser = TestObjectMother.createUser(role = UserRole.USER)
        coEvery { mockUserRepository.getUser(existingUser.id) } returns existingUser
        coEvery { mockUserRepository.updateUser(any()) } returns true

        // Act
        val result = userService.updateUserRole(existingUser.id, UserRole.ADMINISTRATOR)

        // Assert
        assertTrue(result)
        coVerify { mockUserRepository.updateUser(match { it.id == existingUser.id && it.role == UserRole.ADMINISTRATOR }) }
    }

    @Test
    fun `updateUserRole should return false for non-existent user`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        coEvery { mockUserRepository.getUser(userId) } returns null

        // Act
        val result = userService.updateUserRole(userId, UserRole.ADMINISTRATOR)

        // Assert
        assertFalse(result)
    }


    @Test
    fun `deleteUser should return true when repository succeeds`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        coEvery { mockUserRepository.deleteUser(userId) } returns true

        // Act
        val result = userService.deleteUser(userId)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `deleteUser should return false when repository fails`() = runTest {
        // Arrange
        val userId = UUID.randomUUID()
        coEvery { mockUserRepository.deleteUser(userId) } returns false

        // Act
        val result = userService.deleteUser(userId)

        // Assert
        assertFalse(result)
    }
}