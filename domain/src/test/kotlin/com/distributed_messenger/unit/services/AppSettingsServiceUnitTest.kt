package com.distributed_messenger.unit.services


import com.distributed_messenger.core.AppSettingType
import com.distributed_messenger.data.irepositories.IAppSettingsRepository
import com.distributed_messenger.domain.iservices.IAppSettingsService
import com.distributed_messenger.domain.services.AppSettingsService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.*

@TestMethodOrder(MethodOrderer.Random::class)
class AppSettingsServiceUnitTest {

    @MockK
    private lateinit var mockRepository: IAppSettingsRepository
    private lateinit var service: IAppSettingsService

    // Fixture - общая подготовка для всех тестов
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        service = AppSettingsService(mockRepository)
    }

    // Позитивный тест для getSetting
    @Test
    fun `getSetting should return value when setting exists`() = runTest {
        // Arrange
        val themeType = AppSettingType.THEME
        val expectedValue = 2 // Dark theme
        coEvery { mockRepository.getSetting(themeType) } returns expectedValue

        // Act
        val result = service.getSetting(themeType)

        // Assert
        assertNotNull(result)
        assertEquals(expectedValue, result)
        coVerify(exactly = 1) { mockRepository.getSetting(themeType) }
    }

    // Негативный тест для getSetting
    @Test
    fun `getSetting should return null when setting does not exist`() = runTest {
        // Arrange
        val themeType = AppSettingType.THEME
        coEvery { mockRepository.getSetting(themeType) } returns null

        // Act
        val result = service.getSetting(themeType)

        // Assert
        assertNull(result)
    }

    // Позитивный тест для updateSetting
    @Test
    fun `updateSetting should return true on successful update`() = runTest {
        // Arrange
        val themeType = AppSettingType.THEME
        val newValue = 1
        coEvery { mockRepository.updateSetting(themeType, newValue) } returns true

        // Act
        val result = service.updateSetting(themeType, newValue)

        // Assert
        assertTrue(result)
    }

    // Негативный тест для updateSetting
    @Test
    fun `updateSetting should return false on failed update`() = runTest {
        // Arrange
        val themeType = AppSettingType.THEME
        val newValue = 1
        coEvery { mockRepository.updateSetting(themeType, newValue) } returns false

        // Act
        val result = service.updateSetting(themeType, newValue)

        // Assert
        assertFalse(result)
    }

    // Тест на исключение для updateSetting
    @Test
    fun `updateSetting should propagate exception from repository`() = runTest {
        // Arrange
        val themeType = AppSettingType.THEME
        val invalidValue = 99
        val exception = IllegalArgumentException("Invalid value")
        coEvery { mockRepository.updateSetting(themeType, invalidValue) } throws exception

        // Act & Assert
        val thrownException = assertFailsWith<IllegalArgumentException> {
            service.updateSetting(themeType, invalidValue)
        }
        assertEquals(exception.message, thrownException.message)
    }
}