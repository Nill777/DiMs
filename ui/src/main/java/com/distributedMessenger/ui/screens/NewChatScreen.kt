package com.distributedMessenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.distributedMessenger.presenter.viewmodels.NewChatViewModel
import com.distributedMessenger.ui.NavigationController

@Composable
fun NewChatScreen(viewModel: NewChatViewModel,
                  navigationController: NavigationController) {
    var username by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            when (state) {
                is NewChatViewModel.NewChatState.Loading -> {
                    CircularProgressIndicator()
                }

                is NewChatViewModel.NewChatState.Error -> {
                    Text(
                        text = (state as NewChatViewModel.NewChatState.Error).message,
                        color = colorScheme.error
                    )
                }

                else -> {}
            }
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .background(colorScheme.background)
            )

            Button(
                onClick = {
                    viewModel.createChat(username) { chatId ->
                        navigationController.navigateToChat(chatId)
                    }
                },
//                enabled = username.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Create chat",
                    color = colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
