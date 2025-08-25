package com.distributed_messenger.data.network.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// ВАЖНО: В реальном проекте ключи должны безопасно обмениваться между участниками
// чата (например, по протоколу Signal - X3DH). Здесь для демонстрации мы будем
// генерировать/получать ключ на основе ID чата, что НЕБЕЗОПАСНО, но показывает механику.
class AesGcmMessageCrypto : INetworkCrypto {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val GCM_IV_LENGTH = 12 // bytes
    private val GCM_TAG_LENGTH = 128 // bits

    override suspend fun encrypt(plainText: String, chatId: UUID): String? {
        return try {
            val secretKey = getOrCreateSecretKey(chatId.toString())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv // Получаем сгенерированный вектор инициализации
            val encryptedBytes = cipher.doFinal(plainText.toByteArray())

            // Объединяем IV и зашифрованные данные, чтобы передать их вместе
            val byteBuffer = ByteBuffer.allocate(iv.size + encryptedBytes.size)
            byteBuffer.put(iv)
            byteBuffer.put(encryptedBytes)
            Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
        } catch (e: Exception) {
            // Логирование ошибки
            null
        }
    }

    override suspend fun decrypt(encryptedText: String, chatId: UUID): String? {
        return try {
            val secretKey = getOrCreateSecretKey(chatId.toString())
            val encryptedBytesWithIv = Base64.decode(encryptedText, Base64.NO_WRAP)

            // Извлекаем IV
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedBytesWithIv, 0, GCM_IV_LENGTH)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

            // Дешифруем только данные, пропуская IV
            val decryptedBytes = cipher.doFinal(encryptedBytesWithIv, GCM_IV_LENGTH, encryptedBytesWithIv.size - GCM_IV_LENGTH)
            String(decryptedBytes)
        } catch (e: Exception) {
            // Логирование ошибки
            null
        }
    }

    private fun getOrCreateSecretKey(alias: String): SecretKey {
        return (keyStore.getKey(alias, null) as? SecretKey) ?: generateSecretKey(alias)
    }

    private fun generateSecretKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
        }.build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }
}