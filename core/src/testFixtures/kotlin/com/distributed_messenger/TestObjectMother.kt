package com.distributed_messenger

import com.distributed_messenger.core.*
import java.time.Instant
import java.util.UUID

/**
 * Фабрика для создания тестовых объектов
 * паттерн Object Mother
 */
object TestObjectMother {

    fun createUser(id: UUID = UUID.randomUUID(), username: String = "test-user-${id.toString().take(4)}", role: UserRole = UserRole.USER): User {
        return User(
            id = id,
            username = username,
            role = role,
            blockedUsersId = null,
            profileSettingsId = UUID.randomUUID(),
            appSettingsId = UUID.randomUUID()
        )
    }

    fun createChat(id: UUID = UUID.randomUUID(), creatorId: UUID, companionId: UUID? = null): Chat {
        return Chat(
            id = id,
            name = "Test Chat ${id.toString().take(4)}",
            creatorId = creatorId,
            companionId = companionId,
            isGroupChat = companionId == null
        )
    }

    fun createMessage(id: UUID = UUID.randomUUID(), chatId: UUID, senderId: UUID): Message {
        return Message(
            id = id,
            chatId = chatId,
            senderId = senderId,
            content = "Hello, world!",
            fileId = null,
            timestamp = Instant.now()
        )
    }

    fun createFile(id: UUID = UUID.randomUUID(), uploaderId: UUID): File {
        return File(
            id = id,
            name = "test_file.txt",
            type = "text/plain",
            path = "/path/to/file",
            uploadedBy = uploaderId,
            uploadedTimestamp = Instant.now()
        )
    }

    fun createBlock(id: UUID = UUID.randomUUID(), blockerId: UUID, blockedUserId: UUID): Block {
        return Block(
            id = id,
            blockerId = blockerId,
            blockedUserId = blockedUserId,
            reason = "Test reason",
            timestamp = Instant.now()
        )
    }

    fun createMessageHistory(messageId: UUID): MessageHistory {
        return MessageHistory(
            historyId = UUID.randomUUID(),
            messageId = messageId,
            editedContent = "This is the edited content",
            editTimestamp = Instant.now()
        )
    }
}
