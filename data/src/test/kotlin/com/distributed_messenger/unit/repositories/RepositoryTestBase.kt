package com.distributed_messenger.unit.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distributed_messenger.data.local.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * чтобы не дублировать код инициализации базы данных в каждом тестовом файле
 */
@RunWith(RobolectricTestRunner::class)
abstract class RepositoryTestBase {
    protected lateinit var database: AppDatabase

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // Позволяет выполнять запросы в основном потоке, но только для тестов.
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }
}