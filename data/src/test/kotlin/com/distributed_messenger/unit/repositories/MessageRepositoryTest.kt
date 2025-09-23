package com.distributed_messenger.unit.repositories


import com.distributed_messenger.data.repositories.ChatRepository
import com.distributed_messenger.data.repositories.MessageRepository
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.unit.TestObjectMother
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.*

class MessageRepositoryTest: RepositoryTestBase() {
    private lateinit var messageRepository: MessageRepository
    private val testUser = TestObjectMother.createUser()
    private val testChat = TestObjectMother.createChat(creatorId = testUser.id)

    @Before
    fun setup() {
        messageRepository = MessageRepository(database.messageDao())
        runTest {
            UserRepository(database.userDao()).addUser(testUser)
            ChatRepository(database.chatDao()).addChat(testChat)
        }
    }

    @Test
    fun `addMessage should save a message and allow retrieval`() = runTest {
        // Arrange
        val message = TestObjectMother.createMessage(chatId = testChat.id, senderId = testUser.id)

        // Act
        messageRepository.addMessage(message)

        // Assert
        val retrieved = messageRepository.getMessage(message.id)
        assertNotNull(retrieved)
        assertEquals(message.content, retrieved.content)
    }

    @Test
    fun `getMessagesAfter should return only messages after the given timestamp`() = runTest {
        // Arrange
        val now = Instant.now()
        val oldMessage = TestObjectMother.createMessage(chatId = testChat.id, senderId = testUser.id)
            .copy(timestamp = now.minusSeconds(60))
        val newMesage1 = TestObjectMother.createMessage(chatId = testChat.id, senderId = testUser.id)
            .copy(timestamp = now.plusSeconds(60))
        val newMesage2 = TestObjectMother.createMessage(chatId = testChat.id, senderId = testUser.id)
            .copy(timestamp = now.plusSeconds(120))
        messageRepository.addMessage(oldMessage)
        messageRepository.addMessage(newMesage1)
        messageRepository.addMessage(newMesage2)

        // Act
        val result = messageRepository.getMessagesAfter(testChat.id, now)

        // Assert
        assertEquals(2, result.size)
        val expectedIds = setOf(newMesage1.id, newMesage2.id)
        val actualIds = result.map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }
}