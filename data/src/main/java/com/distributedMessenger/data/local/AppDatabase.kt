package com.distributedMessenger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.distributedMessenger.data.local.dao.AppSettingsDao
import com.distributedMessenger.data.local.dao.BlockDao
import com.distributedMessenger.data.local.dao.ChatDao
import com.distributedMessenger.data.local.dao.FileDao
import com.distributedMessenger.data.local.dao.MessageDao
import com.distributedMessenger.data.local.dao.MessageHistoryDao
import com.distributedMessenger.data.local.dao.UserDao
import com.distributedMessenger.data.local.entities.AppSettingsEntity
import com.distributedMessenger.data.local.entities.BlockEntity
import com.distributedMessenger.data.local.entities.ChatEntity
import com.distributedMessenger.data.local.entities.FileEntity
import com.distributedMessenger.data.local.entities.MessageEntity
import com.distributedMessenger.data.local.entities.MessageHistoryEntity
import com.distributedMessenger.data.local.entities.UserEntity

@Database(
    entities = [
        MessageEntity::class,
        MessageHistoryEntity::class,
        ChatEntity::class,
        UserEntity::class,
        FileEntity::class,
        BlockEntity::class,
        AppSettingsEntity::class
    ],
    version = 4
)
@TypeConverters(Converters::class)

abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun messageHistoryDao(): MessageHistoryDao
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao
    abstract fun fileDao(): FileDao
    abstract fun blockDao(): BlockDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        fun getTestDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        }
    }
}
