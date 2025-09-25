package com.distributed_messenger.unit.services


import com.distributed_messenger.data.irepositories.IChatRepository
import com.distributed_messenger.data.network.transport.IP2PTransport
import com.distributed_messenger.domain.iservices.IChatService
import com.distributed_messenger.domain.services.ChatService
import com.distributed_messenger.TestObjectMother
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.*

class ChatServiceTest {

    @MockK
    private lateinit var mockChatRepository: IChatRepository

    @MockK(relaxed = true) // relaxed = true, чтобы не описывать поведение методов, возвращающих Unit
    private lateinit var mockP2pTransport: IP2PTransport

    private lateinit var chatService: IChatService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        // Инициализируем сервис со всеми необходимыми моками
        chatService = ChatService(mockChatRepository, mockP2pTransport)
    }

    @Test
    fun `createChat should call repository and return new chat id`() = runTest {
        // Arrange
        val expectedChatId = UUID.randomUUID()
        val creatorId = UUID.randomUUID()
        coEvery { mockChatRepository.addChat(any()) } returns expectedChatId

        // Act
        val result = chatService.createChat("Test", creatorId, isGroupChat = false, companionId = null)

        // Assert
        assertEquals(expectedChatId, result)
        coVerify(exactly = 1) { mockChatRepository.addChat(any()) }
    }

    @Test
    fun `getChat should return null when chat does not exist`() = runTest {
        // Arrange
        val chatId = UUID.randomUUID()
        coEvery { mockChatRepository.getChat(chatId) } returns null

        // Act
        val result = chatService.getChat(chatId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `updateChat should return true when successful`() = runTest {
        // Arrange
        val chatId = UUID.randomUUID()
        val originalChat = TestObjectMother.createChat(id = chatId, creatorId = UUID.randomUUID())
        coEvery { mockChatRepository.getChat(chatId) } returns originalChat
        coEvery { mockChatRepository.updateChat(any()) } returns true

        // Act
        val result = chatService.updateChat(chatId, "New Name")

        // Assert
        assertTrue(result)
        coVerify { mockChatRepository.updateChat(match { it.id == chatId && it.name == "New Name" }) }
    }

    @Test
    fun `updateChat should return false when chat not found`() = runTest {
        // Arrange
        val chatId = UUID.randomUUID()
        coEvery { mockChatRepository.getChat(chatId) } returns null

        // Act
        val result = chatService.updateChat(chatId, "New Name")

        // Assert
        assertFalse(result)
        coVerify(exactly = 0) { mockChatRepository.updateChat(any()) }
    }

    @Test
    fun `joinChatNetwork should delegate call to p2pTransport`() = runTest {
        // Arrange
        val chatId = UUID.randomUUID()
        // Для relaxed мока не нужен coEvery, если метод возвращает Unit

        // Act
        chatService.joinChatNetwork(chatId)

        // Assert
        coVerify(exactly = 1) { mockP2pTransport.joinChat(chatId) }
    }

    @Test
    fun `leaveChatNetwork should delegate call to p2pTransport`() = runTest {
        // Arrange
        val chatId = UUID.randomUUID()

        // Act
        chatService.leaveChatNetwork(chatId)

        // Assert
        coVerify(exactly = 1) { mockP2pTransport.leaveChat(chatId) }
    }
}