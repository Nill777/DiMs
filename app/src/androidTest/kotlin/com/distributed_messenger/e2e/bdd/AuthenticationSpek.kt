package com.distributed_messenger.e2e.bdd

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.distributed_messenger.core.UserRole
import com.distributed_messenger.data.irepositories.IUserRepository
import com.distributed_messenger.data.local.AppDatabase
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.domain.iservices.IUserService
import com.distributed_messenger.domain.models.LoginResult
import com.distributed_messenger.domain.services.UserService
import com.distributed_messenger.util.CppBridge
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.runner.junit4.KotestTestRunner
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@RunWith(KotestTestRunner::class)
class AuthenticationBehaviorSpec : BehaviorSpec({
    val testPassword = "qwertyuiop"
    lateinit var database: AppDatabase
    lateinit var userRepository: IUserRepository
    lateinit var userService: IUserService

    beforeTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        userRepository = UserRepository(database.userDao())

        val pepper = CppBridge.getPepper(appContext)
        if (pepper.isEmpty()) throw SecurityException("Kotest Setup failed: Could not get pepper.")

        userService = UserService(userRepository, pepper)
    }

    afterTest {
        database.close()
    }

    // Успешный вход
    Given("зарегистрированный пользователь 'testuser'") {
        beforeTest {
            runBlocking { userService.register("testuser", UserRole.USER, testPassword) }
        }

        When("пользователь пытается войти с правильным паролем из секрета") {
            val loginResult = runBlocking { userService.login("testuser", testPassword) }

            Then("вход должен быть успешным") {
                loginResult.shouldBeInstanceOf<LoginResult.Success>()
            }
        }
    }

    // Плановая смена пароля
    Given("зарегистрированный пользователь 'changepass'") {
        var userId: UUID = UUID.randomUUID()
        val oldPassword = "OldPassword"
        val newPassword = "NewPassword"

        beforeTest {
            runBlocking { userId = userService.register("changepass", UserRole.USER, oldPassword) }
        }

        When("он меняет свой пароль со старого на новый") {
            var changeResult: Boolean = false
            beforeTest {
                runBlocking { changeResult = userService.changePassword(userId, oldPassword, newPassword) }
            }

            Then("операция смены пароля должна быть успешной") {
                changeResult.shouldBeTrue()
            }

            And("он может успешно войти с новым паролем") {
                val loginWithNew = runBlocking { userService.login("changepass", newPassword) }
                loginWithNew.shouldBeInstanceOf<LoginResult.Success>()
            }

            And("он НЕ может войти со старым паролем") {
                val loginWithOld = runBlocking { userService.login("changepass", oldPassword) }
                loginWithOld.shouldBeInstanceOf<LoginResult.WrongPassword>()
            }
        }
    }

    // Блокировка аккаунта
    Given("зарегистрированный пользователь 'locker'") {
        beforeTest {
            runBlocking { userService.register("locker", UserRole.USER, testPassword) }
        }

        When("пользователь пытается войти с неверным паролем 3 раза") {
            var finalLoginResult: LoginResult? = null
            beforeTest {
                runBlocking {
                    repeat(UserService.MAX_LOGIN_ATTEMPTS) {
                        finalLoginResult = userService.login("locker", "WrongPassword")
                    }
                }
            }

            Then("последняя попытка должна вернуть результат блокировки") {
                finalLoginResult.shouldBeInstanceOf<LoginResult.AccountLocked>()
            }

            And("аккаунт пользователя 'locker' в базе данных должен быть заблокирован") {
                val user = runBlocking { userRepository.findByUsername("locker") }
                user.shouldNotBeNull()
                user.lockedUntil.shouldNotBeNull()
            }
        }
    }

    // Восстановление аккаунта
    Given("заблокированный пользователь 'lockeduser'") {
        var userId: UUID = UUID.randomUUID()
        beforeTest {
            runBlocking {
                userId = userService.register("lockeduser", UserRole.USER, testPassword)
                val user = userService.getUser(userId)!!
                val lockTime = Instant.now().plus(UserService.LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES)
                val lockedUser = user.copy(lockedUntil = lockTime, failedLoginAttempts = UserService.MAX_LOGIN_ATTEMPTS)
                userRepository.updateUser(lockedUser)
            }
        }

        When("аккаунт пользователя разблокирован администратором") {
            var unlockResult: Boolean = false
            var loginResult: LoginResult? = null
            beforeTest {
                runBlocking {
                    unlockResult = userService.unlockUser(userId)
                    loginResult = userService.login("lockeduser", testPassword)
                }
            }

            Then("операция разблокировки должна быть успешной") {
                unlockResult.shouldBeTrue()
            }

            And("пользователь может снова успешно войти") {
                loginResult.shouldBeInstanceOf<LoginResult.Success>()
            }
        }
    }
})