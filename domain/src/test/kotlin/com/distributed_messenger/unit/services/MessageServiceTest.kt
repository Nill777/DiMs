package com.distributed_messenger.unit.services


import com.distributed_messenger.data.irepositories.IMessageHistoryRepository
import com.distributed_messenger.data.irepositories.IMessageRepository
import com.distributed_messenger.data.network.transport.IP2PTransport
import com.distributed_messenger.domain.iservices.IMessageService
import com.distributed_messenger.domain.services.MessageService
import com.distributed_messenger.unit.TestObjectMother
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import kotlin.test.*

@ExtendWith(MockKExtension::class)
class MessageServiceTest {

    @MockK
    private lateinit var mockMessageRepository: IMessageRepository
    @MockK
    private lateinit var mockHistoryRepository: IMessageHistoryRepository
    @MockK(relaxed = true) // relaxed = true, т.к. transport.sendMessageToChat возвращает Unit
    private lateinit var mockP2pTransport: IP2PTransport

    private lateinit var messageService: IMessageService

    // Fixture - выполняется перед каждым тестом (Требование ЛР №4)
    @BeforeEach
    fun setup() {
        messageService = MessageService(mockMessageRepository, mockHistoryRepository, mockP2pTransport)
    }

    @Test
    fun `sendMessage should save message, send it to network and return id`() = runTest {
        // Arrange
        val expectedMessageId = UUID.randomUUID()
        val senderId = UUID.randomUUID()
        val chatId = UUID.randomUUID()
        coEvery { mockMessageRepository.addMessage(any()) } returns expectedMessageId

        // Act
        val result = messageService.sendMessage(senderId, chatId, "Hello", null)

        // Assert
        assertEquals(expectedMessageId, result, "Метод должен возвращать ID, полученный от репозитория")

        // Проверяем, что сообщение было сохранено в локальную БД
        coVerify(exactly = 1) { mockMessageRepository.addMessage(any()) }
        // Проверяем, что сообщение было отправлено по сети
        coVerify(exactly = 1) { mockP2pTransport.sendMessageToChat(eq(chatId), any()) }
    }

    @Test
    fun `sendMessage should propagate exception when repository fails`() = runTest {
        // Arrange
        val exception = Exception("Database is full")
        coEvery { mockMessageRepository.addMessage(any()) } throws exception

        // Act & Assert
        assertFailsWith<Exception> {
            messageService.sendMessage(UUID.randomUUID(), UUID.randomUUID(), "text", null)
        }

        // Убедимся, что если сохранение не удалось, то отправка по сети не происходит
        coVerify(exactly = 0) { mockP2pTransport.sendMessageToChat(any(), any()) }
    }

    @Test
    fun `getMessage should return message when it exists`() = runTest {
        // Arrange
        val messageId = UUID.randomUUID()
        val expectedMessage = TestObjectMother.createMessage(id = messageId, chatId = UUID.randomUUID(), senderId = UUID.randomUUID())
        coEvery { mockMessageRepository.getMessage(messageId) } returns expectedMessage

        // Act
        val result = messageService.getMessage(messageId)

        // Assert
        assertEquals(expectedMessage, result)
    }

    @Test
    fun `getMessage should return null when it does not exist`() = runTest {
        // Arrange
        val messageId = UUID.randomUUID()
        coEvery { mockMessageRepository.getMessage(messageId) } returns null

        // Act
        val result = messageService.getMessage(messageId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `getChatMessages should return list of messages for a chat`() = runTest {
        // Arrange
        val chatId = UUID.randomUUID()
        val expectedMessages = listOf(
            TestObjectMother.createMessage(chatId = chatId, senderId = UUID.randomUUID()),
            TestObjectMother.createMessage(chatId = chatId, senderId = UUID.randomUUID())
        )
        coEvery { mockMessageRepository.getMessagesByChat(chatId) } returns expectedMessages

        // Act
        val result = messageService.getChatMessages(chatId)

        // Assert
        assertEquals(expectedMessages, result)
    }

    @Test
    fun `getChatMessages should return empty list when no messages in chat`() = runTest {
        // Arrange
        val chatId = UUID.randomUUID()
        coEvery { mockMessageRepository.getMessagesByChat(chatId) } returns emptyList()

        // Act
        val result = messageService.getChatMessages(chatId)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `editMessage should update message, add history record and return true on success`() = runTest {
        // Arrange
        val messageId = UUID.randomUUID()
        val originalContent = "Original Content"
        val newContent = "New Content"
        val originalMessage = TestObjectMother.createMessage(id = messageId, chatId = UUID.randomUUID(), senderId = UUID.randomUUID())
            .copy(content = originalContent)

        coEvery { mockMessageRepository.getMessage(messageId) } returns originalMessage
        coEvery { mockMessageRepository.updateMessage(any()) } returns true
        coEvery { mockHistoryRepository.addMessageHistory(any()) } returns UUID.randomUUID()

        // Act
        val result = messageService.editMessage(messageId, newContent)

        // Assert
        assertTrue(result)
        // Проверяем, что сообщение было обновлено с новым контентом
        coVerify(exactly = 1) { mockMessageRepository.updateMessage(match { it.content == newContent }) }
        // Проверяем, что старая версия была сохранена в историю
        coVerify(exactly = 1) { mockHistoryRepository.addMessageHistory(match {
            it.messageId == messageId && it.editedContent == originalContent
        }) }
    }

    @Test
    fun `editMessage should return false when message to edit is not found`() = runTest {
        // Arrange
        val messageId = UUID.randomUUID()
        coEvery { mockMessageRepository.getMessage(messageId) } returns null

        // Act
        val result = messageService.editMessage(messageId, "New Content")

        // Assert
        assertFalse(result)
        // Убедимся, что никакие методы записи не были вызваны
        coVerify(exactly = 0) { mockMessageRepository.updateMessage(any()) }
        coVerify(exactly = 0) { mockHistoryRepository.addMessageHistory(any()) }
    }

    @Test
    fun `deleteMessage should return true when deletion is successful`() = runTest {
        // Arrange
        val messageId = UUID.randomUUID()
        coEvery { mockMessageRepository.deleteMessage(messageId) } returns true

        // Act
        val result = messageService.deleteMessage(messageId)

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { mockMessageRepository.deleteMessage(messageId) }
    }

    @Test
    fun `deleteMessage should return false when deletion fails`() = runTest {
        // Arrange
        val messageId = UUID.randomUUID()
        coEvery { mockMessageRepository.deleteMessage(messageId) } returns false

        // Act
        val result = messageService.deleteMessage(messageId)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `getMessageHistory should return list of history records`() = runTest {
        // Arrange
        val messageId = UUID.randomUUID()
        val expectedHistory = listOf(
            TestObjectMother.createMessageHistory(messageId),
            TestObjectMother.createMessageHistory(messageId)
        )
        coEvery { mockHistoryRepository.getHistoryForMessage(messageId) } returns expectedHistory

        // Act
        val result = messageService.getMessageHistory(messageId)

        // Assert
        assertEquals(expectedHistory, result)
    }

    @Test
    fun `getMessageHistory should return empty list when no history exists`() = runTest {
        // Arrange
        val messageId = UUID.randomUUID()
        coEvery { mockHistoryRepository.getHistoryForMessage(messageId) } returns emptyList()

        // Act
        val result = messageService.getMessageHistory(messageId)

        // Assert
        assertTrue(result.isEmpty())
    }
}