package com.distributed_messenger.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.distributed_messenger.presenter.viewmodels.AddContactState
import com.distributed_messenger.presenter.viewmodels.AddContactViewModel
import com.distributed_messenger.ui.NavigationController
import com.distributed_messenger.ui.util.QRCodeGenerator

@Composable
fun ShareContactScreen(
    viewModel: AddContactViewModel,
    navigationController: NavigationController
) {
    val state by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // Генерируем inviteId только один раз при первом входе на экран.
    // `remember` гарантирует, что generateInvite() не будет вызываться при рекомпозициях.
    val inviteId by remember { mutableStateOf(viewModel.generateInvite()) }

    // Конвертируем dp в пиксели для генератора QR-кода
    val qrCodeSizePx = with(LocalDensity.current) { 250.dp.toPx() }.toInt()

    // Генерируем QR-код. `remember` с ключом `inviteId` гарантирует,
    // что QR-код будет перерисован только если изменится ID (хотя в нашем случае он не меняется).
    val qrCodeBitmap by remember(inviteId) {
        mutableStateOf(QRCodeGenerator.generateQrCode(inviteId.toString(), qrCodeSizePx))
    }

    // Автоматическая навигация при успешном соединении
    LaunchedEffect(state) {
        if (state is AddContactState.Success) {
            val chatId = (state as AddContactState.Success).chatId
            navigationController.navigateToChat(chatId)
        }
    }

    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Share Contact") },
//                navigationIcon = {
//                    IconButton(onClick = { navigationController.navigateBack() }) {
//                        // Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Отображаем QR-код, если он успешно сгенерировался
            if (qrCodeBitmap != null) {
                Image(
                    bitmap = qrCodeBitmap!!,
                    contentDescription = "Contact QR Code",
                    modifier = Modifier.size(250.dp)
                )
            } else {
                // Заглушка на случай ошибки генерации
                Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                    Text("Error generating QR code")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Отображение статуса
            when (val currentState = state) {
                is AddContactState.WaitingForPeer -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(currentState.message, textAlign = TextAlign.Center)
                }
                is AddContactState.PeerFound -> {
                    Text("Connected to: ${currentState.username}", style = MaterialTheme.typography.titleMedium)
                    Text("Creating chat...")
                }
                is AddContactState.Success -> {
                    Text("Chat created successfully!", style = MaterialTheme.typography.titleMedium)
                }
                is AddContactState.Error -> {
                    Text(currentState.message, color = MaterialTheme.colorScheme.error)
                }
                else -> { // Idle
                    Text("Show this code to a friend", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Or copy your Invite ID:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))


            SelectionContainer {
                Text(
                    text = inviteId.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = {
                clipboardManager.setText(AnnotatedString(inviteId.toString()))
            }) {
                Text("Copy ID")
            }
        }
    }
}