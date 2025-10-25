package com.distributedMessenger.ui.screens

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.presenter.viewmodels.AppSettingsViewModel
import com.distributedMessenger.ui.NavigationController
import com.distributedMessenger.ui.components.SettingItem

@Composable
fun AppSettingsScreen(viewModel: AppSettingsViewModel,
                      navigationController: NavigationController) {
    val settings by viewModel.settingsState.collectAsState()

    LaunchedEffect(Unit) {
        Logger.log("AppSettings", "Screen initialized")
    }

    LazyColumn(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        items(settings) { (type, value) ->
            SettingItem(
                type = type,
                currentValue = value,
                onValueChange = {
                    Logger.log("AppSettings", "Setting changed: $type -> $it")
                    viewModel.updateSetting(type, it)
                }
            )
        }
    }
}
