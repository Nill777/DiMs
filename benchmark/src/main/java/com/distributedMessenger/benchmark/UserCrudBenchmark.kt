package com.distributedMessenger.benchmark


import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.distributedMessenger.core.UserRole
import com.distributedMessenger.data.irepositories.IUserRepository
import com.distributedMessenger.data.local.AppDatabase
import com.distributedMessenger.data.repositories.UserRepository
import com.distributedMessenger.domain.iservices.IUserService
import com.distributedMessenger.domain.services.UserService
import com.distributedMessenger.logger.LogLevel
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.util.OtelSdkManager
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class UserCrudBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var userService: IUserService
    private lateinit var userRepository: IUserRepository
    private lateinit var database: AppDatabase



    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Получаем аргументы, переданные из Gradle/CI
        val arguments = InstrumentationRegistry.getArguments()
        val isTracingEnabled = arguments.getString("isTracingEnabled", "false").toBoolean()
        val logLevelString = arguments.getString("logLevel", "DEBUG")
        val logLevel = try {
            LogLevel.valueOf(logLevelString.uppercase())
        } catch (e: IllegalArgumentException) {
            LogLevel.DEBUG
        }
        Logger.LOGGING_LEVEL = logLevel
        val logDir = context.cacheDir.absolutePath
        Logger.initialize(logDir)
        if (isTracingEnabled) {
            OtelSdkManager.initialize()
        }
        database = AppDatabase.getTestDatabase(context)
        userRepository = UserRepository(database.userDao())
        val tracer: Tracer = OtelSdkManager.getTracer("UserService.Benchmark")
        userService = UserService(userRepository, tracer = tracer)
    }

    @After
    fun cleanup() {
        OtelSdkManager.shutdown()
        database.close()
    }

    @Test
    fun benchmark_RegisterUser() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // runWithTimingDisabled - код, который не должен входить в замер
                var username = ""
                runWithTimingDisabled {
                    username = "benchmark-user-${UUID.randomUUID()}"
                }

                userService.register(username, UserRole.USER, "password")
            }
        }
    }

    @Test
    fun benchmark_UpdateUsername() {
        var userId: UUID
        runBlocking {
            userId = userService.register("user-to-update", UserRole.USER, "password")
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                var newUsername = ""
                runWithTimingDisabled {
                    newUsername = "updated-user-${UUID.randomUUID()}"
                }

                userService.updateUser(userId, newUsername)
            }
        }
    }
}