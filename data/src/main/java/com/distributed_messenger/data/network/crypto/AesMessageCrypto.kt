package com.distributed_messenger.data.network.crypto

import com.distributed_messenger.core.Message
import java.security.Key
import java.time.Instant
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AesMessageCrypto : IMessageCrypto {
    override fun encrypt(message: Message, chatKey: Key): ByteArray {
        // Простая заглушка вместо шифрования
        return "${message.id}|${message.senderId}|${message.chatId}|${message.content}|${message.fileId}|${message.timestamp.epochSecond}"
            .toByteArray()
    }

    override fun decrypt(data: ByteArray, chatKey: Key): Message {
        // Простая заглушка вместо дешифрования
        val parts = String(data).split("|")
        return Message(
            id = UUID.fromString(parts[0]),
            senderId = UUID.fromString(parts[1]),
            chatId = UUID.fromString(parts[2]),
            content = parts[3],
            fileId = if (parts[4] == "null") null else UUID.fromString(parts[4]),
            timestamp = Instant.ofEpochSecond(parts[5].toLong())
        )
    }

    override fun generateChatKey(): SecretKey {
        return KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()
    }
}