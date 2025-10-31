#!/bin/bash
set -e

# Исходники
BENCHMARK_OUTPUT_DIR="benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/"
ANDROID_TEST_OUTPUT_DIR="benchmark/build/outputs/androidTest-results/connected/benchmark/"
# Артефакты
PERFORMANCE_REPORTS_DIR="build/performance-reports"

echo "--- Очистка старых артефактов Benchmark ---"
rm -rf $PERFORMANCE_REPORTS_DIR
mkdir -p $PERFORMANCE_REPORTS_DIR

run_benchmark() {
  local config_name=$1
  local log_level=$2
  local tracing_enabled=$3
  local ARTIFACT_SUBDIR="$PERFORMANCE_REPORTS_DIR/$config_name"
  mkdir -p "$ARTIFACT_SUBDIR"

  echo "--- Прогонка Benchmark Config: $config_name ---"
  ./gradlew :benchmark:clean
  ./gradlew :benchmark:connectedCheck \
    -Pandroid.testInstrumentationRunnerArguments.logLevel="$log_level" \
    -Pandroid.testInstrumentationRunnerArguments.isTracingEnabled="$tracing_enabled" \
    --continue || echo "Benchmark run for $config_name failed but continuing..."

  # трассировка
  find "$BENCHMARK_OUTPUT_DIR"*/*.json -exec cp {} "$ARTIFACT_SUBDIR/" \; || echo "No JSON reports found for $config_name"
  # полный дамп мониторинга
  find "$BENCHMARK_OUTPUT_DIR"*/*.perfetto-trace -exec cp {} "$ARTIFACT_SUBDIR/" \; || echo "No Perfetto traces found for $config_name"
  # логи
  find "$ANDROID_TEST_OUTPUT_DIR"*/logcat-*.txt -exec cp {} "$ARTIFACT_SUBDIR/" \; || echo "No Logcat files found for $config_name"
}

run_benchmark "1_standard_log" "ERROR"   "false"
run_benchmark "2_full_log"     "DEBUG"   "false"
run_benchmark "3_tracing"      "NOTHING" "true"
run_benchmark "4_no_log"       "NOTHING" "false"

echo "--- Все результаты Benchmark в $PERFORMANCE_REPORTS_DIR ---"
