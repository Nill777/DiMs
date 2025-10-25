package com.distributedMessenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.data.local.AppDatabase
import com.distributedMessenger.data.irepositories.IAppSettingsRepository
import com.distributedMessenger.data.irepositories.IBlockRepository
import com.distributedMessenger.data.irepositories.IChatRepository
import com.distributedMessenger.data.irepositories.IFileRepository
import com.distributedMessenger.data.irepositories.IMessageHistoryRepository
import com.distributedMessenger.data.irepositories.IMessageRepository
import com.distributedMessenger.data.irepositories.IUserRepository
import com.distributedMessenger.data.repositories.AppSettingsRepository
import com.distributedMessenger.data.repositories.BlockRepository
import com.distributedMessenger.data.repositories.ChatRepository
import com.distributedMessenger.data.repositories.FileRepository
import com.distributedMessenger.data.repositories.MessageHistoryRepository
import com.distributedMessenger.data.repositories.MessageRepository
import com.distributedMessenger.data.repositories.UserRepository
//import com.distributedMessenger.data.network.crypto.AesGcmMessageCrypto
//import com.distributedMessenger.data.network.crypto.INetworkCrypto
import com.distributedMessenger.data.network.model.DataMessage
import com.distributedMessenger.data.network.model.DataMessageTypeAdapter
import com.distributedMessenger.data.network.model.SignalMessage
import com.distributedMessenger.data.network.model.SignalMessageTypeAdapter
import com.distributedMessenger.data.network.signaling.FirebaseSignalingClient
import com.distributedMessenger.data.network.signaling.ISignalingClient
import com.distributedMessenger.data.network.syncer.DataSyncer
import com.distributedMessenger.data.network.transport.IP2PTransport
import com.distributedMessenger.data.network.transport.P2PTransportManager
import com.distributedMessenger.data.network.webRTC.WebRTCManager
import com.distributedMessenger.domain.services.AppSettingsService
import com.distributedMessenger.domain.services.BlockService
import com.distributedMessenger.domain.services.ChatService
//import com.distributedMessenger.domain.services.FileService
import com.distributedMessenger.domain.services.MessageService
import com.distributedMessenger.domain.services.UserService
import com.distributedMessenger.presenter.viewmodels.AddContactViewModel
import com.distributedMessenger.presenter.viewmodels.AdminViewModel
import com.distributedMessenger.presenter.viewmodels.AppSettingsViewModel
import com.distributedMessenger.presenter.viewmodels.AuthViewModel
import com.distributedMessenger.presenter.viewmodels.ChatListViewModel
import com.distributedMessenger.presenter.viewmodels.ChatViewModel
import com.distributedMessenger.presenter.viewmodels.MessageHistoryViewModel
import com.distributedMessenger.presenter.viewmodels.NewChatViewModel
import com.distributedMessenger.presenter.viewmodels.ProfileViewModel
import com.distributedMessenger.ui.NavigationController
import com.distributedMessenger.ui.screens.AboutScreen
import com.distributedMessenger.ui.screens.AddContactScreen
import com.distributedMessenger.ui.screens.AdminDashboardScreen
import com.distributedMessenger.ui.screens.AdminPanelScreen
import com.distributedMessenger.ui.screens.AppSettingsScreen
import com.distributedMessenger.ui.screens.AuthScreen
import com.distributedMessenger.ui.screens.BlockManagementScreen
import com.distributedMessenger.ui.screens.ChatListScreen
import com.distributedMessenger.ui.screens.ChatScreen
import com.distributedMessenger.ui.screens.MainScreen
import com.distributedMessenger.ui.screens.MessageHistoryScreen
import com.distributedMessenger.ui.screens.NewChatScreen
import com.distributedMessenger.ui.screens.ProfileScreen
import com.distributedMessenger.ui.screens.SettingsScreen
import com.distributedMessenger.ui.screens.ShareContactScreen
import com.distributedMessenger.ui.theme.DistributedMessengerTheme
import com.distributedMessenger.util.CppBridge
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
//    private external fun getPepperFromNdk(appSignatureHash: String): String
//    companion object {
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }
    private lateinit var navController: NavHostController


    // 1. Инициализация базы данных
    private val roomDatabase: AppDatabase? by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            Config.dbName
        )
            .fallbackToDestructiveMigration(dropAllTables = true) // Удаляет данные при изменении схемы
            .build()
    }


    // 2. Сетевые компоненты
    private val gson: Gson by lazy {
        GsonBuilder()
            // Регистрируем адаптер для сигнальных сообщений
            .registerTypeAdapter(SignalMessage::class.java, SignalMessageTypeAdapter())
            // Регистрируем адаптер для сообщений с данными
            .registerTypeAdapter(DataMessage::class.java, DataMessageTypeAdapter())
            .create()
    }

    // 1. Модуль шифрования
