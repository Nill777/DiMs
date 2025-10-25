package com.distributedMessenger.data.network.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class DataMessageTypeAdapter : JsonSerializer<DataMessage>, JsonDeserializer<DataMessage> {

    override fun serialize(
        src: DataMessage?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        // Стандартный механизм сериализации работает отлично
        return context!!.serialize(src, src!!::class.java)
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): DataMessage {
        val jsonObject = json?.asJsonObject ?: throw JsonParseException("Not a JSON object")

        // 1. Смотрим на поле "type", чтобы понять, какой это тип сообщения.
        val type = jsonObject.get("type")?.asString

        // 2. Выбираем конкретный класс для десериализации.
        val clazz = when (type) {
            "HANDSHAKE" -> DataMessage.Handshake::class.java
            "CHAT_MESSAGE" -> DataMessage.ChatMessage::class.java
            "SYNC_REQUEST" -> DataMessage.SyncRequest::class.java
            "SYNC_RESPONSE" -> DataMessage.SyncResponse::class.java
            else -> throw JsonParseException("Unknown DataMessage type: $type")
        }

        // 3. Делегируем десериализацию Gson, но уже с конкретным классом.
        return context!!.deserialize(jsonObject, clazz)
    }
}
