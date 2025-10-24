package com.distributed_messenger.integration.services

import com.distributed_messenger.TestObjectMother
import com.distributed_messenger.core.UserRole
import com.distributed_messenger.data.irepositories.IUserRepository
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.domain.iservices.IUserService
import com.distributed_messenger.domain.models.LoginResult
import com.distributed_messenger.domain.services.EmailService
import com.distributed_messenger.domain.services.UserService
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID
import kotlin.test.*

@TestMethodOrder(MethodOrderer.Random::class)
class UserServiceIntegrationTest : ServiceTestBase() {
    private lateinit var userRepository: IUserRepository
    private lateinit var userService: IUserService

    @Before
    fun setupService() {
        userRepository = UserRepository(database.userDao())
        val mockEmailService = mockk<EmailService>()
        coJustRun { mockEmailService.sendTwoFactorCode(any(), any()) }
        userService = UserService(userRepository, mockEmailService)
    }

    @Test
    fun `register should create a user in the database`() = runTest {
        // Arrange
        val username = "test-user"
        val role = UserRole.USER
        val testPassword = "qwertyuiop"

        // Act
        val newUserId = userService.register(username, role, testPassword)

        // Assert
        val retrievedUser = userRepository.getUser(newUserId)
        assertNotNull(retrievedUser)
        assertEquals(username, retrievedUser.username)
        assertEquals(role, retrievedUser.role)
    }

    @Test
    fun `login should return RequiresTwoFactor for existing username`() = runTest {
        // Arrange
        val username = "test-user"
        val role = UserRole.USER
        val testPassword = "qwertyuiop"
        val user = userService.register(username, role, testPassword)

        // Act
        val loginResult = userService.login(username, testPassword)

        // Assert
        assertTrue(loginResult is LoginResult.RequiresTwoFactor)
    }

    @Test
    fun `login should return UserNotFound for non-existent user`() = runTest {
        // Arrange - база данных пуста
        val testPassword = "qwertyuiop"
        // Act
        val loginResult = userService.login("non-existent-user", testPassword)

        // Assert
        assertTrue(loginResult is LoginResult.UserNotFound)
    }


    @Test
    fun `findByUserName should return user for existing username`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()
        userRepository.addUser(user)

        // Act
        val foundUser = userService.findByUserName(user.username)

        // Assert
        assertNotNull(foundUser)
        assertEquals(user.id, foundUser.id)
        assertEquals(user.username, foundUser.username)
    }

    @Test
    fun `findByUserName should return null for non-existent user`() = runTest {
        // Arrange - база данных пуста

        // Act
        val foundUser = userService.findByUserName("non-existent-user")

        // Assert
        assertNull(foundUser)
    }


    @Test
    fun `getUser should return user for existing id`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()
        userRepository.addUser(user)

        // Act
        val foundUser = userService.getUser(user.id)

        // Assert
        assertNotNull(foundUser)
        assertEquals(user.id, foundUser.id)
    }

    @Test
    fun `getUser should return null for non-existent id`() = runTest {
        // Arrange
        val nonExistentId = UUID.randomUUID()

        // Act
        val foundUser = userService.getUser(nonExistentId)

        // Assert
        assertNull(foundUser)
    }


    @Test
    fun `getAllUsers should return all users from database`() = runTest {
        // Arrange
        userRepository.addUser(TestObjectMother.createUser())
        userRepository.addUser(TestObjectMother.createUser())

        // Act
        val users = userService.getAllUsers()

        // Assert
        assertEquals(2, users.size)
    }

    @Test
    fun `getAllUsers should return empty list when database is empty`() = runTest {
        // Arrange - база данных пуста

        // Act
        val users = userService.getAllUsers()

        // Assert
        assertTrue(users.isEmpty())
    }


    @Test
    fun `updateUser should change username in the database`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()
        userRepository.addUser(user)
        val newUsername = "new-super-name"

        // Act
        val success = userService.updateUser(user.id, newUsername)

        // Assert
        assertTrue(success)
        val updatedUser = userRepository.getUser(user.id)
        assertEquals(newUsername, updatedUser?.username)
    }

    @Test
    fun `updateUser should return false for non-existent user`() = runTest {
        // Arrange
        val nonExistentId = UUID.randomUUID()

        // Act
        val success = userService.updateUser(nonExistentId, "any-name")

        // Assert
        assertFalse(success)
    }


    @Test
    fun `updateUserRole should change role in the database`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser(role = UserRole.USER)
        userRepository.addUser(user)

        // Act
        val success = userService.updateUserRole(user.id, UserRole.ADMINISTRATOR)

        // Assert
        assertTrue(success)
        val updatedUser = userRepository.getUser(user.id)
        assertEquals(UserRole.ADMINISTRATOR, updatedUser?.role)
    }

    @Test
    fun `updateUserRole should return false for non-existent user`() = runTest {
        // Arrange
        val nonExistentId = UUID.randomUUID()

        // Act
        val success = userService.updateUserRole(nonExistentId, UserRole.ADMINISTRATOR)

        // Assert
        assertFalse(success)
    }


    @Test
    fun `deleteUser should remove user from the database`() = runTest {
        // Arrange
        val user = TestObjectMother.createUser()
        userRepository.addUser(user)

        // Act
        val success = userService.deleteUser(user.id)

        // Assert
        assertTrue(success)
        val deletedUser = userRepository.getUser(user.id)
        assertNull(deletedUser)
    }

    @Test
    fun `deleteUser should return false for non-existent user`() = runTest {
        // Arrange
        val nonExistentId = UUID.randomUUID()

        // Act
        val success = userService.deleteUser(nonExistentId)

        // Assert
        assertFalse(success)
    }
}