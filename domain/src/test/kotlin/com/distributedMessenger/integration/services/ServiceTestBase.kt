package com.distributedMessenger.integration.services

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distributedMessenger.data.local.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Базовый класс для всех интеграционных тестов, требующих Android-среды
 * и доступа к in-memory базе данных.
 *
 * 1. Запускает тесты с помощью Robolectric.
 * 2. Создает чистую in-memory базу данных перед каждым тестом (@Before).
 * 3. Закрывает соединение с базой данных после каждого теста (@After).
 */
@RunWith(RobolectricTestRunner::class)
abstract class ServiceTestBase {
    protected lateinit var database: AppDatabase

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }
}
