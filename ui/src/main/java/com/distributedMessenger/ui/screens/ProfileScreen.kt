// ProfileScreen.kt
package com.distributedMessenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.presenter.viewmodels.ProfileViewModel
import com.distributedMessenger.presenter.viewmodels.SessionManager
import com.distributedMessenger.ui.NavigationController
import com.distributedMessenger.ui.R

@Composable
fun ProfileScreen(viewModel: ProfileViewModel,
                  navigationController: NavigationController) {
    val colorScheme = MaterialTheme.colorScheme
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsState()
    // Состояние для текстового поля имени
    var username by remember { mutableStateOf(SessionManager.currentUserName) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    // Обновляем локальное состояние при изменении данных пользователя
    LaunchedEffect(viewModel.user.collectAsState().value?.username) {
        viewModel.user.value?.username?.let {
            username = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.primary) // Цвет фона всего экрана
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Шапка профиля
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenWidth * 0.20f)
                    .background(colorScheme.primary),
//                    .padding(start = 10.dp),
                //            horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(20.dp))

                Box(
                    modifier = Modifier
                        .clickable { navigationController.navigateToChatList() }
//                        .padding(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back",
                        modifier = Modifier.size(25.dp),
                        tint = colorScheme.onPrimary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenWidth * 0.20f)
                    .background(colorScheme.primary),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(17.dp))
                Icon(
                    painter = painterResource(R.drawable.avatar_placeholder),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(50.dp),
                    tint = colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = username,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Поле для изменения имени
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text("New Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        viewModel.updateUsername(username)
                        SessionManager.updateUserName(username)
                        focusManager.clearFocus()
                        Logger.log("ProfileScreen", "Username updated to: $username")
                    }
                )
            )

            Button(
                onClick = {
                    viewModel.updateUsername(username)
                    SessionManager.updateUserName(username)
                    navigationController.navigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text("Save")
            }

            when (state) {
                is ProfileViewModel.ProfileState.Loading -> {
                    CircularProgressIndicator()
                }

                is ProfileViewModel.ProfileState.Error -> {
                    val error = (state as ProfileViewModel.ProfileState.Error)
                    Text(
                        text = error.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {}
            }
        }
    }
}

//        // Кнопка админ-панели
//        if (SessionManager.currentUserRole == UserRole.ADMINISTRATOR) {
//            Button(
//                onClick = {
//                    Logger.log("ProfileScreen", "Navigating to admin dashboard")
//                    navigationController.navigateToAdminDashboard()
//                },
//                modifier = Modifier
//                    .padding(16.dp)
//                    .fillMaxWidth()
//            ) {
//                Text("Админ-панель")
//            }
//        }
