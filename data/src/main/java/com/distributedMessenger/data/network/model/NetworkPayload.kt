package com.distributedMessenger.data.network.model

/**
 * Контейнер для всех сообщений, передаваемых по сети.
 * Содержит зашифрованные данные и мета-информацию.
 */
data class NetworkPayload(
    val senderId: String, // Уникальный ID отправителя
    val encryptedData: String, // Зашифрованные данные (JSON)
    val timestamp: Long = System.currentTimeMillis()
)
