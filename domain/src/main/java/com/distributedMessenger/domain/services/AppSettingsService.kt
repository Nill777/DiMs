package com.distributedMessenger.domain.services

import com.distributedMessenger.core.AppSettingType
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.logger.LoggingWrapper
import com.distributedMessenger.data.irepositories.IAppSettingsRepository
import com.distributedMessenger.domain.iservices.IAppSettingsService

class AppSettingsService(private val repository: IAppSettingsRepository
) : IAppSettingsService {
    private val loggingWrapper = LoggingWrapper(
        origin = this,
        logger = Logger,
        tag = "AppSettingsService"
    )

    override suspend fun loadSettings(): List<Pair<AppSettingType, Int>> =
        loggingWrapper {
            repository.getAllSettings()
        }

    override suspend fun getSetting(type: AppSettingType): Int? =
        loggingWrapper {
            repository.getSetting(type)
        }

    override suspend fun updateSetting(type: AppSettingType, value: Int): Boolean =
        loggingWrapper {
            repository.updateSetting(type, value)
        }

    override suspend fun initializeDefaultSettings() =
        loggingWrapper {
            repository.initializeDefaultSettings()
        }

    override suspend fun addCustomSetting(name: String, defaultValue: Int) =
        loggingWrapper {
            repository.addCustomSetting(name, defaultValue)
        }
}
