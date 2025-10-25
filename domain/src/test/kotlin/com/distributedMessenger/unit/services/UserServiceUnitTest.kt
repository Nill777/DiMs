package com.distributedMessenger.unit.services

import com.distributedMessenger.TestObjectMother
import com.distributedMessenger.core.UserRole
import com.distributedMessenger.data.irepositories.IUserRepository
import com.distributedMessenger.domain.iservices.IUserService
import com.distributedMessenger.domain.models.LoginResult
import com.distributedMessenger.domain.services.UserService
import com.distributedMessenger.domain.util.PasswordHasher
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.Random::class) // запуск тестов в случайном порядке
class UserServiceUnitTest {

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
        val testPassword = "qwertyuiop"
        val userSlot = slot<com.distributedMessenger.core.User>()
        coEvery { mockUserRepository.findByUsername(username) } returns null
        coEvery { mockUserRepository.addUser(capture(userSlot)) } returns expectedUserId

        // Act
        val result = userService.register(username, UserRole.USER, testPassword)

        // Assert
        assertEquals(expectedUserId, result)
        assertEquals(username, userSlot.captured.username)
        assertEquals(UserRole.USER, userSlot.captured.role)
        coVerify(exactly = 1) { mockUserRepository.findByUsername(username) }
        coVerify(exactly = 1) { mockUserRepository.addUser(any()) }
    }

    @Test
    fun `register should propagate exception from repository`() = runTest {
        // Arrange
        val username = "anyUser"
        val testPassword = "qwertyuiop"
        val errorMessage = "Database connection lost"
        coEvery { mockUserRepository.findByUsername(username) } returns null
        coEvery { mockUserRepository.addUser(any()) } throws Exception(errorMessage)

        // Act & Assert
        val exception = assertThrows<Exception> {
            userService.register(username, UserRole.USER, testPassword)
        }
        assertEquals(errorMessage, exception.message)
        coVerify { mockUserRepository.findByUsername(username) }
        coVerify { mockUserRepository.addUser(any()) }
    }


    @Test
    fun `login should return Success for existing username`() = runTest {
        // Arrange
        val username = "existingUser"
        val testPassword = "qwertyuiop"
        val pepper = "test_pepper"
        val passwordHash = PasswordHasher.hashPassword(testPassword, pepper)
        val fakeUser = TestObjectMother.createUser(
            username = username,
            passwordHash = passwordHash
        )
        coEvery { mockUserRepository.findByUsername(username) } returns fakeUser
        // Для updateUser, так как при успешном логине происходит обновление хэша
        coEvery { mockUserRepository.updateUser(any()) } returns true

        // Act
        val testUserService = UserService(mockUserRepository, pepper)
        val result = testUserService.login(username, testPassword)

        // Assert
        assertTrue(result is LoginResult.Success)
        coVerify { mockUserRepository.findByUsername(username) }
    }

    @Test
    fun `login should return UserNotFound for non-existent username`() = runTest {
        // Arrange
        val username = "nonExistentUser"
        val testPassword = "qwertyuiop"
        coEvery { mockUserRepository.findByUsername("nonExistentUser") } returns null

        // Act
        val result = userService.login("nonExistentUser", testPassword)

        // Assert
        assertTrue(result is LoginResult.UserNotFound)
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
        coVerify { mockUserRepository.updateUser(
            match {
                it.id == existingUser.id && it.role == UserRole.ADMINISTRATOR
            })
        }
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
