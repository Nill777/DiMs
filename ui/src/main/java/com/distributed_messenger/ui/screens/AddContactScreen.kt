package com.distributed_messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import com.distributed_messenger.presenter.viewmodels.AddContactState
import com.distributed_messenger.presenter.viewmodels.AddContactViewModel
import com.distributed_messenger.presenter.viewmodels.AuthViewModel.AuthState
import com.distributed_messenger.ui.NavigationController
import com.distributed_messenger.ui.R
import com.distributed_messenger.ui.components.ScannerOverlay
import com.distributed_messenger.ui.util.QRCodeScanner
import java.util.UUID

// Вспомогательная функция для проверки UUID
private val UUID_REGEX =
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()

private fun isStrictlyUuid(text: String): Boolean {
    if (text.isBlank()) return false
    return UUID_REGEX.matches(text)
}

@Composable
fun AddContactScreen(
    viewModel: AddContactViewModel,
    navigationController: NavigationController
) {
    val state by viewModel.state.collectAsState()
    var manualInput by remember { mutableStateOf("") }
    // Используем 'rememberUpdatedState' для безопасного вызова manualInput в LaunchedEffect
    val currentManualInput by rememberUpdatedState(manualInput)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val focusManager = LocalFocusManager.current
    var isTextFieldFocused by remember { mutableStateOf(false) }
    val isInputValidInvite by remember(manualInput) {
        derivedStateOf {
            // Используем новую, строгую проверку
            isStrictlyUuid(manualInput)
        }
    }
//    val isInputValidInvite = remember(manualInput) { isUuidValid(manualInput) }
    // Поле в ошибке, если оно не пустое И не является валидным UUID
    val isInputError = manualInput.isNotEmpty() && !isInputValidInvite
    var labelText: String

    LaunchedEffect(state) {
        if (state is AddContactState.Success) {
            val chatId = (state as AddContactState.Success).chatId
            navigationController.navigateToChat(chatId)
        }
    }
//    // Сбрасываем состояние при выходе с экрана
//    DisposableEffect(Unit) {
//        onDispose {
//            viewModel.resetState()
//        }
//    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.primary) // Цвет фона всего экрана
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(screenWidth * 0.05f),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .background(colorScheme.background)
                // <-- ИЗМЕНЕНИЕ: Делаем всю колонку кликабельной для снятия фокуса
                .clickable(
                    indication = null, // отключаем визуальный эффект нажатия (рябь)
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus() // Снимаем фокус с любого элемента
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenWidth * 0.15f)
                    .background(colorScheme.primary),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .clickable {
                            //                    listViewModel.refreshChats()
                            navigationController.navigateBack()
                        }
//                    .padding(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back",
                        modifier = Modifier.size(25.dp),
                        tint = colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(15.dp))
                Icon(
                    painter = painterResource(R.drawable.side_menu_add_contact_1),
                    contentDescription = "Settings",
                    modifier = Modifier.size(50.dp),
                    tint = colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Add Contact",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onPrimary
                )
            }

            // --- ЗАДАЧA 1: Скрываем сканер, если клавиатура открыта ---
            if (!isTextFieldFocused) {
                Box(
                    modifier = Modifier
                        // Делаем Box 0 по размеру, когда поле в фокусе
//                        .alpha(if (isTextFieldFocused) 0f else 1f)
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(if (isTextFieldFocused) 0f else 0.6f)
//                    .height(250.dp)
//                    .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp)) // скруглённые края

//                    .border(1.dp, Color.Gray, RoundedCornerShape(12.dp)) // рамка с тем же скруглением
                ) {
                    QRCodeScanner(
                        isPaused = isTextFieldFocused,
                        onCodeScanned = { scannedCode ->
                            // Важно! После сканирования убираем фокус, если он есть,
                            // чтобы пользователь видел результат.
//                            focusManager.clearFocus()
                            // 1. Обновляем текстовое поле
                            manualInput = scannedCode
                            // 2. Вызываем событие добавления контакта
                            try {
                                val uuid = UUID.fromString(scannedCode)
                                viewModel.acceptInvite(uuid)
                            } catch (e: IllegalArgumentException) {
                                // Можно показать Snackbar или Toast, если QR-код невалиден
//                            println("Invalid UUID in QR Code: $scannedCode")
                                Logger.log("ui add contact", "Invalid UUID in QR Code: $scannedCode")
                            }
                        }
                    )
                    ScannerOverlay(modifier = Modifier.fillMaxSize())
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                // Отображение состояния
                when (val currentState = state) {
                    is AddContactState.Connecting, is AddContactState.PeerFound -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
//                                .height(screenWidth * 0.15f)
                                .background(colorScheme.background),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.width(4.dp))
                            Text(
                                (currentState as? AddContactState.Connecting)?.message
                                    ?: (currentState as? AddContactState.PeerFound)?.username
                                    ?: "..."
                            )
                        }
                    }
                    is AddContactState.Error -> {
                        Text(currentState.message, color = colorScheme.error)
                    }
                    is AddContactState.Success -> {
                        Text("Success! Navigating to chat...")
                    }
                    else -> {
                        if (!isTextFieldFocused) {
                            Text(
                                text = "Scan QR Code",
                                color = colorScheme.onBackground
                            )
                            Text(
                                text = "Or enter code manually",
                                color = colorScheme.onBackground
                            )
                        } else {
                            Text(
                                text = "Enter code manually",
                                color = colorScheme.onBackground
                            )
                        }
                    }
                }

                when {
                    manualInput.isEmpty() -> {
                        labelText = "Paste invite code here"
                    }
                    isInputValidInvite -> {
                        labelText = "Invite code is correct"
                    }
                    else -> {
                        labelText = "Invalid invite code"
                    }
                }

                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text(labelText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .background(colorScheme.background)
                        .onFocusChanged { focusState ->
                            isTextFieldFocused = focusState.isFocused
                        },
                    // --- ЗАДАЧА 3: Управление состоянием ошибки ---
                    isError = isInputError,
                    singleLine = true,
//                    colors = OutlinedTextFieldDefaults.colors(
//                        // --- ЗАДАЧА 3: Зеленая рамка при успехе ---
//                        focusedBorderColor = if (isInputValidInvite) Color(0xFF4CAF50) else colorScheme.primary,
//                        unfocusedBorderColor = if (isInputValidInvite) Color(0xFF4CAF50) else colorScheme.primary
//                    )
                )
                Button(
                    onClick = {
                        try {
                            viewModel.acceptInvite(UUID.fromString(currentManualInput))
                        } catch (e: IllegalArgumentException) {
                            Logger.log("AddContactScreen", "Invalid UUID from manual input: $currentManualInput", LogLevel.ERROR, e)
                        }
                    },
                    // --- ЗАДАЧА 3: Кнопка активна только с валидным UUID ---
                    enabled = isInputValidInvite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Add Contact",
                        color = colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}