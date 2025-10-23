plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("java-test-fixtures")
    alias(libs.plugins.detekt)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true // Используем наш конфиг поверх стандартного
}