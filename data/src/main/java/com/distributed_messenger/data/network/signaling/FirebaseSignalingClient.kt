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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class FirebaseSignalingClient(private val gson: Gson) : ISignalingClient {

    private val database = Firebase.database
    private val tag = "FirebaseSignalingClient"

    override fun joinRoom(chatId: UUID, myId: PeerId): Flow<Pair<PeerId, SignalMessage>> = callbackFlow {
        Logger.log(tag, "joinRoom '$chatId' as '$myId'")
        val roomRef = database.getReference("rooms").child(chatId.toString())
        val myRef = roomRef.child(myId)

        // Хук на удаление наших данных при отключении
        myRef.onDisconnect().removeValue()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Logger.log(tag, "onDataChange triggered for room '$chatId'. Found ${snapshot.childrenCount} peers.", LogLevel.DEBUG)
                snapshot.children.forEach { peerSnapshot ->
                    val peerId = peerSnapshot.key
                    // Мы реагируем на данные всех, кроме себя
                    if (peerId != null && peerId != myId) {
                        val signalData = peerSnapshot.getValue(String::class.java)
                        if (signalData != null) {
                            try {
                            // Десериализуем сообщение и отправляем в Flow
                                val signalMessage = gson.fromJson(signalData, SignalMessage::class.java)
                                Logger.log(tag, "Parsed signal '${signalMessage::class.simpleName}' from peer '$peerId'")
                                trySend(Pair(peerId, signalMessage))
                            } catch (e: Exception) {
                                Logger.log(tag, "Failed to parse signal from peer '$peerId'. Data: $signalData", LogLevel.ERROR, e)
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Logger.log(tag, "onCancelled Listener for room '$chatId' was cancelled. Error: ${error.message}", LogLevel.ERROR, error.toException())
                close(error.toException())
            }
        }

        roomRef.addValueEventListener(listener)
        Logger.log(tag, "ValueEventListener added for room '$chatId'")

        awaitClose {
            Logger.log(tag, "Removing listener from room '$chatId'")
            roomRef.removeEventListener(listener)
        }
    }

    override fun sendSignal(chatId: UUID, myId: PeerId, signalMessage: SignalMessage) {
        val signalType = signalMessage::class.simpleName
        Logger.log(tag, "sendSignal Sending/updating my signal '$signalType' in room '$chatId'")

        val roomRef = database.getReference("rooms").child(chatId.toString())
        // Мы всегда пишем данные ТОЛЬКО В СВОЙ УЗЕЛ.
        val myRef = roomRef.child(myId)
        val signalJson = gson.toJson(signalMessage)

        Logger.log(tag, "Writing to path: ${myRef.toString()}", LogLevel.DEBUG)
        myRef.setValue(signalJson)
    }
}