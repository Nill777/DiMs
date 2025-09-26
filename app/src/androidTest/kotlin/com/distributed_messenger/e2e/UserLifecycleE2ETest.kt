package com.distributed_messenger.e2e

import com.distributed_messenger.core.UserRole
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.domain.services.UserService
import com.distributed_messenger.presenter.viewmodels.AuthViewModel
import com.distributed_messenger.presenter.viewmodels.ProfileViewModel
import com.distributed_messenger.presenter.viewmodels.SessionManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class UserLifecycleE2ETest : E2ETestBase() {
    private lateinit var authViewModel: AuthViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var userService: UserService
    private lateinit var userRepository: UserRepository

    @Before
    fun setupSystem() {
        // Fixture
        // Собираем всю систему из реальных компонентов
        userRepository = UserRepository(database.userDao())
        userService = UserService(userRepository)
        // ViewModel'и, которые являются точкой входа для теста
        authViewModel = AuthViewModel(userService)
        profileViewModel = ProfileViewModel(userService)
    }

    @Test
    fun userFullLifecycleScenarioCRUD() = runTest {
        // 1: Регистрация нового пользователя
        // Arrange
        val username = "e2e-user"
        val role = UserRole.USER

        // Act
        authViewModel.register(username, role)
        val newUserId = SessionManager.currentUserId

        // Assert
        assertNotNull(newUserId)
        // Проверяем базу данных - через репозиторий
        val userInDbAfterRegister = userRepository.getUser(newUserId)
        assertNotNull(userInDbAfterRegister)
        assertEquals(username, userInDbAfterRegister.username)
        println("E2E Step 1 PASSED: User '${username}' registered with ID ${newUserId}.")

        // 2: Логин пользователя
        // Act
        authViewModel.login(username)
        val loggedInUserId = SessionManager.currentUserId

        // Assert
        assertEquals(newUserId, loggedInUserId)
        println("E2E Step 2 PASSED: User '${username}' logged in successfully.")

        // 3: Обновление профиля (смена имени)
        // Arrange
        val newUsername = "e2e-user-updated"

        // Act
        profileViewModel.updateUsername(newUsername)
        val updateUserName = SessionManager.currentUserName

        // Assert
        assertEquals(newUsername, updateUserName)
        val userInDbAfterUpdate = userRepository.getUser(newUserId)
        assertNotNull(userInDbAfterUpdate)
        assertEquals(newUsername, userInDbAfterUpdate.username)
        println("E2E Step 3 PASSED: Username changed to '${newUsername}'.")

        // 4: Удаление пользователя (очистка)
        // Act
        val deleteSuccess = userService.deleteUser(newUserId)

        // Assert
        assertTrue(deleteSuccess)
        val userInDbAfterDelete = userRepository.getUser(newUserId)
        assertNull(userInDbAfterDelete)
        println("E2E Step 4 PASSED: User successfully deleted.")
    }
}