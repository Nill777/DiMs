package com.distributedMessenger.data.network.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * TypeAdapter для Gson, который умеет правильно (де)сериализовать
 * полиморфный sealed interface SignalMessage.
 *
 * Он работает так:
 * 1. При сериализации: просто делегирует стандартной логике.
 * 2. При десериализации:
 *    a. Сначала смотрит на поле "type" в JSON.
 *    b. В зависимости от значения "type" ("OFFER", "ANSWER", etc.), он решает,
 *       какой конкретный data-класс нужно создать.
 *    c. Делегирует десериализацию этому конкретному классу.
 */
class SignalMessageTypeAdapter : JsonSerializer<SignalMessage>, JsonDeserializer<SignalMessage> {

    override fun serialize(
        src: SignalMessage?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        // Для сериализации используем стандартный механизм, он работает хорошо.
        return context!!.serialize(src, src!!::class.java)
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): SignalMessage {
        val jsonObject = json?.asJsonObject ?: throw JsonParseException("Not a JSON object")

        // 1. Читаем поле "type".
        val type = jsonObject.get("type")?.asString

        // 2. В зависимости от типа, выбираем конкретный класс для десериализации.
        val clazz = when (type) {
            "OFFER" -> SignalMessage.Offer::class.java
            "ANSWER" -> SignalMessage.Answer::class.java
            "ICE_CANDIDATES" -> SignalMessage.IceCandidates::class.java
            else -> throw JsonParseException("Unknown signal type: $type")
        }

        // 3. Говорим Gson продолжить десериализацию, но уже для конкретного класса.
        return context!!.deserialize(jsonObject, clazz)
    }
}
