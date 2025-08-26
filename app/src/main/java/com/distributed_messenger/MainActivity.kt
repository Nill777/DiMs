package com.distributed_messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.distributed_messenger.logger.Logger
import com.distributed_messenger.data.local.AppDatabase
import com.distributed_messenger.data.irepositories.IAppSettingsRepository
import com.distributed_messenger.data.irepositories.IBlockRepository
import com.distributed_messenger.data.irepositories.IChatRepository
import com.distributed_messenger.data.irepositories.IFileRepository
import com.distributed_messenger.data.irepositories.IMessageHistoryRepository
import com.distributed_messenger.data.irepositories.IMessageRepository
import com.distributed_messenger.data.irepositories.IUserRepository
import com.distributed_messenger.data.repositories.AppSettingsRepository
import com.distributed_messenger.data.repositories.BlockRepository
import com.distributed_messenger.data.repositories.ChatRepository
import com.distributed_messenger.data.repositories.FileRepository
import com.distributed_messenger.data.repositories.MessageHistoryRepository
import com.distributed_messenger.data.repositories.MessageRepository
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.data.network.crypto.AesGcmMessageCrypto
import com.distributed_messenger.data.network.crypto.INetworkCrypto
import com.distributed_messenger.data.network.signaling.FirebaseSignalingClient
import com.distributed_messenger.data.network.signaling.ISignalingClient
import com.distributed_messenger.data.network.syncer.DataSyncer
import com.distributed_messenger.data.network.transport.IP2PTransport
import com.distributed_messenger.data.network.transport.P2PTransportManager
import com.distributed_messenger.data.network.webRTC.WebRTCManager
import com.distributed_messenger.domain.services.AppSettingsService
import com.distributed_messenger.domain.services.BlockService
import com.distributed_messenger.domain.services.ChatService
import com.distributed_messenger.domain.services.FileService
import com.distributed_messenger.domain.services.MessageService
import com.distributed_messenger.domain.services.UserService
import com.distributed_messenger.presenter.viewmodels.AdminViewModel
import com.distributed_messenger.presenter.viewmodels.AppSettingsViewModel
import com.distributed_messenger.presenter.viewmodels.AuthViewModel
import com.distributed_messenger.presenter.viewmodels.ChatListViewModel
import com.distributed_messenger.presenter.viewmodels.ChatViewModel
import com.distributed_messenger.presenter.viewmodels.MessageHistoryViewModel
import com.distributed_messenger.presenter.viewmodels.NewChatViewModel
import com.distributed_messenger.presenter.viewmodels.ProfileViewModel
import com.distributed_messenger.ui.NavigationController
import com.distributed_messenger.ui.screens.AboutScreen
import com.distributed_messenger.ui.screens.AdminDashboardScreen
import com.distributed_messenger.ui.screens.AdminPanelScreen
import com.distributed_messenger.ui.screens.AppSettingsScreen
import com.distributed_messenger.ui.screens.AuthScreen
import com.distributed_messenger.ui.screens.BlockManagementScreen
import com.distributed_messenger.ui.screens.ChatListScreen
import com.distributed_messenger.ui.screens.ChatScreen
import com.distributed_messenger.ui.screens.MainScreen
import com.distributed_messenger.ui.screens.MessageHistoryScreen
import com.distributed_messenger.ui.screens.NewChatScreen
//import com.distributed_messenger.ui.screens.NewChatScreen
import com.distributed_messenger.ui.screens.ProfileScreen
import com.distributed_messenger.ui.screens.SettingsScreen
import com.distributed_messenger.ui.theme.DistributedMessengerTheme
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
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
    // --- НОВАЯ ИНИЦИАЛИЗАЦИЯ СЕТЕВОГО СЛОЯ ---

    private val gson by lazy { Gson() }

    // 1. Модуль шифрования (замените на свой по необходимости)
    private val messageCrypto: INetworkCrypto by lazy { AesGcmMessageCrypto() }

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
            roomDatabase ?: throw IllegalStateException("Room database not initialized")
        )
    }

    // 3. Сервисы
    private val userService by lazy { UserService(repositories.userRepository) }
    private val chatService by lazy {
        ChatService(
            repositories.chatRepository,
            p2pTransportManager
        )
    }
    private val fileService by lazy { FileService(repositories.fileRepository) }
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
        // Инициализация конфига перед использованием
        Config.initialize(applicationContext)

        val dir = File(applicationContext.getExternalFilesDir(null), Config.logDir).apply { mkdirs() }
        Logger.initialize(dir.absolutePath)

        dataSyncer.start()

        setContent {
            val appSettingsViewModel: AppSettingsViewModel = viewModel(
                factory = factory { AppSettingsViewModel(appSettingsService) }
            )
            DistributedMessengerTheme(
                appSettingsViewModel = appSettingsViewModel
            ) {
                // remember гарантирует, что объект не пересоздаётся при рекомпозициях.
                navController = rememberNavController()
                val navigationController = NavigationController(navController)

                NavHost(navController, startDestination = "auth") {
                    composable("auth") {
                        val viewModel: AuthViewModel = viewModel(
                            factory = factory { AuthViewModel(userService) }
                        )
                        AuthScreen(
                            viewModel = viewModel,
                            navigationController = navigationController
                        )
                    }
                    composable("home") {
                        MainScreen(navigationController = navigationController)
                    }
                    composable("chat_list") {
                        val chatListViewModel: ChatListViewModel = viewModel(
                            factory = factory { ChatListViewModel(chatService, messageService, userService) }
                        )
                        // Мы можем получить доступ к любой другой ViewModel точно так же.
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
