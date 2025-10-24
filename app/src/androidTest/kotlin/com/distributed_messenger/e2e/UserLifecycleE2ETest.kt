package com.distributed_messenger.e2e

import com.distributed_messenger.core.UserRole
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.domain.services.UserService
import com.distributed_messenger.presenter.viewmodels.AuthViewModel
import com.distributed_messenger.presenter.viewmodels.ProfileViewModel
import com.distributed_messenger.presenter.viewmodels.SessionManager
import com.distributed_messenger.BuildConfig
import com.distributed_messenger.domain.services.EmailService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class UserLifecycleE2ETest : E2ETestBase() {
    private lateinit var authViewModel: AuthViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var userService: UserService
    private lateinit var userRepository: UserRepository
    private lateinit var emailService: EmailService

    // Fixture
    @Before
    fun setupSystem() {
        // Собираем всю систему из реальных компонентов
        userRepository = UserRepository(database.userDao())
        emailService = EmailService(
            smtpHost = "smtp.yandex.ru",
            smtpPort = "465",
            imapHost = "imap.yandex.ru",
            username = BuildConfig.GMAIL_USERNAME,
            appPassword = BuildConfig.GMAIL_APP_PASSWORD
        )
        userService = UserService(userRepository, emailService)
        // ViewModel'и, которые являются точкой входа для теста
        authViewModel = AuthViewModel(userService)
        profileViewModel = ProfileViewModel(userService)
    }

    @After
    fun cleanupSession() {
        SessionManager.logout()
    }

    @Test
    fun userFullLifecycleScenarioCRUD() = runTest {
        // 1: Регистрация нового пользователя
        // Arrange
        val username = "e2e-user"
        val role = UserRole.USER
        val testPassword = "qwertyuiop"

        // Act
        authViewModel.register(username, testPassword, role).join()
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
        authViewModel.login(username, testPassword).join()
        val loggedInUserId = SessionManager.currentUserId

        // Assert
        assertEquals(newUserId, loggedInUserId)
        println("E2E Step 2 PASSED: User '${username}' logged in successfully.")

        // 3: Обновление профиля (смена имени)
        // Arrange
        val newUsername = "e2e-user-updated"

        // Act
        profileViewModel.updateUsername(newUsername).join()
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