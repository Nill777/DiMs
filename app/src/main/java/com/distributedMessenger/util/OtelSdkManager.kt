package com.distributedMessenger.util

import android.util.Log
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import java.util.concurrent.TimeUnit

object OtelSdkManager {
    private var sdk: OpenTelemetrySdk? = null
    private const val TAG = "OtelSdkManager_DEBUG" // <-- НАШ УНИКАЛЬНЫЙ ТЕГ ДЛЯ ФИЛЬТРАЦИИ

    fun initialize() {
        if (sdk != null) {
            Log.w(TAG, "SDK already initialized. Skipping.")
            return
        }
        Log.i(TAG, "--- Otel SDK Initialization Initiated ---")

        val jaegerEndpoint = "http://10.0.2.2:4317"
        Log.d(TAG, "Exporter endpoint set to: $jaegerEndpoint")

        val spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(jaegerEndpoint)
            .setTimeout(2, TimeUnit.SECONDS)
            .build()

        val spanProcessor = BatchSpanProcessor.builder(spanExporter)
            .setScheduleDelay(1, TimeUnit.SECONDS)
            .build()

        val resource = Resource.getDefault()
            .merge(Resource.create(io.opentelemetry.api.common.Attributes.of(
                ResourceAttributes.SERVICE_NAME, "distributed-messenger-app"
            )))

        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(spanProcessor)
            .setResource(resource)
            .build()

        this.sdk = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal()

        Log.i(TAG, "--- Otel SDK Initialization Finished ---")
    }

    fun getTracer(instrumentationName: String = "com.distributed_messenger"): Tracer {
        return GlobalOpenTelemetry.getTracer(instrumentationName)
    }

    fun shutdown() {
        Log.i(TAG, "--- Otel SDK Shutdown Initiated ---")

        val sdkProvider = sdk?.sdkTracerProvider ?: run {
            Log.w(TAG, "SDK is null, cannot perform shutdown.")
            return
        }

        try {
            // ПРИНУДИТЕЛЬНАЯ ОТПРАВКА ДАННЫХ
            Log.d(TAG, "Attempting to force flush spans...")
            val flushResult = sdkProvider.forceFlush().join(10, TimeUnit.SECONDS) // Увеличим таймаут
            Log.i(TAG, ">>>>> Force flush completed. Result (true=success, false=timeout): $flushResult <<<<<")

            // ШТАТНОЕ ЗАВЕРШЕНИЕ
            Log.d(TAG, "Attempting to shutdown SDK provider...")
            val shutdownResult = sdkProvider.shutdown().join(10, TimeUnit.SECONDS) // Увеличим таймаут
            Log.i(TAG, ">>>>> SDK provider shutdown completed. Result: $shutdownResult <<<<<")

        } catch (e: InterruptedException) {
            Log.w(TAG, "Shutdown process was interrupted", e)
            Thread.currentThread().interrupt()
        } finally {
            sdk = null
            GlobalOpenTelemetry.resetForTest()
            Log.i(TAG, "--- Otel SDK Shutdown Finished ---")
        }
    }
}
