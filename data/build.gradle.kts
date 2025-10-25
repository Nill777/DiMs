plugins {
    alias(libs.plugins.android.library)  // Android-библиотека
    alias(libs.plugins.kotlin.android)    // Kotlin для Android (включает JVM-функциональность)
    alias(libs.plugins.ksp)
    alias(libs.plugins.allure.framework)
}

android {
    namespace = "com.distributedMessenger.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
        jvmTarget = "11" // Указываем JVM-таргет здесь
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.exportSchema", "true")
}

dependencies {
    implementation(project(":logger"))
    implementation(project(":core"))

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Unit тесты
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testImplementation(testFixtures(project(":core")))

    // Robolectric
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    // Allure для JUnit 4
    testImplementation(libs.allure.junit4)

    // Android тесты
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core.ktx)

    // Network
//    implementation(libs.webrtc)
    implementation(libs.stream.webrtc)
    implementation(libs.socket.io)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)

    // JSON
    implementation(libs.gson)

    // Crypto
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite)
}