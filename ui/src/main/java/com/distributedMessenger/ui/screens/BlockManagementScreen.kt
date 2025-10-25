package com.distributedMessenger.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.distributedMessenger.logger.LogLevel
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.presenter.viewmodels.AdminViewModel
import com.distributedMessenger.ui.NavigationController
import com.distributedMessenger.ui.components.BlockItem

@Composable
fun BlockManagementScreen(
    viewModel: AdminViewModel,
    navigationController: NavigationController
) {
    val users by viewModel.users.collectAsState()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        Logger.log("BlockManagement", "Loading users for blocking")
        viewModel.loadUsers()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (state) {
            AdminViewModel.AdminState.Loading -> {
                Logger.log("BlockManagement", "Loading state")
                CircularProgressIndicator()
            }

            is AdminViewModel.AdminState.Error -> {
                Logger.log(
                    "BlockManagement",
                    "Error: ${(state as AdminViewModel.AdminState.Error).message}",
                    LogLevel.ERROR
                )
                Text(
                    (state as AdminViewModel.AdminState.Error).message,
                    color = Color.Red
                )
            }

            else -> {}
        }

        LazyColumn {
            items(users) { user ->
                BlockItem(
                    user = user,
                    onBlock = {
                        Logger.log("BlockManagement", "Blocking user: ${user.id}")
                        viewModel.blockUser(user.id)
                    },
                    onUnblock = {
                        Logger.log("BlockManagement", "Unblocking user: ${user.id}")
                        viewModel.unblockUser(user.id)
                    }
                )
            }
        }
    }
}