//    private val messageCrypto: INetworkCrypto by lazy { AesGcmMessageCrypto() }

    // 2. WebRTC
    private val webRTCManager by lazy { WebRTCManager(applicationContext) }

    // 3. Сигнальный клиент
    private val signalingClient: ISignalingClient by lazy { FirebaseSignalingClient(gson) }

    // 4. Главный транспортный менеджер
    private val p2pTransportManager: IP2PTransport by lazy {
        P2PTransportManager(webRTCManager, signalingClient, gson)
    }

    // 5. Синхронизатор данных
    private val dataSyncer by lazy {
        DataSyncer(p2pTransportManager, repositories.messageRepository)
    }

    // 2. Фабрика репозиториев
    private val repositories: RepositoriesContainer by lazy {
        RoomRepositories(
            roomDatabase ?: error("Room database not initialized")
        )
    }

    // 3. Сервисы
    private val userService by lazy {
        // Получаем массив всех хэшей подписей
//        val currentSignatureHashes = Signature.getAppSignatureHashes(applicationContext)
//        if (currentSignatureHashes.isNullOrEmpty()) {
//            throw SecurityException("Could not retrieve app signatures. The APK might be corrupted")
//        }
//        val sortedConcatenatedHash = currentSignatureHashes.sorted().joinToString("")
//        val pepper = getPepperFromNdk(sortedConcatenatedHash)
//
//        if (pepper.isEmpty()) {
//            throw SecurityException("Security checks failed. Tampered, insecure, or unsigned environment detected")
//        }
        val pepper = CppBridge.getPepper(applicationContext)
        UserService(repositories.userRepository, pepper)
    }
    private val chatService by lazy {
        ChatService(
            repositories.chatRepository,
            p2pTransportManager
        )
    }
