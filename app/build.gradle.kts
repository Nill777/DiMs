import java.time.Duration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.allure.framework)
    alias(libs.plugins.detekt)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.distributedMessenger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.distributedMessenger"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunner = "io.qameta.allure.android.runners.AllureAndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.exportSchema" to "true"
                )
            }
        }

        externalNativeBuild {
            cmake {
                // передаем КЛЮЧ для расшифровки "перца" в C++ код во время сборки
                // 1. Берем из переменных окружения (для CI/CD)
                // 2. Или из local.properties (для локальной разработки)
                val aesKey = System.getenv("AES_KEY") ?: localProperties.getProperty("AES_KEY") // 32 байта
                val aesIv = System.getenv("AES_IV") ?: localProperties.getProperty("AES_IV")    // 16 байт
                // Fail Fast
                if (aesKey.isNullOrBlank() || aesIv.isNullOrBlank()) {
                    throw GradleException("""
                        
                        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        !!! CRITICAL: AES_KEY or AES_IV not found.
                        !!!
                        !!! To fix this for local builds, add the following to your
                        !!! root project's 'local.properties' file (and ensure it's in .gitignore):
                        !!!
                        !!! AES_KEY=your_32_byte_aes_key
                        !!! AES_IV=your_16_byte_iv
                        !!!
                        !!! For CI/CD builds, ensure these are set as environment variables.
                        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    """.trimIndent())
                }
                arguments.add("-DAES_KEY=$aesKey")
                arguments.add("-DAES_IV=$aesIv")
//                println("Arguments $arguments")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
//        debug {
//            val testPassword = System.getenv("TEST_USER_PASSWORD") ?: localProperties.getProperty("AES_KEY") ?: "qwertyuiop"
//            buildConfigField("String", "TEST_USER_PASSWORD", "\"$testPassword\"")
//        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.pickFirsts.add("META-INF/DEPENDENCIES")
        resources.pickFirsts.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        resources.excludes.add("win32-x86/attach_hotspot_windows.dll")
        resources.excludes.add("win32-x86-64/attach_hotspot_windows.dll")
        resources.excludes.add("linux-x86/attach_hotspot_linux.so")
        resources.excludes.add("linux-x86-64/attach_hotspot_linux.so")
        resources.excludes.add("META-INF/licenses/ASM")
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }

    // Для тестов с корутинами
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
    // Этот блок применяет конфигурацию ко всем подмодулям проекта
    subprojects {
        // Находим все задачи типа Test (например, testDebugUnitTest)
        tasks.withType<Test>().configureEach {
            if (project.hasProperty("unitTests")) {
                filter {
                    includeTestsMatching("*UnitTest")
                }
            }
            if (project.hasProperty("integrationTests")) {
                filter {
                    includeTestsMatching("*IntegrationTest")
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1" // версия CMake из SDK Manager
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.exportSchema", "true")
}

detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true // Используем наш конфиг поверх стандартного
}

dependencies {
    implementation(project(":logger"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":presenter"))
    implementation(project(":ui"))
    // Базовые зависимости
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.core.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Navigation
     implementation(libs.androidx.navigation.compose)

    // Unit тесты
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlin.test)

    // Android тесты
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.kotlin.test)
    // Allure
    androidTestImplementation(libs.allure.kotlin.android)
    androidTestImplementation(libs.allure.kotlin.junit4)
    androidTestImplementation(testFixtures(project(":core")))

    // Kotest для BDD тестов
    androidTestImplementation(libs.kotest.runner.junit4)
    androidTestImplementation(libs.kotest.assertions.core)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Firebase
    implementation(platform(libs.firebase.bom))

    // JSON
    implementation(libs.gson)
}
/** Исходник НЕ ТРОЖ! Он КРОВЬЮ НАПИСАН!!!
tasks.register("runAndroidTestsWithoutUninstallForGetAllureData") {
    group = "Verification"
    description = "Installs and runs Android tests but does not uninstall the app afterwards."

    // Сначала убедимся, что оба APK установлены
    dependsOn("installDebug", "installDebugAndroidTest")

    doLast {
        // Получаем путь к ADB
        val adb = android.sdkDirectory.resolve("platform-tools/adb")
        // Собираем имя компонента для запуска тестов
        // Формат: com.your.package.test/androidx.test.runner.AndroidJUnitRunner
        val instrumentationRunner = "${android.defaultConfig.applicationId}.test/${android.defaultConfig.testInstrumentationRunner}"

        println("Running instrumentation tests...")
        // Выполняем команду adb для запуска тестов
        exec {
            commandLine(
                adb.absolutePath, "shell", "am", "instrument",
                "-w", // Ждать завершения
                instrumentationRunner
            )
            // Игнорируем код ошибки, так как даже при падении тестов отчеты могут сгенерироваться
            isIgnoreExitValue = true
        }
        println("Instrumentation tests finished.")


        val appId = android.defaultConfig.applicationId
        val outputDir = project.layout.buildDirectory.get().asFile.resolve("allure-results")
        val deviceTempDir = "/data/local/tmp/allure-results"

        println("Pulling Allure results from device to a temporary file...")
        // ПАТАМУШТА что БЛЯТЬ, ПАТАМУШТА НЕЛЬЗЯ ПРОСТО СКОПИРОВАТЬ, ограничение доступа мать его
        println("Copying results to temporary location on device...")
        // run-as выполняет команду от имени приложения
        exec {
            commandLine(
                adb.absolutePath, "shell",
                "run-as", appId,
                "sh", "-c", "'cd files && tar cf - allure-results | tar xvf - -C /data/local/tmp'"
            )
            isIgnoreExitValue = true // tar может выдавать безобидные ошибки о правах(со слов нейронки)
        }

        // А теперь уже на САМ DESCTOP кидаем архив
        println("Pulling results from device...")
        outputDir.deleteRecursively() // Очищаем локальную папку на пк перед выгрузкой
        exec {
            commandLine(adb.absolutePath, "pull", deviceTempDir, outputDir.absolutePath)
        }


        // выпилить папку с выгруженным tar
        println("Cleaning up temporary directory on device...")
        exec {
            commandLine(adb.absolutePath, "shell", "rm", "-rf", deviceTempDir)
        }
        println("Allure results successfully pulled to ${outputDir.path}")
    }
}
 */

tasks.register("runAndroidTestsWithoutUninstallForGetAllureData") {
    group = "Verification"
    description = "Installs and runs Android tests but does not uninstall the app afterwards."

    // Сначала убедимся, что оба APK установлены
    dependsOn("installDebug", "installDebugAndroidTest")

    doLast {
        // Получаем путь к ADB
        val adb = android.sdkDirectory.resolve("platform-tools/adb")
        // Собираем имя компонента для запуска тестов
        // Формат: com.your.package.test/androidx.test.runner.AndroidJUnitRunner
        val instrumentationRunner = "${android.defaultConfig.applicationId}.test/${android.defaultConfig.testInstrumentationRunner}"

        println("Running instrumentation tests...")
        // Выполняем команду adb для запуска тестов
        exec {
            commandLine(
                adb.absolutePath, "shell", "am", "instrument",
                "-w", // Ждать завершения
                instrumentationRunner
            )
            // Игнорируем код ошибки, так как даже при падении тестов отчеты могут сгенерироваться
            isIgnoreExitValue = true
        }
        println("Instrumentation tests finished.")


        val appId = android.defaultConfig.applicationId
        val outputDir = project.layout.buildDirectory.get().asFile.resolve("allure-results")
        val tempTarFile = project.layout.buildDirectory.get().asFile.resolve("tmp/allure-results.tar")

        // ПАТАМУШТА что БЛЯТЬ, ПАТАМУШТА НЕЛЬЗЯ ПРОСТО СКОПИРОВАТЬ, ограничение доступа мать его
        println("Pulling Allure results directly to a local .tar file...")
        tempTarFile.parentFile.mkdirs() // если tmp ещё не существует

        exec {
            // Запускаем шелл на КОМПЬЮТЕРЕ и передаем ему всю команду как одну строку
            commandLine(
                "sh", "-c",
                // Вся команда в кавычках, чтобы шелл обработал ее целиком
                "\"${adb.absolutePath}\" exec-out run-as $appId " +
                        "sh -c 'cd files/allure-results && tar cf - .' > \"${tempTarFile.absolutePath}\""
            )
            isIgnoreExitValue = true // tar может выдавать безобидные ошибки о правах(со слов нейронки)
        }

        if (tempTarFile.exists() && tempTarFile.length() > 0) {
            println("Extracting results from ${tempTarFile.path}")
            outputDir.deleteRecursively()
            outputDir.mkdirs()

            copy {
                from(tarTree(tempTarFile))
                into(outputDir)
            }
            tempTarFile.delete()
            println("Allure results successfully pulled and extracted to ${outputDir.path}")
        } else {
            println("Warning: No Allure results were found on the device.")
        }
    }
}

val avdName = "Pixel_2_API_27"

tasks.register<Exec>("startEmulator") {
    group = "Emulator"
    description = "Starts the Android emulator"

    // Получаем путь к утилите эмулятора из Android SDK
    val emulator = android.sdkDirectory.resolve("emulator/emulator")
    val adb = android.sdkDirectory.resolve("platform-tools/adb")

    doFirst {
        exec {
            commandLine(adb.absolutePath, "kill-server")
            isIgnoreExitValue = true
        }
    }

    println("Starting emulator $avdName...")

//    commandLine(
//        emulator.absolutePath,
//        "-avd", avdName,
//        "-no-snapshot-load", // Гарантирует "холодный", чистый запуск
//        "-no-window",        // Не показывать окно эмулятора
//        "-no-audio",         // Отключить звук
//        "-no-boot-anim"      // Отключить анимацию загрузки для ускорения
//    )

    // Запускаем shell, который, в свою очередь, запускает эмулятор в фоне
    commandLine(
        "sh", "-c",
        // Вся команда передается как одна строка.
        // `&` в конце — это команда шеллу запустить процесс в фоне.
        // `>/dev/null 2>&1` перенаправляет весь вывод в "никуда", чтобы он не засорял лог Gradle.
        "\"${emulator.absolutePath}\" -avd $avdName -no-snapshot-load -no-window -no-audio -no-boot-anim >/dev/null 2>&1 &"
    )
}

tasks.register<Exec>("waitForEmulator") {
    group = "Emulator"
    description = "Waits until the emulator is fully booted and ready"

    dependsOn(tasks.named("startEmulator"))

    val adb = android.sdkDirectory.resolve("platform-tools/adb")

    // Перезапускаем adb сервер с правами root перед ожиданием
    doFirst {
        exec {
            commandLine(adb.absolutePath, "root")
            isIgnoreExitValue = true
            standardOutput = System.out // Показываем вывод для отладки
        }
        exec {
            commandLine(adb.absolutePath, "wait-for-device")
            timeout.set(Duration.ofMinutes(1))
        }
    }

    // Используем 'sh -c' для выполнения цикла ожидания в шелле.
    // Скрипт опрашивает системное свойство 'sys.boot_completed' раз в 2 секунды
    // Как только свойство станет '1', цикл завершится.
    commandLine(
        "sh", "-c",
        "while [[ \"$( \"${adb.absolutePath}\" shell getprop sys.boot_completed | tr -d '\\r' )\" != \"1\" ]] ; do echo 'Waiting for emulator...'; sleep 2; done"
    )

    // Устанавливаем таймаут, чтобы не ждать вечно, если эмулятор не сможет запуститься
    timeout.set(Duration.ofMinutes(5))
}

tasks.register<Exec>("stopEmulator") {
    group = "Emulator"
    description = "Stops the running emulator"

    // Всегда выполняем, даже если тесты упали
//    mustRunAfter(tasks.named("runTestsAndPullReport"))

    val adb = android.sdkDirectory.resolve("platform-tools/adb")

    commandLine(adb.absolutePath, "emu", "kill")
    isIgnoreExitValue = true
}

//tasks.register("runIntegrationTestsOnEmulator") {
//    group = "Verification"
//    description = "Runs ONLY integration tests (com.distributedMessenger.integration) and pulls their results"
//
//    dependsOn(tasks.named("waitForEmulator"))
//    dependsOn("installDebug", "installDebugAndroidTest")
//
//    doLast {
//        val adb = android.sdkDirectory.resolve("platform-tools/adb")
//        val appId = android.defaultConfig.applicationId
//        val instrumentationRunner = "${android.defaultConfig.applicationId}.test/${android.defaultConfig.testInstrumentationRunner}"
//        val outputDir = project.layout.buildDirectory.get().asFile.resolve("allure-results-integration")
//        val tempTarFile = project.layout.buildDirectory.get().asFile.resolve("tmp/allure-results.tar")
//
//        // Запуск ТОЛЬКО интеграционных тестов
//        println("Running integration tests...")
//        exec {
//            commandLine(
//                adb.absolutePath, "shell", "am", "instrument", "-w",
////                "-e", "package", "com.distributedMessenger.integration.repositories", // ФИЛЬТР ПО ПАКЕТУ
//                instrumentationRunner
//            )
//            isIgnoreExitValue = true
//        }
//
//        println("Integration tests finished.")
//
//        // Выгрузка результатов
//        println("Pulling Allure results directly to a local .tar file...")
//        tempTarFile.parentFile.mkdirs() // если tmp ещё не существует
//
//        exec {
//            // Запускаем шелл на КОМПЬЮТЕРЕ и передаем ему всю команду как одну строку
//            commandLine(
//                "sh", "-c",
//                // Вся команда в кавычках, чтобы шелл обработал ее целиком
//                "\"${adb.absolutePath}\" exec-out run-as $appId " +
//                        "sh -c 'cd files/allure-results && tar cf - .' > \"${tempTarFile.absolutePath}\""
//            )
//            isIgnoreExitValue = true // tar может выдавать безобидные ошибки о правах(со слов нейронки)
//        }
//
//        if (tempTarFile.exists() && tempTarFile.length() > 0) {
//            println("Extracting results from ${tempTarFile.path}")
//            outputDir.deleteRecursively()
//            outputDir.mkdirs()
//
//            copy {
//                from(tarTree(tempTarFile))
//                into(outputDir)
//            }
//            tempTarFile.delete()
//            println("Allure results successfully pulled and extracted to ${outputDir.path}")
//        } else {
//            println("Warning: No Allure results were found on the device.")
//        }
//    }
//}

tasks.register("runE2ETestsOnEmulator") {
    group = "Verification"
    description = "Runs ONLY E2E tests (com.distributedMessenger.e2e) and pulls their results"

    dependsOn(tasks.named("waitForEmulator"))
    dependsOn("installDebug", "installDebugAndroidTest")

    doLast {
        val adb = android.sdkDirectory.resolve("platform-tools/adb")
        val appId = android.defaultConfig.applicationId
        val instrumentationRunner = "${android.defaultConfig.applicationId}.test/${android.defaultConfig.testInstrumentationRunner}"
        val outputDir = project.layout.buildDirectory.get().asFile.resolve("allure-results-e2e")
        val tempTarFile = project.layout.buildDirectory.get().asFile.resolve("tmp/allure-results.tar")

        // Запуск ТОЛЬКО E2E тестов
        println("Running E2E tests...")
        exec {
            commandLine(
                adb.absolutePath, "shell", "am", "instrument", "-w",
//                "-e", "package", "com.distributedMessenger.e2e", // ФИЛЬТР ПО ПАКЕТУ
                instrumentationRunner
            )
            isIgnoreExitValue = true
        }
        println("E2E tests finished.")

        // Выгрузка результатов
        println("Pulling Allure results directly to a local .tar file...")
        tempTarFile.parentFile.mkdirs() // если tmp ещё не существует

        exec {
            // Запускаем шелл на КОМПЬЮТЕРЕ и передаем ему всю команду как одну строку
            commandLine(
                "sh", "-c",
                // Вся команда в кавычках, чтобы шелл обработал ее целиком
                "\"${adb.absolutePath}\" exec-out run-as $appId " +
                        "sh -c 'cd files/allure-results && tar cf - .' > \"${tempTarFile.absolutePath}\""
            )
            isIgnoreExitValue = true // tar может выдавать безобидные ошибки о правах(со слов нейронки)
        }

        if (tempTarFile.exists() && tempTarFile.length() > 0) {
            println("Extracting results from ${tempTarFile.path}")
            outputDir.deleteRecursively()
            outputDir.mkdirs()

            copy {
                from(tarTree(tempTarFile))
                into(outputDir)
            }
            tempTarFile.delete()
            println("Allure results successfully pulled and extracted to ${outputDir.path}")
        } else {
            println("Warning: No Allure results were found on the device.")
        }
    }
}

//tasks.register("runIntegrationTestsWithEmulator") {
//    group = "Verification"
//    description = "Starts emulator, runs Integration tests and stops emulator"
//
//    // Определяем строгий порядок выполнения
//    dependsOn(tasks.named("waitForEmulator"))
//    dependsOn(tasks.named("runIntegrationTestsOnEmulator").get().mustRunAfter(tasks.named("waitForEmulator")))
//
//    // В самом конце, независимо от результата, всегда гасим эмулятор
//    finalizedBy(tasks.named("stopEmulator"))
//}

tasks.register("runE2ETestsWithEmulator") {
    group = "Verification"
    description = "Starts emulator, runs E2E tests and stops emulator"

    // Определяем строгий порядок выполнения
    dependsOn(tasks.named("waitForEmulator"))
    dependsOn(tasks.named("runE2ETestsOnEmulator").get().mustRunAfter(tasks.named("waitForEmulator")))

    // В самом конце, независимо от результата, всегда гасим эмулятор
    finalizedBy(tasks.named("stopEmulator"))
}

// отладка Android тестов
tasks.register<Exec>("debugAndroidTests") {
    group = "Verification"
    description = "Runs all instrumented tests and prints detailed logcat output."

    // Запускаем на уже подключенном/запущенном эмуляторе
    dependsOn("installDebug", "installDebugAndroidTest")

    val adb = android.sdkDirectory.resolve("platform-tools/adb")
    val appId = android.defaultConfig.applicationId
    val instrumentationRunner = "${android.defaultConfig.applicationId}.test/${android.defaultConfig.testInstrumentationRunner}"

    // Сначала очищаем logcat, чтобы видеть только ошибки от нашего запуска
    commandLine(adb.absolutePath, "logcat", "-c")
    doLast {
        println("\n--- RUNNING INSTRUMENTATION ---")
        // Запускаем тесты
        exec {
            commandLine(adb.absolutePath, "shell", "am", "instrument", "-w", instrumentationRunner)
            isIgnoreExitValue = true // Продолжаем, даже если тесты упали
        }

        println("\n--- CAPTURING LOGCAT ---")
        // Сразу после падения теста, выводим полный logcat в консоль
        exec {
            commandLine(adb.absolutePath, "logcat", "-d", "*:E") // "-d" - dump, "*:E" - все ошибки
        }
    }
}