package com.distributedMessenger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.distributedMessenger.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE message_id = :id")
    suspend fun getMessageById(id: UUID): MessageEntity?

    @Query("SELECT * FROM messages")
    suspend fun getAllMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId AND timestamp > :timestamp ORDER BY timestamp ASC")
    suspend fun getMessagesAfter(chatId: UUID, timestamp: Instant): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId")
    suspend fun getMessagesByChatId(chatId: UUID): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp ASC")
    fun getMessagesByChatIdFlow(chatId: UUID): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageByChatId(chatId: UUID): MessageEntity?

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity): Int

    // @Delete
    @Query("DELETE FROM messages WHERE message_id = :id")
    suspend fun deleteMessage(id: UUID): Int
}
