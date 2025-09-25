plugins {
    alias(libs.plugins.android.library)  // Android-библиотека
    alias(libs.plugins.kotlin.android)    // Kotlin для Android (включает JVM-функциональность)
    alias(libs.plugins.allure.framework)
}

android {
    namespace = "com.distributed_messenger.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    testOptions {
        unitTests.all { test ->
            // Указывает Gradle использовать тестовый движок JUnit 5 (Jupiter)
//            test.useJUnitPlatform()
            test.maxParallelForks = 4 // запускать тесты в 4 параллельных процессах на одной jvm
            // Запускать каждый тест-класс в отдельном JVM процессе
            // test.forkEvery = 1L // каждый тест-метод в отдельной jvm (очень медленно)
            // test.forkEvery = 100L // Можно поставить большое число, чтобы все тесты класса шли в одной jvm
        }
    }
    // Это говорит Android Gradle Plugin создать нужные source sets и конфигурации.
    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(project(":logger"))
    implementation(project(":core"))
    implementation(project(":data"))

    // Coroutines для suspend-функций
    implementation(libs.kotlinx.coroutines.core)

    // Unit тесты
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testImplementation(testFixtures(project(":core")))

    // Allure для JUnit 4
    testImplementation(libs.allure.junit4)
}