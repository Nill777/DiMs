plugins {
    alias(libs.plugins.android.library)  // Android-библиотека
    alias(libs.plugins.kotlin.android)    // Kotlin для Android (включает JVM-функциональность)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.distributed_messenger.logger"
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
}

detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true // Используем наш конфиг поверх стандартного
}

dependencies {
    // Logging
    implementation(libs.timber)
    implementation(libs.kotlin.reflect)
}