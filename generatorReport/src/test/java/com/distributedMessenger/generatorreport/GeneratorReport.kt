package com.distributedMessenger.generatorreport

import io.qameta.allure.Allure
import org.junit.Test
import java.io.File

class GeneratorReport {
    private val reportsDir = File("../build/performance-reports") // Путь к артефактам

    @Test
    fun standardLoggingReport() {
        attachArtifactsForConfig("1_standard_log")
    }

    @Test
    fun fullLoggingReport() {
        attachArtifactsForConfig("2_full_log")
    }

    @Test
    fun tracingReport() {
        attachArtifactsForConfig("3_tracing")
    }

    @Test
    fun noLoggingReport() {
        attachArtifactsForConfig("4_no_log")
    }

    private fun attachArtifactsForConfig(configName: String) {
        val configDir = File(reportsDir, configName)
        if (!configDir.exists()) {
            println("Warning: Directory not found for config $configName")
            return
        }
        println("ЕБАААААААТЬ")
        configDir.listFiles()?.forEach { file ->
            val mimeType = when (file.extension) {
                "json" -> "application/json"
                "perfetto-trace" -> "application/octet-stream"
                "txt" -> "text/plain"
                else -> "application/octet-stream"
            }
            Allure.addAttachment(file.name, mimeType, file.inputStream(), ".${file.extension}")
        }
    }
}