//    private val fileService by lazy { FileService(repositories.fileRepository) }
    private val messageService by lazy {
        MessageService(
            repositories.messageRepository,
            repositories.messageHistoryRepository,
            p2pTransportManager
        )
    }
    private val blockService by lazy { BlockService(repositories.blockRepository) }
    private val appSettingsService by lazy { AppSettingsService(repositories.appSettingsRepository) }

    // 4. ViewModels

    private val appSettingsViewModel: AppSettingsViewModel by viewModels {
        factory {
            AppSettingsViewModel(
                appSettingsService
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeAppConfig()
        dataSyncer.start()
        setupContent()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Корректно завершаем работу сети
        lifecycleScope.launch {
            // Теперь это вызов suspend-функции
            p2pTransportManager.shutdown()
        }
    }

    // Вспомогательная функция для создания фабрик ViewModel
    private inline fun <VM : ViewModel> factory(crossinline creator: () -> VM) =
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
        }

    private fun initializeAppConfig() {
        // Инициализация конфига перед использованием
        Config.initialize(applicationContext)
        val dir = File(applicationContext.getExternalFilesDir(null), Config.logDir).apply { mkdirs() }
        Logger.initialize(dir.absolutePath)
    }

    private fun setupContent() {
        setContent {
            val appSettingsViewModel: AppSettingsViewModel = viewModel(
                factory = factory { AppSettingsViewModel(appSettingsService) }
            )
            DistributedMessengerTheme(
                appSettingsViewModel = appSettingsViewModel
            ) {
                // remember гарантирует, что объект не пересоздаётся при рекомпозициях.
                navController = rememberNavController()
                val navGraph = AppNavigation(navController)
                NavHost(navController = navController, startDestination = "auth") {
                    with(navGraph) {
                        authGraph()
                        chatGraph(appSettingsViewModel)
                        messageGraph()
                        contactGraph()
                        settingsGraph()
                        adminGraph()
                    }
                }
            }
        }
    }

    private inner class AppNavigation(
        private val navController: NavHostController
    ) {
        private val navigationController = NavigationController(navController)

        fun NavGraphBuilder.authGraph() {
            composable("auth") {
                val viewModel: AuthViewModel = viewModel(
                    factory = factory { AuthViewModel(userService) }
                )
                AuthScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
        }

        fun NavGraphBuilder.chatGraph(appSettingsViewModel: AppSettingsViewModel) {
            composable("home") {
                MainScreen(navigationController = navigationController)
            }
            composable("chat_list") {
                val chatListViewModel: ChatListViewModel = viewModel(
                    factory = factory {
                        ChatListViewModel(
                            chatService,
                            messageService,
                            userService
                        )
                    }
                )
                // Мы можем получить доступ к любой другой ViewModel точно так же
                // Compose сам разберется, что AuthViewModel привязана к родительской активности
                // и вернет тот же экземпляр.
                val authViewModel: AuthViewModel = viewModel(
                    factory = factory { AuthViewModel(userService) }
                )

                ChatListScreen(
                    viewModel = chatListViewModel,
                    authViewModel = authViewModel,
                    appSettingsViewModel = appSettingsViewModel,
                    navigationController = navigationController
                )
            }
            composable("chat/{chatId}") { backStackEntry ->
                val chatId = UUID.fromString(backStackEntry.arguments?.getString("chatId"))

                // Точно так же работает и для VM с параметрами из навигации
                val viewModel: ChatViewModel = viewModel(
                    // Важно передать ключ (key), чтобы Compose знал, что для разных chatId
                    // нужны РАЗНЫЕ экземпляры ViewModel.
                    key = "chat_vm_$chatId",
                    factory = factory { ChatViewModel(messageService, chatService, chatId) }
                )
                ChatScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
            composable("new_chat") {
                val viewModel: NewChatViewModel = viewModel(
                    factory = factory { NewChatViewModel(userService, chatService) }
                )
                NewChatScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
        }

        fun NavGraphBuilder.messageGraph() {
            composable("message_history/{messageId}") { backStackEntry ->
                val messageId = UUID.fromString(backStackEntry.arguments?.getString("messageId"))
                val viewModel: MessageHistoryViewModel = viewModel(
                    key = "history_vm_$messageId",
                    factory = factory { MessageHistoryViewModel(messageService) }
                )
                MessageHistoryScreen(
                    viewModel = viewModel,
                    messageId = messageId,
                    navigationController = navigationController
                )
            }
        }

        fun NavGraphBuilder.contactGraph() {
            composable("add_contact") {
                val viewModel: AddContactViewModel = viewModel(
//                    factory = factory { AddContactViewModel(chatService, userService) }
                    factory = factory { AddContactViewModel(chatService) }
                )
                AddContactScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
            composable("share_contact") {
                val viewModel: AddContactViewModel = viewModel(
//                    factory = factory { AddContactViewModel(chatService, userService) }
                    factory = factory { AddContactViewModel(chatService) }
                )
                ShareContactScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
        }

        fun NavGraphBuilder.settingsGraph() {
            composable("profile") {
                val viewModel: ProfileViewModel = viewModel(
                    factory = factory { ProfileViewModel(userService) }
                )
                ProfileScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
            composable("settings") {
                SettingsScreen(navigationController = navigationController)
            }
            composable("about_program") {
                AboutScreen()
            }
        }

        fun NavGraphBuilder.adminGraph() {
            composable("admin_dashboard") {
                val viewModel: AdminViewModel = viewModel(
                    factory = factory { AdminViewModel(userService, blockService) }
                )
                AdminDashboardScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
            composable("role_management") {
                val viewModel: AdminViewModel = viewModel(
                    factory = factory { AdminViewModel(userService, blockService) }
                )
                AdminPanelScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
            composable("block_management") {
                val viewModel: AdminViewModel = viewModel(
                    factory = factory { AdminViewModel(userService, blockService) }
                )
                BlockManagementScreen(
                    viewModel = viewModel,
                    navigationController = navigationController
                )
            }
            composable("app_settings") {
                AppSettingsScreen(
                    viewModel = appSettingsViewModel,
                    navigationController = navigationController
                )
            }
        }
    }
}

// Классы-контейнеры для репозиториев
interface RepositoriesContainer {
    val userRepository: IUserRepository
    val chatRepository: IChatRepository
    val fileRepository: IFileRepository
    val messageRepository: IMessageRepository
    val messageHistoryRepository: IMessageHistoryRepository
    val blockRepository: IBlockRepository
    val appSettingsRepository: IAppSettingsRepository
}

private class RoomRepositories(db: AppDatabase) : RepositoriesContainer {
    override val userRepository: IUserRepository = UserRepository(db.userDao())
    override val chatRepository: IChatRepository = ChatRepository(db.chatDao())
    override val fileRepository: IFileRepository = FileRepository(db.fileDao())
    override val messageRepository: IMessageRepository = MessageRepository(db.messageDao())
    override val messageHistoryRepository: IMessageHistoryRepository = MessageHistoryRepository(db.messageHistoryDao())
    override val blockRepository: IBlockRepository = BlockRepository(db.blockDao())
    override val appSettingsRepository: IAppSettingsRepository = AppSettingsRepository(db.appSettingsDao())
}
