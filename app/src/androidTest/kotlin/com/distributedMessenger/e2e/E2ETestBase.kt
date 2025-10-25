package com.distributedMessenger.e2e

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.distributedMessenger.data.local.AppDatabase
import io.qameta.allure.android.runners.AllureAndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Базовый класс для всех интеграционных тестов, требующих Android-среды
 * и доступа к in-memory базе данных.
 *
 * 1. Запускает тесты с помощью Robolectric.
 * 2. Создает чистую in-memory базу данных перед каждым тестом (@Before).
 * 3. Закрывает соединение с базой данных после каждого теста (@After).
 */
@RunWith(AllureAndroidJUnit4::class)
abstract class E2ETestBase {
    protected lateinit var database: AppDatabase

    @Before
    fun setupDatabase() {
//        val context = InstrumentationRegistry.getInstrumentation().targetContext
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