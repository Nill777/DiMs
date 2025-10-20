package com.distributed_messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.distributed_messenger.ui.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.distributed_messenger.presenter.viewmodels.AuthViewModel
import com.distributed_messenger.presenter.viewmodels.AuthViewModel.AuthState
import com.distributed_messenger.core.UserRole
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import com.distributed_messenger.ui.NavigationController
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.Duration

private enum class AuthMode {
    Login,
    Register
}

@Composable
fun AuthScreen(viewModel: AuthViewModel,
               navigationController: NavigationController
) {
    // Подписываемся на "статус" от ViewModel
    // collectAsState() превращает Flow в "живое состояние", которое автоматически обновляет UI
    val authState by viewModel.authState.collectAsState()
    // Поля ввода
    // remember — "запоминание" значения между обновлениями UI
    // mutableStateOf — "магнитная доска", изменения на которой автоматически обновляют экран
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Состояние для переключения между входом и регистрацией
    var authMode by remember { mutableStateOf(AuthMode.Login) }
    var passwordVisible by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.RegistrationSuccess -> {
                Logger.log("AuthScreen", "Registration success")
                navigationController.navigateToChatList()
            }
            is AuthState.LoginSuccess -> {
                Logger.log("AuthScreen", "Login success")
                navigationController.navigateToChatList()
            }
            is AuthState.Error -> {
                Logger.log("AuthScreen", "Auth error: ${(authState as AuthState.Error).message}", LogLevel.ERROR)
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            // ТАБЫ для переключения
            TabRow(
                selectedTabIndex = authMode.ordinal,
                containerColor = colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = authMode == AuthMode.Login,
                    onClick = { authMode = AuthMode.Login },
                    text = { Text("Login") }
                )
                Tab(
                    selected = authMode == AuthMode.Register,
                    onClick = { authMode = AuthMode.Register },
                    text = { Text("Register") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Поле ввода username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background),
                singleLine = true
            )

            // Поле для password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background),
                singleLine = true,
                // Скрываем вводимые символы
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                // Указываем, что это поле для пароля
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val iconId = if (passwordVisible)
                        R.drawable.eye
                    else
                        R.drawable.mask_1

                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(id = iconId),
                            contentDescription = description,
                            tint = Color.Unspecified
                        )
                    }
                }
            )

            // Отображение ошибки
            val currentAuthState = authState
            if (currentAuthState is AuthState.Error) {
                Text(
                    text = currentAuthState.message,
                    color = colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (currentAuthState is AuthState.Locked) {
                // Если аккаунт заблокирован, показываем таймер
                LockoutTimer(lockedUntil = currentAuthState.lockedUntil)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Динамическая кнопка зависит от режима authMode
            Button(
                onClick = {
                    when (authMode) {
                        AuthMode.Login -> viewModel.login(username, password)
                        AuthMode.Register -> viewModel.register(username, password, UserRole.USER) // По умолчанию USER
                    }
                },
                enabled = authState !is AuthState.Loading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                // Показываем спиннер прямо на кнопке во время загрузки
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (authMode == AuthMode.Login) "Sign In" else "Sign Up",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun LockoutTimer(lockedUntil: Instant) {
    // Состояние для хранения отформатированной строки времени
    var remainingTime by remember { mutableStateOf("") }

    // LaunchedEffect запускает корутину, которая будет жить, пока lockedUntil не изменится.
    LaunchedEffect(lockedUntil) {
        while (true) {
            val now = Instant.now()
            val duration = Duration.between(now, lockedUntil)

            if (duration.isNegative || duration.isZero) {
                remainingTime = "You can try to log in again now."
                break // Выходим из цикла, если время вышло
            }

            // Форматируем оставшееся время в минуты и секунды
            val minutes = duration.toMinutes()
            val seconds = duration.seconds % 60
            remainingTime = "Account locked. Try again in ${minutes}m ${seconds}s"

            delay(1000) // Ждем 1 секунду
        }
    }

    Text(
        text = remainingTime,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 8.dp)
    )
}