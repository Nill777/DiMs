plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) // Добавлен плагин KSP для Room
//    id("com.google.gms.google-services")
//    id("io.qameta.allure")
    alias(libs.plugins.google.services)
    alias(libs.plugins.allure.framework)

}

android {
    namespace = "com.distributed_messenger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.distributed_messenger"
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
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.exportSchema", "true")
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

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Firebase
    implementation(platform(libs.firebase.bom))

    // JSON
    implementation(libs.gson)
}
/**
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