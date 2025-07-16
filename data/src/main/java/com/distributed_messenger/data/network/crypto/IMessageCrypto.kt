package com.distributed_messenger.data.network.crypto

import com.distributed_messenger.core.Message
import java.security.Key

interface IMessageCrypto {
    fun encrypt(message: Message, chatKey: Key): ByteArray
    fun decrypt(data: ByteArray, chatKey: Key): Message
    fun generateChatKey(): Key
}

