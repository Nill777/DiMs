package com.distributedMessenger.data.network.crypto

import java.util.UUID

/**
 * Интерфейс для шифрования и дешифрования сетевых сообщений.
 */
interface INetworkCrypto {
    /**
     * Шифрует текстовое сообщение.
     * @param plainText Исходный текст.
     * @param chatId ID чата, используется для получения ключа шифрования.
     * @return Зашифрованные данные в виде строки (например, Base64).
     */
    suspend fun encrypt(plainText: String, chatId: UUID): String?

    /**
     * Дешифрует текстовое сообщение.
     * @param encryptedText Зашифрованные данные.
     * @param chatId ID чата, используется для получения ключа шифрования.
     * @return Исходный текст.
     */
    suspend fun decrypt(encryptedText: String, chatId: UUID): String?
}
