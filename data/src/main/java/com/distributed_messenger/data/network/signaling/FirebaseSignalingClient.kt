package com.distributed_messenger.data.network.signaling

import com.distributed_messenger.data.network.PeerId
import com.distributed_messenger.data.network.model.SignalMessage
import com.distributed_messenger.logger.LogLevel
import com.distributed_messenger.logger.Logger
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class FirebaseSignalingClient(private val gson: Gson) : ISignalingClient {

    private val database = Firebase.database
    private val tag = "FirebaseSignalingClient"

    override fun joinRoom(chatId: UUID, myId: PeerId): Flow<Map<PeerId, String?>> = callbackFlow {
        Logger.log(tag, "joinRoom '$chatId' as '$myId'")
        val roomRef = database.getReference("rooms").child(chatId.toString())
        val myRef = roomRef.child(myId)

        // Хук на удаление наших данных при отключении
        myRef.onDisconnect().removeValue()
        // --- КЛЮЧЕВОЕ ИЗМЕНЕНИЕ ---
        // Немедленно объявляем о своем присутствии, записывая простое значение.
        // Это действие создаст узел rooms/{chatId}/{myId} в Firebase,
        // и все остальные слушатели комнаты получат уведомление onDataChange.
//        myRef.setValue("presence")
//            .addOnSuccessListener {
//                Logger.log(tag, "Successfully announced presence in room '$chatId'", LogLevel.DEBUG)
//            }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Logger.log(tag, "onDataChange triggered for room '$chatId'. Found ${snapshot.childrenCount} peers.", LogLevel.DEBUG)
                // Собираем всех пиров в комнате
                val peersMap = snapshot.children.mapNotNull { peerSnapshot ->
                    val peerId = peerSnapshot.key
                    if (peerId.isNullOrEmpty() || peerId == myId) {
                        null
                    } else {
                        // Firebase без проблем прочитает "presence" или JSON как String.
                        val signalData = peerSnapshot.getValue(String::class.java)
                        peerId to signalData
                    }
                }.toMap() // Убираем себя и пустые ключи

                Logger.log(tag, "onDataChange Room state updated. Peers found: ${peersMap.keys}", LogLevel.DEBUG)
                trySend(peersMap) // Отправляем полную карту состояния комнаты
            }
            override fun onCancelled(error: DatabaseError) {
                Logger.log(tag, "onCancelled Listener for room '$chatId' was cancelled. Error: ${error.message}", LogLevel.ERROR, error.toException())
                close(error.toException())
            }
        }
        // операция **ЧТЕНИЯ**. Вы говорите Firebase: "Начни, пожалуйста, слушать этот адрес (`rooms/{inviteId}`).
        // Если там что-то появится или изменится, сообщи мне". Слушатель не создает данные, он только на них реагирует.
        roomRef.addValueEventListener(listener)
        Logger.log(tag, "ValueEventListener added for room '$chatId'")

        // Вы говорите серверу Firebase: "Вот тебе инструкция. Если я вдруг отключусь нештатно (пропадет интернет, закроется приложение),
        // **тогда** выполни эту инструкцию и удали мой узел". Сама по себе эта команда не создает никаких данных в реальном времени.
        awaitClose {
            Logger.log(tag, "Removing listener from room '$chatId'")
            roomRef.removeEventListener(listener)
        }
    }

    // первая операция записи — это вызов `myRef.setValue(signalJson)` внутри функции `sendSignal`.
    override fun sendSignal(chatId: UUID, myId: PeerId, signalMessage: SignalMessage) {
        val signalType = signalMessage.type
        Logger.log(tag, "sendSignal Sending/updating my signal '$signalType' in room '$chatId'")

        val roomRef = database.getReference("rooms").child(chatId.toString())
        // Мы всегда пишем данные ТОЛЬКО В СВОЙ УЗЕЛ.
        val myRef = roomRef.child(myId)
        val signalJson = gson.toJson(signalMessage)

        Logger.log(tag, "Writing to path: ${myRef.toString()}", LogLevel.DEBUG)
//        myRef.setValue(signalJson)
        // Просто записываем данные в свой узел.
        // Другие участники, подписанные на `roomRef`, увидят это изменение.
        myRef.setValue(signalJson)
            .addOnSuccessListener {
                Logger.log(tag, "sendSignal: Successfully wrote to path: ${myRef.toString()}", LogLevel.DEBUG)
            }
            .addOnFailureListener { e ->
                Logger.log(tag, "sendSignal: FAILED to write to path: ${myRef.toString()}", LogLevel.ERROR, e)
            }
    }
}