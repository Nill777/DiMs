package com.distributed_messenger.unit.repositories


import com.distributed_messenger.RepositoryTestBase
import com.distributed_messenger.data.repositories.ChatRepository
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.TestObjectMother
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.*

class ChatRepositoryUnitTest : RepositoryTestBase() {

    private lateinit var chatRepository: ChatRepository
    private val testUser1 = TestObjectMother.createUser()
    private val testUser2 = TestObjectMother.createUser()

    @Before
    fun setup() {
        chatRepository = ChatRepository(database.chatDao())
        val userRepository = UserRepository(database.userDao())
        runTest {
            userRepository.addUser(testUser1)
            userRepository.addUser(testUser2)
        }
    }

    @Test
    fun `addChat should correctly save a chat`() = runTest {
        // Arrange
        val chat = TestObjectMother.createChat(creatorId = testUser1.id, companionId = testUser2.id)

        // Act
        chatRepository.addChat(chat)

        // Assert
        val retrievedChat = chatRepository.getChat(chat.id)
        assertNotNull(retrievedChat)
        assertEquals(chat.name, retrievedChat.name)
    }

    @Test
    fun `getChat for non-existent id should return null`() = runTest {
        // Arrange
        val nonExistentId = UUID.randomUUID()

        // Act
        val retrievedChat = chatRepository.getChat(nonExistentId)

        // Assert
        assertNull(retrievedChat)
    }

    @Test
    fun `getChatsByUser should return all chats for a specific user`() = runTest {
        // Arrange
        val chat1 = TestObjectMother.createChat(creatorId = testUser1.id, companionId = testUser2.id)
        val chat2 = TestObjectMother.createChat(creatorId = testUser1.id) // Group chat
        val chat3 = TestObjectMother.createChat(creatorId = testUser2.id) // Another user's chat
        chatRepository.addChat(chat1)
        chatRepository.addChat(chat2)
        chatRepository.addChat(chat3)

        // Act
        val user1Chats = chatRepository.getChatsByUser(testUser1.id)

        // Assert
        assertEquals(2, user1Chats.size)
        val expectedIds = setOf(chat1.id, chat2.id)
        val actualIds = user1Chats.map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }
}