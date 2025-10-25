package com.distributedMessenger.unit.repositories


import com.distributedMessenger.RepositoryTestBase
import com.distributedMessenger.TestObjectMother
import com.distributedMessenger.data.repositories.ChatRepository
import com.distributedMessenger.data.repositories.MessageHistoryRepository
import com.distributedMessenger.data.repositories.MessageRepository
import com.distributedMessenger.data.repositories.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MessageHistoryRepositoryUnitTest : RepositoryTestBase() {

    private lateinit var messageHistoryRepository: MessageHistoryRepository
    private val testMessage = TestObjectMother.createMessage(
        chatId = UUID.randomUUID(),
        senderId = UUID.randomUUID()
    )

    @Before
    fun setup() {
        messageHistoryRepository = MessageHistoryRepository(database.messageHistoryDao())
        runTest {
            // Need to insert dependencies to satisfy foreign keys
            UserRepository(database.userDao()).addUser(TestObjectMother.createUser(id = testMessage.senderId))
            ChatRepository(database.chatDao()).addChat(
                TestObjectMother.createChat(
                    id = testMessage.chatId,
                    creatorId = testMessage.senderId
                )
            )
            MessageRepository(database.messageDao()).addMessage(testMessage)
        }
    }

    @Test
    fun `addMessageHistory should save a history entry`() = runTest {
        // Arrange
        val historyEntry = TestObjectMother.createMessageHistory(messageId = testMessage.id)

        // Act
        messageHistoryRepository.addMessageHistory(historyEntry)

        // Assert
        val retrievedHistory = messageHistoryRepository.getHistoryForMessage(testMessage.id)
        assertEquals(1, retrievedHistory.size)
        assertEquals(historyEntry.editedContent, retrievedHistory.first().editedContent)
    }

    @Test
    fun `getHistoryForMessage for a message with no history should return an empty list`() =
        runTest {
            // Act
            val history = messageHistoryRepository.getHistoryForMessage(testMessage.id)

            // Assert
            assertNotNull(history)
            assertTrue(history.isEmpty())
        }
}
