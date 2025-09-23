package com.distributed_messenger.unit.repositories

import com.distributed_messenger.unit.TestObjectMother
import com.distributed_messenger.core.AppSettingType
import com.distributed_messenger.data.repositories.AppSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class AppSettingsRepositoryTest : RepositoryTestBase() {
    private lateinit var appSettingsRepository: AppSettingsRepository

    @Before
    fun setup() {
        appSettingsRepository = AppSettingsRepository(database.appSettingsDao())
    }

    @Test
    fun `initializeDefaultSettings should populate db only if it is empty`() = runTest {
        // Act (first call)
        appSettingsRepository.initializeDefaultSettings()

        // Assert
        val allSettings = appSettingsRepository.getAllSettings()
        assertEquals(AppSettingType.entries.size, allSettings.size)
        assertEquals(AppSettingType.entries.first().possibleValues.keys.first(), allSettings.first().second)

        // Act (second call) - should do nothing
        appSettingsRepository.updateSetting(AppSettingType.THEME, 1)
        appSettingsRepository.initializeDefaultSettings()

        // Assert
        val themeSetting = appSettingsRepository.getSetting(AppSettingType.THEME)
        assertEquals(1, themeSetting) // Should not be reset to default
    }

    @Test
    fun `updateSetting with valid value should succeed`() = runTest {
        // Arrange
        appSettingsRepository.initializeDefaultSettings()
        val themeType = AppSettingType.THEME
        val newValue = 1 // Assuming 1 is a valid value for Theme

        // Act
        val result = appSettingsRepository.updateSetting(themeType, newValue)

        // Assert
        assertTrue(result)
        assertEquals(newValue, appSettingsRepository.getSetting(themeType))
    }

    @Test
    fun `updateSetting with invalid value should throw IllegalArgumentException`() {
        // Arrange
        runTest { appSettingsRepository.initializeDefaultSettings() }
        val themeType = AppSettingType.THEME
        val invalidValue = 99 // Assuming 99 is not a valid value

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            runTest { appSettingsRepository.updateSetting(themeType, invalidValue) }
        }
    }
}