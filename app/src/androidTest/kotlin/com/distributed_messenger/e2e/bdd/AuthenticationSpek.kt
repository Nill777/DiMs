package com.distributed_messenger.e2e.bdd

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.distributed_messenger.BuildConfig
import com.distributed_messenger.core.UserRole
import com.distributed_messenger.data.irepositories.IUserRepository
import com.distributed_messenger.data.local.AppDatabase
import com.distributed_messenger.data.repositories.UserRepository
import com.distributed_messenger.domain.iservices.IUserService
import com.distributed_messenger.domain.models.LoginResult
import com.distributed_messenger.domain.services.EmailService
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
import kotlin.test.DefaultAsserter.assertNotNull

@RunWith(KotestTestRunner::class)
class AuthenticationBehaviorSpec : BehaviorSpec({
    val testPassword = "qwertyuiop"
    val testUsername by lazy { BuildConfig.GMAIL_USERNAME }
    lateinit var database: AppDatabase
    lateinit var userRepository: IUserRepository
    lateinit var userService: IUserService
    lateinit var emailService: EmailService

    beforeTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        userRepository = UserRepository(database.userDao())

        val pepper = CppBridge.getPepper(appContext)
        if (pepper.isEmpty()) throw SecurityException("Kotest Setup failed: Could not get pepper.")

        emailService = EmailService(
            smtpHost = "smtp.yandex.ru",
            smtpPort = "465",
            imapHost = "imap.yandex.ru",
            username = BuildConfig.GMAIL_USERNAME,
            appPassword = BuildConfig.GMAIL_APP_PASSWORD
        )

        userService = UserService(userRepository, emailService, pepper)
    }

    afterTest {
        database.close()
    }

    // Успешный вход
    Given("зарегистрированный пользователь 'testuser'") {
        beforeTest {
            runBlocking { userService.register(testUsername, UserRole.USER, testPassword) }
        }

        When("пользователь вводит правильный логин и пароль") {
            var firstStepResult: LoginResult? = null
            beforeTest {
                runBlocking { firstStepResult = userService.login(testUsername, testPassword) }
            }

            Then("система должна запросить второй фактор") {
                firstStepResult.shouldBeInstanceOf<LoginResult.RequiresTwoFactor>()
            }

            // верный код
            When("пользователь вводит правильный 2FA код") {
                var finalLoginResult: LoginResult? = null
                beforeTest {
                    runBlocking {
                        val codeFromEmail = emailService.findLatestTwoFactorCode()
                        assertNotNull(codeFromEmail, "Did not find 2FA code in email timeout")
                        finalLoginResult = userService.verifyTwoFactor(testUsername, codeFromEmail!!)
                    }
                }

                Then("вход должен быть успешным") {
                    finalLoginResult.shouldBeInstanceOf<LoginResult.Success>()
                }
            }

            // неверный код
            When("пользователь вводит НЕПРАВИЛЬНЫЙ 2FA код") {
                var finalLoginResult: LoginResult? = null
                beforeTest {
                    runBlocking {
                        finalLoginResult = userService.verifyTwoFactor(testUsername, "000000")
                    }
                }

                Then("вход должен провалиться") {
                    finalLoginResult.shouldBeInstanceOf<LoginResult.InvalidTwoFactorCode>()
                }
            }
        }
    }

    // Плановая смена пароля
    Given("зарегистрированный пользователь 'changepass'") {
        var userId: UUID = UUID.randomUUID()
        val oldPassword = "OldPassword"
        val newPassword = "NewPassword"

        beforeTest {
            runBlocking { userId = userService.register(testUsername, UserRole.USER, oldPassword) }
        }

        When("он меняет свой пароль со старого на новый") {
            var changeResult: Boolean = false
            beforeTest {
                runBlocking { changeResult = userService.changePassword(userId, oldPassword, newPassword) }
            }

            Then("операция смены пароля должна быть успешной") {
                changeResult.shouldBeTrue()
            }

            And("он может успешно пройти первый этап входа с новым паролем") {
                val loginWithNew = runBlocking { userService.login(testUsername, newPassword) }
                loginWithNew.shouldBeInstanceOf<LoginResult.RequiresTwoFactor>()
            }

            And("он НЕ может войти со старым паролем") {
                val loginWithOld = runBlocking { userService.login(testUsername, oldPassword) }
                loginWithOld.shouldBeInstanceOf<LoginResult.WrongPassword>()
            }
        }
    }

    // Блокировка аккаунта
    Given("зарегистрированный пользователь 'locker'") {
        beforeTest {
            runBlocking { userService.register(testUsername, UserRole.USER, testPassword) }
        }

        When("пользователь пытается войти с неверным паролем ${UserService.MAX_LOGIN_ATTEMPTS} раза") {
            var finalLoginResult: LoginResult? = null
            beforeTest {
                runBlocking {
                    repeat(UserService.MAX_LOGIN_ATTEMPTS) {
                        finalLoginResult = userService.login(testUsername, "WrongPassword")
                    }
                }
            }

            Then("последняя попытка должна вернуть результат блокировки") {
                finalLoginResult.shouldBeInstanceOf<LoginResult.AccountLocked>()
            }

            And("аккаунт пользователя 'locker' в базе данных должен быть заблокирован") {
                val user = runBlocking { userRepository.findByUsername(testUsername) }
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
                userId = userService.register(testUsername, UserRole.USER, testPassword)
                val user = userService.getUser(userId)!!
                val lockTime = Instant.now().plus(UserService.LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES)
                val lockedUser = user.copy(lockedUntil = lockTime, failedLoginAttempts = UserService.MAX_LOGIN_ATTEMPTS)
                userRepository.updateUser(lockedUser)
            }
        }

        When("аккаунт пользователя разблокирован администратором") {
            var unlockResult: Boolean = false
            beforeTest {
                runBlocking {
                    unlockResult = userService.unlockUser(userId)
                }
            }

            Then("операция разблокировки должна быть успешной") {
                unlockResult.shouldBeTrue()
            }

            And("пользователь может снова успешно пройти первый этап входа") {
                val loginResult = runBlocking { userService.login(testUsername, testPassword) }
                loginResult.shouldBeInstanceOf<LoginResult.RequiresTwoFactor>()
            }
        }
    }
})