package com.distributed_messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.distributed_messenger.logger.Logger
import com.distributed_messenger.data.local.AppDatabase
import com.distributed_messenger.data.local.irepositories.IAppSettingsRepository
import com.distributed_messenger.data.local.irepositories.IBlockRepository
import com.distributed_messenger.data.local.irepositories.IChatRepository
import com.distributed_messenger.data.local.irepositories.IFileRepository
import com.distributed_messenger.data.local.irepositories.IMessageHistoryRepository
import com.distributed_messenger.data.local.irepositories.IMessageRepository
import com.distributed_messenger.data.local.irepositories.IUserRepository
import com.distributed_messenger.data.local.repositories.AppSettingsRepository
import com.distributed_messenger.data.local.repositories.BlockRepository
import com.distributed_messenger.data.local.repositories.ChatRepository
import com.distributed_messenger.data.local.repositories.FileRepository
import com.distributed_messenger.data.local.repositories.MessageHistoryRepository
import com.distributed_messenger.data.local.repositories.MessageRepository
import com.distributed_messenger.data.local.repositories.UserRepository
import com.distributed_messenger.data.network.connection.ConnectionManagerImpl
import com.distributed_messenger.data.network.crypto.AesMessageCrypto
import com.distributed_messenger.data.network.peer.DhtConfig
import com.distributed_messenger.data.network.peer.DhtNetwork
import com.distributed_messenger.data.network.peer.DhtPeerDiscoverer
import com.distributed_messenger.data.network.syncer.IncomingMessageSyncer
import com.distributed_messenger.data.network.syncer.OutcomingMessageSyncer
import com.distributed_messenger.data.network.transport.WebRtcTransport
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
    private val dhtNetwork by lazy { DhtNetwork(DhtConfig.defaultConfig()) }
    private val peerDiscoverer by lazy { DhtPeerDiscoverer(dhtNetwork) }
    private val p2pTransport by lazy { WebRtcTransport(applicationContext) }
    private val messageCrypto by lazy { AesMessageCrypto() }

    private val connectionManager by lazy {
        ConnectionManagerImpl(
            peerDiscoverer = peerDiscoverer,
            p2pTransport = p2pTransport
        )
    }

    private val outcomingMessageSyncer by lazy {
        OutcomingMessageSyncer(
            peerDiscoverer = peerDiscoverer,
            p2pTransport = p2pTransport,
            messageCrypto = messageCrypto
        )
    }

    // 2. Фабрика репозиториев
    private val repositories: RepositoriesContainer by lazy {
        RoomRepositories(
            roomDatabase ?: throw IllegalStateException("Room database not initialized"),
            outcomingMessageSyncer
        )
    }

    private val incomingMessageSyncer by lazy {
        IncomingMessageSyncer(
            peerDiscoverer = peerDiscoverer,
            p2pTransport = p2pTransport,
            messageCrypto = messageCrypto,
            messageRepo = repositories.messageRepository
        )
    }

    // 3. Сервисы
    private val userService by lazy { UserService(repositories.userRepository) }
    private val chatService by lazy { ChatService(repositories.chatRepository) }
    private val fileService by lazy { FileService(repositories.fileRepository) }
    private val messageService by lazy {
        MessageService(
            repositories.messageRepository,
            repositories.messageHistoryRepository,
            connectionManager
        )
    }
    private val blockService by lazy { BlockService(repositories.blockRepository) }
    private val appSettingsService by lazy { AppSettingsService(repositories.appSettingsRepository) }

    // 4. ViewModels
    private val authViewModel: AuthViewModel by viewModels {
        factory {
            AuthViewModel(
                userService
            )
        }
    }
    private val chatListViewModel: ChatListViewModel by viewModels {
        factory {
            ChatListViewModel(
                chatService,
                messageService,
                userService
            )
        }
    }
    private val newChatViewModel: NewChatViewModel by viewModels {
        factory {
            NewChatViewModel(
                userService,
                chatService
            )
        }
    }
    private val messageHistoryViewModel: MessageHistoryViewModel by viewModels {
        factory {
            MessageHistoryViewModel(
                messageService
            )
        }
    }
    private val profileViewModel: ProfileViewModel by viewModels {
        factory {
            ProfileViewModel(
                userService
            )
        }
    }
    private val adminViewModel: AdminViewModel by viewModels {
        factory {
            AdminViewModel(
                userService,
                blockService
            )
        }
    }
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

        setContent {
            DistributedMessengerTheme(
                appSettingsViewModel = appSettingsViewModel
            ) {
                // remember гарантирует, что объект не пересоздаётся при рекомпозициях.
                navController = rememberNavController()
                val navigationController = NavigationController(navController)

                NavHost(navController, startDestination = "auth") {
                    composable("auth") {
                        AuthScreen(
                            viewModel = authViewModel,
                            navigationController = navigationController
                        )
                    }
                    composable("home") {
                        MainScreen(navigationController = navigationController)
                    }
                    composable("chat_list") {
                        ChatListScreen(
                            viewModel = chatListViewModel,
                            authViewModel = authViewModel,
                            appSettingsViewModel = appSettingsViewModel,
                            navigationController = navigationController
                        )
                    }
                    composable("chat/{chatId}") { backStackEntry ->
                        val chatId = UUID.fromString(backStackEntry.arguments?.getString("chatId"))
                        ChatScreen(
                            viewModel = ChatViewModel(
                                messageService = messageService,
                                chatService = chatService,
                                chatId = chatId
                            ),
//                            listViewModel = ChatListViewModel(
//                                chatService = chatService,
//                                messageService = messageService
//                            ),
                            navigationController = navigationController
                        )
                    }
                    composable("new_chat") {
                        NewChatScreen(
                            viewModel = newChatViewModel,
                            navigationController = navigationController
                        )
                    }
                    composable("message_history/{messageId}") { backStackEntry ->
                        val messageId =
                            UUID.fromString(backStackEntry.arguments?.getString("messageId"))
                        MessageHistoryScreen(
                            viewModel = messageHistoryViewModel,
                            messageId = messageId,
                            navigationController = navigationController
                        )
                    }
                    composable("profile") {
                        ProfileScreen(
                            viewModel = profileViewModel,
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
                        AdminDashboardScreen(
                            viewModel = adminViewModel,
                            navigationController = navigationController
                        )
                    }
                    composable("role_management") {
                        AdminPanelScreen(
                            viewModel = adminViewModel,
                            navigationController = navigationController
                        )
                    }
                    composable("block_management") {
                        BlockManagementScreen(
                            viewModel = adminViewModel,
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

        lifecycleScope.launch {
            connectionManager.stop()
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

private class RoomRepositories(db: AppDatabase, outcomingMessageSyncer: OutcomingMessageSyncer) : RepositoriesContainer {
    override val userRepository: IUserRepository = UserRepository(db.userDao())
    override val chatRepository: IChatRepository = ChatRepository(db.chatDao())
    override val fileRepository: IFileRepository = FileRepository(db.fileDao())
    override val messageRepository: IMessageRepository = MessageRepository(db.messageDao(), outcomingMessageSyncer)
    override val messageHistoryRepository: IMessageHistoryRepository = MessageHistoryRepository(db.messageHistoryDao())
    override val blockRepository: IBlockRepository = BlockRepository(db.blockDao())
    override val appSettingsRepository: IAppSettingsRepository = AppSettingsRepository(db.appSettingsDao())
}
