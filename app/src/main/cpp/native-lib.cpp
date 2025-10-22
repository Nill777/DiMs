#include <jni.h>
#include <string>
#include <unistd.h>
#include <fstream>
#include <vector>
#include <android/log.h>
extern "C" {
    #include "aes.h"
}
#define LOG_TAG "NDK_PEPPER_DEBUG"
// --- КЛЮЧИ И ДАННЫЕ, ВНЕДРЯЕМЫЕ НА ЭТАПЕ СБОРКИ ---
// через gradlew прокидываем из cpp
#ifndef AES_KEY
#define AES_KEY "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"  // 32-байтовая заглушка
#endif
#ifndef AES_IV
#define AES_IV "0123456789abcdef0123456789abcdef"                                   // 16-байтовая заглушка
#endif

std::vector<uint8_t> hex_to_bytes(const std::string& hex) {
    if (hex.length() % 2 != 0) {
        throw std::invalid_argument("Hex string must have an even number of characters");
    }
    std::vector<uint8_t> bytes;
    for (unsigned int i = 0; i < hex.length(); i += 2) {
        std::string byteString = hex.substr(i, 2);
        uint8_t byte = (uint8_t) strtol(byteString.c_str(), NULL, 16);
        bytes.push_back(byte);
    }
    return bytes;
}

// Проверка №1: Ищем бинарник 'su', который является признаком root-доступа
bool isRooted() {
    // Список стандартных путей, где может лежать 'su'
    const char* paths[] = {
        "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
        "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
        "/su/bin/su"
    };
    for (const char* path : paths) {
        if (access(path, F_OK) == 0) {
            // Файл найден, скорее всего, устройство рутовано
            return true;
        }
    }
    return false;
}

// Проверка №2: Проверяем, не подключен ли к нам отладчик
bool isDebuggerAttached() {
    std::ifstream status_file("/proc/self/status");
    if (status_file.is_open()) {
        std::string line;
        while (std::getline(status_file, line)) {
            if (line.rfind("TracerPid:", 0) == 0) {
                // Если TracerPid не 0, значит, нас кто-то отлаживает
                int tracer_pid = std::stoi(line.substr(10));
                if (tracer_pid != 0) {
                    return true;
                }
                break;
            }
        }
        status_file.close();
    }
    return false;
}

// Функция для расшифровки "перца"
std::string getDecryptedPepper() {
    // TODO перед релизом вставить сюда массив, сгенерированный  Kotlin-скриптом
    unsigned char encrypted_pepper[] = {
        0x24, 0x6a, 0xc0, 0xcf, 0x9f, 0xe6, 0x02, 0x8f, 0x2d, 0x00, 0xef, 0x71, 0x93, 0x72, 0x38, 0x62, 0x24, 0xf6, 0xd1, 0x71, 0xc2, 0x04, 0x7a, 0x1a, 0x64, 0x5d, 0x81, 0x18, 0x4f, 0x29, 0xbc, 0x02, 0x7a, 0x26, 0x82, 0xf4, 0x74, 0x28, 0x80, 0xd8, 0x56, 0x21, 0x48, 0x0b, 0x57, 0x8c, 0x45, 0x74, 0xff, 0xa7, 0x21, 0x80, 0x02, 0x45, 0x6f, 0xf0, 0xf1, 0xa7, 0x93, 0x51, 0xa8, 0x10, 0x02, 0x6d
    };
    unsigned int encrypted_pepper_len = sizeof(encrypted_pepper);

    // Получаем ключ и IV из макросов, установленных Gradle
    std::vector<uint8_t> key_bytes = hex_to_bytes(AES_KEY);
    std::vector<uint8_t> iv_bytes = hex_to_bytes(AES_IV);

    // __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "AES_KEY received by C++: [%s]", AES_KEY);
    // __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "AES_IV received by C++:  [%s]", AES_IV);
    
    // Проверка правильности длины ключей
    if (key_bytes.size() != 32 || iv_bytes.size() != 16) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "C++: FATAL: Invalid key or IV length after hex conversion");
        return "";
    }

    std::vector<uint8_t> buffer(encrypted_pepper, encrypted_pepper + sizeof(encrypted_pepper));

    // Расшифровываем
    struct AES_ctx ctx;
    AES_init_ctx_iv(&ctx, key_bytes.data(), iv_bytes.data());
    AES_CBC_decrypt_buffer(&ctx, buffer.data(), buffer.size());
    
    // tiny-AES-c выполняет расшифровку "in-place", поэтому результат в том же буфере
    // Убираем PKCS7 паддинг
    uint8_t padding = buffer.back();
    buffer.resize(buffer.size() - padding);

    return std::string(buffer.begin(), buffer.end());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_distributed_1messenger_util_CppBridge_getPepperFromNdk(
        JNIEnv* env,
        jobject /* this */,
        jstring appSignatureHash) {

    // --- ЗАПУСК ПРОВЕРОК ---
    if (isRooted() || isDebuggerAttached()) {
        return env->NewStringUTF(""); // Провал
    }

    // Проверка подписи APK
     // Наша подпись
    const char* knownGoodSignatureHash = "a3e95b6002e2f1a711642b241464a2b79c9c55b81d9a36a2ae3f07c925aad8af";
    const char* providedSignatureHash = env->GetStringUTFChars(appSignatureHash, 0);
    bool isSignatureValid = (strcmp(knownGoodSignatureHash, providedSignatureHash) == 0);
    env->ReleaseStringUTFChars(appSignatureHash, providedSignatureHash);

    if (!isSignatureValid) {
        return env->NewStringUTF(""); // Провал
    }

    // Все проверки пройдены, возвращаем расшифрованный "перец"
    std::string pepper = getDecryptedPepper();
    return env->NewStringUTF(pepper.c_str());
}