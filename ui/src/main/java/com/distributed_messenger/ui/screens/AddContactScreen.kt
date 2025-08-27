package com.distributed_messenger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.distributed_messenger.presenter.viewmodels.AddContactState
import com.distributed_messenger.presenter.viewmodels.AddContactViewModel
import com.distributed_messenger.ui.NavigationController
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID

@Composable
fun AddContactScreen(
    viewModel: AddContactViewModel,
    navigationController: NavigationController
) {
    val state by viewModel.state.collectAsState()
    var manualInput by remember { mutableStateOf("") }

    // Слушаем состояние и выполняем навигацию при успехе
//    LaunchedEffect(Unit) {
//        viewModel.state.collectLatest { currentState ->
//            if (currentState is AddContactState.Success) {
//                navigationController.navigateToChat(currentState.chatId) {
//                    popUpTo("chat_list") { inclusive = false }
//                }
//                viewModel.resetState()
//            }
//        }
//    }

//    // Сбрасываем состояние при выходе с экрана
//    DisposableEffect(Unit) {
//        onDispose {
//            viewModel.resetState()
//        }
//    }

    Scaffold(
//        topBar = { TopAppBar(title = { Text("Add New Contact") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { /* TODO: Запустить экран сканера камеры */
                    // Пока эмулируем результат
                    val fakeScanResult = UUID.randomUUID()
                    viewModel.acceptInvite(fakeScanResult)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan QR Code")
            }

            Spacer(Modifier.height(32.dp))
            Text("Or enter code manually")

            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                label = { Text("Paste invite code here") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.acceptInvite(UUID.fromString(manualInput)) },
                enabled = manualInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Contact")
            }

            Spacer(Modifier.height(32.dp))

            // Отображение состояния
            when (val currentState = state) {
                is AddContactState.Connecting, is AddContactState.PeerFound -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text((currentState as? AddContactState.Connecting)?.message
                        ?: (currentState as? AddContactState.PeerFound)?.username ?: "...")
                }
                is AddContactState.Error -> {
                    Text(currentState.message, color = MaterialTheme.colorScheme.error)
                }
                is AddContactState.Success -> {
                    Text("Success! Navigating to chat...")
                }
                else -> {}
            }
        }
    }
}